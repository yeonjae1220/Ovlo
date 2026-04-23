package me.yeonjae.ovlo.adapter.out.persistence.repository;

import me.yeonjae.ovlo.adapter.out.persistence.entity.PostJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostJpaRepository extends JpaRepository<PostJpaEntity, Long> {
    List<PostJpaEntity> findByBoardIdAndDeletedFalseAndHiddenByWithdrawalFalse(Long boardId, Pageable pageable);
    long countByBoardIdAndDeletedFalseAndHiddenByWithdrawalFalse(Long boardId);
    List<PostJpaEntity> findAllByDeletedFalseAndHiddenByWithdrawalFalse(Pageable pageable);
    long countByDeletedFalseAndHiddenByWithdrawalFalse();

    @Modifying
    @Query("UPDATE PostJpaEntity p SET p.hiddenByWithdrawal = true WHERE p.authorId = :authorId")
    void hideAllByAuthorId(@Param("authorId") Long authorId);
}
