package me.yeonjae.ovlo.adapter.out.office;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import me.yeonjae.ovlo.shared.metrics.OfficeMetrics;

import me.yeonjae.ovlo.application.port.out.office.LoadNoticePort;
import me.yeonjae.ovlo.domain.office.model.Notice;
import me.yeonjae.ovlo.support.NoticeFixtures;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;

class KeywordNoticeRetrievalTest {
    @Test void syntheticGoldenSetRecallAndRanks() throws Exception {
        var corpus = NoticeRetrievalGoldenSet.corpus();
        Map<Long, Notice> documents = new LinkedHashMap<>();
        for (var doc : corpus.notices()) {
            Notice n = NoticeRetrievalGoldenSet.notice(doc, 1L, 1L);
            n.assignId(doc.id()); documents.put(doc.id(), n);
        }
        var load = mock(LoadNoticePort.class);
        when(load.findActiveForStudent(1L, null, NoticeRetrievalGoldenSet.TODAY)).thenReturn(List.copyOf(documents.values()));
        NoticeRetrievalGoldenSet.evaluate("keyword", new KeywordNoticeSearchAdapter(load, NoticeFixtures.governance(load), new OfficeMetrics(new SimpleMeterRegistry())), 1L, documents);
    }
}
