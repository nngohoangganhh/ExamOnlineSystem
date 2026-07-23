package com.hrm.project_spring.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class EmailService {

    /**
     * Mô phỏng việc gửi email chứa mã khôi phục mật khẩu.
     */
    public void sendResetPasswordEmail(String toEmail, String token) {
        log.info("----------------------------------------------------------");
        log.info("ĐANG GỬI EMAIL ĐẶT LẠI MẬT KHẨU...");
        log.info("Đến: {}", toEmail);
        log.info("Chủ đề: Yêu cầu đặt lại mật khẩu của bạn");
        log.info("Mã token để đặt lại mật khẩu của bạn là: {}", token);
        log.info("Mã này có hiệu lực trong 15 phút.");
        log.info("----------------------------------------------------------");
    }

    /**
     * UC08: Gửi email kích hoạt tài khoản cho user mới.
     */
    public void sendActivationEmail(String toEmail, String fullName, String activationToken) {
        String activationLink = "http://localhost:8080/api/users/activate?token=" + activationToken;
        log.info("==========================================================");
        log.info("ĐANG GỬI EMAIL KÍCH HOẠT TÀI KHOẢN...");
        log.info("Đến: {}", toEmail);
        log.info("Chủ đề: Kích hoạt tài khoản Exam-Sys của bạn");
        log.info("Xin chào {},", fullName);
        log.info("Link kích hoạt: {}", activationLink);
        log.info("Link này có hiệu lực trong 7 ngày.");
        log.info("==========================================================");
    }

    /**
     * UC10: Gửi email thông báo tài khoản bị khóa.
     *
     * @param toEmail    Địa chỉ email người nhận
     * @param fullName   Tên người dùng
     * @param reason     Lý do khóa
     * @param lockUntil  Thời hạn khóa (null = vô thời hạn)
     */
    public void sendAccountLockedEmail(String toEmail, String fullName, String reason, LocalDateTime lockUntil) {
        log.info("==========================================================");
        log.info("ĐANG GỬI EMAIL THÔNG BÁO KHÓA TÀI KHOẢN...");
        log.info("Đến: {}", toEmail);
        log.info("Chủ đề: Tài khoản Exam-Sys của bạn đã bị khóa");
        log.info("Xin chào {},", fullName);
        log.info("Tài khoản của bạn đã bị khóa với lý do: {}", reason);
        if (lockUntil != null) {
            log.info("Thời hạn khóa đến: {}", lockUntil.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        } else {
            log.info("Tài khoản bị khóa vô thời hạn. Vui lòng liên hệ Admin để biết thêm thông tin.");
        }
        log.info("==========================================================");
    }

    /**
     * UC11: Gửi email thông báo tài khoản bị xóa.
     *
     * @param toEmail  Địa chỉ email người nhận (sẽ có hậu tố _deleted_)
     * @param fullName Tên người dùng
     * @param reason   Lý do xóa
     */
    public void sendAccountDeletedEmail(String toEmail, String fullName, String reason) {
        log.info("==========================================================");
        log.info("ĐANG GỬI EMAIL THÔNG BÁO XÓA TÀI KHOẢN...");
        log.info("Đến: {}", toEmail);
        log.info("Chủ đề: Tài khoản Exam-Sys của bạn đã bị xóa");
        log.info("Xin chào {},", fullName);
        log.info("Tài khoản của bạn đã bị xóa khỏi hệ thống với lý do: {}", reason);
        log.info("Nếu bạn cho rằng đây là nhầm lẫn, vui lòng liên hệ Admin trong vòng 30 ngày để khôi phục.");
        log.info("==========================================================");
    }

    /**
     * UC03: Gửi email xác nhận mật khẩu đã được đặt lại thành công.
     *
     * @param toEmail  Địa chỉ email người nhận
     * @param fullName Tên người dùng
     */
    public void sendPasswordResetSuccessEmail(String toEmail, String fullName) {
        log.info("==========================================================");
        log.info("ĐANG GỬI EMAIL XÁC NHẬN ĐẶT LẠI MẬT KHẨU...");
        log.info("Đến: {}", toEmail);
        log.info("Chủ đề: Mật khẩu Exam-Sys của bạn đã được thay đổi");
        log.info("Xin chào {},", fullName);
        log.info("Mật khẩu tài khoản của bạn vừa được đặt lại thành công.");
        log.info("Nếu bạn không thực hiện hành động này, hãy liên hệ Admin ngay lập tức.");
        log.info("==========================================================");
    }
}
