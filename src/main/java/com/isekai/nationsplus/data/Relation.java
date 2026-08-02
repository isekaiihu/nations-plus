package com.isekai.nationsplus.data;

public enum Relation {
    ALLY,
    NEUTRAL,
    WAR;

    public static Relation fromString(String s) {
        if (s == null) return NEUTRAL;
        try {
            return valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NEUTRAL;
        }
    }
}
