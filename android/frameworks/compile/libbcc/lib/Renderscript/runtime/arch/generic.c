/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


#include "rs_types.rsh"

extern short __attribute__((overloadable, always_inline)) rsClamp(short amount, short low, short high);
extern float4 __attribute__((overloadable)) clamp(float4 amount, float4 low, float4 high);
extern uchar4 __attribute__((overloadable)) convert_uchar4(short4);
extern float __attribute__((overloadable)) sqrt(float);


/*
 * CLAMP
 */

extern float __attribute__((overloadable)) clamp(float amount, float low, float high) {
    return amount < low ? low : (amount > high ? high : amount);
}

extern float2 __attribute__((overloadable)) clamp(float2 amount, float2 low, float2 high) {
    float2 r;
    r.x = amount.x < low.x ? low.x : (amount.x > high.x ? high.x : amount.x);
    r.y = amount.y < low.y ? low.y : (amount.y > high.y ? high.y : amount.y);
    return r;
}

extern float3 __attribute__((overloadable)) clamp(float3 amount, float3 low, float3 high) {
    float3 r;
    r.x = amount.x < low.x ? low.x : (amount.x > high.x ? high.x : amount.x);
    r.y = amount.y < low.y ? low.y : (amount.y > high.y ? high.y : amount.y);
    r.z = amount.z < low.z ? low.z : (amount.z > high.z ? high.z : amount.z);
    return r;
}

extern float4 __attribute__((overloadable)) clamp(float4 amount, float4 low, float4 high) {
    float4 r;
    r.x = amount.x < low.x ? low.x : (amount.x > high.x ? high.x : amount.x);
    r.y = amount.y < low.y ? low.y : (amount.y > high.y ? high.y : amount.y);
    r.z = amount.z < low.z ? low.z : (amount.z > high.z ? high.z : amount.z);
    r.w = amount.w < low.w ? low.w : (amount.w > high.w ? high.w : amount.w);
    return r;
}

extern float2 __attribute__((overloadable)) clamp(float2 amount, float low, float high) {
    float2 r;
    r.x = amount.x < low ? low : (amount.x > high ? high : amount.x);
    r.y = amount.y < low ? low : (amount.y > high ? high : amount.y);
    return r;
}

extern float3 __attribute__((overloadable)) clamp(float3 amount, float low, float high) {
    float3 r;
    r.x = amount.x < low ? low : (amount.x > high ? high : amount.x);
    r.y = amount.y < low ? low : (amount.y > high ? high : amount.y);
    r.z = amount.z < low ? low : (amount.z > high ? high : amount.z);
    return r;
}

extern float4 __attribute__((overloadable)) clamp(float4 amount, float low, float high) {
    float4 r;
    r.x = amount.x < low ? low : (amount.x > high ? high : amount.x);
    r.y = amount.y < low ? low : (amount.y > high ? high : amount.y);
    r.z = amount.z < low ? low : (amount.z > high ? high : amount.z);
    r.w = amount.w < low ? low : (amount.w > high ? high : amount.w);
    return r;
}


/*
 * FMAX
 */

extern float __attribute__((overloadable)) fmax(float v1, float v2) {
    return v1 > v2 ? v1 : v2;
}

extern float2 __attribute__((overloadable)) fmax(float2 v1, float2 v2) {
    float2 r;
    r.x = v1.x > v2.x ? v1.x : v2.x;
    r.y = v1.y > v2.y ? v1.y : v2.y;
    return r;
}

extern float3 __attribute__((overloadable)) fmax(float3 v1, float3 v2) {
    float3 r;
    r.x = v1.x > v2.x ? v1.x : v2.x;
    r.y = v1.y > v2.y ? v1.y : v2.y;
    r.z = v1.z > v2.z ? v1.z : v2.z;
    return r;
}

