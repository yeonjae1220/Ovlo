package me.yeonjae.ovlo.adapter.out.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "office_source_scope")
public class OfficeSourceScopeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(name = "office_id") public Long officeId;
    @Column(name = "department", nullable = false, length = 200) public String department;
    @Column(name = "url", nullable = false, length = 1000) public String url;
    @Column(name = "enabled") public boolean enabled;
    @Version
    @Column(name = "revision") public long revision;
}
