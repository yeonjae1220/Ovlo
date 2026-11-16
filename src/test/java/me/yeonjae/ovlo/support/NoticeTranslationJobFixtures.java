package me.yeonjae.ovlo.support;
import me.yeonjae.ovlo.application.port.out.office.NoticeTranslationJobPort;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
public final class NoticeTranslationJobFixtures {
    public static NoticeTranslationJobPort available() {
        var jobs=mock(NoticeTranslationJobPort.class);
        lenient().when(jobs.claim(anyLong(),anyInt(),any())).thenAnswer(i -> java.util.Optional.of(
                new NoticeTranslationJobPort.Claim(i.getArgument(0),i.getArgument(1),i.getArgument(2),java.util.UUID.randomUUID())));
        lenient().when(jobs.isHeld(any())).thenReturn(true);
        lenient().when(jobs.beginAttempt(any())).thenReturn(true);
        return jobs;
    }
}
