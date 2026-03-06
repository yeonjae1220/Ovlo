package me.yeonjae.ovlo.adapter.out.persistence.entity;

import jakarta.persistence.*;
import me.yeonjae.ovlo.domain.board.model.BoardCategory;
import me.yeonjae.ovlo.domain.board.model.LocationScope;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;

@Entity
@Table(name = "board")
public class BoardJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String name;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(nullable = false) @Enumerated(EnumType.STRING) private BoardCategory category;
    @Column(nullable = false) @Enumerated(EnumType.STRING) private LocationScope scope;
    @Column(name = "creator_id", nullable = false) private Long creatorId;
    @Column(name = "university_id") private Long universityId;
    @Column(nullable = false) private boolean active;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    public BoardJpaEntity() {}
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getDescription() { return description; } public void setDescription(String description) { this.description = description; }
    public BoardCategory getCategory() { return category; } public void setCategory(BoardCategory category) { this.category = category; }
    public LocationScope getScope() { return scope; } public void setScope(LocationScope scope) { this.scope = scope; }
    public Long getCreatorId() { return creatorId; } public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }
    public Long getUniversityId() { return universityId; } public void setUniversityId(Long universityId) { this.universityId = universityId; }
    public boolean isActive() { return active; } public void setActive(boolean active) { this.active = active; }
}
