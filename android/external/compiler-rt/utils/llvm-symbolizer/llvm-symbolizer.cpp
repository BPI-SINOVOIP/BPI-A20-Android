//===-- llvm-symbolizer.cpp - Simple addr2line-like symbolizer ------------===//
//
//                     The LLVM Compiler Infrastructure
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
//
// This utility works much like "addr2line". It is able of transforming
// tuples (module name, module offset) to code locations (function name,
// file, line number, column number). It is targeted for compiler-rt tools
// (especially AddressSanitizer and ThreadSanitizer) that can use it
// to symbolize stack traces in their error reports.
//
//===----------------------------------------------------------------------===//

#include "llvm/ADT/OwningPtr.h"
#include "llvm/ADT/StringRef.h"
#include "llvm/DebugInfo/DIContext.h"
#include "llvm/Object/ObjectFile.h"
#include "llvm/Support/CommandLine.h"
#include "llvm/Support/Debug.h"
#include "llvm/Support/ManagedStatic.h"
#include "llvm/Support/MemoryBuffer.h"
#include "llvm/Support/PrettyStackTrace.h"
#include "llvm/Support/Signals.h"
#include "llvm/Support/raw_ostream.h"

#include <cstdio>
#include <cstring>
#include <map>
#include <string>

using namespace llvm;
using namespace object;
using std::string;

static cl::opt<bool>
UseSymbolTable("use-symbol-table", cl::init(true),
               cl::desc("Prefer names in symbol table to names "
                        "in debug info"));

static cl::opt<bool>
PrintFunctions("functions", cl::init(true),
               cl::desc("Print function names as well as line "
                        "information for a given address"));

static cl::opt<bool>
PrintInlining("inlining", cl::init(true),
              cl::desc("Print all inlined frames for a given address"));

static cl::opt<bool>
Demangle("demangle", cl::init(true),
         cl::desc("Demangle function names"));

static StringRef ToolInvocationPath;

static bool error(error_code ec) {
  if (!ec) return false;
  errs() << ToolInvocationPath << ": error reading file: "
         << ec.message() << ".\n";
  return true;
}

static uint32_t getDILineInfoSpecifierFlags() {
  uint32_t Flags = llvm::DILineInfoSpecifier::FileLineInfo |
                   llvm::DILineInfoSpecifier::AbsoluteFilePath;
  if (PrintFunctions)
    Flags |= llvm::DILineInfoSpecifier::FunctionName;
  return Flags;
}

static void patchFunctionNameInDILineInfo(const string &NewFunctionName,
                                          DILineInfo &LineInfo) {
  string FileName = LineInfo.getFileName();
  LineInfo = DILineInfo(StringRef(FileName), StringRef(NewFunctionName),
                        LineInfo.getLine(), LineInfo.getColumn());
}

