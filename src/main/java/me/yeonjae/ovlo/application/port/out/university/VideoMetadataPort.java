package me.yeonjae.ovlo.application.port.out.university;

public interface VideoMetadataPort {

    VideoMetadata lookup(String youtubeUrl);

    enum Status {
        /** 원본에 있다. title·channel 이 채워진다. */
        FOUND,
        /** 원본에서 삭제·비공개됐다. */
        GONE,
        /** 판정할 수 없다(일시 오류, 임베드 금지 등). */
        UNKNOWN,
        /** 원본이 요청을 제한하고 있다 — 이번 회차를 멈춰야 한다. */
        THROTTLED
    }

    record VideoMetadata(Status status, String title, String channel) {
        public static VideoMetadata of(Status status) {
            return new VideoMetadata(status, null, null);
        }
    }
}