extern float4 __attribute__((overloadable)) fmax(float4 v1, float4 v2) {
    float4 r;
    r.x = v1.x > v2.x ? v1.x : v2.x;
    r.y = v1.y > v2.y ? v1.y : v2.y;
    r.z = v1.z > v2.z ? v1.z : v2.z;
    r.w = v1.w > v2.w ? v1.w : v2.w;
    return r;
}

extern float2 __attribute__((overloadable)) fmax(float2 v1, float v2) {
    float2 r;
    r.x = v1.x > v2 ? v1.x : v2;
    r.y = v1.y > v2 ? v1.y : v2;
    return r;
}

extern float3 __attribute__((overloadable)) fmax(float3 v1, float v2) {
    float3 r;
    r.x = v1.x > v2 ? v1.x : v2;
    r.y = v1.y > v2 ? v1.y : v2;
    r.z = v1.z > v2 ? v1.z : v2;
    return r;
}

extern float4 __attribute__((overloadable)) fmax(float4 v1, float v2) {
    float4 r;
    r.x = v1.x > v2 ? v1.x : v2;
    r.y = v1.y > v2 ? v1.y : v2;
    r.z = v1.z > v2 ? v1.z : v2;
    r.w = v1.w > v2 ? v1.w : v2;
    return r;
}

extern float __attribute__((overloadable)) fmin(float v1, float v2) {
    return v1 < v2 ? v1 : v2;
}


/*
 * FMIN
 */
extern float2 __attribute__((overloadable)) fmin(float2 v1, float2 v2) {
    float2 r;
    r.x = v1.x < v2.x ? v1.x : v2.x;
    r.y = v1.y < v2.y ? v1.y : v2.y;
    return r;
}

extern float3 __attribute__((overloadable)) fmin(float3 v1, float3 v2) {
    float3 r;
    r.x = v1.x < v2.x ? v1.x : v2.x;
    r.y = v1.y < v2.y ? v1.y : v2.y;
    r.z = v1.z < v2.z ? v1.z : v2.z;
    return r;
}

extern float4 __attribute__((overloadable)) fmin(float4 v1, float4 v2) {
    float4 r;
    r.x = v1.x < v2.x ? v1.x : v2.x;
    r.y = v1.y < v2.y ? v1.y : v2.y;
    r.z = v1.z < v2.z ? v1.z : v2.z;
    r.w = v1.w < v2.w ? v1.w : v2.w;
    return r;
}

extern float2 __attribute__((overloadable)) fmin(float2 v1, float v2) {
    float2 r;
    r.x = v1.x < v2 ? v1.x : v2;
    r.y = v1.y < v2 ? v1.y : v2;
    return r;
}

extern float3 __attribute__((overloadable)) fmin(float3 v1, float v2) {
    float3 r;
    r.x = v1.x < v2 ? v1.x : v2;
    r.y = v1.y < v2 ? v1.y : v2;
    r.z = v1.z < v2 ? v1.z : v2;
    return r;
}

extern float4 __attribute__((overloadable)) fmin(float4 v1, float v2) {
    float4 r;
    r.x = v1.x < v2 ? v1.x : v2;
    r.y = v1.y < v2 ? v1.y : v2;
    r.z = v1.z < v2 ? v1.z : v2;
    r.w = v1.w < v2 ? v1.w : v2;
    return r;
}


/*
 * MAX
 */

extern char __attribute__((overloadable)) max(char v1, char v2) {
    return v1 > v2 ? v1 : v2;
}

extern char2 __attribute__((overloadable)) max(char2 v1, char2 v2) {
    char2 r;
    r.x = v1.x > v2.x ? v1.x : v2.x;
    r.y = v1.y > v2.y ? v1.y : v2.y;
    return r;
}

extern char3 __attribute__((overloadable)) max(char3 v1, char3 v2) {
    char3 r;
    r.x = v1.x > v2.x ? v1.x : v2.x;
    r.y = v1.y > v2.y ? v1.y : v2.y;
    r.z = v1.z > v2.z ? v1.z : v2.z;
    return r;
}

