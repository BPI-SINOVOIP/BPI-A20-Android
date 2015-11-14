//===- ELFDynamic.h -------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_ELF_DYNAMIC_SECTION_H
#define MCLD_ELF_DYNAMIC_SECTION_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif

#include <llvm/Support/ELF.h>
#include <mcld/LD/LDSection.h>
#include <vector>
#include <cstring>

namespace mcld
{

class GNULDBackend;
class ELFFileFormat;
class MCLDInfo;
class MemoryRegion;

namespace elf_dynamic {

/** \class EntryIF
*  \brief EntryIF provides a common interface for one entry in the dynamic
*  section
*/
class EntryIF
{
protected:
  EntryIF();

public:
  virtual ~EntryIF(); 

  virtual EntryIF* clone() const = 0;
  virtual size_t size() const = 0;
  virtual size_t symbolSize() const = 0;
  virtual size_t relSize() const = 0;
  virtual size_t relaSize() const = 0;
  virtual size_t emit(uint8_t* pAddress) const = 0;
  virtual void setValue(uint64_t pTag, uint64_t pValue) = 0;
};

template<size_t BITNUMBER, bool LITTLEENDIAN>
class Entry
{ };

template<>
class Entry<32, true> : public EntryIF
{
public:
  typedef llvm::ELF::Elf32_Dyn  Pair;
  typedef llvm::ELF::Elf32_Sym  Symbol;
  typedef llvm::ELF::Elf32_Rel  Rel;
  typedef llvm::ELF::Elf32_Rela Rela;

public:
  inline Entry();

  inline ~Entry();

  Entry* clone() const
  { return new Entry(); }

  size_t size() const
  { return sizeof(Pair); }

  size_t symbolSize() const
  { return sizeof(Symbol); }

  size_t relSize() const
  { return sizeof(Rel); }

  size_t relaSize() const
  { return sizeof(Rela); }

  inline void setValue(uint64_t pTag, uint64_t pValue);

  inline size_t emit(uint8_t* pAddress) const;

private:
  Pair m_Pair;
}; 

#include "ELFDynamic.tcc"

} // namespace of elf_dynamic

/** \class ELFDynamic
 *  \brief ELFDynamic is the .dynamic section in ELF shared and executable
 *  files.
 */
class ELFDynamic
{
public:
  typedef std::vector<elf_dynamic::EntryIF*> EntryListType;
  typedef EntryListType::iterator iterator;
  typedef EntryListType::const_iterator const_iterator;

public:
  ELFDynamic(const GNULDBackend& pParent);

  virtual ~ELFDynamic();

  size_t size() const;

  size_t entrySize() const;

  size_t numOfBytes() const;

  /// reserveEntries - reserve entries
  void reserveEntries(const MCLDInfo& pInfo,
                      const ELFFileFormat& pFormat);

  /// reserveNeedEntry - reserve on DT_NEED entry.
  void reserveNeedEntry();
  
  /// applyEntries - apply entries
  void applyEntries(const MCLDInfo& pInfo,
                    const ELFFileFormat& pFormat);

  void applySoname(uint64_t pStrTabIdx);

  iterator needBegin()
  { return m_NeedList.begin(); }

  iterator needEnd()
  { return m_NeedList.end(); }

  const_iterator needBegin() const
  { return m_NeedList.begin(); }

  const_iterator needEnd() const
  { return m_NeedList.end(); }

  /// emit
  void emit(const LDSection& pSection, MemoryRegion& pRegion) const;

protected:
  /// reserveTargetEntries - reserve target dependent entries
  virtual void reserveTargetEntries(const ELFFileFormat& pFormat) = 0;

  /// applyTargetEntries - apply target-dependant
  virtual void applyTargetEntries(const ELFFileFormat& pFormat) = 0;

protected:
  void reserveOne(uint64_t pTag);

  void applyOne(uint64_t pTag, uint64_t pValue);

  size_t symbolSize() const;

private:
  EntryListType m_EntryList;
  EntryListType m_NeedList;
  elf_dynamic::EntryIF* m_pEntryFactory;

  // The entry reserved and the entry being applied are not must matched.
  // For better performance, we use a simple counter and apply entry one-by-one
  // by the counter. m_Idx is the counter indicating to the entry being applied.
  size_t m_Idx;
};

} // namespace of mcld

#endif

