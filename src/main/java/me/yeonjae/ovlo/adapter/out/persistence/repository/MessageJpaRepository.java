package me.yeonjae.ovlo.adapter.out.persistence.repository;

import me.yeonjae.ovlo.adapter.out.persistence.entity.MessageJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface MessageJpaRepository extends JpaRepository<MessageJpaEntity, Long> {
    String VISIBLE="m.chatRoomId=:chatRoomId AND m.hiddenByWithdrawal=false AND NOT EXISTS (SELECT c.id FROM OfficeConsultationJpaEntity c WHERE c.chatRoomId=m.chatRoomId AND (c.personalDataErasedAt IS NOT NULL OR c.personalDataExpiresAt<=CURRENT_TIMESTAMP))";
    @Query("SELECT m FROM MessageJpaEntity m WHERE "+VISIBLE+" ORDER BY m.sentAt ASC")
    List<MessageJpaEntity> findByChatRoomIdAndHiddenByWithdrawalFalseOrderBySentAtAsc(@Param("chatRoomId") Long chatRoomId);
    @Query("SELECT m FROM MessageJpaEntity m WHERE "+VISIBLE+" ORDER BY m.sentAt DESC")
    List<MessageJpaEntity> findByChatRoomIdAndHiddenByWithdrawalFalseOrderBySentAtDesc(@Param("chatRoomId") Long chatRoomId, Pageable pageable);

    @Query("SELECT COUNT(m) FROM MessageJpaEntity m WHERE m.chatRoomId = :chatRoomId AND m.senderId != :memberId AND m.sentAt > :since AND m.hiddenByWithdrawal = false")
    long countUnread(@Param("chatRoomId") Long chatRoomId, @Param("memberId") Long memberId, @Param("since") Instant since);

    @Modifying
    @Query("UPDATE MessageJpaEntity m SET m.hiddenByWithdrawal = true WHERE m.senderId = :senderId")
    void hideAllBySenderId(@Param("senderId") Long senderId);
}