extern char4 __attribute__((overloadable)) max(char4 v1, char4 v2) {
    char4 r;
    r.x = v1.x > v2.x ? v1.x : v2.x;
    r.y = v1.y > v2.y ? v1.y : v2.y;
    r.z = v1.z > v2.z ? v1.z : v2.z;
    r.w = v1.w > v2.w ? v1.w : v2.w;
    return r;
}

extern short __attribute__((overloadable)) max(short v1, short v2) {
    return v1 > v2 ? v1 : v2;
}

extern short2 __attribute__((overloadable)) max(short2 v1, short2 v2) {
    short2 r;
    r.x = v1.x > v2.x ? v1.x : v2.x;
    r.y = v1.y > v2.y ? v1.y : v2.y;
    return r;
}

extern short3 __attribute__((overloadable)) max(short3 v1, short3 v2) {
    short3 r;
    r.x = v1.x > v2.x ? v1.x : v2.x;
    r.y = v1.y > v2.y ? v1.y : v2.y;
    r.z = v1.z > v2.z ? v1.z : v2.z;
    return r;
}

extern short4 __attribute__((overloadable)) max(short4 v1, short4 v2) {
    short4 r;
    r.x = v1.x > v2.x ? v1.x : v2.x;
    r.y = v1.y > v2.y ? v1.y : v2.y;
    r.z = v1.z > v2.z ? v1.z : v2.z;
    r.w = v1.w > v2.w ? v1.w : v2.w;
    return r;
}

extern int __attribute__((overloadable)) max(int v1, int v2) {
    return v1 > v2 ? v1 : v2;
}

extern int2 __attribute__((overloadable)) max(int2 v1, int2 v2) {
    int2 r;
    r.x = v1.x > v2.x ? v1.x : v2.x;
    r.y = v1.y > v2.y ? v1.y : v2.y;
    return r;
}

extern int3 __attribute__((overloadable)) max(int3 v1, int3 v2) {
    int3 r;
    r.x = v1.x > v2.x ? v1.x : v2.x;
    r.y = v1.y > v2.y ? v1.y : v2.y;
    r.z = v1.z > v2.z ? v1.z : v2.z;
    return r;
}

extern int4 __attribute__((overloadable)) max(int4 v1, int4 v2) {
    int4 r;
    r.x = v1.x > v2.x ? v1.x : v2.x;
    r.y = v1.y > v2.y ? v1.y : v2.y;
    r.z = v1.z > v2.z ? v1.z : v2.z;
    r.w = v1.w > v2.w ? v1.w : v2.w;
    return r;
}

extern int64_t __attribute__((overloadable)) max(int64_t v1, int64_t v2) {
    return v1 > v2 ? v1 : v2;
}

extern long2 __attribute__((overloadable)) max(long2 v1, long2 v2) {
    long2 r;
    r.x = v1.x > v2.x ? v1.x : v2.x;
    r.y = v1.y > v2.y ? v1.y : v2.y;
    return r;
}

extern long3 __attribute__((overloadable)) max(long3 v1, long3 v2) {
    long3 r;
    r.x = v1.x > v2.x ? v1.x : v2.x;
    r.y = v1.y > v2.y ? v1.y : v2.y;
    r.z = v1.z > v2.z ? v1.z : v2.z;
    return r;
}

extern long4 __attribute__((overloadable)) max(long4 v1, long4 v2) {
    long4 r;
    r.x = v1.x > v2.x ? v1.x : v2.x;
    r.y = v1.y > v2.y ? v1.y : v2.y;
    r.z = v1.z > v2.z ? v1.z : v2.z;
    r.w = v1.w > v2.w ? v1.w : v2.w;
    return r;
}

extern uchar __attribute__((overloadable)) max(uchar v1, uchar v2) {
    return v1 > v2 ? v1 : v2;
}

