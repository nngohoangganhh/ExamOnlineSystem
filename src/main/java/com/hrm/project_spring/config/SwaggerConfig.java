package com.hrm.project_spring.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Hệ Thống Thi Trắc Nghiệm API")
                        .description("""
                                ## API quản lý hệ thống thi trắc nghiệm
                                
                                ### Hướng dẫn sử dụng:
                                1. Gọi **POST /api/auth/login** để lấy token
                                2. Nhấn nút **Authorize 🔒**
                                3. Nhập JWT token
                                4. Gọi các API khác bình thường
                                
                                ### Các role mặc định:
                                - **ADMIN** – Toàn quyền quản lý hệ thống
                                - **TEACHER** – Quản lý đề thi, câu hỏi
                                - **STUDENT** – Thi và xem kết quả
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Dev Team")
                                .email("admin@examSystem.com")))
                .servers(List.of(
                        new Server().url("/").description("Current Server")
                ))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Nhập JWT token")));
    }
}