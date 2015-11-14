//===- ELFDynObjReader.cpp ------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <llvm/ADT/Twine.h>
#include <llvm/ADT/OwningPtr.h>
#include <llvm/Support/ErrorHandling.h>

#include <mcld/LD/ELFDynObjReader.h>
#include <mcld/LD/ELFReader.h>
#include <mcld/MC/MCLDInput.h>
#include <mcld/MC/MCLinker.h>
#include <mcld/Target/GNULDBackend.h>
#include <mcld/Support/MemoryRegion.h>

#include <string>

using namespace mcld;

//==========================
// ELFDynObjReader
ELFDynObjReader::ELFDynObjReader(GNULDBackend& pBackend, MCLinker& pLinker)
  : DynObjReader(),
    m_pELFReader(0),
    m_Linker(pLinker) {
  if (32 == pBackend.bitclass() && pBackend.isLittleEndian())
    m_pELFReader = new ELFReader<32, true>(pBackend);
}

ELFDynObjReader::~ELFDynObjReader()
{
  delete m_pELFReader;
}

/// isMyFormat
bool ELFDynObjReader::isMyFormat(Input &pInput) const
{
  assert(pInput.hasMemArea());

  // Don't warning about the frequently requests.
  // MemoryArea has a list of cache to handle this.
  size_t hdr_size = m_pELFReader->getELFHeaderSize();
  MemoryRegion* region = pInput.memArea()->request(pInput.fileOffset(),
                                                   hdr_size);

  uint8_t* ELF_hdr = region->start();
  bool result = true;
  if (!m_pELFReader->isELF(ELF_hdr))
    result = false;
  else if (!m_pELFReader->isMyEndian(ELF_hdr))
    result = false;
  else if (!m_pELFReader->isMyMachine(ELF_hdr))
    result = false;
  else if (MCLDFile::DynObj != m_pELFReader->fileType(ELF_hdr))
    result = false;
  pInput.memArea()->release(region);
  return result;
}

/// readDSO
bool ELFDynObjReader::readDSO(Input& pInput)
{
  assert(pInput.hasMemArea());

  size_t hdr_size = m_pELFReader->getELFHeaderSize();
  MemoryRegion* region = pInput.memArea()->request(pInput.fileOffset(),
                                                   hdr_size);
  uint8_t* ELF_hdr = region->start();

  bool shdr_result = m_pELFReader->readSectionHeaders(pInput, m_Linker, ELF_hdr);
  pInput.memArea()->release(region);

  // read .dynamic to get the correct SONAME
  bool dyn_result = m_pELFReader->readDynamic(pInput);

  return (shdr_result && dyn_result);
}

/// readSymbols
bool ELFDynObjReader::readSymbols(Input& pInput)
{
  assert(pInput.hasMemArea());

  LDSection* symtab_shdr = pInput.context()->getSection(".dynsym");
  if (NULL == symtab_shdr) {
    note(diag::note_has_no_symtab) << pInput.name()
                                   << pInput.path()
                                   << ".dynsym";
    return true;
  }

  LDSection* strtab_shdr = symtab_shdr->getLink();
  if (NULL == strtab_shdr) {
    fatal(diag::fatal_cannot_read_strtab) << pInput.name()
                                          << pInput.path()
                                          << ".dynsym";
    return false;
  }

  MemoryRegion* symtab_region = pInput.memArea()->request(
              pInput.fileOffset() + symtab_shdr->offset(), symtab_shdr->size());

  MemoryRegion* strtab_region = pInput.memArea()->request(
              pInput.fileOffset() + strtab_shdr->offset(), strtab_shdr->size());
  char* strtab = reinterpret_cast<char*>(strtab_region->start());
  bool result = m_pELFReader->readSymbols(pInput, m_Linker, *symtab_region,
                                            strtab);
  pInput.memArea()->release(symtab_region);
  pInput.memArea()->release(strtab_region);

  return result;
}

