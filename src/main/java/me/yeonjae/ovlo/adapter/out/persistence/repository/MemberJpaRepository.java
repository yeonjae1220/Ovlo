package me.yeonjae.ovlo.adapter.out.persistence.repository;

import me.yeonjae.ovlo.adapter.out.persistence.entity.MemberJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberJpaRepository extends JpaRepository<MemberJpaEntity, Long> {
    Optional<MemberJpaEntity> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
    List<MemberJpaEntity> findByNicknameContainingIgnoreCase(String keyword);

    /** 본교 대학 ID만 프로젝션 — 멤버 없음/본교 null이면 empty. */
    @Query("select m.homeUniversityId from MemberJpaEntity m where m.id = :id")
    Optional<Long> findHomeUniversityIdById(@Param("id") Long id);
}
