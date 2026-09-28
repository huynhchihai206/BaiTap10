package vn.iotstar.controller;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class AuthController {
    @GetMapping({"/", "/login"}) public String login() { return "login"; }
    @GetMapping("/user/profile") public String profile() { return "profile"; }
}
