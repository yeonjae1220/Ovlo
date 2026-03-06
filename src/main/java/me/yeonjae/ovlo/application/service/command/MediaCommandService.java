package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.UploadMediaCommand;
import me.yeonjae.ovlo.application.dto.result.MediaResult;
import me.yeonjae.ovlo.application.port.in.media.UploadMediaUseCase;
import me.yeonjae.ovlo.application.port.out.media.ImageConverterPort;
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
    private final ImageConverterPort imageConverterPort;

    public MediaCommandService(
            SaveMediaPort saveMediaPort,
            StoragePort storagePort,
            ImageConverterPort imageConverterPort) {
        this.saveMediaPort = saveMediaPort;
        this.storagePort = storagePort;
        this.imageConverterPort = imageConverterPort;
    }

    @Override
    public MediaResult upload(UploadMediaCommand command) {
        byte[] data = command.data();
        MediaType mediaType = command.mediaType();

        // HEIC 파일은 JPEG로 변환 후 저장
        if (mediaType.isHeic()) {
            data = imageConverterPort.convertHeicToJpeg(data);
            mediaType = MediaType.IMAGE_JPEG;
        }

        StoragePath storagePath = storagePort.store(data, mediaType, command.originalFilename());
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
