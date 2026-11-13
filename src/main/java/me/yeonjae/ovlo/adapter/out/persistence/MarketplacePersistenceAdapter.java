package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.application.port.out.marketplace.MarketplaceStore;
import me.yeonjae.ovlo.application.dto.command.MarketplaceSearch;
import me.yeonjae.ovlo.application.dto.result.ListingTranslation;
import me.yeonjae.ovlo.application.dto.result.ListingResult;
import me.yeonjae.ovlo.domain.marketplace.model.*;
import me.yeonjae.ovlo.domain.marketplace.exception.MarketplaceException;
import me.yeonjae.ovlo.domain.media.model.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;
import static me.yeonjae.ovlo.domain.marketplace.exception.MarketplaceException.Kind.*;

@Repository
public class MarketplacePersistenceAdapter implements MarketplaceStore {
    private final NamedParameterJdbcTemplate jdbc;
    public MarketplacePersistenceAdapter(NamedParameterJdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Override public Optional<Listing> find(long id, boolean lock) {
        return jdbc.query("SELECT * FROM marketplace_listing WHERE id=:id" + (lock ? " FOR UPDATE" : ""), Map.of("id", id), this::listing).stream().findFirst();
    }
    private Listing listing(ResultSet r, int row) throws SQLException {
        long id=r.getLong("id");
        var photos=jdbc.query("SELECT photo_id FROM marketplace_listing_photo WHERE listing_id=:id ORDER BY position", Map.of("id", id), (p,n)->p.getObject(1, UUID.class));
        return listing(r,photos);
    }
    private Listing listing(ResultSet r,List<UUID> photos) throws SQLException {
        long id=r.getLong("id");
        var tags=Arrays.stream(r.getString("tags").split(",")).filter(s->!s.isBlank()).map(ListingTag::valueOf).collect(Collectors.toSet());
        var details=new ListingDetails(r.getString("title"),r.getString("description"),r.getString("source_language"),r.getString("category"),r.getString("item_condition"),
                r.getBoolean("free"),r.getBoolean("bundle"),r.getBoolean("partial_pickup"),new AskingPrice(r.getBigDecimal("amount"),r.getString("currency")),
                new PickupLocation(r.getString("country_code"),r.getString("city"),r.getObject("university_id",Long.class),r.getString("meeting_point")),
                r.getObject("pickup_from",java.time.LocalDate.class),r.getObject("pickup_until",java.time.LocalDate.class),tags,photos);
        return Listing.restore(id,r.getLong("seller_id"),details,ListingStatus.valueOf(r.getString("status")),r.getBoolean("hidden"),r.getBoolean("deleted"),r.getLong("version"),r.getTimestamp("created_at").toInstant());
    }
    @Override public List<ListingResult> search(long actor, MarketplaceSearch s) {
        StringBuilder sql=new StringBuilder("""
                WITH page AS (
                SELECT l.*,seller.nickname AS seller_name,school.name_en AS university_name
                FROM marketplace_listing l JOIN member seller ON seller.id=l.seller_id AND seller.status='ACTIVE'
                LEFT JOIN global_universities school ON school.id=l.university_id WHERE NOT l.deleted
                """);
        var p=new MapSqlParameterSource("actor",actor).addValue("offset",s.page()*20);
        if (s.mine()) sql.append(" AND l.seller_id=:actor");
        else sql.append(" AND NOT l.hidden AND NOT EXISTS (SELECT 1 FROM marketplace_block b WHERE (b.actor_id=:actor AND b.target_id=l.seller_id) OR (b.target_id=:actor AND b.actor_id=l.seller_id))");
        filter(sql,p,"country","l.country_code",s.country()); filter(sql,p,"city","lower(l.city)",s.city()==null?null:s.city().trim().toLowerCase(Locale.ROOT));
        filter(sql,p,"school","l.university_id",s.universityId()); filter(sql,p,"category","l.category",s.category()); filter(sql,p,"currency","l.currency",s.currency());
        filter(sql,p,"free","l.free",s.free()); filter(sql,p,"bundle","l.bundle",s.bundle());
        if (s.keyword()!=null && !s.keyword().isBlank()) { sql.append(" AND position(:keyword in lower(l.title)) > 0"); p.addValue("keyword",s.keyword().trim().toLowerCase(Locale.ROOT)); }
        if (s.tag()!=null) { sql.append(" AND l.tags LIKE :tag"); p.addValue("tag","%,"+s.tag().name()+",%"); }
        if (s.availableOnly()) sql.append(" AND l.status <> 'COMPLETED'");
        // Page first, then aggregate just those photos. One statement regardless of page size.
        sql.append("""
                 ORDER BY l.id DESC LIMIT 21 OFFSET :offset
                ), photos AS (
                    SELECT lp.listing_id,array_agg(lp.photo_id ORDER BY lp.position) AS photo_ids
                    FROM marketplace_listing_photo lp JOIN page ON page.id=lp.listing_id GROUP BY lp.listing_id
                )
                SELECT page.*,photos.photo_ids FROM page JOIN photos ON photos.listing_id=page.id ORDER BY page.id DESC
                """);
        return jdbc.query(sql.toString(),p,(r,n)->{
            var photoArray=r.getArray("photo_ids");
            List<UUID> photos;
            try { photos=Arrays.stream((Object[])photoArray.getArray()).map(value->UUID.fromString(value.toString())).toList(); }
            finally { photoArray.free(); }
            var listing=listing(r,photos);
            return new ListingResult(listing.id(),listing.sellerId(),r.getString("seller_name"),r.getString("university_name"),
                    listing.details(),listing.status(),listing.hidden(),listing.version(),listing.createdAt());
        });
    }
    private void filter(StringBuilder sql, MapSqlParameterSource p, String key, String column, Object value) {
        if (value!=null && !value.equals("")) { sql.append(" AND ").append(column).append("=:").append(key); p.addValue(key,value); }
    }
    @Override public Listing save(Listing l, long expectedVersion) {
        var d=l.details(); var p=new MapSqlParameterSource();
        p.addValue("seller_id",l.sellerId()).addValue("title",d.title()).addValue("description",d.description()).addValue("source_language",d.sourceLanguage())
                .addValue("category",d.category()).addValue("item_condition",d.condition()).addValue("free",d.free()).addValue("bundle",d.bundle()).addValue("partial_pickup",d.partialPickup())
                .addValue("amount",d.price().amount()).addValue("currency",d.price().currency()).addValue("country_code",d.location().countryCode()).addValue("city",d.location().city())
                .addValue("university_id",d.location().universityId()).addValue("meeting_point",d.location().meetingPoint()).addValue("pickup_from",d.pickupFrom()).addValue("pickup_until",d.pickupUntil())
                .addValue("tags",","+d.tags().stream().map(Enum::name).sorted().collect(Collectors.joining(","))+",").addValue("status",l.status().name())
                .addValue("hidden",l.hidden()).addValue("deleted",l.deleted());
        long id;
        if (l.id()==null) {
            var keys=new GeneratedKeyHolder();
            String columns=String.join(",",p.getValues().keySet());
            String values=p.getValues().keySet().stream().map(k->":"+k).collect(Collectors.joining(","));
            jdbc.update("INSERT INTO marketplace_listing ("+columns+") VALUES ("+values+")",p,keys,new String[]{"id"});
            id=Objects.requireNonNull(keys.getKey()).longValue();
        } else {
            id=l.id();
            String assignments=p.getValues().keySet().stream().map(k->k+"=:"+k).collect(Collectors.joining(","));
            p.addValue("id",id).addValue("expected",expectedVersion);
            if (jdbc.update("UPDATE marketplace_listing SET "+assignments+", version=version+1 WHERE id=:id AND version=:expected",p)!=1)
                throw new MarketplaceException(CONFLICT,"Listing changed. Reload before saving.");
            jdbc.update("""
                    UPDATE marketplace_photo SET orphaned_at=CURRENT_TIMESTAMP
                    WHERE id IN (SELECT photo_id FROM marketplace_listing_photo WHERE listing_id=:id)
                    AND id NOT IN (:retained)
                    """,Map.of("id",id,"retained",d.photoIds()));
            jdbc.update("DELETE FROM marketplace_listing_photo WHERE listing_id=:id",Map.of("id",id));
            jdbc.update("DELETE FROM marketplace_translation WHERE listing_id=:id",Map.of("id",id));
        }
        for(int i=0;i<d.photoIds().size();i++) jdbc.update("INSERT INTO marketplace_listing_photo(listing_id,photo_id,position) VALUES(:id,:photo,:position)",Map.of("id",id,"photo",d.photoIds().get(i),"position",i));
        jdbc.update("UPDATE marketplace_photo SET orphaned_at=NULL WHERE id IN (:ids)",Map.of("ids",d.photoIds()));
        return find(id,false).orElseThrow();
    }
    @Override public void addPhoto(Photo p) {
        jdbc.update("INSERT INTO marketplace_photo(id,owner_id,storage_path,storage_type) VALUES(:id,:owner,:path,:type)",Map.of("id",p.id(),"owner",p.owner(),"path",p.path().path(),"type",p.path().storageType().name()));
    }
    @Override public Optional<Photo> photo(UUID id) {
        return jdbc.query("SELECT * FROM marketplace_photo WHERE id=:id",Map.of("id",id),(r,n)->new Photo(id,r.getLong("owner_id"),new StoragePath(r.getString("storage_path"),StorageType.valueOf(r.getString("storage_type"))))).stream().findFirst();
    }
    @Override public void validatePhotos(long owner, Long listingId, List<UUID> ids) {
        // Stable lock ordering serializes attempts to attach the same upload to different listings.
        for(UUID id:ids.stream().sorted().toList()) {
            var owners=jdbc.queryForList("SELECT owner_id FROM marketplace_photo WHERE id=:id AND NOT deletion_pending FOR UPDATE",Map.of("id",id),Long.class);
            if(owners.isEmpty() || owners.getFirst()!=owner) throw new MarketplaceException(FORBIDDEN,"Photo does not belong to you");
            var attached=jdbc.queryForList("SELECT listing_id FROM marketplace_listing_photo WHERE photo_id=:id",Map.of("id",id),Long.class);
            if(!attached.isEmpty() && !Objects.equals(attached.getFirst(),listingId)) throw new MarketplaceException(CONFLICT,"Photo already belongs to another listing");
        }
    }
    @Override public boolean photoVisible(UUID id, long viewer) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM marketplace_photo p
                JOIN member owner ON owner.id=p.owner_id AND owner.status='ACTIVE'
                LEFT JOIN marketplace_listing_photo lp ON lp.photo_id=p.id
                LEFT JOIN marketplace_listing l ON l.id=lp.listing_id
                WHERE p.id=:id AND NOT p.deletion_pending AND ((lp.listing_id IS NULL AND p.owner_id=:viewer) OR
                    (NOT l.deleted AND NOT l.hidden AND NOT EXISTS (
                        SELECT 1 FROM marketplace_block b WHERE
                        (b.actor_id=:viewer AND b.target_id=l.seller_id) OR
                        (b.target_id=:viewer AND b.actor_id=l.seller_id))))
                """,Map.of("id",id,"viewer",viewer),Long.class)>0;
    }
    @Override public List<me.yeonjae.ovlo.application.dto.result.BlockedMarketplaceMember> blockedMembers(long actor,Long before) {
        String sql="SELECT b.target_id,m.nickname FROM marketplace_block b JOIN member m ON m.id=b.target_id WHERE b.actor_id=:actor";
        var parameters=new MapSqlParameterSource("actor",actor);
        if(before!=null) {sql+=" AND b.target_id<:before";parameters.addValue("before",before);}
        // Keyset pagination remains stable when a member removes a block on an earlier page.
        sql+=" ORDER BY b.target_id DESC LIMIT 21";
        return jdbc.query(sql,parameters,(r,n)->new me.yeonjae.ovlo.application.dto.result.BlockedMarketplaceMember(r.getLong(1),r.getString(2)));
    }
    @Override public boolean blocked(long a,long b) {
        return jdbc.queryForObject("SELECT count(*) FROM marketplace_block WHERE (actor_id=:a AND target_id=:b) OR (actor_id=:b AND target_id=:a)",Map.of("a",a,"b",b),Long.class)>0;
    }
    @Override public void block(long actor,long target,boolean block) {
        var p=Map.of("a",actor,"b",target);
        if(block) jdbc.update("INSERT INTO marketplace_block(actor_id,target_id) VALUES(:a,:b) ON CONFLICT DO NOTHING",p);
        else jdbc.update("DELETE FROM marketplace_block WHERE actor_id=:a AND target_id=:b",p);
    }
    @Override public Optional<Long> chat(long listing,long buyer) {
        return jdbc.queryForList("SELECT room_id FROM marketplace_chat WHERE listing_id=:l AND buyer_id=:b",Map.of("l",listing,"b",buyer),Long.class).stream().findFirst();
    }
    @Override public void linkChat(long listing,long buyer,long room) {
        jdbc.update("INSERT INTO marketplace_chat(listing_id,buyer_id,seller_id,room_id) SELECT id,:b,seller_id,:r FROM marketplace_listing WHERE id=:l",Map.of("l",listing,"b",buyer,"r",room));
    }
    @Override public void report(long listing,long reporter,String reason) {
        jdbc.update("INSERT INTO marketplace_report(listing_id,reporter_id,reason) VALUES(:l,:r,:reason) ON CONFLICT DO NOTHING",Map.of("l",listing,"r",reporter,"reason",reason));
    }
    @Override public List<Report> reports() {
        return jdbc.query("SELECT r.id,r.listing_id,l.title,r.reason,l.hidden,l.version FROM marketplace_report r JOIN marketplace_listing l ON l.id=r.listing_id ORDER BY r.id DESC LIMIT 100",Map.of(),
                (r,n)->new Report(r.getLong(1),r.getLong(2),r.getString(3),r.getString(4),r.getBoolean(5),r.getLong(6)));
    }
    @Override public void audit(long listing,String actor,String action) {
        jdbc.update("INSERT INTO marketplace_audit(listing_id,actor,action) VALUES(:l,:a,:s)",Map.of("l",listing,"a",actor,"s",action));
    }
    @Override public Optional<ListingTranslation> translation(long id,long version,String lang) {
        return jdbc.query("SELECT * FROM marketplace_translation WHERE listing_id=:id AND version=:v AND language=:lang",Map.of("id",id,"v",version,"lang",lang),
                (r,n)->new ListingTranslation(id,version,lang,r.getString("title"),r.getString("description"),true)).stream().findFirst();
    }
    @Override public void cacheTranslation(ListingTranslation t) {
        jdbc.update("INSERT INTO marketplace_translation(listing_id,version,language,title,description) VALUES(:id,:v,:lang,:title,:body) ON CONFLICT DO NOTHING",
                Map.of("id",t.listingId(),"v",t.version(),"lang",t.language(),"title",t.title(),"body",t.description()));
    }
}
