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

    String ACCESS = """
        EXISTS (SELECT 1 FROM chat_room_participant p JOIN member m ON m.id=p.member_id
                WHERE p.chat_room_id=cr.id AND p.member_id=:memberId AND m.status='ACTIVE')
        AND NOT EXISTS (
            SELECT 1 FROM office_consultation c WHERE c.chat_room_id=cr.id
            AND c.member_id<>:memberId
            AND NOT EXISTS (
                SELECT 1 FROM office_staff s JOIN office o ON o.id=s.office_id
                WHERE s.office_id=c.office_id AND s.member_id=:memberId
                AND s.status='ACTIVE' AND o.status IN ('PILOT','ACTIVE')))
        """;

    @Query(value = "SELECT cr.* FROM chat_room cr WHERE " + ACCESS + " ORDER BY cr.created_at DESC", nativeQuery = true)
    List<ChatRoomJpaEntity> findByMemberId(@Param("memberId") Long memberId);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM chat_room cr WHERE cr.id=:roomId AND " + ACCESS + ")", nativeQuery = true)
    boolean existsMember(@Param("roomId") Long roomId, @Param("memberId") Long memberId);
}
