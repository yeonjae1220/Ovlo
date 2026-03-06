package me.yeonjae.ovlo.adapter.out.persistence.repository;

import me.yeonjae.ovlo.adapter.out.persistence.entity.UniversityJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UniversityJpaRepository extends JpaRepository<UniversityJpaEntity, Long> {
}
