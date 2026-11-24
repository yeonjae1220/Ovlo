package me.yeonjae.ovlo.application.port.out.media;

import me.yeonjae.ovlo.domain.media.model.StoragePath;
import java.util.List;

public interface MediaDeletionPort {
    record Pending(Long mediaId, StoragePath path) {}
    List<Pending> claim(int limit);
    void complete(Long mediaId);
}
