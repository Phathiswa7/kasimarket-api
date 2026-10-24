package com.kasimarket.kasimarket_api.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// This class creates and reads login tokens (JWTs).
// A token is a signed piece of text. Only our app knows the secret key,
// so nobody can fake a token or change one without us noticing.
@Service
public class JwtService {

    // The key used to sign tokens, built from the secret in application.properties.
    private final SecretKey key;

    // How long a token stays valid, in milliseconds.
    private final long expirationMs;

    // @Value reads a setting from application.properties.
    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration-ms}") long expirationMs) {
        // Turn the secret text into a proper cryptographic key.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    // Creates a token for a person who has just logged in.
    public String generateToken(String email, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(email)                       // who the token belongs to
                .claim("role", role)                  // extra info: CLIENT, WORKER or ADMIN
                .issuedAt(now)                        // when we made it
                .expiration(new Date(now.getTime() + expirationMs))  // when it stops working
                .signWith(key)                        // sign it with our secret key
                .compact();                           // turn it into the final text
    }

    // Reads a token and returns what is inside it.
    // If the token is fake or expired, this throws an error.
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)                      // check the signature with our key
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
