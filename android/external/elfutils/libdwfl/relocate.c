/* Relocate debug information.
   Copyright (C) 2005, 2006, 2007, 2008 Red Hat, Inc.
   This file is part of Red Hat elfutils.

   Red Hat elfutils is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by the
   Free Software Foundation; version 2 of the License.

   Red Hat elfutils is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.

   You should have received a copy of the GNU General Public License along
   with Red Hat elfutils; if not, write to the Free Software Foundation,
   Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301 USA.

   In addition, as a special exception, Red Hat, Inc. gives You the
   additional right to link the code of Red Hat elfutils with code licensed
   under any Open Source Initiative certified open source license
   (http://www.opensource.org/licenses/index.php) which requires the
   distribution of source code with any binary distribution and to
   distribute linked combinations of the two.  Non-GPL Code permitted under
   this exception must only link to the code of Red Hat elfutils through
   those well defined interfaces identified in the file named EXCEPTION
   found in the source code files (the "Approved Interfaces").  The files
   of Non-GPL Code may instantiate templates or use macros or inline
   functions from the Approved Interfaces without causing the resulting
   work to be covered by the GNU General Public License.  Only Red Hat,
   Inc. may make changes or additions to the list of Approved Interfaces.
   Red Hat's grant of this exception is conditioned upon your not adding
   any new exceptions.  If you wish to add a new Approved Interface or
   exception, please contact Red Hat.  You must obey the GNU General Public
   License in all respects for all of the Red Hat elfutils code and other
   code used in conjunction with Red Hat elfutils except the Non-GPL Code
   covered by this exception.  If you modify this file, you may extend this
   exception to your version of the file, but you are not obligated to do
   so.  If you do not wish to provide this exception without modification,
   you must delete this exception statement from your version and license
   this file solely under the GPL without exception.

   Red Hat elfutils is an included package of the Open Invention Network.
   An included package of the Open Invention Network is a package for which
   Open Invention Network licensees cross-license their patents.  No patent
   license is granted, either expressly or impliedly, by designation as an
   included package.  Should you wish to participate in the Open Invention
   Network licensing program, please visit www.openinventionnetwork.com
   <http://www.openinventionnetwork.com>.  */

#include "libdwflP.h"

typedef uint8_t GElf_Byte;

/* Adjust *VALUE to add the load address of the SHNDX section.
   We update the section header in place to cache the result.  */

Dwfl_Error
internal_function
__libdwfl_relocate_value (Dwfl_Module *mod, Elf *elf, size_t *shstrndx,
			  Elf32_Word shndx, GElf_Addr *value)
{
  Elf_Scn *refscn = elf_getscn (elf, shndx);
  GElf_Shdr refshdr_mem, *refshdr = gelf_getshdr (refscn, &refshdr_mem);
  if (refshdr == NULL)
    return DWFL_E_LIBELF;

  if (refshdr->sh_addr == 0 && (refshdr->sh_flags & SHF_ALLOC))
    {
      /* This is a loaded section.  Find its actual
	 address and update the section header.  */

      if (*shstrndx == SHN_UNDEF
	  && unlikely (elf_getshstrndx (elf, shstrndx) < 0))
	return DWFL_E_LIBELF;

      const char *name = elf_strptr (elf, *shstrndx, refshdr->sh_name);
      if (unlikely (name == NULL))
	return DWFL_E_LIBELF;

      if ((*mod->dwfl->callbacks->section_address) (MODCB_ARGS (mod),
						    name, shndx, refshdr,
						    &refshdr->sh_addr))
	return CBFAIL;

      if (refshdr->sh_addr == (Dwarf_Addr) -1l)
	/* The callback indicated this section wasn't really loaded but we
	   don't really care.  */
	refshdr->sh_addr = 0;	/* Make no adjustment below.  */

      /* Update the in-core file's section header to show the final
	 load address (or unloadedness).  This serves as a cache,
	 so we won't get here again for the same section.  */
      if (likely (refshdr->sh_addr != 0)
	  && unlikely (! gelf_update_shdr (refscn, refshdr)))
	return DWFL_E_LIBELF;
    }

  /* Apply the adjustment.  */
  *value += refshdr->sh_addr;
  return DWFL_E_NOERROR;
}


