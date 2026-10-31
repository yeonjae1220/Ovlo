package me.yeonjae.ovlo.adapter.out.persistence.repository;

import me.yeonjae.ovlo.adapter.out.persistence.entity.ChatMessageTranslationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatMessageTranslationJpaRepository extends JpaRepository<ChatMessageTranslationJpaEntity, Long> {
    List<ChatMessageTranslationJpaEntity> findByMessageIdInAndLang(List<Long> messageIds, String lang);
    Optional<ChatMessageTranslationJpaEntity> findByMessageIdAndLang(Long messageId, String lang);
}
