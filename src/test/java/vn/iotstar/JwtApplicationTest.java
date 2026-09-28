package vn.iotstar;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import vn.iotstar.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:jwt_test;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "security.jwt.secret-key=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE="
})
@AutoConfigureMockMvc
class JwtApplicationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    private static final String EMAIL = "student@example.com";
    private static final String PASSWORD = "Demo123!";
    @BeforeEach void reset() { users.deleteAll(); }
    String json(Object value) throws Exception { return mapper.writeValueAsString(value); }
    void signup() throws Exception {
        mvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", EMAIL, "password", PASSWORD, "fullName", "Nguyễn Văn A"))))
                .andExpect(status().isOk()).andExpect(jsonPath("password").doesNotExist())
                .andExpect(jsonPath("fullName").value("Nguyễn Văn A"));
    }
    String login() throws Exception {
        var result = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", EMAIL, "password", PASSWORD))))
                .andExpect(status().isOk()).andExpect(jsonPath("expiresIn").value(3600000)).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
    @Test void signupLoginAndProtectedApis() throws Exception {
        signup();
        assertThat(users.findByEmail(EMAIL).orElseThrow().getPassword()).startsWith("$2").isNotEqualTo(PASSWORD);
        String token = login();
        assertThat(SignedJWT.parse(token).getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.HS256);
        for (String path : List.of("/users/me", "/users", "/users/"))
            mvc.perform(get(path).header("Authorization", "Bearer " + token)).andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("password"))))
                    .andExpect(cookie().doesNotExist("JSESSIONID"));
    }
    @Test void anonymousRequestsNeedAuthentication() throws Exception {
        for (String path : List.of("/users/me", "/users/"))
            mvc.perform(get(path)).andExpect(status().isUnauthorized()).andExpect(header().string("WWW-Authenticate", "Bearer"));
    }
    @Test void badPasswordReturns401() throws Exception {
        signup();
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", EMAIL, "password", "wrong-password")))).andExpect(status().isUnauthorized());
    }
    @Test void duplicateEmailReturns409() throws Exception {
        signup();
        mvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", "STUDENT@example.com", "password", PASSWORD, "fullName", "Test"))))
                .andExpect(status().isConflict());
    }
    @Test void invalidInputReturns400() throws Exception {
        mvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", "bad", "password", "1", "fullName", "")))).andExpect(status().isBadRequest());
    }
    @Test void malformedTokenReturns401() throws Exception {
        mvc.perform(get("/users/me").header("Authorization", "Bearer not-a-jwt")).andExpect(status().isUnauthorized());
    }
    @Test void changedPayloadReturns401() throws Exception {
        signup(); String[] parts = login().split("\\.");
        parts[1] = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"attacker\"}".getBytes(StandardCharsets.UTF_8));
        mvc.perform(get("/users/me").header("Authorization", "Bearer " + String.join(".", parts)))
                .andExpect(status().isUnauthorized());
    }
    JWTClaimsSet.Builder claims() {
        Instant now = Instant.now();
        return new JWTClaimsSet.Builder().subject(EMAIL).issuer("jwt-nimbus-demo").audience("jwt-nimbus-client")
                .issueTime(Date.from(now.minusSeconds(60))).expirationTime(Date.from(now.plusSeconds(600)));
    }
    String sign(JWTClaimsSet claims, JWSAlgorithm algorithm, byte[] key) throws Exception {
        SignedJWT token = new SignedJWT(new JWSHeader.Builder(algorithm).type(JOSEObjectType.JWT).build(), claims);
        token.sign(new MACSigner(key)); return token.serialize();
    }
    byte[] key() { return "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8); }
    void rejected(JWTClaimsSet claims) throws Exception {
        mvc.perform(get("/users/me").header("Authorization", "Bearer " + sign(claims, JWSAlgorithm.HS256, key())))
                .andExpect(status().isUnauthorized());
    }
    @Test void expiredTokenReturns401() throws Exception {
        signup(); rejected(claims().expirationTime(Date.from(Instant.now().minusSeconds(10))).build());
    }
    @Test void missingExpirationReturns401() throws Exception {
        signup(); rejected(claims().expirationTime(null).build());
    }
    @Test void wrongIssuerOrAudienceReturns401() throws Exception {
        signup(); rejected(claims().issuer("other").build()); rejected(claims().audience("other").build());
    }
    @Test void futureNotBeforeOrIssuedAtReturns401() throws Exception {
        signup(); Date future = Date.from(Instant.now().plusSeconds(300));
        rejected(claims().notBeforeTime(future).build()); rejected(claims().issueTime(future).build());
    }
    @Test void wrongKeyReturns401() throws Exception {
        signup(); mvc.perform(get("/users/me").header("Authorization", "Bearer " + sign(claims().build(), JWSAlgorithm.HS256, new byte[32])))
                .andExpect(status().isUnauthorized());
    }
    @Test void unexpectedAlgorithmReturns401() throws Exception {
        signup(); mvc.perform(get("/users/me").header("Authorization", "Bearer " + sign(claims().build(), JWSAlgorithm.HS512, new byte[64])))
                .andExpect(status().isUnauthorized());
    }
    @Test void unsignedTokenReturns401() throws Exception {
        signup(); mvc.perform(get("/users/me").header("Authorization", "Bearer " + new PlainJWT(claims().build()).serialize()))
                .andExpect(status().isUnauthorized());
    }
    @Test void lockedUserReturns403AtLoginAndWithExistingToken() throws Exception {
        signup(); String token = login(); var user = users.findByEmail(EMAIL).orElseThrow();
        user.setLocked(true); users.save(user);
        mvc.perform(get("/users/me").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", EMAIL, "password", PASSWORD)))).andExpect(status().isForbidden());
    }
    @Test void deletedUserTokenReturns401() throws Exception {
        signup(); String token = login(); users.deleteAll();
        mvc.perform(get("/users/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }
    @Test void authenticatedButDisallowedRouteReturns403() throws Exception {
        signup(); mvc.perform(get("/not-allowed").header("Authorization", "Bearer " + login())).andExpect(status().isForbidden());
    }
    @Test void publicViewsAndJavascriptAreAvailable() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(view().name("login"));
        mvc.perform(get("/user/profile")).andExpect(status().isOk()).andExpect(view().name("profile"));
        mvc.perform(get("/js/mainjs.js")).andExpect(status().isOk());
    }
}
