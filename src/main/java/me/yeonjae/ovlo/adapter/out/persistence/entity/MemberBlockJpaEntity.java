package me.yeonjae.ovlo.adapter.out.persistence.entity;
import jakarta.persistence.*;
import java.io.Serializable;
@Entity @Table(name="member_block") @IdClass(MemberBlockJpaEntity.Key.class)
public class MemberBlockJpaEntity {
    @Id @Column(name="actor_id") private Long actorId;
    @Id @Column(name="target_id") private Long targetId;
    protected MemberBlockJpaEntity() {}
    public record Key(Long actorId,Long targetId) implements Serializable {}
}
