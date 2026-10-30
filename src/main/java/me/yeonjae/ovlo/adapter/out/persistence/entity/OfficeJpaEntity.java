package me.yeonjae.ovlo.adapter.out.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "office",
        uniqueConstraints = @UniqueConstraint(name = "uq_office_university", columnNames = "global_univ_id"))
public class OfficeJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "global_univ_id", nullable = false)
    private Long globalUnivId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 20)
    private String status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "office", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<OfficeStaffJpaEntity> staff = new ArrayList<>();

    public OfficeJpaEntity() {}

    public void addStaff(OfficeStaffJpaEntity s) {
        s.setOffice(this);
        staff.add(s);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getGlobalUnivId() { return globalUnivId; }
    public void setGlobalUnivId(Long globalUnivId) { this.globalUnivId = globalUnivId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public List<OfficeStaffJpaEntity> getStaff() { return staff; }
}
