
package com.allwinnertech.dragonsn.jni;

public class ReadPrivateJNI {

    static {
        System.loadLibrary("allwinnertech_read_private");
        native_init();
    }

    public static native boolean native_init();

    public native String native_get_parameter(String name);

    public native boolean native_set_parameter(String name, String value);

    public native void native_release();

}
