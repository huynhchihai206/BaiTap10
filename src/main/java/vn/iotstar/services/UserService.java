package vn.iotstar.services;
import org.springframework.stereotype.Service;
import vn.iotstar.models.UserResponse;
import vn.iotstar.repository.UserRepository;
import java.util.List;
@Service
public class UserService {
    private final UserRepository users;
    public UserService(UserRepository users) { this.users = users; }
    public List<UserResponse> allUsers() { return users.findAll().stream().map(UserResponse::from).toList(); }
}
