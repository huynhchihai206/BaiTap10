package vn.iotstar.services;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import vn.iotstar.entity.User;
import vn.iotstar.models.*;
import vn.iotstar.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class AuthenticationService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final AuthenticationManager authenticationManager;
    public AuthenticationService(UserRepository users, PasswordEncoder passwords, AuthenticationManager manager) {
        this.users = users; this.passwords = passwords; this.authenticationManager = manager;
    }
    public User signup(RegisterUserModel input) {
        String email = normalize(input.email());
        checkPasswordLength(input.password());
        if (users.existsByEmail(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã được đăng ký");
        return users.save(new User(input.fullName().trim(), email, passwords.encode(input.password())));
    }
    public User authenticate(LoginUserModel input) {
        checkPasswordLength(input.password());
        return (User) authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalize(input.email()), input.password())).getPrincipal();
    }
    private String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    private void checkPasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu tối đa 72 byte UTF-8");
    }
}
