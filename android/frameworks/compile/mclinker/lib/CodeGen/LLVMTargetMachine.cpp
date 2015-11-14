//===- LLVMTargetMachine.cpp ----------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//

#include <mcld/CodeGen/SectLinker.h>
#include <mcld/CodeGen/SectLinkerOption.h>
#include <mcld/MC/MCLDFile.h>
#include <mcld/Support/RealPath.h>
#include <mcld/Support/TargetRegistry.h>
#include <mcld/Target/TargetMachine.h>
#include <mcld/Target/TargetLDBackend.h>

#include <llvm/ADT/OwningPtr.h>
#include <llvm/Analysis/Passes.h>
#include <llvm/Analysis/Verifier.h>
#include <llvm/Assembly/PrintModulePass.h>
#include <llvm/CodeGen/AsmPrinter.h>
#include <llvm/CodeGen/MachineFunctionAnalysis.h>
#include <llvm/CodeGen/MachineModuleInfo.h>
#include <llvm/CodeGen/GCStrategy.h>
#include <llvm/CodeGen/Passes.h>
#include <llvm/MC/MCAsmInfo.h>
#include <llvm/MC/MCStreamer.h>
#include <llvm/MC/MCInstrInfo.h>
#include <llvm/MC/MCSubtargetInfo.h>
#include <llvm/MC/MCObjectStreamer.h>
#include <llvm/MC/MCAssembler.h>
#include <llvm/MC/MCObjectWriter.h>
#include <llvm/MC/MCContext.h>
#include <llvm/PassManager.h>
#include <llvm/Support/CommandLine.h>
#include <llvm/Support/Debug.h>
#include <llvm/Support/TargetRegistry.h>
#include <llvm/Support/FormattedStream.h>
#include <llvm/Support/ToolOutputFile.h>
#include <llvm/Target/TargetData.h>
#include <llvm/Target/TargetInstrInfo.h>
#include <llvm/Target/TargetLowering.h>
#include <llvm/Target/TargetOptions.h>
#include <llvm/Target/TargetSubtargetInfo.h>
#include <llvm/Target/TargetLoweringObjectFile.h>
#include <llvm/Target/TargetRegisterInfo.h>
#include <llvm/Transforms/Scalar.h>

#include <string>

using namespace mcld;
using namespace llvm;

//===----------------------------------------------------------------------===//
/// Arguments
//===----------------------------------------------------------------------===//
// Enable or disable FastISel. Both options are needed, because
// FastISel is enabled by default with -fast, and we wish to be
// able to enable or disable fast-isel independently from -O0.

static cl::opt<cl::boolOrDefault>
ArgEnableFastISelOption("lfast-isel", cl::Hidden,
  cl::desc("Enable the \"fast\" instruction selector"));

static cl::opt<bool>
ArgShowMCEncoding("lshow-mc-encoding",
                cl::Hidden,
                cl::desc("Show encoding in .s output"));

static cl::opt<bool>
ArgShowMCInst("lshow-mc-inst",
              cl::Hidden,
              cl::desc("Show instruction structure in .s output"));

static cl::opt<cl::boolOrDefault>
ArgAsmVerbose("fverbose-asm",
              cl::desc("Put extra commentary information in the \
                       generated assembly code to make it more readable."),
              cl::init(cl::BOU_UNSET));

static bool getVerboseAsm() {
  switch (ArgAsmVerbose) {
  default:
  case cl::BOU_UNSET: return TargetMachine::getAsmVerbosityDefault();
  case cl::BOU_TRUE:  return true;
  case cl::BOU_FALSE: return false;
  }
}


//===---------------------------------------------------------------------===//
/// LLVMTargetMachine
//===----------------------------------------------------------------------===//
mcld::LLVMTargetMachine::LLVMTargetMachine(llvm::TargetMachine &pTM,
                                           const mcld::Target& pTarget,
                                           const std::string& pTriple )
  : m_TM(pTM), m_pTarget(&pTarget), m_Triple(pTriple) {
}

mcld::LLVMTargetMachine::~LLVMTargetMachine() {
  m_pTarget = 0;
}

const mcld::Target& mcld::LLVMTargetMachine::getTarget() const
{
  return *m_pTarget;
}

