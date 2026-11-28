package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.port.out.moderation.ReportRetentionPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportRetentionServiceTest {

    @Test
    @DisplayName("1년(윤년 포함 달력 기준) 전 시각을 기준으로 익명화하고 처리 건수를 돌려준다")
    void anonymizesReportsOlderThanOneYear() {
        ReportRetentionPort port = mock(ReportRetentionPort.class);
        when(port.anonymizeOlderThan(any())).thenReturn(3);
        Clock clock = Clock.fixed(Instant.parse("2028-03-01T00:00:00Z"), ZoneOffset.UTC);

        int count = new ReportRetentionService(port, clock).anonymizeExpired();

        assertThat(count).isEqualTo(3);
        verify(port).anonymizeOlderThan(Instant.parse("2027-03-01T00:00:00Z"));
    }
}
