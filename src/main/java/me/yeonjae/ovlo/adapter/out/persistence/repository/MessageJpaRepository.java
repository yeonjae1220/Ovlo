package me.yeonjae.ovlo.adapter.out.persistence.repository;

import me.yeonjae.ovlo.adapter.out.persistence.entity.MessageJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageJpaRepository extends JpaRepository<MessageJpaEntity, Long> {
    List<MessageJpaEntity> findByChatRoomIdAndHiddenByWithdrawalFalseOrderBySentAtAsc(Long chatRoomId);
    List<MessageJpaEntity> findByChatRoomIdAndHiddenByWithdrawalFalseOrderBySentAtDesc(Long chatRoomId, Pageable pageable);

    @Query("SELECT COUNT(m) FROM MessageJpaEntity m WHERE m.chatRoomId = :chatRoomId AND m.senderId != :memberId AND m.sentAt > :since AND m.hiddenByWithdrawal = false")
    long countUnread(@Param("chatRoomId") Long chatRoomId, @Param("memberId") Long memberId, @Param("since") LocalDateTime since);

    @Modifying
    @Query("UPDATE MessageJpaEntity m SET m.hiddenByWithdrawal = true WHERE m.senderId = :senderId")
    void hideAllBySenderId(@Param("senderId") Long senderId);
}
