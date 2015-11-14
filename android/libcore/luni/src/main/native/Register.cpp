/*
 * Copyright (C) 2010 The Android Open Source Project
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

#define LOG_TAG "libcore" // We'll be next to "dalvikvm" in the log; make the distinction clear.

#include "JniConstants.h"
#include "ScopedLocalFrame.h"

#include <stdlib.h>

// DalvikVM calls this on startup, so we can statically register all our native methods.
int JNI_OnLoad(JavaVM* vm, void*) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        ALOGE("JavaVM::GetEnv() failed");
        abort();
    }

    ScopedLocalFrame localFrame(env);

    JniConstants::init(env);

#define REGISTER(FN) extern void FN(JNIEnv*); FN(env)
    REGISTER(register_java_io_Console);
    REGISTER(register_java_io_File);
    REGISTER(register_java_io_ObjectStreamClass);
    REGISTER(register_java_lang_Character);
    REGISTER(register_java_lang_Math);
    REGISTER(register_java_lang_ProcessManager);
    REGISTER(register_java_lang_RealToString);
    REGISTER(register_java_lang_StrictMath);
    REGISTER(register_java_lang_StringToReal);
    REGISTER(register_java_lang_System);
    REGISTER(register_java_math_NativeBN);
    REGISTER(register_java_nio_ByteOrder);
    REGISTER(register_java_nio_charset_Charsets);
    REGISTER(register_java_text_Bidi);
    REGISTER(register_java_util_regex_Matcher);
    REGISTER(register_java_util_regex_Pattern);
    REGISTER(register_java_util_zip_Adler32);
    REGISTER(register_java_util_zip_CRC32);
    REGISTER(register_java_util_zip_Deflater);
    REGISTER(register_java_util_zip_Inflater);
    REGISTER(register_libcore_icu_ICU);
    REGISTER(register_libcore_icu_NativeBreakIterator);
    REGISTER(register_libcore_icu_NativeCollation);
    REGISTER(register_libcore_icu_NativeConverter);
    REGISTER(register_libcore_icu_NativeDecimalFormat);
    REGISTER(register_libcore_icu_NativeIDN);
    REGISTER(register_libcore_icu_NativeNormalizer);
    REGISTER(register_libcore_icu_NativePluralRules);
    REGISTER(register_libcore_icu_TimeZones);
    REGISTER(register_libcore_io_AsynchronousCloseMonitor);
    REGISTER(register_libcore_io_Memory);
    REGISTER(register_libcore_io_OsConstants);
    REGISTER(register_libcore_io_Posix);
    REGISTER(register_libcore_net_RawSocket);
    REGISTER(register_org_apache_harmony_dalvik_NativeTestTarget);
    REGISTER(register_org_apache_harmony_xml_ExpatParser);
    REGISTER(register_org_apache_harmony_xnet_provider_jsse_NativeCrypto);
#undef REGISTER
    return JNI_VERSION_1_6;
}
