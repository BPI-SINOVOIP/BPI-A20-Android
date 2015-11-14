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

package com.android.cts.multiuserstorageapp;

import android.os.Environment;
import android.test.AndroidTestCase;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Test multi-user emulated storage environment, ensuring that each user has
 * isolated storage minus shared OBB directory.
 */
public class MultiUserStorageTest extends AndroidTestCase {
    private static final String TAG = "MultiUserStorageTest";

    private static final String FILE_PREFIX = "MUST_";

    private final String FILE_SINGLETON = FILE_PREFIX + "singleton";
    private final String FILE_OBB_SINGLETON = FILE_PREFIX + "obb_singleton";
    private final String FILE_OBB_API_SINGLETON = FILE_PREFIX + "obb_api_singleton";
    private final String FILE_MY_UID = FILE_PREFIX + android.os.Process.myUid();

    private static final int OBB_API_VALUE = 0xcafe;
    private static final int OBB_VALUE = 0xf00d;

    private void wipeTestFiles(File dir) {
        dir.mkdirs();
        for (File file : dir.listFiles()) {
            if (file.getName().startsWith(FILE_PREFIX)) {
                Log.d(TAG, "Wiping " + file);
                file.delete();
            }
        }
    }

    public void cleanIsolatedStorage() throws Exception {
        wipeTestFiles(Environment.getExternalStorageDirectory());
    }

    public void writeIsolatedStorage() throws Exception {
        writeInt(buildApiPath(FILE_SINGLETON), android.os.Process.myUid());
        writeInt(buildApiPath(FILE_MY_UID), android.os.Process.myUid());
    }

    public void readIsolatedStorage() throws Exception {
        // Expect that the value we wrote earlier is still valid and wasn't
        // overwritten by us running as another user.
        assertEquals("Failed to read singleton file from API path", android.os.Process.myUid(),
                readInt(buildApiPath(FILE_SINGLETON)));
        assertEquals("Failed to read singleton file from env path", android.os.Process.myUid(),
                readInt(buildEnvPath(FILE_SINGLETON)));
        assertEquals("Failed to read singleton file from raw path", android.os.Process.myUid(),
                readInt(buildRawPath(FILE_SINGLETON)));

        assertEquals("Failed to read UID file from API path", android.os.Process.myUid(),
                readInt(buildApiPath(FILE_MY_UID)));
    }

    public void cleanObbStorage() throws Exception {
        wipeTestFiles(getContext().getObbDir());
    }

    public void writeObbStorage() throws Exception {
        writeInt(buildApiObbPath(FILE_OBB_API_SINGLETON), OBB_API_VALUE);
        writeInt(buildEnvObbPath(FILE_OBB_SINGLETON), OBB_VALUE);
    }

    public void readObbStorage() throws Exception {
        assertEquals("Failed to read OBB file from API path", OBB_API_VALUE,
                readInt(buildApiObbPath(FILE_OBB_API_SINGLETON)));

        assertEquals("Failed to read OBB file from env path", OBB_VALUE,
                readInt(buildEnvObbPath(FILE_OBB_SINGLETON)));
        assertEquals("Failed to read OBB file from raw path", OBB_VALUE,
                readInt(buildRawObbPath(FILE_OBB_SINGLETON)));
    }

    private File buildApiObbPath(String file) {
        return new File(getContext().getObbDir(), file);
    }

    private File buildEnvObbPath(String file) {
        return new File(new File(System.getenv("EXTERNAL_STORAGE"), "Android/obb"), file);
    }

    private File buildRawObbPath(String file) {
        return new File("/sdcard/Android/obb/", file);
    }

    private static File buildApiPath(String file) {
        return new File(Environment.getExternalStorageDirectory(), file);
    }

    private static File buildEnvPath(String file) {
        return new File(System.getenv("EXTERNAL_STORAGE"), file);
    }

    private static File buildRawPath(String file) {
        return new File("/sdcard/", file);
    }

    private static void writeInt(File file, int value) throws IOException {
        final DataOutputStream os = new DataOutputStream(new FileOutputStream(file));
        try {
            os.writeInt(value);
        } finally {
            os.close();
        }
    }

    private static int readInt(File file) throws IOException {
        final DataInputStream is = new DataInputStream(new FileInputStream(file));
        try {
            return is.readInt();
        } finally {
            is.close();
        }
    }
}
