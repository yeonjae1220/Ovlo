package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.UploadMediaCommand;
import me.yeonjae.ovlo.application.dto.result.MediaResult;
import me.yeonjae.ovlo.application.port.in.media.UploadMediaUseCase;

import me.yeonjae.ovlo.application.port.out.media.SaveMediaPort;
import me.yeonjae.ovlo.application.port.out.media.StoragePort;
import me.yeonjae.ovlo.domain.media.model.MediaFile;
import me.yeonjae.ovlo.domain.media.model.MediaType;
import me.yeonjae.ovlo.domain.media.model.StoragePath;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MediaCommandService implements UploadMediaUseCase {

    private final SaveMediaPort saveMediaPort;
    private final StoragePort storagePort;
    private final me.yeonjae.ovlo.application.port.out.media.MediaContentPort contentPort;

    public MediaCommandService(
            SaveMediaPort saveMediaPort,
            StoragePort storagePort,
            me.yeonjae.ovlo.application.port.out.media.MediaContentPort contentPort) {
        this.contentPort = contentPort;
        this.saveMediaPort = saveMediaPort;
        this.storagePort = storagePort;
    }

    @Override
    public MediaResult upload(UploadMediaCommand command) {
        byte[] data = contentPort.validate(command.data(), command.mediaType());
        MediaType mediaType = command.mediaType();


        StoragePath storagePath = storagePort.store(data, mediaType, command.originalFilename());
        // File storage is outside the DB transaction. Remove it if commit fails,
        // including a withdrawal that races with the metadata INSERT.
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) storagePort.delete(storagePath);
                    }
                });
        }
        MediaFile mediaFile = MediaFile.create(
                new MemberId(command.uploaderId()),
                mediaType,
                storagePath,
                command.originalFilename(),
                data.length
        );
        return MediaResult.from(saveMediaPort.save(mediaFile));
    }
}
