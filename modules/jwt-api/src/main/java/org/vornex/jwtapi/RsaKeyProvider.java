package org.vornex.jwtapi;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Getter
@Component
public class RsaKeyProvider {
    private static final Logger log = LoggerFactory.getLogger(RsaKeyProvider.class);
    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final String activeKeyId;


    public RsaKeyProvider(
            @Value("${jwt.private.key}") String privateKeyBase64,
            @Value("${jwt.public.key}") String publicKeyBase64// почему из application.yml, а не из .env напрямую.
    ) {
        try {
            this.privateKey = decodePrivateKey(privateKeyBase64);
            this.publicKey = decodePublicKey(publicKeyBase64);
            validateKeySize(publicKey, privateKey);
            this.activeKeyId = computeKeyId(publicKey);
            log.info("RSA keys successfully loaded");

        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize RSA keys", e);
        }
    }


    private RSAPrivateKey decodePrivateKey(String base64PKey) throws Exception {
        byte[] keyBytes = decodeKeyMaterial(base64PKey);
        var kf = KeyFactory.getInstance("RSA");
        var keySpec = new PKCS8EncodedKeySpec(keyBytes);
        return (RSAPrivateKey) kf.generatePrivate(keySpec);
    }

    private RSAPublicKey decodePublicKey(String base64Key) throws Exception {
        byte[] keyBytes = decodeKeyMaterial(base64Key);
        var kf = KeyFactory.getInstance("RSA");
        var keySpec = new X509EncodedKeySpec(keyBytes);
        return (RSAPublicKey) kf.generatePublic(keySpec);
    }

    private void validateKeySize(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
        if (publicKey.getModulus().bitLength() < 2048 || privateKey.getModulus().bitLength() < 2048) {
            throw new IllegalStateException("RSA key too small");
        }
        if (!publicKey.getModulus().equals(privateKey.getModulus())) {
            throw new IllegalStateException("Public/Private RSA key mismatch (different modulus)");
        }
    }

    /**
     * Поддерживает:
     * - PEM с заголовками "-----BEGIN ...-----"
     * - raw base64 без заголовков
     * Также бросает исключение для PKCS#1 приватного ключа (-----BEGIN RSA PRIVATE KEY-----),
     * т.к. KeyFactory ожидает PKCS#8 (-----BEGIN PRIVATE KEY-----).
     */
    private static byte[] decodeKeyMaterial(String keyMaterial) throws Exception {
        String trimmed = keyMaterial.trim();
        String sanitized = trimmed.replaceAll("\\s+", "");
        if (sanitized.startsWith("-----BEGIN RSA PRIVATE KEY-----")) {
            throw new IllegalStateException(
                    "PKCS#1 private key detected. Provide PKCS#8 (-----BEGIN PRIVATE KEY-----)."
            );
        }
        if (sanitized.startsWith("-----BEGIN")) {
            // strip headers and newlines
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new StringReader(sanitized))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("-----BEGIN") || line.startsWith("-----END")) continue;
                    sb.append(line.trim());
                }
            }
            return Base64.getDecoder().decode(sb.toString());
        } else {
            // assume raw base64 (no headers)
            return Base64.getDecoder().decode(sanitized);
        }
    }

    /**
     * Вычисляем kid как base64url(SHA-256(publicKey.getEncoded()))
     * Это удобно: kid будет детерминированно связан с публичным ключом.
     */
    private static String computeKeyId(PublicKey publicKey) {
        try {
            byte[] encoded = publicKey.getEncoded(); // X.509
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(encoded);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 всегда должен быть доступен в JVM; на всякий случай — fallback
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