/* Cache used by relocate_getsym.  */
struct reloc_symtab_cache
{
  Elf *symelf;
  Elf_Data *symdata;
  Elf_Data *symxndxdata;
  Elf_Data *symstrdata;
  size_t symshstrndx;
  size_t strtabndx;
};
#define RELOC_SYMTAB_CACHE(cache)	\
  struct reloc_symtab_cache cache =	\
    { NULL, NULL, NULL, NULL, SHN_UNDEF, SHN_UNDEF }

/* This is just doing dwfl_module_getsym, except that we must always use
   the symbol table in RELOCATED itself when it has one, not MOD->symfile.  */
static Dwfl_Error
relocate_getsym (Dwfl_Module *mod,
		 Elf *relocated, struct reloc_symtab_cache *cache,
		 int symndx, GElf_Sym *sym, GElf_Word *shndx)
{
  if (cache->symdata == NULL)
    {
      if (mod->symfile == NULL || mod->symfile->elf != relocated)
	{
	  /* We have to look up the symbol table in the file we are
	     relocating, if it has its own.  These reloc sections refer to
	     the symbol table in this file, and a symbol table in the main
	     file might not match.  However, some tools did produce ET_REL
	     .debug files with relocs but no symtab of their own.  */
	  Elf_Scn *scn = NULL;
	  while ((scn = elf_nextscn (relocated, scn)) != NULL)
	    {
	      GElf_Shdr shdr_mem, *shdr = gelf_getshdr (scn, &shdr_mem);
	      if (shdr != NULL)
		switch (shdr->sh_type)
		  {
		  default:
		    continue;
		  case SHT_SYMTAB:
		    cache->symelf = relocated;
		    cache->symdata = elf_getdata (scn, NULL);
		    cache->strtabndx = shdr->sh_link;
		    if (unlikely (cache->symdata == NULL))
		      return DWFL_E_LIBELF;
		    break;
		  case SHT_SYMTAB_SHNDX:
		    cache->symxndxdata = elf_getdata (scn, NULL);
		    if (unlikely (cache->symxndxdata == NULL))
		      return DWFL_E_LIBELF;
		    break;
		  }
	      if (cache->symdata != NULL && cache->symxndxdata != NULL)
		break;
	    }
	}
      if (cache->symdata == NULL)
	{
	  /* We might not have looked for a symbol table file yet,
	     when coming from __libdwfl_relocate_section.  */
	  if (unlikely (mod->symfile == NULL)
	      && unlikely (INTUSE(dwfl_module_getsymtab) (mod) < 0))
	    return dwfl_errno ();

	  /* The symbol table we have already cached is the one from
	     the file being relocated, so it's what we need.  Or else
	     this is an ET_REL .debug file with no .symtab of its own;
	     the symbols refer to the section indices in the main file.  */
	  cache->symelf = mod->symfile->elf;
	  cache->symdata = mod->symdata;
	  cache->symxndxdata = mod->symxndxdata;
	  cache->symstrdata = mod->symstrdata;
	}
    }

  if (unlikely (gelf_getsymshndx (cache->symdata, cache->symxndxdata,
				  symndx, sym, shndx) == NULL))
    return DWFL_E_LIBELF;

  if (sym->st_shndx != SHN_XINDEX)
    *shndx = sym->st_shndx;

  switch (*shndx)
    {
    case SHN_ABS:
    case SHN_UNDEF:
    case SHN_COMMON:
      return DWFL_E_NOERROR;
    }

  return __libdwfl_relocate_value (mod, cache->symelf, &cache->symshstrndx,
				   *shndx, &sym->st_value);
}

