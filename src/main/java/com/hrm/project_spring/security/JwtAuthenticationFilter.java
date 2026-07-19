package com.hrm.project_spring.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j // Sử dụng Log thay vì System.out.println để chuyên nghiệp hơn
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // 1. Kiểm tra sự tồn tại của Header Authorization
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 2. Trích xuất token và loại bỏ khoảng trắng thừa
            jwt = authHeader.substring(7).trim();
            // 3. Kiểm tra xem token có thực sự tồn tại sau chữ "Bearer " hay không
            // Nếu token là chuỗi rỗng hoặc chữ "null" (lỗi từ frontend), bỏ qua filter này
            if (jwt.isBlank() || jwt.chars().filter(ch -> ch == '.').count() != 2) {
                log.warn("Invalid JWT format");
                filterChain.doFilter(request, response);
                return;
            }

            // 4. Giải mã username từ token
            username = jwtService.extractUsername(jwt);

            // 5. Nếu có username và chưa được xác thực trong SecurityContext
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                // 6. Kiểm tra tính hợp lệ của token
                if (jwtService.isAccessTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Lưu thông tin xác thực vào hệ thống
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.info("Authenticated user: {}", username);
                }
            }
        } catch (Exception e) {
            // In lỗi ra log để debug nhưng không làm sập ứng dụng
            log.error("Cannot set user authentication: {}", e.getMessage());
        }
        // Luôn gọi doFilter để request được tiếp tục xử lý
        filterChain.doFilter(request, response);
    }
}