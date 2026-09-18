package com.treasure.restart.helper;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtUtils {

    private static final String SECRET =
            "treasure-restart-server-secret-key-2026";

    private static final long EXPIRE_TIME =
            7 * 24 * 60 * 60 * 1000L;

    private static final SecretKey KEY =
            Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    public static String generateToken(Long userId) {

        Date now = new Date();

        Date expire = new Date(
                now.getTime() + EXPIRE_TIME
        );

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expire)
                .signWith(KEY)
                .compact();
    }

    public static Long getUserId(String token) {

        String subject = Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();

        return Long.valueOf(subject);
    }
}