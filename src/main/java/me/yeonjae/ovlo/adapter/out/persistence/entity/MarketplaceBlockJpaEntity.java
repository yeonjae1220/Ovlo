package me.yeonjae.ovlo.adapter.out.persistence.entity;
import jakarta.persistence.*;
import java.io.Serializable;
@Entity @Table(name="marketplace_block") @IdClass(MarketplaceBlockJpaEntity.Key.class)
public class MarketplaceBlockJpaEntity {
    @Id @Column(name="actor_id") private Long actorId;
    @Id @Column(name="target_id") private Long targetId;
    protected MarketplaceBlockJpaEntity() {}
    public record Key(Long actorId,Long targetId) implements Serializable {}
}
