package me.yeonjae.ovlo.adapter.out.persistence.repository;

import me.yeonjae.ovlo.adapter.out.persistence.entity.MessageJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MessageJpaRepository extends JpaRepository<MessageJpaEntity, Long> {
    List<MessageJpaEntity> findByChatRoomIdOrderBySentAtAsc(Long chatRoomId);
}
