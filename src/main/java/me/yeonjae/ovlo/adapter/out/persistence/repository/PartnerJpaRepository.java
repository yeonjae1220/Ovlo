package me.yeonjae.ovlo.adapter.out.persistence.repository;

import me.yeonjae.ovlo.adapter.out.persistence.entity.PartnerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartnerJpaRepository extends JpaRepository<PartnerJpaEntity, Long> {
    List<PartnerJpaEntity> findAllByOrderBySortOrderAscIdAsc();
    List<PartnerJpaEntity> findByEnabledTrueOrderBySortOrderAscIdAsc();
}