extern uchar2 __attribute__((overloadable)) max(uchar2 v1, uchar2 v2) {
    uchar2 r;
    r.x = v1.x > v2.x ? v1.x : v2.x;
    r.y = v1.y > v2.y ? v1.y : v2.y;
    return r;
}

extern uchar3 __attribute__((overloadable)) max(uchar3 v1, uchar3 v2) {
    uchar3 r;
    r.x = v1.x > v2.x ? v1.x : v2.x;
    r.y = v1.y > v2.y ? v1.y : v2.y;
    r.z = v1.z > v2.z ? v1.z : v2.z;
    return r;
}

extern uchar4 __attribute__((overloadable)) max(uchar4 v1, uchar4 v2) {
    uchar4 r;
    r.x = v1.x > v2.x ? v1.x : v2.x;
    r.y = v1.y > v2.y ? v1.y : v2.y;
    r.z = v1.z > v2.z ? v1.z : v2.z;
    r.w = v1.w > v2.w ? v1.w : v2.w;
    return r;
}

extern ushort __attribute__((overloadable)) max(ushort v1, ushort v2) {
    return v1 > v2 ? v1 : v2;
}

extern ushort2 __attribute__((overloadable)) max(ushort2 v1, ushort2 v2) {
    ushort2 r;
    r.x = v1.x > v2.x ? v1.x : v2.x;
    r.y = v1.y > v2.y ? v1.y : v2.y;
    return r;
}

extern ushort3 __attribute__((overloadable)) max(ushort3 v1, ushort3 v2) {
    ushort3 r;
    r.x = v1.x > v2.x ? v1.x : v2.x;
    r.y = v1.y > v2.y ? v1.y : v2.y;
    r.z = v1.z > v2.z ? v1.z : v2.z;
    return r;
}

extern ushort4 __attribute__((overloadable)) max(ushort4 v1, ushort4 v2) {
    ushort4 r;
    r.x = v1.x > v2.x ? v1.x : v2.x;
    r.y = v1.y > v2.y ? v1.y : v2.y;
    r.z = v1.z > v2.z ? v1.z : v2.z;
    r.w = v1.w > v2.w ? v1.w : v2.w;
    return r;
}

extern uint __attribute__((overloadable)) max(uint v1, uint v2) {
    return v1 > v2 ? v1 : v2;
}

extern uint2 __attribute__((overloadable)) max(uint2 v1, uint2 v2) {
    uint2 r;
    r.x = v1.x > v2.x ? v1.x : v2.x;
    r.y = v1.y > v2.y ? v1.y : v2.y;
    return r;
}

extern uint3 __attribute__((overloadable)) max(uint3 v1, uint3 v2) {
    uint3 r;
    r.x = v1.x > v2.x ? v1.x : v2.x;
    r.y = v1.y > v2.y ? v1.y : v2.y;
    r.z = v1.z > v2.z ? v1.z : v2.z;
    return r;
}

extern uint4 __attribute__((overloadable)) max(uint4 v1, uint4 v2) {
    uint4 r;
    r.x = v1.x > v2.x ? v1.x : v2.x;
    r.y = v1.y > v2.y ? v1.y : v2.y;
    r.z = v1.z > v2.z ? v1.z : v2.z;
    r.w = v1.w > v2.w ? v1.w : v2.w;
    return r;
}

extern ulong __attribute__((overloadable)) max(ulong v1, ulong v2) {
    return v1 > v2 ? v1 : v2;
}

extern ulong2 __attribute__((overloadable)) max(ulong2 v1, ulong2 v2) {
    ulong2 r;
    r.x = v1.x > v2.x ? v1.x : v2.x;
    r.y = v1.y > v2.y ? v1.y : v2.y;
    return r;
}

