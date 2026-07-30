package com.hrm.project_spring.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;


    @Value("${spring.mail.username}")
    private String fromEmail;


    @Value("${app.frontend-url}")
    private String frontendUrl;


    /**
     * UC08:
     * Gửi email kích hoạt tài khoản
     */
    public void sendActivationEmail(
            String toEmail,
            String fullName,
            String activationToken
    ) {
        String activationLink = frontendUrl + "api/users/activate?token=" + activationToken;

        String content = """
                <h3>Xin chào %s</h3>
                
                <p>Tài khoản của bạn đã được tạo trên hệ thống.</p>
                
                <p>Vui lòng click link bên dưới để kích hoạt tài khoản:</p>
                
                <a href="%s">
                    Kích hoạt tài khoản
                </a>
                
                <p>
                Link có hiệu lực trong 7 ngày.
                </p>
                
                """
                .formatted(fullName, activationLink);
        sendHtmlEmail(
                toEmail,
                "Kích hoạt tài khoản Exam-Sys",
                content
        );
    }
    /**
     * UC03:
     * Gửi email reset password
     */
    public void sendResetPasswordEmail(
            String toEmail,
            String token
    ) {

        String resetLink =
                frontendUrl + "/api/auth/reset-password?token=" + token;


        String content = """
                <h3>Yêu cầu đặt lại mật khẩu</h3>
                
                <p>
                Một yêu cầu reset password đã được gửi.
                </p>
                
                <a href="%s">
                    Đặt lại mật khẩu
                </a>
                
                <p>
                Link có hiệu lực trong 15 phút.
                </p>
                
                """
                .formatted(resetLink);


        sendHtmlEmail(
                toEmail,
                "Đặt lại mật khẩu Exam-Sys",
                content
        );
    }


    /**
     * UC10:
     * Email thông báo khóa tài khoản
     */
    public void sendAccountLockedEmail(
            String toEmail,
            String fullName,
            String reason,
            LocalDateTime lockUntil
    ) {
        String lockTime =
                lockUntil != null
                        ?
                        lockUntil.format(
                                DateTimeFormatter.ofPattern(
                                        "dd/MM/yyyy HH:mm"
                                )
                        )
                        :
                        "Vô thời hạn";


        String content = """
                <h3>Xin chào %s</h3>
                
                <p>
                Tài khoản của bạn đã bị khóa.
                </p>
                
                <p>
                Lý do: %s
                </p>
                
                <p>
                Thời hạn: %s
                </p>
                """
                .formatted(
                        fullName,
                        reason,
                        lockTime
                );


        sendHtmlEmail(
                toEmail,
                "Tài khoản Exam-Sys bị khóa",
                content
        );
    }
    /**
     * UC11:
     * Email thông báo xóa tài khoản
     */
    public void sendAccountDeletedEmail(
            String toEmail,
            String fullName,
            String reason
    ) {

        String content = """
                <h3>Xin chào %s</h3>
                
                <p>
                Tài khoản của bạn đã bị xóa khỏi hệ thống.
                </p>
                
                <p>
                Lý do: %s
                </p>
                """
                .formatted(
                        fullName,
                        reason
                );


        sendHtmlEmail(
                toEmail,
                "Tài khoản Exam-Sys đã bị xóa",
                content
        );
    }
    /**
     * UC03:
     * Email xác nhận đổi mật khẩu thành công
     */
    public void sendPasswordResetSuccessEmail(
            String toEmail,
            String fullName
    ) {


        String content = """
                <h3>Xin chào %s</h3>
                
                <p>
                Mật khẩu của bạn đã được thay đổi thành công.
                </p>
                
                <p>
                Nếu không phải bạn thực hiện,
                hãy liên hệ Admin.
                </p>
                """
                .formatted(fullName);


        sendHtmlEmail(
                toEmail,
                "Mật khẩu đã được thay đổi",
                content
        );
    }
    /**
     * Hàm dùng chung gửi email HTML
     */
    private void sendHtmlEmail(
            String toEmail,
            String subject,
            String content
    ) {

        try {

            MimeMessage message =
                    mailSender.createMimeMessage();


            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            message,
                            true,
                            "UTF-8"
                    );


            helper.setFrom(fromEmail);

            helper.setTo(toEmail);

            helper.setSubject(subject);

            helper.setText(
                    content,
                    true
            );


            mailSender.send(message);


            log.info(
                    "Email sent successfully to {}",
                    toEmail
            );


        } catch (MessagingException e) {

            log.error(
                    "Cannot send email to {}",
                    toEmail,
                    e
            );

            throw new RuntimeException(
                    "Gửi email thất bại"
            );
        }
    }
}