package vn.iotstar.controller;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import vn.iotstar.models.*;
import vn.iotstar.services.*;
@RestController
@RequestMapping("/auth")
public class AuthenticationController {
    private final AuthenticationService auth;
    private final JwtService jwt;
    public AuthenticationController(AuthenticationService auth, JwtService jwt) { this.auth = auth; this.jwt = jwt; }
    @PostMapping("/signup") public UserResponse signup(@Valid @RequestBody RegisterUserModel input) {
        return UserResponse.from(auth.signup(input));
    }
    @PostMapping("/login") public LoginResponse login(@Valid @RequestBody LoginUserModel input) {
        return new LoginResponse(jwt.generateToken(auth.authenticate(input)), jwt.getExpirationTime());
    }
}