extern ulong3 __attribute__((overloadable)) max(ulong3 v1, ulong3 v2) {
    ulong3 r;
    r.x = v1.x > v2.x ? v1.x : v2.x;
    r.y = v1.y > v2.y ? v1.y : v2.y;
    r.z = v1.z > v2.z ? v1.z : v2.z;
    return r;
}

extern ulong4 __attribute__((overloadable)) max(ulong4 v1, ulong4 v2) {
    ulong4 r;
    r.x = v1.x > v2.x ? v1.x : v2.x;
    r.y = v1.y > v2.y ? v1.y : v2.y;
    r.z = v1.z > v2.z ? v1.z : v2.z;
    r.w = v1.w > v2.w ? v1.w : v2.w;
    return r;
}

extern float __attribute__((overloadable)) max(float v1, float v2) {
    return fmax(v1, v2);
}

extern float2 __attribute__((overloadable)) max(float2 v1, float2 v2) {
    return fmax(v1, v2);
}

extern float2 __attribute__((overloadable)) max(float2 v1, float v2) {
    return fmax(v1, v2);
}

extern float3 __attribute__((overloadable)) max(float3 v1, float3 v2) {
    return fmax(v1, v2);
}

extern float3 __attribute__((overloadable)) max(float3 v1, float v2) {
    return fmax(v1, v2);
}

extern float4 __attribute__((overloadable)) max(float4 v1, float4 v2) {
    return fmax(v1, v2);
}

extern float4 __attribute__((overloadable)) max(float4 v1, float v2) {
    return fmax(v1, v2);
}


/*
 * MIN
 */

extern int8_t __attribute__((overloadable)) min(int8_t v1, int8_t v2) {
    return v1 < v2 ? v1 : v2;
}

extern char2 __attribute__((overloadable)) min(char2 v1, char2 v2) {
    char2 r;
    r.x = v1.x < v2.x ? v1.x : v2.x;
    r.y = v1.y < v2.y ? v1.y : v2.y;
    return r;
}

extern char3 __attribute__((overloadable)) min(char3 v1, char3 v2) {
    char3 r;
    r.x = v1.x < v2.x ? v1.x : v2.x;
    r.y = v1.y < v2.y ? v1.y : v2.y;
    r.z = v1.z < v2.z ? v1.z : v2.z;
    return r;
}

extern char4 __attribute__((overloadable)) min(char4 v1, char4 v2) {
    char4 r;
    r.x = v1.x < v2.x ? v1.x : v2.x;
    r.y = v1.y < v2.y ? v1.y : v2.y;
    r.z = v1.z < v2.z ? v1.z : v2.z;
    r.w = v1.w < v2.w ? v1.w : v2.w;
    return r;
}

extern int16_t __attribute__((overloadable)) min(int16_t v1, int16_t v2) {
    return v1 < v2 ? v1 : v2;
}

extern short2 __attribute__((overloadable)) min(short2 v1, short2 v2) {
    short2 r;
    r.x = v1.x < v2.x ? v1.x : v2.x;
    r.y = v1.y < v2.y ? v1.y : v2.y;
    return r;
}

extern short3 __attribute__((overloadable)) min(short3 v1, short3 v2) {
    short3 r;
    r.x = v1.x < v2.x ? v1.x : v2.x;
    r.y = v1.y < v2.y ? v1.y : v2.y;
    r.z = v1.z < v2.z ? v1.z : v2.z;
    return r;
}

extern short4 __attribute__((overloadable)) min(short4 v1, short4 v2) {
    short4 r;
    r.x = v1.x < v2.x ? v1.x : v2.x;
    r.y = v1.y < v2.y ? v1.y : v2.y;
    r.z = v1.z < v2.z ? v1.z : v2.z;
    r.w = v1.w < v2.w ? v1.w : v2.w;
    return r;
}

extern int32_t __attribute__((overloadable)) min(int32_t v1, int32_t v2) {
    return v1 < v2 ? v1 : v2;
}

