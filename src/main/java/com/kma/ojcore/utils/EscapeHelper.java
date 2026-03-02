package com.kma.ojcore.utils;

public class EscapeHelper {
    public static String escapeLike(String param) {
        if (param == null) {
            return null;
        }
        return "%" + param.trim().replace("\\","\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_")
                .replace("\\n","\\\\n")
                .replace("\\s","\\\\s")
                .replace("\\t","\\\\t")
                + "%";
    }
}