/// Turn exception handling constructs into something the code generators can
/// handle.
static void addPassesToHandleExceptions(llvm::TargetMachine *TM,
                                        PassManagerBase &PM) {
  switch (TM->getMCAsmInfo()->getExceptionHandlingType()) {
  case llvm::ExceptionHandling::SjLj:
    // SjLj piggy-backs on dwarf for this bit. The cleanups done apply to both
    // Dwarf EH prepare needs to be run after SjLj prepare. Otherwise,
    // catch info can get misplaced when a selector ends up more than one block
    // removed from the parent invoke(s). This could happen when a landing
    // pad is shared by multiple invokes and is also a target of a normal
    // edge from elsewhere.
    PM.add(createSjLjEHPreparePass(TM->getTargetLowering()));
    // FALLTHROUGH
  case llvm::ExceptionHandling::DwarfCFI:
  case llvm::ExceptionHandling::ARM:
  case llvm::ExceptionHandling::Win64:
    PM.add(createDwarfEHPass(TM));
    break;
  case llvm::ExceptionHandling::None:
    PM.add(createLowerInvokePass(TM->getTargetLowering()));

    // The lower invoke pass may create unreachable code. Remove it.
    PM.add(createUnreachableBlockEliminationPass());
    break;
  }
}


static llvm::MCContext *addPassesToGenerateCode(llvm::LLVMTargetMachine *TM,
                                     PassManagerBase &PM,
                                     bool DisableVerify)
{
  // Targets may override createPassConfig to provide a target-specific sublass.
  TargetPassConfig *PassConfig = TM->createPassConfig(PM);

  // Set PassConfig options provided by TargetMachine.
  PassConfig->setDisableVerify(DisableVerify);

  PM.add(PassConfig);

  PassConfig->addIRPasses();

  addPassesToHandleExceptions(TM, PM);

  PassConfig->addISelPrepare();

  // Install a MachineModuleInfo class, which is an immutable pass that holds
  // all the per-module stuff we're generating, including MCContext.
  MachineModuleInfo *MMI =
    new MachineModuleInfo(*TM->getMCAsmInfo(), *TM->getRegisterInfo(),
                          &TM->getTargetLowering()->getObjFileLowering());
  PM.add(MMI);
  MCContext *Context = &MMI->getContext(); // Return the MCContext by-ref.

  // Set up a MachineFunction for the rest of CodeGen to work on.
  PM.add(new MachineFunctionAnalysis(*TM));

  // Enable FastISel with -fast, but allow that to be overridden.
  if (ArgEnableFastISelOption == cl::BOU_TRUE ||
      (TM->getOptLevel() == CodeGenOpt::None &&
       ArgEnableFastISelOption != cl::BOU_FALSE))
    TM->setFastISel(true);

  // Ask the target for an isel.
  if (PassConfig->addInstSelector())
    return NULL;

  PassConfig->addMachinePasses();

  PassConfig->setInitialized();

  return Context;

}

bool mcld::LLVMTargetMachine::addPassesToEmitFile(PassManagerBase &pPM,
                                             formatted_raw_ostream &Out,
                                             const std::string& pOutputFilename,
                                             mcld::CodeGenFileType pFileType,
                                             CodeGenOpt::Level pOptLvl,
                                             SectLinkerOption *pLinkerOpt,
                                             bool pDisableVerify)
{

  llvm::MCContext* Context =
          addPassesToGenerateCode(static_cast<llvm::LLVMTargetMachine*>(&m_TM),
                                  pPM, pDisableVerify);
  if (!Context)
    return true;

  switch(pFileType) {
  default:
  case mcld::CGFT_NULLFile:
    assert(0 && "fatal: file type is not set!");
    break;
  case CGFT_ASMFile: {
    assert(Context != 0 && "Failed to get MCContext");

    if (getTM().hasMCSaveTempLabels())
      Context->setAllowTemporaryLabels(false);

    if (addCompilerPasses(pPM,
                          Out,
                          pOutputFilename,
                          Context))
      return true;

    pPM.add(createGCInfoDeleter()); // not in addPassesToMC
    break;
  }
  case CGFT_OBJFile: {
    assert(Context != 0 && "Failed to get MCContext");

    if (getTM().hasMCSaveTempLabels())
      Context->setAllowTemporaryLabels(false);
    if (addAssemblerPasses(pPM,
                           Out,
                           pOutputFilename,
                           Context))
      return true;

    pPM.add(createGCInfoDeleter()); // not in addPassesToMC
    break;
  }
  case CGFT_EXEFile: {
    if (pLinkerOpt == NULL)
      return true;

    if (addLinkerPasses(pPM,
                        pLinkerOpt,
                        pOutputFilename,
                        MCLDFile::Exec,
                        Context))
      return true;
    break;
  }
  case CGFT_DSOFile: {
    if (pLinkerOpt == NULL)
      return true;

    if (addLinkerPasses(pPM,
                        pLinkerOpt,
                        pOutputFilename,
                        MCLDFile::DynObj,
                        Context))
      return true;
    break;
  }
  } // switch
  return false;
}