extern int2 __attribute__((overloadable)) min(int2 v1, int2 v2) {
    int2 r;
    r.x = v1.x < v2.x ? v1.x : v2.x;
    r.y = v1.y < v2.y ? v1.y : v2.y;
    return r;
}

extern int3 __attribute__((overloadable)) min(int3 v1, int3 v2) {
    int3 r;
    r.x = v1.x < v2.x ? v1.x : v2.x;
    r.y = v1.y < v2.y ? v1.y : v2.y;
    r.z = v1.z < v2.z ? v1.z : v2.z;
    return r;
}

extern int4 __attribute__((overloadable)) min(int4 v1, int4 v2) {
    int4 r;
    r.x = v1.x < v2.x ? v1.x : v2.x;
    r.y = v1.y < v2.y ? v1.y : v2.y;
    r.z = v1.z < v2.z ? v1.z : v2.z;
    r.w = v1.w < v2.w ? v1.w : v2.w;
    return r;
}

extern int64_t __attribute__((overloadable)) min(int64_t v1, int64_t v2) {
    return v1 < v2 ? v1 : v2;
}

extern long2 __attribute__((overloadable)) min(long2 v1, long2 v2) {
    long2 r;
    r.x = v1.x < v2.x ? v1.x : v2.x;
    r.y = v1.y < v2.y ? v1.y : v2.y;
    return r;
}

extern long3 __attribute__((overloadable)) min(long3 v1, long3 v2) {
    long3 r;
    r.x = v1.x < v2.x ? v1.x : v2.x;
    r.y = v1.y < v2.y ? v1.y : v2.y;
    r.z = v1.z < v2.z ? v1.z : v2.z;
    return r;
}

extern long4 __attribute__((overloadable)) min(long4 v1, long4 v2) {
    long4 r;
    r.x = v1.x < v2.x ? v1.x : v2.x;
    r.y = v1.y < v2.y ? v1.y : v2.y;
    r.z = v1.z < v2.z ? v1.z : v2.z;
    r.w = v1.w < v2.w ? v1.w : v2.w;
    return r;
}

extern uchar __attribute__((overloadable)) min(uchar v1, uchar v2) {
    return v1 < v2 ? v1 : v2;
}

extern uchar2 __attribute__((overloadable)) min(uchar2 v1, uchar2 v2) {
    uchar2 r;
    r.x = v1.x < v2.x ? v1.x : v2.x;
    r.y = v1.y < v2.y ? v1.y : v2.y;
    return r;
}

extern uchar3 __attribute__((overloadable)) min(uchar3 v1, uchar3 v2) {
    uchar3 r;
    r.x = v1.x < v2.x ? v1.x : v2.x;
    r.y = v1.y < v2.y ? v1.y : v2.y;
    r.z = v1.z < v2.z ? v1.z : v2.z;
    return r;
}

extern uchar4 __attribute__((overloadable)) min(uchar4 v1, uchar4 v2) {
    uchar4 r;
    r.x = v1.x < v2.x ? v1.x : v2.x;
    r.y = v1.y < v2.y ? v1.y : v2.y;
    r.z = v1.z < v2.z ? v1.z : v2.z;
    r.w = v1.w < v2.w ? v1.w : v2.w;
    return r;
}

extern ushort __attribute__((overloadable)) min(ushort v1, ushort v2) {
    return v1 < v2 ? v1 : v2;
}

extern ushort2 __attribute__((overloadable)) min(ushort2 v1, ushort2 v2) {
    ushort2 r;
    r.x = v1.x < v2.x ? v1.x : v2.x;
    r.y = v1.y < v2.y ? v1.y : v2.y;
    return r;
}

extern ushort3 __attribute__((overloadable)) min(ushort3 v1, ushort3 v2) {
    ushort3 r;
    r.x = v1.x < v2.x ? v1.x : v2.x;
    r.y = v1.y < v2.y ? v1.y : v2.y;
    r.z = v1.z < v2.z ? v1.z : v2.z;
    return r;
}