/* Handle an undefined symbol.  We really only support ET_REL for Linux
   kernel modules, and offline archives.  The behavior of the Linux module
   loader is very simple and easy to mimic.  It only matches magically
   exported symbols, and we match any defined symbols.  But we get the same
   answer except when the module's symbols are undefined and would prevent
   it from being loaded.  */
static Dwfl_Error
resolve_symbol (Dwfl_Module *referer, struct reloc_symtab_cache *symtab,
		GElf_Sym *sym, GElf_Word shndx)
{
  /* First we need its name.  */
  if (sym->st_name != 0)
    {
      if (symtab->symstrdata == NULL)
	{
	  /* Cache the strtab for this symtab.  */
	  assert (referer->symfile == NULL
		  || referer->symfile->elf != symtab->symelf);
	  symtab->symstrdata = elf_getdata (elf_getscn (symtab->symelf,
							symtab->strtabndx),
					    NULL);
	  if (unlikely (symtab->symstrdata == NULL))
	    return DWFL_E_LIBELF;
	}
      if (unlikely (sym->st_name >= symtab->symstrdata->d_size))
	return DWFL_E_BADSTROFF;

      const char *name = symtab->symstrdata->d_buf;
      name += sym->st_name;

      for (Dwfl_Module *m = referer->dwfl->modulelist; m != NULL; m = m->next)
	if (m != referer)
	  {
	    /* Get this module's symtab.
	       If we got a fresh error reading the table, report it.
	       If we just have no symbols in this module, no harm done.  */
	    if (m->symdata == NULL
		&& m->symerr == DWFL_E_NOERROR
		&& INTUSE(dwfl_module_getsymtab) (m) < 0
		&& m->symerr != DWFL_E_NO_SYMTAB)
	      return m->symerr;

	    for (size_t ndx = 1; ndx < m->syments; ++ndx)
	      {
		sym = gelf_getsymshndx (m->symdata, m->symxndxdata,
					ndx, sym, &shndx);
		if (unlikely (sym == NULL))
		  return DWFL_E_LIBELF;
		if (sym->st_shndx != SHN_XINDEX)
		  shndx = sym->st_shndx;

		/* We are looking for a defined global symbol with a name.  */
		if (shndx == SHN_UNDEF || shndx == SHN_COMMON
		    || GELF_ST_BIND (sym->st_info) == STB_LOCAL
		    || sym->st_name == 0)
		  continue;

		/* Get this candidate symbol's name.  */
		if (unlikely (sym->st_name >= m->symstrdata->d_size))
		  return DWFL_E_BADSTROFF;
		const char *n = m->symstrdata->d_buf;
		n += sym->st_name;

		/* Does the name match?  */
		if (strcmp (name, n))
		  continue;

		/* We found it!  */
		if (shndx == SHN_ABS)
		  return DWFL_E_NOERROR;

		/* In an ET_REL file, the symbol table values are relative
		   to the section, not to the module's load base.  */
		size_t symshstrndx = SHN_UNDEF;
		return __libdwfl_relocate_value (m, m->symfile->elf,
						 &symshstrndx,
						 shndx, &sym->st_value);
	      }
	  }
    }

  return DWFL_E_RELUNDEF;
}

