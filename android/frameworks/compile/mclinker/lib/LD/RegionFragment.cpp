//===- RegionFragment.cpp -------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//

#include <mcld/LD/RegionFragment.h>

using namespace mcld;

//===----------------------------------------------------------------------===//
// RegionFragment
//===----------------------------------------------------------------------===//
RegionFragment::RegionFragment(MemoryRegion& pRegion, SectionData* pSD)
  : Fragment(Fragment::Region, pSD), m_Region(pRegion) {
}

RegionFragment::~RegionFragment()
{
}