extern ushort4 __attribute__((overloadable)) min(ushort4 v1, ushort4 v2) {
    ushort4 r;
    r.x = v1.x < v2.x ? v1.x : v2.x;
    r.y = v1.y < v2.y ? v1.y : v2.y;
    r.z = v1.z < v2.z ? v1.z : v2.z;
    r.w = v1.w < v2.w ? v1.w : v2.w;
    return r;
}

extern uint __attribute__((overloadable)) min(uint v1, uint v2) {
    return v1 < v2 ? v1 : v2;
}

extern uint2 __attribute__((overloadable)) min(uint2 v1, uint2 v2) {
    uint2 r;
    r.x = v1.x < v2.x ? v1.x : v2.x;
    r.y = v1.y < v2.y ? v1.y : v2.y;
    return r;
}

extern uint3 __attribute__((overloadable)) min(uint3 v1, uint3 v2) {
    uint3 r;
    r.x = v1.x < v2.x ? v1.x : v2.x;
    r.y = v1.y < v2.y ? v1.y : v2.y;
    r.z = v1.z < v2.z ? v1.z : v2.z;
    return r;
}

extern uint4 __attribute__((overloadable)) min(uint4 v1, uint4 v2) {
    uint4 r;
    r.x = v1.x < v2.x ? v1.x : v2.x;
    r.y = v1.y < v2.y ? v1.y : v2.y;
    r.z = v1.z < v2.z ? v1.z : v2.z;
    r.w = v1.w < v2.w ? v1.w : v2.w;
    return r;
}

extern ulong __attribute__((overloadable)) min(ulong v1, ulong v2) {
    return v1 < v2 ? v1 : v2;
}

extern ulong2 __attribute__((overloadable)) min(ulong2 v1, ulong2 v2) {
    ulong2 r;
    r.x = v1.x < v2.x ? v1.x : v2.x;
    r.y = v1.y < v2.y ? v1.y : v2.y;
    return r;
}

extern ulong3 __attribute__((overloadable)) min(ulong3 v1, ulong3 v2) {
    ulong3 r;
    r.x = v1.x < v2.x ? v1.x : v2.x;
    r.y = v1.y < v2.y ? v1.y : v2.y;
    r.z = v1.z < v2.z ? v1.z : v2.z;
    return r;
}

extern ulong4 __attribute__((overloadable)) min(ulong4 v1, ulong4 v2) {
    ulong4 r;
    r.x = v1.x < v2.x ? v1.x : v2.x;
    r.y = v1.y < v2.y ? v1.y : v2.y;
    r.z = v1.z < v2.z ? v1.z : v2.z;
    r.w = v1.w < v2.w ? v1.w : v2.w;
    return r;
}

extern float __attribute__((overloadable)) min(float v1, float v2) {
    return fmin(v1, v2);
}

extern float2 __attribute__((overloadable)) min(float2 v1, float2 v2) {
    return fmin(v1, v2);
}

extern float2 __attribute__((overloadable)) min(float2 v1, float v2) {
    return fmin(v1, v2);
}

extern float3 __attribute__((overloadable)) min(float3 v1, float3 v2) {
    return fmin(v1, v2);
}

extern float3 __attribute__((overloadable)) min(float3 v1, float v2) {
    return fmin(v1, v2);
}

extern float4 __attribute__((overloadable)) min(float4 v1, float4 v2) {
    return fmin(v1, v2);
}

extern float4 __attribute__((overloadable)) min(float4 v1, float v2) {
    return fmin(v1, v2);
}


/*
 * YUV
 */

