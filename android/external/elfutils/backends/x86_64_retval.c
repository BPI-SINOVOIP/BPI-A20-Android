/* Function return value location for Linux/x86-64 ABI.
   Copyright (C) 2005, 2007 Red Hat, Inc.
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

   Red Hat elfutils is an included package of the Open Invention Network.
   An included package of the Open Invention Network is a package for which
   Open Invention Network licensees cross-license their patents.  No patent
   license is granted, either expressly or impliedly, by designation as an
   included package.  Should you wish to participate in the Open Invention
   Network licensing program, please visit www.openinventionnetwork.com
   <http://www.openinventionnetwork.com>.  */

#ifdef HAVE_CONFIG_H
# include <config.h>
#endif

#include <assert.h>
#include <dwarf.h>

#define BACKEND x86_64_
#include "libebl_CPU.h"


/* %rax, or pair %rax, %rdx.  */
static const Dwarf_Op loc_intreg[] =
  {
    { .atom = DW_OP_reg0 }, { .atom = DW_OP_piece, .number = 8 },
    { .atom = DW_OP_reg1 }, { .atom = DW_OP_piece, .number = 8 },
  };
#define nloc_intreg	1
#define nloc_intregpair	4

/* %st(0), or pair %st(0), %st(1).  */
static const Dwarf_Op loc_x87reg[] =
  {
    { .atom = DW_OP_regx, .number = 33 },
    { .atom = DW_OP_piece, .number = 10 },
    { .atom = DW_OP_regx, .number = 34 },
    { .atom = DW_OP_piece, .number = 10 },
  };
#define nloc_x87reg	1
#define nloc_x87regpair	4

/* %xmm0, or pair %xmm0, %xmm1.  */
static const Dwarf_Op loc_ssereg[] =
  {
    { .atom = DW_OP_reg17 }, { .atom = DW_OP_piece, .number = 16 },
    { .atom = DW_OP_reg18 }, { .atom = DW_OP_piece, .number = 16 },
  };
#define nloc_ssereg	1
#define nloc_sseregpair	4

/* The return value is a structure and is actually stored in stack space
   passed in a hidden argument by the caller.  But, the compiler
   helpfully returns the address of that space in %rax.  */
static const Dwarf_Op loc_aggregate[] =
  {
    { .atom = DW_OP_breg0, .number = 0 }
  };
#define nloc_aggregate 1


int
x86_64_return_value_location (Dwarf_Die *functypedie, const Dwarf_Op **locp)
{
  /* Start with the function's type, and get the DW_AT_type attribute,
     which is the type of the return value.  */

  Dwarf_Attribute attr_mem;
  Dwarf_Attribute *attr = dwarf_attr_integrate (functypedie, DW_AT_type,
						&attr_mem);
  if (attr == NULL)
    /* The function has no return value, like a `void' function in C.  */
    return 0;

  Dwarf_Die die_mem;
  Dwarf_Die *typedie = dwarf_formref_die (attr, &die_mem);
  int tag = dwarf_tag (typedie);

  /* Follow typedefs and qualifiers to get to the actual type.  */
  while (tag == DW_TAG_typedef
	 || tag == DW_TAG_const_type || tag == DW_TAG_volatile_type
	 || tag == DW_TAG_restrict_type || tag == DW_TAG_mutable_type)
    {
      attr = dwarf_attr_integrate (typedie, DW_AT_type, &attr_mem);
      typedie = dwarf_formref_die (attr, &die_mem);
      tag = dwarf_tag (typedie);
    }

  Dwarf_Word size;
  switch (tag)
    {
    case -1:
      return -1;

    case DW_TAG_subrange_type:
      if (! dwarf_hasattr_integrate (typedie, DW_AT_byte_size))
	{
	  attr = dwarf_attr_integrate (typedie, DW_AT_type, &attr_mem);
	  typedie = dwarf_formref_die (attr, &die_mem);
	  tag = dwarf_tag (typedie);
	}
      /* Fall through.  */

    case DW_TAG_base_type:
    case DW_TAG_enumeration_type:
    case DW_TAG_pointer_type:
    case DW_TAG_ptr_to_member_type:
      if (dwarf_formudata (dwarf_attr_integrate (typedie, DW_AT_byte_size,
						 &attr_mem), &size) != 0)
	{
	  if (tag == DW_TAG_pointer_type || tag == DW_TAG_ptr_to_member_type)
	    size = 8;
	  else
	    return -1;
	}
      if (tag == DW_TAG_base_type)
	{
	  Dwarf_Word encoding;
	  if (dwarf_formudata (dwarf_attr_integrate (typedie, DW_AT_encoding,
						     &attr_mem),
			       &encoding) != 0)
	    return -1;

	  switch (encoding)
	    {
	    case DW_ATE_complex_float:
	      switch (size)
		{
		case 4 * 2:	/* complex float */
		case 8 * 2:	/* complex double */
		  *locp = loc_ssereg;
		  return nloc_sseregpair;
		case 16 * 2:	/* complex long double */
		  *locp = loc_x87reg;
		  return nloc_x87regpair;
		}
	      return -2;

	    case DW_ATE_float:
	      switch (size)
		{
		case 4:	/* float */
		case 8:	/* double */
		  *locp = loc_ssereg;
		  return nloc_ssereg;
		case 16:	/* long double */
		  /* XXX distinguish __float128, which is sseregpair?? */
		  *locp = loc_x87reg;
		  return nloc_x87reg;
		}
	      return -2;
	    }
	}

    intreg:
      *locp = loc_intreg;
      if (size <= 8)
	return nloc_intreg;
      if (size <= 16)
	return nloc_intregpair;

    large:
      *locp = loc_aggregate;
      return nloc_aggregate;

    case DW_TAG_structure_type:
    case DW_TAG_class_type:
    case DW_TAG_union_type:
    case DW_TAG_array_type:
      if (dwarf_formudata (dwarf_attr_integrate (typedie, DW_AT_byte_size,
						 &attr_mem), &size) != 0)
	return -1;
      if (size > 16)
	goto large;

      /* XXX
	 Must examine the fields in picayune ways to determine the
	 actual answer.  This will be right for small C structs
	 containing integer types and similarly simple cases.
      */

      goto intreg;
    }

  /* XXX We don't have a good way to return specific errors from ebl calls.
     This value means we do not understand the type, but it is well-formed
     DWARF and might be valid.  */
  return -2;
}
