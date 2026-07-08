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
}
