//===- MCLDOptions.cpp ----------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/MC/MCLDOptions.h>
#include <mcld/MC/MCLDInput.h>

using namespace mcld;

//===----------------------------------------------------------------------===//
// ScriptOptions
ScriptOptions::ScriptOptions()
{
}

ScriptOptions::~ScriptOptions()
{
}

//===----------------------------------------------------------------------===//
// GeneralOptions
GeneralOptions::GeneralOptions()
  : m_pDefaultBitcode(NULL),
    m_Verbose(-1),
    m_MaxErrorNum(-1),
    m_MaxWarnNum(-1),
    m_ExecStack(Unknown),
    m_CommPageSize(0x0),
    m_MaxPageSize(0x0),
    m_bCombReloc(true),
    m_bNoUndefined(false),
    m_bInitFirst(false),
    m_bInterPose(false),
    m_bLoadFltr(false),
    m_bMulDefs(false),
    m_bNoCopyReloc(false),
    m_bNoDefaultLib(false),
    m_bNoDelete(false),
    m_bNoDLOpen(false),
    m_bNoDump(false),
    m_bRelro(false),
    m_bNow(false),
    m_bOrigin(false),
    m_bTrace(false),
    m_Bsymbolic(false),
    m_Bgroup(false),
    m_bPIE(false),
    m_bColor(true),
    m_bAllowShlibUndefined(true),
    m_bCreateEhFrameHdr(false)
{
}

GeneralOptions::~GeneralOptions()
{
}

bool GeneralOptions::hasDefaultLDScript() const
{
  return true;
}

const char* GeneralOptions::defaultLDScript() const
{
  return NULL;
}

void GeneralOptions::setDefaultLDScript(const std::string& pFilename)
{
}

void GeneralOptions::setSysroot(const mcld::sys::fs::Path &pSysroot)
{
  m_Sysroot.assign(pSysroot);
}

void GeneralOptions::addZOption(const ZOption& pOption)
{
  switch (pOption.kind()) {
    case ZOption::CombReloc:
      m_bCombReloc = true;
      break;
    case ZOption::NoCombReloc:
      m_bCombReloc = false;
      break;
    case ZOption::Defs:
      m_bNoUndefined = true;
      break;
    case ZOption::ExecStack:
      m_ExecStack = YES;
      break;
    case ZOption::NoExecStack:
      m_ExecStack = NO;
      break;
    case ZOption::InitFirst:
      m_bInitFirst = true;
      break;
    case ZOption::InterPose:
      m_bInterPose = true;
      break;
    case ZOption::LoadFltr:
      m_bLoadFltr = true;
      break;
    case ZOption::MulDefs:
      m_bMulDefs = true;
      break;
    case ZOption::NoCopyReloc:
      m_bNoCopyReloc = true;
      break;
    case ZOption::NoDefaultLib:
      m_bNoDefaultLib = true;
      break;
    case ZOption::NoDelete:
      m_bNoDelete = true;
      break;
    case ZOption::NoDLOpen:
      m_bNoDLOpen = true;
      break;
    case ZOption::NoDump:
      m_bNoDump = true;
      break;
    case ZOption::NoRelro:
      m_bRelro = false;
      break;
    case ZOption::Relro:
      m_bRelro = true;
      break;
    case ZOption::Lazy:
      m_bNow = false;
      break;
    case ZOption::Now:
      m_bNow = true;
      break;
    case ZOption::Origin:
      m_bOrigin = true;
      break;
    case ZOption::CommPageSize:
      m_CommPageSize = pOption.pageSize();
      break;
    case ZOption::MaxPageSize:
      m_MaxPageSize = pOption.pageSize();
      break;
    case ZOption::Unknown:
    default:
      assert(false && "Not a recognized -z option.");
      break;
  }
}
