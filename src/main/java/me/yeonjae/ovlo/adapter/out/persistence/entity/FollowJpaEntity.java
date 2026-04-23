package me.yeonjae.ovlo.adapter.out.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "follow")
public class FollowJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "follower_id", nullable = false) private Long followerId;
    @Column(name = "followee_id", nullable = false) private Long followeeId;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "hidden_by_withdrawal", nullable = false) private boolean hiddenByWithdrawal;
    public FollowJpaEntity() {}
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getFollowerId() { return followerId; } public void setFollowerId(Long followerId) { this.followerId = followerId; }
    public Long getFolloweeId() { return followeeId; } public void setFolloweeId(Long followeeId) { this.followeeId = followeeId; }
    public boolean isHiddenByWithdrawal() { return hiddenByWithdrawal; } public void setHiddenByWithdrawal(boolean hiddenByWithdrawal) { this.hiddenByWithdrawal = hiddenByWithdrawal; }
}
