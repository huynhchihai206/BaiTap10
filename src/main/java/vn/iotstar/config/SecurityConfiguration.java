package vn.iotstar.config;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import vn.iotstar.filter.JwtAuthenticationFilter;
import vn.iotstar.services.JwtService;
import vn.iotstar.exception.SecurityErrorWriter;
@Configuration
public class SecurityConfiguration {
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwt,
                                                  UserDetailsService users, SecurityErrorWriter errors) throws Exception {
        return http
                // API uses explicit Bearer headers, never authentication cookies.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable()).httpBasic(basic -> basic.disable()).logout(logout -> logout.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/auth/signup", "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/", "/login", "/user/profile", "/js/**", "/css/**", "/images/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/users", "/users/", "/users/me").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(errorsConfig -> errorsConfig
                        .authenticationEntryPoint((req, res, ex) -> errors.write(res, 401, "Bạn cần đăng nhập"))
                        .accessDeniedHandler((req, res, ex) -> errors.write(res, 403, "Bạn không có quyền truy cập")))
                .addFilterBefore(new JwtAuthenticationFilter(jwt, users, errors), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