bool mcld::LLVMTargetMachine::addCompilerPasses(PassManagerBase &pPM,
                                                formatted_raw_ostream &Out,
                                                const std::string& pOutputFilename,
                                                llvm::MCContext *&Context)
{
  const MCAsmInfo &MAI = *getTM().getMCAsmInfo();
  const MCInstrInfo &MII = *getTM().getInstrInfo();
  const MCRegisterInfo &MRI = *getTM().getRegisterInfo();
  const MCSubtargetInfo &STI = getTM().getSubtarget<MCSubtargetInfo>();

  MCInstPrinter *InstPrinter =
    getTarget().get()->createMCInstPrinter(MAI.getAssemblerDialect(), MAI,
                                           MII,
                                           Context->getRegisterInfo(), STI);

  MCCodeEmitter* MCE = 0;
  MCAsmBackend *MAB = 0;
  if (ArgShowMCEncoding) {
    MCE = getTarget().get()->createMCCodeEmitter(MII, MRI, STI, *Context);
    MAB = getTarget().get()->createMCAsmBackend(m_Triple);
  }


  // now, we have MCCodeEmitter and MCAsmBackend, we can create AsmStreamer.
  OwningPtr<MCStreamer> AsmStreamer(
    getTarget().get()->createAsmStreamer(*Context, Out,
                                         getVerboseAsm(),
                                         getTM().hasMCUseLoc(),
                                         getTM().hasMCUseCFI(),
                                         getTM().hasMCUseDwarfDirectory(),
                                         InstPrinter,
                                         MCE, MAB,
                                         ArgShowMCInst));

  llvm::MachineFunctionPass* funcPass =
    getTarget().get()->createAsmPrinter(getTM(), *AsmStreamer.get());

  if (funcPass == 0)
    return true;
  // If successful, createAsmPrinter took ownership of AsmStreamer
  AsmStreamer.take();
  pPM.add(funcPass);
  return false;
}

bool mcld::LLVMTargetMachine::addAssemblerPasses(PassManagerBase &pPM,
                                                 formatted_raw_ostream &Out,
                                                 const std::string& pOutputFilename,
                                                 llvm::MCContext *&Context)
{
  // MCCodeEmitter
  const MCInstrInfo &MII = *getTM().getInstrInfo();
  const MCRegisterInfo &MRI = *getTM().getRegisterInfo();
  const MCSubtargetInfo &STI = getTM().getSubtarget<MCSubtargetInfo>();
  MCCodeEmitter* MCE =
    getTarget().get()->createMCCodeEmitter(MII, MRI, STI, *Context);

  // MCAsmBackend
  MCAsmBackend* MAB = getTarget().get()->createMCAsmBackend(m_Triple);
  if (MCE == 0 || MAB == 0)
    return true;

  // now, we have MCCodeEmitter and MCAsmBackend, we can create AsmStreamer.
  OwningPtr<MCStreamer> AsmStreamer(getTarget().get()->createMCObjectStreamer(
                                                              m_Triple,
                                                              *Context,
                                                              *MAB,
                                                              Out,
                                                              MCE,
                                                              getTM().hasMCRelaxAll(),
                                                              getTM().hasMCNoExecStack()));
  AsmStreamer.get()->InitSections();
  MachineFunctionPass *funcPass = getTarget().get()->createAsmPrinter(getTM(),
                                                                      *AsmStreamer.get());
  if (funcPass == 0)
    return true;
  // If successful, createAsmPrinter took ownership of AsmStreamer
  AsmStreamer.take();
  pPM.add(funcPass);
  return false;
}

bool mcld::LLVMTargetMachine::addLinkerPasses(PassManagerBase &pPM,
                                              SectLinkerOption *pLinkerOpt,
                                              const std::string &pOutputFilename,
                                              MCLDFile::Type pOutputLinkType,
                                              llvm::MCContext *&Context)
{
  TargetLDBackend* ldBackend = getTarget().createLDBackend(m_Triple);
  if (0 == ldBackend)
    return true;

  // set up output's SOName
  if (pOutputLinkType == MCLDFile::DynObj &&
      pLinkerOpt->info().output().name().empty()) {
    // if the output is a shared object, and the option -soname was not
    // enable, set soname as the output file name.
    pLinkerOpt->info().output().setSOName(pOutputFilename);
  }

  pLinkerOpt->info().output().setPath(sys::fs::RealPath(pOutputFilename));
  pLinkerOpt->info().output().setType(pOutputLinkType);

  MachineFunctionPass* funcPass = getTarget().createSectLinker(m_Triple,
                                                               *pLinkerOpt,
                                                               *ldBackend);
  if (0 == funcPass)
    return true;

  pPM.add(funcPass);
  return false;
}

