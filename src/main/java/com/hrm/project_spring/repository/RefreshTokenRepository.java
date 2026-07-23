package com.hrm.project_spring.repository;

import com.hrm.project_spring.entity.RefreshToken;
import com.hrm.project_spring.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // Tìm token theo chuỗi token (dùng cho refreshToken flow)
    Optional<RefreshToken> findByToken(String refreshToken);

    // Đếm số thiết bị đang active của user (BR-004: giới hạn 3 thiết bị)
    long countByUserAndRevokedFalseAndExpiresAtAfter(User user, LocalDateTime now);

    // Lấy token cũ nhất còn sống (dùng khi quá 3 thiết bị → revoke cái cũ nhất)
    Optional<RefreshToken> findTopByUserAndRevokedFalseOrderByCreatedAtAsc(User user);

    // Lấy token mới nhất còn sống (dùng khi đổi mật khẩu → giữ phiên hiện tại)
    Optional<RefreshToken> findTopByUserAndRevokedFalseOrderByCreatedAtDesc(User user);

    // Xóa toàn bộ token của user (dùng cho: logout, forgotPassword, resetPassword)
    @Modifying
    @Transactional
    void deleteAllByUser(User user);

    // Thu hồi tất cả token của user TRỪ token hiện tại (dùng sau changePassword)
    // → Các thiết bị khác bị kick ra, thiết bị đang đổi mật khẩu vẫn giữ phiên
    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken rt SET rt.revoked = true " +
           "WHERE rt.user = :user AND rt.id != :currentId")
    void revokeAllByUserExceptCurrent(@Param("user") User user, @Param("currentId") Long currentId);
}
