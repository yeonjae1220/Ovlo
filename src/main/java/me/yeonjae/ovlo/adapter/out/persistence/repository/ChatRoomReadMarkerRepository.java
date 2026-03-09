package me.yeonjae.ovlo.adapter.out.persistence.repository;

import me.yeonjae.ovlo.adapter.out.persistence.entity.ChatRoomReadMarkerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatRoomReadMarkerRepository
        extends JpaRepository<ChatRoomReadMarkerJpaEntity, ChatRoomReadMarkerJpaEntity.ReadMarkerId> {

    @Query("SELECT m FROM ChatRoomReadMarkerJpaEntity m WHERE m.id.chatRoomId = :chatRoomId")
    List<ChatRoomReadMarkerJpaEntity> findAllByChatRoomId(@Param("chatRoomId") Long chatRoomId);
}
