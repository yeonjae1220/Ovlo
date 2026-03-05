package me.yeonjae.ovlo.domain.media.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StoragePathTest {

    @Test
    @DisplayName("경로와 스토리지 타입으로 StoragePath를 생성할 수 있다")
    void shouldCreate_storagePath() {
        StoragePath storagePath = new StoragePath("uploads/test.jpg", StorageType.LOCAL);

        assertThat(storagePath.path()).isEqualTo("uploads/test.jpg");
        assertThat(storagePath.storageType()).isEqualTo(StorageType.LOCAL);
    }

    @Test
    @DisplayName("path가 null이면 예외가 발생한다")
    void shouldThrow_whenPathIsNull() {
        assertThatThrownBy(() -> new StoragePath(null, StorageType.LOCAL))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("저장 경로는 필수입니다");
    }

    @Test
    @DisplayName("path가 빈 값이면 예외가 발생한다")
    void shouldThrow_whenPathIsBlank() {
        assertThatThrownBy(() -> new StoragePath("  ", StorageType.LOCAL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("저장 경로는 빈 값일 수 없습니다");
    }

    @Test
    @DisplayName("같은 경로와 타입의 StoragePath는 동일하다")
    void shouldBeEqual_whenSamePathAndType() {
        StoragePath a = new StoragePath("uploads/test.jpg", StorageType.LOCAL);
        StoragePath b = new StoragePath("uploads/test.jpg", StorageType.LOCAL);

        assertThat(a).isEqualTo(b);
    }
}
