
#ifndef __IR_KEYMAP_H__

#define __IR_KEYMAP_H__


/* IR IO Mapping Config */
//0--PIOB10, 1--PIOB17
#define IR_IO_MAPPING			1

/*IR Key Match Config*/
#define IR_CHECK_ADDR_CODE  
#define IR_ADDR_CODE		    (0x9f00)
#define INPUT_VALUE_MAPPING

/*
0x0d -- KEY_ESC
0x12 -- KEY_UP
0x18 -- KEY_DOWN
0x16 -- KEY_LEFT
0x14 -- KEY_RIGHT
0x15 -- KEY_ENTER
0x1F -- KEY_VOLUMEDOWN
0x1E -- KEY_VOLUMEUP
0x00 -- KEY_POWER
*/

#ifdef INPUT_VALUE_MAPPING
static const unsigned int ir_keycodes[]=
{
	[0x00] = KEY_RESERVED,		
	[0x01] = KEY_UP,			
	[0x02] = KEY_RESERVED,			
	[0x03] = KEY_RESERVED,				
	[0x04] = KEY_RESERVED,			
	[0x05] = KEY_RESERVED,			
	[0x06] = KEY_RESERVED,			
	[0x07] = KEY_RESERVED,	
	[0x08] = KEY_RESERVED,		
	[0x09] = KEY_DOWN,			
	[0x0A] = KEY_RESERVED,			
	[0x0B] = KEY_RESERVED,				
	[0x0C] = KEY_RESERVED,			
	[0x0D] = KEY_ESC,			
	[0x0E] = KEY_RESERVED,			
	[0x0F] = KEY_MENU,	
	[0x10] = KEY_RESERVED,		
	[0x11] = KEY_RIGHT,			
	[0x12] = KEY_POWER,			
	[0x13] = KEY_RESERVED,				
	[0x14] = KEY_RESERVED,			
	[0x15] = KEY_RESERVED,			
	[0x16] = KEY_RESERVED,			
	[0x17] = KEY_RESERVED,	
	[0x18] = KEY_RESERVED,		
	[0x19] = KEY_LEFT,			
	[0x1A] = KEY_RESERVED,			
	[0x1B] = KEY_RESERVED,				
	[0x1C] = KEY_VOLUMEUP,			
	[0x1D] = KEY_RESERVED,			
	[0x1E] = KEY_RESERVED,			
	[0x1F] = KEY_RESERVED,	
	[0x20] = KEY_RESERVED,		
	[0x21] = KEY_RESERVED,			
	[0x22] = KEY_RESERVED,			
	[0x23] = KEY_RESERVED,				
	[0x24] = KEY_RESERVED,			
	[0x25] = KEY_RESERVED,			
	[0x26] = KEY_RESERVED,			
	[0x27] = KEY_RESERVED,	
	[0x28] = KEY_RESERVED,		
	[0x29] = KEY_RESERVED,			
	[0x2A] = KEY_RESERVED,			
	[0x2B] = KEY_RESERVED,				
	[0x2C] = KEY_RESERVED,			
	[0x2D] = KEY_RESERVED,			
	[0x2E] = KEY_RESERVED,			
	[0x2F] = KEY_RESERVED,	
	[0x30] = KEY_RESERVED,		
	[0x31] = KEY_RESERVED,			
	[0x32] = KEY_RESERVED,			
	[0x33] = KEY_RESERVED,				
	[0x34] = KEY_RESERVED,			
	[0x35] = KEY_RESERVED,			
	[0x36] = KEY_RESERVED,			
	[0x37] = KEY_RESERVED,	
	[0x38] = KEY_RESERVED,		
	[0x39] = KEY_RESERVED,			
	[0x3A] = KEY_RESERVED,			
	[0x3B] = KEY_RESERVED,				
	[0x3C] = KEY_RESERVED,			
	[0x3D] = KEY_RESERVED,			
	[0x3E] = KEY_RESERVED,			
	[0x3F] = KEY_RESERVED,	
	[0x40] = KEY_ENTER,		
	[0x41] = KEY_RESERVED,			
	[0x42] = KEY_RESERVED,			
	[0x43] = KEY_RESERVED,				
	[0x44] = KEY_RESERVED,			
	[0x45] = KEY_RESERVED,			
	[0x46] = KEY_RESERVED,			
	[0x47] = KEY_RESERVED,	
	[0x48] = KEY_RESERVED,		
	[0x49] = KEY_RESERVED,			
	[0x4A] = KEY_RESERVED,			
	[0x4B] = KEY_RESERVED,				
	[0x4C] = KEY_RESERVED,			
	[0x4D] = KEY_RESERVED,			
	[0x4E] = KEY_RESERVED,			
	[0x4F] = KEY_RESERVED,	
	[0x50] = KEY_RESERVED,		
	[0x51] = KEY_RESERVED,			
	[0x52] = KEY_RESERVED,			
	[0x53] = KEY_RESERVED,				
	[0x54] = KEY_RESERVED,			
	[0x55] = KEY_RESERVED,			
	[0x56] = KEY_RESERVED,			
	[0x57] = KEY_RESERVED,	
	[0x58] = KEY_RESERVED,		
	[0x59] = KEY_RESERVED,			
	[0x5A] = KEY_RESERVED,			
	[0x5B] = KEY_RESERVED,				
	[0x5C] = KEY_RESERVED,			
	[0x5D] = KEY_RESERVED,			
	[0x5E] = KEY_RESERVED,			
	[0x5F] = KEY_RESERVED,	
	[0x60] = KEY_RESERVED,		
	[0x61] = KEY_RESERVED,			
	[0x62] = KEY_RESERVED,			
	[0x63] = KEY_RESERVED,				
	[0x64] = KEY_RESERVED,			
	[0x65] = KEY_RESERVED,			
	[0x66] = KEY_RESERVED,			
	[0x67] = KEY_RESERVED,	
	[0x68] = KEY_RESERVED,		
	[0x69] = KEY_RESERVED,			
	[0x6A] = KEY_RESERVED,			
	[0x6B] = KEY_RESERVED,				
	[0x6C] = KEY_RESERVED,			
	[0x6D] = KEY_RESERVED,			
	[0x6E] = KEY_RESERVED,			
	[0x6F] = KEY_RESERVED,	
	[0x70] = KEY_RESERVED,		
	[0x71] = KEY_RESERVED,			
	[0x72] = KEY_RESERVED,			
	[0x73] = KEY_RESERVED,				
	[0x74] = KEY_RESERVED,			
	[0x75] = KEY_RESERVED,			
	[0x76] = KEY_RESERVED,			
	[0x77] = KEY_RESERVED,	
	[0x78] = KEY_RESERVED,		
	[0x79] = KEY_RESERVED,			
	[0x7A] = KEY_RESERVED,			
	[0x7B] = KEY_RESERVED,				
	[0x7C] = KEY_RESERVED,			
	[0x7D] = KEY_RESERVED,			
	[0x7E] = KEY_RESERVED,			
	[0x7F] = KEY_VOLUMEDOWN,
	[0x80] = KEY_RESERVED,		
	[0x81] = KEY_RESERVED,			
	[0x82] = KEY_RESERVED,			
	[0x83] = KEY_RESERVED,				
	[0x84] = KEY_RESERVED,			
	[0x85] = KEY_RESERVED,			
	[0x86] = KEY_RESERVED,			
	[0x87] = KEY_RESERVED,	
	[0x88] = KEY_RESERVED,		
	[0x89] = KEY_RESERVED,			
	[0x8A] = KEY_RESERVED,			
	[0x8B] = KEY_RESERVED,				
	[0x8C] = KEY_RESERVED,			
	[0x8D] = KEY_RESERVED,			
	[0x8E] = KEY_RESERVED,			
	[0x8F] = KEY_RESERVED,	
	[0x90] = KEY_RESERVED,		
	[0x91] = KEY_RESERVED,			
	[0x92] = KEY_RESERVED,			
	[0x93] = KEY_RESERVED,				
	[0x94] = KEY_RESERVED,			
	[0x95] = KEY_RESERVED,			
	[0x96] = KEY_RESERVED,			
	[0x97] = KEY_RESERVED,	
	[0x98] = KEY_RESERVED,		
	[0x99] = KEY_RESERVED,			
	[0x9A] = KEY_RESERVED,			
	[0x9B] = KEY_RESERVED,				
	[0x9C] = KEY_RESERVED,			
	[0x9D] = KEY_RESERVED,			
	[0x9E] = KEY_RESERVED,			
	[0x9F] = KEY_RESERVED,	
	[0xA0] = KEY_RESERVED,		
	[0xA1] = KEY_RESERVED,			
	[0xA2] = KEY_RESERVED,			
	[0xA3] = KEY_RESERVED,				
	[0xA4] = KEY_RESERVED,			
	[0xA5] = KEY_RESERVED,			
	[0xA6] = KEY_RESERVED,			
	[0xA7] = KEY_RESERVED,	
	[0xA8] = KEY_RESERVED,		
	[0xA9] = KEY_RESERVED,			
	[0xAA] = KEY_RESERVED,			
	[0xAB] = KEY_RESERVED,				
	[0xAC] = KEY_RESERVED,			
	[0xAD] = KEY_RESERVED,			
	[0xAE] = KEY_RESERVED,			
	[0xAF] = KEY_RESERVED,	
	[0xB0] = KEY_RESERVED,		
	[0xB1] = KEY_RESERVED,			
	[0xB2] = KEY_RESERVED,			
	[0xB3] = KEY_RESERVED,				
	[0xB4] = KEY_RESERVED,			
	[0xB5] = KEY_RESERVED,			
	[0xB6] = KEY_RESERVED,			
	[0xB7] = KEY_RESERVED,	
	[0xB8] = KEY_RESERVED,		
	[0xB9] = KEY_RESERVED,			
	[0xBA] = KEY_RESERVED,			
	[0xBB] = KEY_RESERVED,				
	[0xBC] = KEY_RESERVED,			
	[0xBD] = KEY_RESERVED,			
	[0xBE] = KEY_RESERVED,			
	[0xBF] = KEY_RESERVED,	
	[0xC0] = KEY_RESERVED,		
	[0xC1] = KEY_RESERVED,			
	[0xC2] = KEY_RESERVED,			
	[0xC3] = KEY_RESERVED,				
	[0xC4] = KEY_RESERVED,			
	[0xC5] = KEY_RESERVED,			
	[0xC6] = KEY_RESERVED,			
	[0xC7] = KEY_RESERVED,	
	[0xC8] = KEY_RESERVED,		
	[0xC9] = KEY_RESERVED,			
	[0xCA] = KEY_RESERVED,			
	[0xCB] = KEY_RESERVED,				
	[0xCC] = KEY_RESERVED,			
	[0xCD] = KEY_RESERVED,			
	[0xCE] = KEY_RESERVED,			
	[0xCF] = KEY_RESERVED,	
	[0xD0] = KEY_RESERVED,		
	[0xD1] = KEY_RESERVED,			
	[0xD2] = KEY_RESERVED,			
	[0xD3] = KEY_RESERVED,				
	[0xD4] = KEY_RESERVED,			
	[0xD5] = KEY_RESERVED,			
	[0xD6] = KEY_RESERVED,			
	[0xD7] = KEY_RESERVED,	
	[0xD8] = KEY_RESERVED,		
	[0xD9] = KEY_RESERVED,			
	[0xDA] = KEY_RESERVED,			
	[0xDB] = KEY_RESERVED,				
	[0xDC] = KEY_RESERVED,			
	[0xDD] = KEY_RESERVED,			
	[0xDE] = KEY_RESERVED,			
	[0xDF] = KEY_RESERVED,	
	[0xE0] = KEY_RESERVED,		
	[0xE1] = KEY_RESERVED,			
	[0xE2] = KEY_RESERVED,			
	[0xE3] = KEY_RESERVED,				
	[0xE4] = KEY_RESERVED,			
	[0xE5] = KEY_RESERVED,			
	[0xE6] = KEY_RESERVED,			
	[0xE7] = KEY_RESERVED,	
	[0xE8] = KEY_RESERVED,		
	[0xE9] = KEY_RESERVED,			
	[0xEA] = KEY_RESERVED,			
	[0xEB] = KEY_RESERVED,				
	[0xEC] = KEY_RESERVED,			
	[0xED] = KEY_RESERVED,			
	[0xEE] = KEY_RESERVED,			
	[0xEF] = KEY_RESERVED,	
	[0xF0] = KEY_RESERVED,		
	[0xF1] = KEY_RESERVED,			
	[0xF2] = KEY_RESERVED,			
	[0xF3] = KEY_RESERVED,				
	[0xF4] = KEY_RESERVED,			
	[0xF5] = KEY_RESERVED,			
	[0xF6] = KEY_RESERVED,			
	[0xF7] = KEY_RESERVED,	
	[0xF8] = KEY_RESERVED,		
	[0xF9] = KEY_RESERVED,			
	[0xFA] = KEY_RESERVED,			
	[0xFB] = KEY_RESERVED,				
	[0xFC] = KEY_RESERVED,			
	[0xFD] = KEY_RESERVED,			
	[0xFE] = KEY_RESERVED,			
	[0xFF] = KEY_RESERVED				
};
#else
static const unsigned int ir_keycodes[]=
{
	[0x00] = 0xff,
	[0x01] = 0x01,	
	[0x02] = 0x02,	
	[0x03] = 0x03,		
	[0x04] = 0x04,	
	[0x05] = 0x05,	
	[0x06] = 0x06,	
	[0x07] = 0x07,
	[0x08] = 0x08,
	[0x09] = 0x09,	
	[0x0A] = 0x0A,	
	[0x0B] = 0x0B,		
	[0x0C] = 0x0C,	
	[0x0D] = 0x0D,
	[0x0E] = 0x0E,	
	[0x0F] = 0x0F,
	[0x10] = 0x10,
	[0x11] = 0x11,	
	[0x12] = 0x12,
	[0x13] = 0x13,		
	[0x14] = 0x14,
	[0x15] = 0x15,
	[0x16] = 0x16,
	[0x17] = 0x17,
	[0x18] = 0x18,
	[0x19] = 0x19,	
	[0x1A] = 0x1A,	
	[0x1B] = 0x1B,		
	[0x1C] = 0x1C,	
	[0x1D] = 0x1D,	
	[0x1E] = 0x1E,	
	[0x1F] = 0x1F,	
	[0x20] = 0x20,
	[0x21] = 0x21,	
	[0x22] = 0x22,	
	[0x23] = 0x23,		
	[0x24] = 0x24,	
	[0x25] = 0x25,	
	[0x26] = 0x26,	
	[0x27] = 0x27,
	[0x28] = 0x28,
	[0x29] = 0x29,	
	[0x2A] = 0x2A,	
	[0x2B] = 0x2B,		
	[0x2C] = 0x2C,	
	[0x2D] = 0x2D,	
	[0x2E] = 0x2E,	
	[0x2F] = 0x2F,
	[0x30] = 0x30,
	[0x31] = 0x31,	
	[0x32] = 0x32,	
	[0x33] = 0x33,		
	[0x34] = 0x34,	
	[0x35] = 0x35,	
	[0x36] = 0x36,	
	[0x37] = 0x37,
	[0x38] = 0x38,
	[0x39] = 0x39,	
	[0x3A] = 0x3A,	
	[0x3B] = 0x3B,		
	[0x3C] = 0x3C,	
	[0x3D] = 0x3D,	
	[0x3E] = 0x3E,	
	[0x3F] = 0x3F,
	[0x40] = 0x40,
	[0x41] = 0x41,	
	[0x42] = 0x42,	
	[0x43] = 0x43,		
	[0x44] = 0x44,	
	[0x45] = 0x45,	
	[0x46] = 0x46,	
	[0x47] = 0x47,
	[0x48] = 0x48,
	[0x49] = 0x49,	
	[0x4A] = 0x4A,	
	[0x4B] = 0x4B,		
	[0x4C] = 0x4C,	
	[0x4D] = 0x4D,	
	[0x4E] = 0x4E,	
	[0x4F] = 0x4F,
	[0x50] = 0x50,
	[0x51] = 0x51,	
	[0x52] = 0x52,	
	[0x53] = 0x53,		
	[0x54] = 0x54,	
	[0x55] = 0x55,	
	[0x56] = 0x56,	
	[0x57] = 0x57,
	[0x58] = 0x58,
	[0x59] = 0x59,	
	[0x5A] = 0x5A,	
	[0x5B] = 0x5B,		
	[0x5C] = 0x5C,	
	[0x5D] = 0x5D,	
	[0x5E] = 0x5E,	
	[0x5F] = 0x5F,
	[0x60] = 0x60,
	[0x61] = 0x61,	
	[0x62] = 0x62,	
	[0x63] = 0x63,		
	[0x64] = 0x64,	
	[0x65] = 0x65,	
	[0x66] = 0x66,	
	[0x67] = 0x67,
	[0x68] = 0x68,
	[0x69] = 0x69,	
	[0x6A] = 0x6A,	
	[0x6B] = 0x6B,		
	[0x6C] = 0x6C,	
	[0x6D] = 0x6D,	
	[0x6E] = 0x6E,	
	[0x6F] = 0x6F,
	[0x70] = 0x70,
	[0x71] = 0x71,	
	[0x72] = 0x72,	
	[0x73] = 0x73,		
	[0x74] = 0x74,	
	[0x75] = 0x75,	
	[0x76] = 0x76,	
	[0x77] = 0x77,
	[0x78] = 0x78,
	[0x79] = 0x79,	
	[0x7A] = 0x7A,	
	[0x7B] = 0x7B,		
	[0x7C] = 0x7C,	
	[0x7D] = 0x7D,	
	[0x7E] = 0x7E,	
	[0x7F] = 0x7F,
	[0x80] = 0x80,
	[0x81] = 0x81,	
	[0x82] = 0x82,	
	[0x83] = 0x83,		
	[0x84] = 0x84,	
	[0x85] = 0x85,	
	[0x86] = 0x86,	
	[0x87] = 0x87,
	[0x88] = 0x88,
	[0x89] = 0x89,	
	[0x8A] = 0x8A,	
	[0x8B] = 0x8B,		
	[0x8C] = 0x8C,	
	[0x8D] = 0x8D,	
	[0x8E] = 0x8E,	
	[0x8F] = 0x8F,
	[0x90] = 0x90,
	[0x91] = 0x91,	
	[0x92] = 0x92,	
	[0x93] = 0x93,		
	[0x94] = 0x94,	
	[0x95] = 0x95,	
	[0x96] = 0x96,	
	[0x97] = 0x97,
	[0x98] = 0x98,
	[0x99] = 0x99,	
	[0x9A] = 0x9A,	
	[0x9B] = 0x9B,		
	[0x9C] = 0x9C,	
	[0x9D] = 0x9D,	
	[0x9E] = 0x9E,	
	[0x9F] = 0x9F,
	[0xA0] = 0xA0,
	[0xA1] = 0xA1,	
	[0xA2] = 0xA2,	
	[0xA3] = 0xA3,		
	[0xA4] = 0xA4,	
	[0xA5] = 0xA5,	
	[0xA6] = 0xA6,	
	[0xA7] = 0xA7,
	[0xA8] = 0xA8,
	[0xA9] = 0xA9,	
	[0xAA] = 0xAA,	
	[0xAB] = 0xAB,		
	[0xAC] = 0xAC,	
	[0xAD] = 0xAD,	
	[0xAE] = 0xAE,	
	[0xAF] = 0xAF,
	[0xB0] = 0xB0,
	[0xB1] = 0xB1,	
	[0xB2] = 0xB2,	
	[0xB3] = 0xB3,		
	[0xB4] = 0xB4,	
	[0xB5] = 0xB5,	
	[0xB6] = 0xB6,	
	[0xB7] = 0xB7,
	[0xB8] = 0xB8,
	[0xB9] = 0xB9,	
	[0xBA] = 0xBA,	
	[0xBB] = 0xBB,		
	[0xBC] = 0xBC,	
	[0xBD] = 0xBD,	
	[0xBE] = 0xBE,	
	[0xBF] = 0xBF,
	[0xC0] = 0xC0,
	[0xC1] = 0xC1,	
	[0xC2] = 0xC2,	
	[0xC3] = 0xC3,		
	[0xC4] = 0xC4,	
	[0xC5] = 0xC5,	
	[0xC6] = 0xC6,	
	[0xC7] = 0xC7,
	[0xC8] = 0xC8,
	[0xC9] = 0xC9,	
	[0xCA] = 0xCA,	
	[0xCB] = 0xCB,		
	[0xCC] = 0xCC,	
	[0xCD] = 0xCD,	
	[0xCE] = 0xCE,	
	[0xCF] = 0xCF,
	[0xD0] = 0xD0,
	[0xD1] = 0xD1,	
	[0xD2] = 0xD2,	
	[0xD3] = 0xD3,		
	[0xD4] = 0xD4,	
	[0xD5] = 0xD5,	
	[0xD6] = 0xD6,	
	[0xD7] = 0xD7,
	[0xD8] = 0xD8,
	[0xD9] = 0xD9,	
	[0xDA] = 0xDA,	
	[0xDB] = 0xDB,		
	[0xDC] = 0xDC,	
	[0xDD] = 0xDD,	
	[0xDE] = 0xDE,	
	[0xDF] = 0xDF,
	[0xE0] = 0xE0,
	[0xE1] = 0xE1,	
	[0xE2] = 0xE2,	
	[0xE3] = 0xE3,		
	[0xE4] = 0xE4,	
	[0xE5] = 0xE5,	
	[0xE6] = 0xE6,	
	[0xE7] = 0xE7,
	[0xE8] = 0xE8,
	[0xE9] = 0xE9,	
	[0xEA] = 0xEA,	
	[0xEB] = 0xEB,		
	[0xEC] = 0xEC,	
	[0xED] = 0xED,	
	[0xEE] = 0xEE,	
	[0xEF] = 0xEF,
	[0xF0] = 0xF0,
	[0xF1] = 0xF1,	
	[0xF2] = 0xF2,	
	[0xF3] = 0xF3,		
	[0xF4] = 0xF4,	
	[0xF5] = 0xF5,	
	[0xF6] = 0xF6,	
	[0xF7] = 0xF7,
	[0xF8] = 0xF8,
	[0xF9] = 0xF9,	
	[0xFA] = 0xFA,	
	[0xFB] = 0xFB,		
	[0xFC] = 0xFC,	
	[0xFD] = 0xFD,	
	[0xFE] = 0xFE,	
	[0xFF] = 0xFF     	
};

#endif

#endif /*__IR_KEYMAP_H__*/
