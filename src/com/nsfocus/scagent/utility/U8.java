package com.nsfocus.scagent.utility;

public class U8 {
    public static short f(byte i) {
        return (short) ((short)i & 0xff);
    }

    public static byte t(short l) {
        return (byte) l;
    }
}
