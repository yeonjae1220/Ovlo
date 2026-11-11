package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.MarketplaceSearch;
import me.yeonjae.ovlo.application.dto.result.ListingResult;
import me.yeonjae.ovlo.application.port.in.marketplace.MarketplaceUseCase;
import me.yeonjae.ovlo.application.port.in.verification.GetMyVerificationStatusQuery;
import me.yeonjae.ovlo.application.port.out.marketplace.*;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.application.port.out.university.LoadUniversityPort;
import me.yeonjae.ovlo.application.port.out.media.StoragePort;
import me.yeonjae.ovlo.application.port.out.chat.SaveChatPort;
import me.yeonjae.ovlo.domain.marketplace.model.*;
import me.yeonjae.ovlo.domain.marketplace.exception.MarketplaceException;
import me.yeonjae.ovlo.domain.member.model.*;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import me.yeonjae.ovlo.domain.verification.model.TrustLevel;
import me.yeonjae.ovlo.domain.media.model.MediaType;
import me.yeonjae.ovlo.domain.chat.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import java.util.*;
import static me.yeonjae.ovlo.domain.marketplace.exception.MarketplaceException.Kind.*;

@Service @Transactional
public class MarketplaceService implements MarketplaceUseCase {
    private final MarketplaceStore store;
    private final LoadMemberPort members;
    private final LoadUniversityPort schools;
    private final GetMyVerificationStatusQuery verification;
    private final MarketplaceImagePort images;
    private final StoragePort storage;
    private final SaveChatPort chats;
    public MarketplaceService(MarketplaceStore store, LoadMemberPort members, LoadUniversityPort schools,
            GetMyVerificationStatusQuery verification, MarketplaceImagePort images, StoragePort storage, SaveChatPort chats) {
        this.store=store; this.members=members; this.schools=schools; this.verification=verification; this.images=images; this.storage=storage; this.chats=chats;
    }
    private Member active(long actor) {
        return members.findById(new MemberId(actor)).filter(m->m.getStatus().isActive()).orElseThrow(()->new MarketplaceException(FORBIDDEN,"Active membership required"));
    }
    private void seller(long actor) {
        active(actor);
        if(!TrustLevel.parseOrUnverified(verification.getByMemberId(actor).trustLevel()).atLeast(TrustLevel.STUDENT))
            throw new MarketplaceException(FORBIDDEN,"Verify your school email before listing items");
    }
    private Listing load(long id,boolean lock) { return store.find(id,lock).orElseThrow(()->new MarketplaceException(NOT_FOUND,"Listing not found")); }
    private void visible(Listing l,long actor) {
        if(l.deleted() || (l.hidden() && l.sellerId()!=actor) || store.blocked(actor,l.sellerId())) throw new MarketplaceException(NOT_FOUND,"Listing not found");
    }
    private ListingResult result(Listing l) {
        String name=members.findById(new MemberId(l.sellerId())).map(Member::getNickname).orElse("Member");
        String school=l.details().location().universityId()==null?null:schools.findById(new UniversityId(l.details().location().universityId())).map(u->u.getName()).orElse(null);
        return new ListingResult(l.id(),l.sellerId(),name,school,l.details(),l.status(),l.hidden(),l.version(),l.createdAt());
    }
    private void school(ListingDetails d) {
        if(d.location().universityId()!=null) {
            var u=schools.findById(new UniversityId(d.location().universityId())).orElseThrow(()->new IllegalArgumentException("Unknown university"));
            if(u.getCountryCode()!=null && !u.getCountryCode().value().equalsIgnoreCase(d.location().countryCode())) throw new IllegalArgumentException("University and pickup country must match");
        }
    }
    private void version(Listing l,long expected) { if(l.version()!=expected) throw new MarketplaceException(CONFLICT,"Listing changed. Reload before saving."); }
    @Override @Transactional(readOnly=true) public ListingPage search(long actor,MarketplaceSearch search) {
        active(actor);
        if(search.page()<0 || search.page()>1000) throw new IllegalArgumentException("Invalid page");
        var rows=store.search(actor,search);
        return new ListingPage(rows.stream().limit(20).map(this::result).toList(),rows.size()>20);
    }
    @Override @Transactional(readOnly=true) public ListingResult get(long actor,long id) { active(actor); var l=load(id,false); visible(l,actor); return result(l); }
    @Override public ListingResult create(long actor,ListingDetails details) {
        seller(actor); school(details); store.validatePhotos(actor,null,details.photoIds());
        return result(store.save(Listing.create(actor,details),0));
    }
    @Override public ListingResult edit(long actor,long id,long expected,ListingDetails details) {
        active(actor); var l=load(id,true); l.requireOwner(actor); version(l,expected); school(details);
        store.validatePhotos(actor,id,details.photoIds()); l.edit(actor,details);
        return result(store.save(l,expected));
    }
    @Override public ListingResult status(long actor,long id,long expected,ListingStatus status) {
        active(actor); var l=load(id,true); l.requireOwner(actor); version(l,expected); l.changeStatus(actor,status);
        return result(store.save(l,expected));
    }
    @Override public void delete(long actor,long id,long expected) {
        active(actor); var l=load(id,true); l.requireOwner(actor); version(l,expected); l.delete(actor); store.save(l,expected);
    }
    @Override public UUID upload(long actor,byte[] data) {
        seller(actor);
        byte[] sanitized=images.sanitize(data);
        var path=storage.store(sanitized,MediaType.IMAGE_JPEG,"marketplace.jpg");
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) { if(status!=STATUS_COMMITTED) storage.delete(path); }
        });
        var id=UUID.randomUUID(); store.addPhoto(new MarketplaceStore.Photo(id,actor,path)); return id;
    }
    @Override @Transactional(readOnly=true) public byte[] photo(long actor,UUID id) {
        active(actor);
        if(!store.photoVisible(id,actor)) throw new MarketplaceException(NOT_FOUND,"Photo not found");
        return storage.retrieve(store.photo(id).orElseThrow(()->new MarketplaceException(NOT_FOUND,"Photo not found")).path());
    }
    @Override public long contact(long actor,long id) {
        active(actor); var l=load(id,true); visible(l,actor); active(l.sellerId());
        if(actor==l.sellerId()) throw new IllegalArgumentException("Cannot contact yourself");
        var existing=store.chat(id,actor); if(existing.isPresent()) return existing.get();
        if(l.hidden() || l.status()==ListingStatus.COMPLETED) throw new MarketplaceException(CONFLICT,"Listing is no longer available");
        // A dedicated two-person GROUP avoids the general DM's member-pair reuse.
        var room=chats.save(ChatRoom.create(ChatRoomType.GROUP,"[Market #"+id+"] "+l.details().title(),List.of(new MemberId(actor),new MemberId(l.sellerId()))));
        store.linkChat(id,actor,room.getId().value()); return room.getId().value();
    }
    @Override public void report(long actor,long id,String reason) {
        active(actor); var l=load(id,false); visible(l,actor);
        store.report(id,actor,PickupLocation.text(reason,1000,"Reason"));
    }
    @Override public void block(long actor,long target,boolean blocked) {
        active(actor); if(actor==target) throw new IllegalArgumentException("Cannot block yourself");
        if(members.findById(new MemberId(target)).isEmpty()) throw new MarketplaceException(NOT_FOUND,"Member not found");
        store.block(actor,target,blocked);
    }
    @Override @Transactional(readOnly=true) public List<MarketplaceStore.Report> reports() { return store.reports(); }
    @Override public void moderate(String admin,long id,long expected,boolean hidden) {
        var l=load(id,true); version(l,expected); l.moderate(hidden); store.save(l,expected); store.audit(id,admin,hidden?"HIDE":"RESTORE");
    }
}