extern uchar4 __attribute__((overloadable)) rsYuvToRGBA_uchar4(uchar y, uchar u, uchar v) {
    short Y = ((short)y) - 16;
    short U = ((short)u) - 128;
    short V = ((short)v) - 128;

    short4 p;
    p.r = (Y * 298 + V * 409 + 128) >> 8;
    p.g = (Y * 298 - U * 100 - V * 208 + 128) >> 8;
    p.b = (Y * 298 + U * 516 + 128) >> 8;
    p.a = 255;
    p.r = rsClamp(p.r, (short)0, (short)255);
    p.g = rsClamp(p.g, (short)0, (short)255);
    p.b = rsClamp(p.b, (short)0, (short)255);

    return convert_uchar4(p);
}

static float4 yuv_U_values = {0.f, -0.392f * 0.003921569f, +2.02 * 0.003921569f, 0.f};
static float4 yuv_V_values = {1.603f * 0.003921569f, -0.815f * 0.003921569f, 0.f, 0.f};

extern float4 __attribute__((overloadable)) rsYuvToRGBA_float4(uchar y, uchar u, uchar v) {
    float4 color = (float)y * 0.003921569f;
    float4 fU = ((float)u) - 128.f;
    float4 fV = ((float)v) - 128.f;

    color += fU * yuv_U_values;
    color += fV * yuv_V_values;
    color = clamp(color, 0.f, 1.f);
    return color;
}


/*
 * half_RECIP
 */

extern float __attribute__((overloadable)) half_recip(float v) {
    // FIXME:  actual algorithm for generic approximate reciprocal
    return 1.f / v;
}

extern float2 __attribute__((overloadable)) half_recip(float2 v) {
    float2 r;
    r.x = half_recip(r.x);
    r.y = half_recip(r.y);
    return r;
}

extern float3 __attribute__((overloadable)) half_recip(float3 v) {
    float3 r;
    r.x = half_recip(r.x);
    r.y = half_recip(r.y);
    r.z = half_recip(r.z);
    return r;
}

extern float4 __attribute__((overloadable)) half_recip(float4 v) {
    float4 r;
    r.x = half_recip(r.x);
    r.y = half_recip(r.y);
    r.z = half_recip(r.z);
    r.w = half_recip(r.w);
    return r;
}


/*
 * half_SQRT
 */

extern float __attribute__((overloadable)) half_sqrt(float v) {
    return sqrt(v);
}

extern float2 __attribute__((overloadable)) half_sqrt(float2 v) {
    float2 r;
    r.x = half_sqrt(v.x);
    r.y = half_sqrt(v.y);
    return r;
}

extern float3 __attribute__((overloadable)) half_sqrt(float3 v) {
    float3 r;
    r.x = half_sqrt(v.x);
    r.y = half_sqrt(v.y);
    r.z = half_sqrt(v.z);
    return r;
}

extern float4 __attribute__((overloadable)) half_sqrt(float4 v) {
    float4 r;
    r.x = half_sqrt(v.x);
    r.y = half_sqrt(v.y);
    r.z = half_sqrt(v.z);
    r.w = half_sqrt(v.w);
    return r;
}


/*
 * half_rsqrt
 */

extern float __attribute__((overloadable)) half_rsqrt(float v) {
    return 1.f / sqrt(v);
}

extern float2 __attribute__((overloadable)) half_rsqrt(float2 v) {
    float2 r;
    r.x = half_rsqrt(v.x);
    r.y = half_rsqrt(v.y);
    return r;
}

extern float3 __attribute__((overloadable)) half_rsqrt(float3 v) {
    float3 r;
    r.x = half_rsqrt(v.x);
    r.y = half_rsqrt(v.y);
    r.z = half_rsqrt(v.z);
    return r;
}

extern float4 __attribute__((overloadable)) half_rsqrt(float4 v) {
    float4 r;
    r.x = half_rsqrt(v.x);
    r.y = half_rsqrt(v.y);
    r.z = half_rsqrt(v.z);
    r.w = half_rsqrt(v.w);
    return r;
}

