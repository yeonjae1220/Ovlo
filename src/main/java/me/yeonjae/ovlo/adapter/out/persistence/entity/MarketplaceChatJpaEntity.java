package me.yeonjae.ovlo.adapter.out.persistence.entity;
import jakarta.persistence.*;
/** Schema metadata also supplies the H2 development schema; writes go through the marketplace port. */
@Entity @Table(name="marketplace_chat",uniqueConstraints=@UniqueConstraint(columnNames={"listing_id","buyer_id"}))
public class MarketplaceChatJpaEntity {
    @Id @Column(name="room_id") private Long roomId;
    @Column(name="listing_id",nullable=false) private Long listingId;
    @Column(name="buyer_id",nullable=false) private Long buyerId;
    @Column(name="seller_id",nullable=false) private Long sellerId;
    protected MarketplaceChatJpaEntity() {}
}
