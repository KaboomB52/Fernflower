package org.jetbrains.java.decompiler.util;

import java.util.regex.Pattern;

public class Util {

    private final static Pattern RTRIM = Pattern.compile("\\s+$");

    public static String rtrim(String s) {
        return RTRIM.matcher(s).replaceAll("");
    }
}
