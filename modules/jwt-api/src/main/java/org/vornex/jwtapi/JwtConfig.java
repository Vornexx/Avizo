package org.vornex.jwtapi;

public record JwtConfig(
        String issuer,
        String audience,
        long clockSkewSeconds
) {
    public boolean checkIssuer() {
        return issuer != null && !issuer.isBlank();
    }

    public boolean checkAudience() {
        return audience != null && !audience.isBlank();
    }
}