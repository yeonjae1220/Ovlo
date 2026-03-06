package me.yeonjae.ovlo.adapter.out.persistence.repository;

import me.yeonjae.ovlo.adapter.out.persistence.entity.MediaFileJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaFileJpaRepository extends JpaRepository<MediaFileJpaEntity, Long> {
}