static Dwfl_Error
relocate_section (Dwfl_Module *mod, Elf *relocated, const GElf_Ehdr *ehdr,
		  size_t shstrndx, struct reloc_symtab_cache *reloc_symtab,
		  Elf_Scn *scn, GElf_Shdr *shdr,
		  Elf_Scn *tscn, bool debugscn, bool partial)
{
  /* First, fetch the name of the section these relocations apply to.  */
  GElf_Shdr tshdr_mem;
  GElf_Shdr *tshdr = gelf_getshdr (tscn, &tshdr_mem);
  const char *tname = elf_strptr (relocated, shstrndx, tshdr->sh_name);
  if (tname == NULL)
    return DWFL_E_LIBELF;

  if (debugscn && ! ebl_debugscn_p (mod->ebl, tname))
    /* This relocation section is not for a debugging section.
       Nothing to do here.  */
    return DWFL_E_NOERROR;

  /* Fetch the section data that needs the relocations applied.  */
  Elf_Data *tdata = elf_rawdata (tscn, NULL);
  if (tdata == NULL)
    return DWFL_E_LIBELF;

  /* Apply one relocation.  Returns true for any invalid data.  */
  Dwfl_Error relocate (GElf_Addr offset, const GElf_Sxword *addend,
		       int rtype, int symndx)
  {
    /* First see if this is a reloc we can handle.
       If we are skipping it, don't bother resolving the symbol.  */
    Elf_Type type = ebl_reloc_simple_type (mod->ebl, rtype);
    if (unlikely (type == ELF_T_NUM))
      return DWFL_E_BADRELTYPE;

    /* First, resolve the symbol to an absolute value.  */
    GElf_Addr value;

    if (symndx == STN_UNDEF)
      /* When strip removes a section symbol referring to a
	 section moved into the debuginfo file, it replaces
	 that symbol index in relocs with STN_UNDEF.  We
	 don't actually need the symbol, because those relocs
	 are always references relative to the nonallocated
	 debugging sections, which start at zero.  */
      value = 0;
    else
      {
	GElf_Sym sym;
	GElf_Word shndx;
	Dwfl_Error error = relocate_getsym (mod, relocated, reloc_symtab,
					    symndx, &sym, &shndx);
	if (unlikely (error != DWFL_E_NOERROR))
	  return error;

	if (shndx == SHN_UNDEF || shndx == SHN_COMMON)
	  {
	    /* Maybe we can figure it out anyway.  */
	    error = resolve_symbol (mod, reloc_symtab, &sym, shndx);
	    if (error != DWFL_E_NOERROR)
	      return error;
	  }

	value = sym.st_value;
      }

    /* These are the types we can relocate.  */
#define TYPES		DO_TYPE (BYTE, Byte); DO_TYPE (HALF, Half);	\
    DO_TYPE (WORD, Word); DO_TYPE (SWORD, Sword);			\
    DO_TYPE (XWORD, Xword); DO_TYPE (SXWORD, Sxword)
    size_t size;
    switch (type)
      {
#define DO_TYPE(NAME, Name)			\
	case ELF_T_##NAME:			\
	  size = sizeof (GElf_##Name);		\
	break
	TYPES;
#undef DO_TYPE
      default:
	return DWFL_E_BADRELTYPE;
      }

    if (offset + size > tdata->d_size)
      return DWFL_E_BADRELOFF;

#define DO_TYPE(NAME, Name) GElf_##Name Name;
    union { TYPES; } tmpbuf;
#undef DO_TYPE
    Elf_Data tmpdata =
      {
	.d_type = type,
	.d_buf = &tmpbuf,
	.d_size = size,
	.d_version = EV_CURRENT,
      };
    Elf_Data rdata =
      {
	.d_type = type,
	.d_buf = tdata->d_buf + offset,
	.d_size = size,
	.d_version = EV_CURRENT,
      };

    /* XXX check for overflow? */
    if (addend)
      {
	/* For the addend form, we have the value already.  */
	value += *addend;
	switch (type)
	  {
#define DO_TYPE(NAME, Name)			\
	    case ELF_T_##NAME:			\
	      tmpbuf.Name = value;		\
	    break
	    TYPES;
#undef DO_TYPE
	  default:
	    abort ();
	  }
      }
    else
      {
	/* Extract the original value and apply the reloc.  */
	Elf_Data *d = gelf_xlatetom (relocated, &tmpdata, &rdata,
				     ehdr->e_ident[EI_DATA]);
	if (d == NULL)
	  return DWFL_E_LIBELF;
	assert (d == &tmpdata);
	switch (type)
	  {
#define DO_TYPE(NAME, Name)				\
	    case ELF_T_##NAME:				\
	      tmpbuf.Name += (GElf_##Name) value;	\
	    break
	    TYPES;
#undef DO_TYPE
	  default:
	    abort ();
	  }
      }

    /* Now convert the relocated datum back to the target
       format.  This will write into rdata.d_buf, which
       points into the raw section data being relocated.  */
    Elf_Data *s = gelf_xlatetof (relocated, &rdata, &tmpdata,
				 ehdr->e_ident[EI_DATA]);
    if (s == NULL)
      return DWFL_E_LIBELF;
    assert (s == &rdata);

    /* We have applied this relocation!  */
    return DWFL_E_NOERROR;
  }

  /* Fetch the relocation section and apply each reloc in it.  */
  Elf_Data *reldata = elf_getdata (scn, NULL);
  if (reldata == NULL)
    return DWFL_E_LIBELF;

  Dwfl_Error result = DWFL_E_NOERROR;
  bool first_badreltype = true;
  inline void check_badreltype (void)
  {
    if (first_badreltype)
      {
	first_badreltype = false;
	if (ebl_get_elfmachine (mod->ebl) == EM_NONE)
	  /* This might be because ebl_openbackend failed to find
	     any libebl_CPU.so library.  Diagnose that clearly.  */
	  result = DWFL_E_UNKNOWN_MACHINE;
      }
  }

  size_t nrels = shdr->sh_size / shdr->sh_entsize;
  size_t complete = 0;
  if (shdr->sh_type == SHT_REL)
    for (size_t relidx = 0; !result && relidx < nrels; ++relidx)
      {
	GElf_Rel rel_mem, *r = gelf_getrel (reldata, relidx, &rel_mem);
	if (r == NULL)
	  return DWFL_E_LIBELF;
	result = relocate (r->r_offset, NULL,
			   GELF_R_TYPE (r->r_info),
			   GELF_R_SYM (r->r_info));
	check_badreltype ();
	if (partial)
	  switch (result)
	    {
	    case DWFL_E_NOERROR:
	      /* We applied the relocation.  Elide it.  */
	      memset (&rel_mem, 0, sizeof rel_mem);
	      gelf_update_rel (reldata, relidx, &rel_mem);
	      ++complete;
	      break;
	    case DWFL_E_BADRELTYPE:
	    case DWFL_E_RELUNDEF:
	      /* We couldn't handle this relocation.  Skip it.  */
	      result = DWFL_E_NOERROR;
	      break;
	    default:
	      break;
	    }
      }
  else
    for (size_t relidx = 0; !result && relidx < nrels; ++relidx)
      {
	GElf_Rela rela_mem, *r = gelf_getrela (reldata, relidx,
					       &rela_mem);
	if (r == NULL)
	  return DWFL_E_LIBELF;
	result = relocate (r->r_offset, &r->r_addend,
			   GELF_R_TYPE (r->r_info),
			   GELF_R_SYM (r->r_info));
	check_badreltype ();
	if (partial)
	  switch (result)
	    {
	    case DWFL_E_NOERROR:
	      /* We applied the relocation.  Elide it.  */
	      memset (&rela_mem, 0, sizeof rela_mem);
	      gelf_update_rela (reldata, relidx, &rela_mem);
	      ++complete;
	      break;
	    case DWFL_E_BADRELTYPE:
	    case DWFL_E_RELUNDEF:
	      /* We couldn't handle this relocation.  Skip it.  */
	      result = DWFL_E_NOERROR;
	      break;
	    default:
	      break;
	    }
      }

  if (likely (result == DWFL_E_NOERROR))
    {
      if (!partial || complete == nrels)
	/* Mark this relocation section as being empty now that we have
	   done its work.  This affects unstrip -R, so e.g. it emits an
	   empty .rela.debug_info along with a .debug_info that has
	   already been fully relocated.  */
	nrels = 0;
      else if (complete != 0)
	{
	  /* We handled some of the relocations but not all.
	     We've zeroed out the ones we processed.
	     Now remove them from the section.  */

	  size_t next = 0;
	  if (shdr->sh_type == SHT_REL)
	    for (size_t relidx = 0; relidx < nrels; ++relidx)
	      {
		GElf_Rel rel_mem;
		GElf_Rel *r = gelf_getrel (reldata, relidx, &rel_mem);
		if (r->r_info != 0 || r->r_offset != 0)
		  {
		    if (next != relidx)
		      gelf_update_rel (reldata, next, r);
		    ++next;
		  }
	      }
	  else
	    for (size_t relidx = 0; relidx < nrels; ++relidx)
	      {
		GElf_Rela rela_mem;
		GElf_Rela *r = gelf_getrela (reldata, relidx, &rela_mem);
		if (r->r_info != 0 || r->r_offset != 0 || r->r_addend != 0)
		  {
		    if (next != relidx)
		      gelf_update_rela (reldata, next, r);
		    ++next;
		  }
	      }
	  nrels = next;
	}

      shdr->sh_size = reldata->d_size = nrels * shdr->sh_entsize;
      gelf_update_shdr (scn, shdr);
    }

  return result;
}

Dwfl_Error
internal_function
__libdwfl_relocate (Dwfl_Module *mod, Elf *debugfile, bool debug)
{
  assert (mod->e_type == ET_REL);

  GElf_Ehdr ehdr_mem;
  const GElf_Ehdr *ehdr = gelf_getehdr (debugfile, &ehdr_mem);
  if (ehdr == NULL)
    return DWFL_E_LIBELF;

  size_t d_shstrndx;
  if (elf_getshstrndx (debugfile, &d_shstrndx) < 0)
    return DWFL_E_LIBELF;

  RELOC_SYMTAB_CACHE (reloc_symtab);

  /* Look at each section in the debuginfo file, and process the
     relocation sections for debugging sections.  */
  Dwfl_Error result = DWFL_E_NOERROR;
  Elf_Scn *scn = NULL;
  while (result == DWFL_E_NOERROR
	 && (scn = elf_nextscn (debugfile, scn)) != NULL)
    {
      GElf_Shdr shdr_mem;
      GElf_Shdr *shdr = gelf_getshdr (scn, &shdr_mem);

      if ((shdr->sh_type == SHT_REL || shdr->sh_type == SHT_RELA)
	  && shdr->sh_size != 0)
	{
	  /* It's a relocation section.  */

	  Elf_Scn *tscn = elf_getscn (debugfile, shdr->sh_info);
	  if (unlikely (tscn == NULL))
	    result = DWFL_E_LIBELF;
	  else
	    result = relocate_section (mod, debugfile, ehdr, d_shstrndx,
				       &reloc_symtab, scn, shdr, tscn,
				       debug, !debug);
	}
    }

  return result;
}

Dwfl_Error
internal_function
__libdwfl_relocate_section (Dwfl_Module *mod, Elf *relocated,
			    Elf_Scn *relocscn, Elf_Scn *tscn, bool partial)
{
  GElf_Ehdr ehdr_mem;
  GElf_Shdr shdr_mem;

  RELOC_SYMTAB_CACHE (reloc_symtab);

  size_t shstrndx;
  if (elf_getshstrndx (relocated, &shstrndx) < 0)
    return DWFL_E_LIBELF;

  return (__libdwfl_module_getebl (mod)
	  ?: relocate_section (mod, relocated,
			       gelf_getehdr (relocated, &ehdr_mem), shstrndx,
			       &reloc_symtab,
			       relocscn, gelf_getshdr (relocscn, &shdr_mem),
			       tscn, false, partial));
}
