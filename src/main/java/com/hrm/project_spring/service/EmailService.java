package com.hrm.project_spring.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    /**
     * Mô phỏng việc gửi email chứa mã khôi phục mật khẩu.
     * Trong thực tế, bạn sẽ dùng JavaMailSender để cấu hình SMTP ở đây.
     *
     * @param toEmail Địa chỉ email người nhận
     * @param token   Mã token đặt lại mật khẩu
     */
    public void sendResetPasswordEmail(String toEmail, String token) {
        log.info("----------------------------------------------------------");
        log.info("ĐANG GỬI EMAIL ĐẶT LẠI MẬT KHẨU...");
        log.info("Đến: {}", toEmail);
        log.info("Chủ đề: Yêu cầu đặt lại mật khẩu của bạn");
        log.info("Nội dung:");
        log.info("Mã token để đặt lại mật khẩu của bạn là: {}", token);
        log.info("Mã này có hiệu lực trong 15 phút.");
        log.info("Vui lòng nhập mã này cùng với mật khẩu mới vào form reset password.");
        log.info("----------------------------------------------------------");
    }

    /**
     * UC08: Gửi email kích hoạt tài khoản cho user mới.
     * Trong thực tế sẽ dùng JavaMailSender + template HTML.
     *
     * @param toEmail         Địa chỉ email người nhận
     * @param fullName        Tên người dùng
     * @param activationToken Token kích hoạt (32 byte hex)
     */
    public void sendActivationEmail(String toEmail, String fullName, String activationToken) {
        String activationLink = "http://localhost:8080/api/auth/activate?token=" + activationToken;
        log.info("==========================================================");
        log.info("ĐANG GỬI EMAIL KÍCH HOẠT TÀI KHOẢN...");
        log.info("Đến: {}", toEmail);
        log.info("Chủ đề: Kích hoạt tài khoản Exam-Sys của bạn");
        log.info("Nội dung:");
        log.info("Xin chào {},", fullName);
        log.info("Tài khoản của bạn đã được tạo trên hệ thống Exam-Sys.");
        log.info("Vui lòng click link dưới đây để kích hoạt tài khoản:");
        log.info("Link kích hoạt: {}", activationLink);
        log.info("Link này có hiệu lực trong 7 ngày.");
        log.info("==========================================================");
    }
}

