package org.vornex.jwtapi;

// TokenKind.java
public enum TokenKind {
    ACCESS, REFRESH;

    public String asClaimValue() {
        return switch (this) {
            case ACCESS -> "access";
            case REFRESH -> "refresh";
        };
    }

    public static TokenKind fromClaimValue(String v) {
        return switch (v) {
            case "access" -> ACCESS;
            case "refresh" -> REFRESH;
            default -> throw new IllegalArgumentException("Unknown token_use: " + v);
        };
    }
}
