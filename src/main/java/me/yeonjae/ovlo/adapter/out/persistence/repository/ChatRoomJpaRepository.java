package me.yeonjae.ovlo.adapter.out.persistence.repository;

import me.yeonjae.ovlo.adapter.out.persistence.entity.ChatRoomJpaEntity;
import me.yeonjae.ovlo.domain.chat.model.ChatRoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomJpaRepository extends JpaRepository<ChatRoomJpaEntity, Long> {

    @Query("SELECT cr.id FROM ChatRoomJpaEntity cr " +
            "WHERE cr.type = :type " +
            "AND :memberId1 MEMBER OF cr.participantIds " +
            "AND :memberId2 MEMBER OF cr.participantIds")
    Optional<Long> findDmRoomId(@Param("type") ChatRoomType type,
                                @Param("memberId1") Long memberId1,
                                @Param("memberId2") Long memberId2);

    @Query("SELECT cr FROM ChatRoomJpaEntity cr WHERE :memberId MEMBER OF cr.participantIds ORDER BY cr.createdAt DESC")
    List<ChatRoomJpaEntity> findByMemberId(@Param("memberId") Long memberId);

    @Query("SELECT CASE WHEN COUNT(cr) > 0 THEN true ELSE false END " +
            "FROM ChatRoomJpaEntity cr WHERE cr.id = :roomId AND :memberId MEMBER OF cr.participantIds")
    boolean existsMember(@Param("roomId") Long roomId, @Param("memberId") Long memberId);
}