namespace {
class ModuleInfo {
  OwningPtr<ObjectFile> Module;
  OwningPtr<DIContext> DebugInfoContext;
 public:
  ModuleInfo(ObjectFile *Obj, DIContext *DICtx)
      : Module(Obj), DebugInfoContext(DICtx) {}
  DILineInfo symbolizeCode(uint64_t ModuleOffset) const {
    DILineInfo LineInfo;
    if (DebugInfoContext) {
      LineInfo = DebugInfoContext->getLineInfoForAddress(
          ModuleOffset, getDILineInfoSpecifierFlags());
    }
    // Override function name from symbol table if necessary.
    if (PrintFunctions && UseSymbolTable) {
      string Function;
      if (getFunctionNameFromSymbolTable(ModuleOffset, Function)) {
        patchFunctionNameInDILineInfo(Function, LineInfo);
      }
    }
    return LineInfo;
  }
  DIInliningInfo symbolizeInlinedCode(uint64_t ModuleOffset) const {
    DIInliningInfo InlinedContext;
    if (DebugInfoContext) {
      InlinedContext = DebugInfoContext->getInliningInfoForAddress(
          ModuleOffset, getDILineInfoSpecifierFlags());
    }
    // Make sure there is at least one frame in context.
    if (InlinedContext.getNumberOfFrames() == 0) {
      InlinedContext.addFrame(DILineInfo());
    }
    // Override the function name in lower frame with name from symbol table.
    if (PrintFunctions && UseSymbolTable) {
      DIInliningInfo PatchedInlinedContext;
      for (uint32_t i = 0, n = InlinedContext.getNumberOfFrames();
           i != n; i++) {
        DILineInfo LineInfo = InlinedContext.getFrame(i);
        if (i == n - 1) {
          string Function;
          if (getFunctionNameFromSymbolTable(ModuleOffset, Function)) {
            patchFunctionNameInDILineInfo(Function, LineInfo);
          }
        }
        PatchedInlinedContext.addFrame(LineInfo);
      }
      InlinedContext = PatchedInlinedContext;
    }
    return InlinedContext;
  }
 private:
  bool getFunctionNameFromSymbolTable(size_t Address,
                                      string &FunctionName) const {
    assert(Module);
    error_code ec;
    for (symbol_iterator si = Module->begin_symbols(),
                         se = Module->end_symbols();
                         si != se; si.increment(ec)) {
      if (error(ec)) return false;
      uint64_t SymbolAddress;
      uint64_t SymbolSize;
      if (error(si->getAddress(SymbolAddress))) continue;
      if (error(si->getSize(SymbolSize))) continue;
      // FIXME: If a function has alias, there are two entries in symbol table
      // with same address size. Make sure we choose the correct one.
      if (SymbolAddress <= Address && Address < SymbolAddress + SymbolSize) {
        StringRef Name;
        if (error(si->getName(Name))) continue;
        FunctionName = Name.str();
        return true;
      }
    }
    return false;
  }
};

typedef std::map<string, ModuleInfo*> ModuleMapTy;
typedef ModuleMapTy::iterator ModuleMapIter;
typedef ModuleMapTy::const_iterator ModuleMapConstIter;
}  // namespace

static ModuleMapTy Modules;

static bool isFullNameOfDwarfSection(const StringRef &FullName,
                                     const StringRef &ShortName) {
  static const char kDwarfPrefix[] = "__DWARF,";
  StringRef Name = FullName;
  // Skip "__DWARF," prefix.
  if (Name.startswith(kDwarfPrefix))
    Name = Name.substr(strlen(kDwarfPrefix));
  // Skip . and _ prefixes.
  Name = Name.substr(Name.find_first_not_of("._"));
  return (Name == ShortName);
}

// Returns true if the object endianness is known.
static bool getObjectEndianness(const ObjectFile *Obj,
                                bool &IsLittleEndian) {
  // FIXME: Implement this when libLLVMObject allows to do it easily.
  IsLittleEndian = true;
  return true;
}

static ModuleInfo *getOrCreateModuleInfo(const string &ModuleName) {
  ModuleMapIter I = Modules.find(ModuleName);
  if (I != Modules.end())
    return I->second;

  OwningPtr<MemoryBuffer> Buff;
  MemoryBuffer::getFile(ModuleName, Buff);
  ObjectFile *Obj = ObjectFile::createObjectFile(Buff.take());
  if (Obj == 0) {
    // Module name doesn't point to a valid object file.
    Modules.insert(make_pair(ModuleName, (ModuleInfo*)0));
    return 0;
  }

  DIContext *Context = 0;
  bool IsLittleEndian;
  if (getObjectEndianness(Obj, IsLittleEndian)) {
    StringRef DebugInfoSection;
    StringRef DebugAbbrevSection;
    StringRef DebugLineSection;
    StringRef DebugArangesSection;
    StringRef DebugStringSection;
    StringRef DebugRangesSection;
    error_code ec;
    for (section_iterator i = Obj->begin_sections(),
                          e = Obj->end_sections();
                          i != e; i.increment(ec)) {
      if (error(ec)) break;
      StringRef Name;
      if (error(i->getName(Name))) continue;
      StringRef Data;
      if (error(i->getContents(Data))) continue;
      if (isFullNameOfDwarfSection(Name, "debug_info"))
        DebugInfoSection = Data;
      else if (isFullNameOfDwarfSection(Name, "debug_abbrev"))
        DebugAbbrevSection = Data;
      else if (isFullNameOfDwarfSection(Name, "debug_line"))
        DebugLineSection = Data;
      // Don't use debug_aranges for now, as address ranges contained
      // there may not cover all instructions in the module
      // else if (isFullNameOfDwarfSection(Name, "debug_aranges"))
      //   DebugArangesSection = Data;
      else if (isFullNameOfDwarfSection(Name, "debug_str"))
        DebugStringSection = Data;
      else if (isFullNameOfDwarfSection(Name, "debug_ranges"))
        DebugRangesSection = Data;
    }

    Context = DIContext::getDWARFContext(
        IsLittleEndian, DebugInfoSection, DebugAbbrevSection,
        DebugArangesSection, DebugLineSection, DebugStringSection,
        DebugRangesSection);
    assert(Context);
  }

  ModuleInfo *Info = new ModuleInfo(Obj, Context);
  Modules.insert(make_pair(ModuleName, Info));
  return Info;
}

