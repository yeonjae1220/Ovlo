package me.yeonjae.ovlo.application.port.out.media;

/** Current account state and attachment visibility, checked in persistence. */
public interface MediaAccessPort {
    boolean canRead(Long mediaId, Long requesterId);
    boolean canUseAsProfileImage(Long mediaId, Long requesterId);
    boolean canReadAttachment(Long mediaId, Long requesterId, Long officeId);
    boolean isOwnedUpload(Long mediaId, Long requesterId);
}
