package vn.iotstar.filter;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.iotstar.services.JwtService;
import vn.iotstar.exception.SecurityErrorWriter;
import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UserDetailsService users;
    private final SecurityErrorWriter errors;
    public JwtAuthenticationFilter(JwtService jwt, UserDetailsService users, SecurityErrorWriter errors) {
        this.jwt = jwt; this.users = users; this.errors = errors;
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                              FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            try {
                String email = jwt.validateToken(header.substring(7)).getSubject();
                var user = users.loadUserByUsername(email);
                if (!user.isAccountNonLocked()) throw new LockedException("Tài khoản bị khóa");
                if (!user.isEnabled()) throw new DisabledException("Tài khoản đã bị vô hiệu hóa");
                var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (AccountStatusException exception) {
                SecurityContextHolder.clearContext();
                errors.write(response, 403, "Tài khoản bị khóa hoặc vô hiệu hóa"); return;
            } catch (AuthenticationException exception) {
                SecurityContextHolder.clearContext();
                errors.write(response, 401, "JWT không hợp lệ hoặc đã hết hạn"); return;
            }
        }
        chain.doFilter(request, response);
    }
}