// Assume that __cxa_demangle is provided by libcxxabi.
extern "C" char *__cxa_demangle(const char *mangled_name, char *output_buffer,
                                size_t *length, int *status);

static void printDILineInfo(DILineInfo LineInfo) {
  // By default, DILineInfo contains "<invalid>" for function/filename it
  // cannot fetch. We replace it to "??" to make our output closer to addr2line.
  static const string kDILineInfoBadString = "<invalid>";
  static const string kSymbolizerBadString = "??";
  if (PrintFunctions) {
    string FunctionName = LineInfo.getFunctionName();
    if (FunctionName == kDILineInfoBadString)
      FunctionName = kSymbolizerBadString;
    if (Demangle) {
      int status = 0;
      char *DemangledName = __cxa_demangle(
          FunctionName.c_str(), 0, 0, &status);
      if (status == 0) {
        FunctionName = DemangledName;
        free(DemangledName);
      }
    }
    outs() << FunctionName << "\n";
  }
  string Filename = LineInfo.getFileName();
  if (Filename == kDILineInfoBadString)
    Filename = kSymbolizerBadString;
  outs() << Filename <<
         ":" << LineInfo.getLine() <<
         ":" << LineInfo.getColumn() <<
         "\n";
}

static void symbolize(string ModuleName, string ModuleOffsetStr) {
  ModuleInfo *Info = getOrCreateModuleInfo(ModuleName);
  uint64_t Offset = 0;
  if (Info == 0 ||
      StringRef(ModuleOffsetStr).getAsInteger(0, Offset)) {
    printDILineInfo(DILineInfo());
  } else if (PrintInlining) {
    DIInliningInfo InlinedContext = Info->symbolizeInlinedCode(Offset);
    uint32_t FramesNum = InlinedContext.getNumberOfFrames();
    assert(FramesNum > 0);
    for (uint32_t i = 0; i < FramesNum; i++) {
      DILineInfo LineInfo = InlinedContext.getFrame(i);
      printDILineInfo(LineInfo);
    }
  } else {
    DILineInfo LineInfo = Info->symbolizeCode(Offset);
    printDILineInfo(LineInfo);
  }

  outs() << "\n";  // Print extra empty line to mark the end of output.
  outs().flush();
}

static bool parseModuleNameAndOffset(string &ModuleName,
                                     string &ModuleOffsetStr) {
  static const int kMaxInputStringLength = 1024;
  static const char kDelimiters[] = " \n";
  char InputString[kMaxInputStringLength];
  if (!fgets(InputString, sizeof(InputString), stdin))
    return false;
  ModuleName = "";
  ModuleOffsetStr = "";
  // FIXME: Handle case when filename is given in quotes.
  if (char *FilePath = strtok(InputString, kDelimiters)) {
    ModuleName = FilePath;
    if (char *OffsetStr = strtok((char*)0, kDelimiters))
      ModuleOffsetStr = OffsetStr;
  }
  return true;
}

int main(int argc, char **argv) {
  // Print stack trace if we signal out.
  sys::PrintStackTraceOnErrorSignal();
  PrettyStackTraceProgram X(argc, argv);
  llvm_shutdown_obj Y;  // Call llvm_shutdown() on exit.

  cl::ParseCommandLineOptions(argc, argv, "llvm symbolizer for compiler-rt\n");
  ToolInvocationPath = argv[0];

  string ModuleName;
  string ModuleOffsetStr;
  while (parseModuleNameAndOffset(ModuleName, ModuleOffsetStr)) {
    symbolize(ModuleName, ModuleOffsetStr);
  }
  return 0;
}
