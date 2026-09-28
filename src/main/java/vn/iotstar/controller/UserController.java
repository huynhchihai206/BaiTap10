package vn.iotstar.controller;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.iotstar.entity.User;
import vn.iotstar.models.UserResponse;
import vn.iotstar.services.UserService;
import java.util.List;
@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService users;
    public UserController(UserService users) { this.users = users; }
    @GetMapping("/me") public UserResponse me(@AuthenticationPrincipal User user) { return UserResponse.from(user); }
    @GetMapping({"", "/"}) public List<UserResponse> allUsers() { return users.allUsers(); }
}
