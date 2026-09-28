package vn.iotstar.services;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.text.ParseException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    private final byte[] secret;
    private final long expirationTime;
    private final String issuer;
    private final String audience;

    public JwtService(@Value("${security.jwt.secret-key}") String base64Secret,
                      @Value("${security.jwt.expiration-time}") long expirationTime,
                      @Value("${security.jwt.issuer}") String issuer,
                      @Value("${security.jwt.audience}") String audience) {
        if (base64Secret.isBlank()) {
            secret = new byte[32];
            new SecureRandom().nextBytes(secret);
            LoggerFactory.getLogger(JwtService.class).warn(
                    "Demo key generated: tokens expire on restart. Set JWT_SECRET_BASE64 for a persistent key.");
        } else {
            secret = Base64.getDecoder().decode(base64Secret);
        }
        if (secret.length < 32) throw new IllegalArgumentException("HS256 requires at least 32 secret bytes");
        if (expirationTime < 1000) throw new IllegalArgumentException("JWT lifetime must be at least 1000 ms");
        this.expirationTime = expirationTime; this.issuer = issuer; this.audience = audience;
    }

    public String generateToken(UserDetails user) {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(user.getUsername()).issuer(issuer).audience(audience)
                .issueTime(Date.from(now)).notBeforeTime(Date.from(now))
                .expirationTime(Date.from(now.plusMillis(expirationTime)))
                .jwtID(UUID.randomUUID().toString()).build();
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build(), claims);
        try {
            jwt.sign(new MACSigner(secret));
            return jwt.serialize();
        } catch (JOSEException exception) {
            throw new IllegalStateException("Cannot sign JWT", exception);
        }
    }

    // Only return claims after both signature AND claim validation succeed.
    public JWTClaimsSet validateToken(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm())
                    || !JOSEObjectType.JWT.equals(jwt.getHeader().getType())
                    || !jwt.verify(new MACVerifier(secret))) {
                throw invalid();
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date now = new Date();
            if (claims.getSubject() == null || claims.getSubject().isBlank()
                    || !issuer.equals(claims.getIssuer())
                    || !claims.getAudience().contains(audience)
                    || claims.getExpirationTime() == null || !claims.getExpirationTime().after(now)
                    || claims.getIssueTime() == null || claims.getIssueTime().after(now)
                    || !claims.getExpirationTime().after(claims.getIssueTime())
                    || (claims.getNotBeforeTime() != null && claims.getNotBeforeTime().after(now))) {
                throw invalid();
            }
            return claims;
        } catch (ParseException | JOSEException | IllegalArgumentException exception) {
            throw invalid();
        }
    }
    private BadCredentialsException invalid() {
        return new BadCredentialsException("JWT không hợp lệ hoặc đã hết hạn");
    }
    public long getExpirationTime() { return expirationTime; }
}
