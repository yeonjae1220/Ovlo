package me.yeonjae.ovlo.adapter.out.office;

import me.yeonjae.ovlo.application.port.out.office.NoticeSearchPort;
import me.yeonjae.ovlo.domain.office.model.*;
import me.yeonjae.ovlo.support.NoticeFixtures;
import tools.jackson.databind.json.JsonMapper;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

/** Shared retrieval-only runner: the real adapters decide candidates; no answer/LLM mocks return gold. */
final class NoticeRetrievalGoldenSet {
    /** Keeps the bypass ceiling at the ranked limit so the 28-document fixture measures ranked retrieval. */
    static final int RANKED_ONLY_BYPASS_CEILING = 8;
    static final LocalDate TODAY = LocalDate.of(2027, 3, 2);
    static final Instant NOW = Instant.parse("2027-03-02T09:00:00Z");
    private static final JsonMapper JSON = JsonMapper.builder().build();
    record Text(String title, String body) {}
    record Document(long id, NoticeCategory category, Map<String, Text> texts) {}
    record Query(String id, String lang, String kind, List<String> history, String question, List<Long> relevant) {
        // Mirrors InquiryCommandService: current question after at most two preceding questions.
        String searchQuestion() {
            var preceding = history.stream().skip(Math.max(0, history.size() - 2)).toList();
            return preceding.isEmpty() ? question : String.join(" ", preceding) + " " + question;
        }
    }
    record Corpus(int schemaVersion, String description, List<Document> notices, List<Query> cases) {}
    record Row(String id, String lang, String kind, String question, String searchQuestion,
               List<Long> relevant, List<Long> retrieved, Map<Long, Integer> ranks, double recallAt8, int firstRank) {}
    record Group(int cases, int misses, double recallAt8) {}
    record Report(String adapter, String fixtureSha256, double recallAt8, Map<String, Group> byKind,
                  Map<String, Group> byLanguage, List<Row> results) {}
    record Baseline(String fixtureSha256, double minimumRecallAt8, List<String> mustRetrieve) {}

    static byte[] resource(String name) throws Exception {
        try (var stream = NoticeRetrievalGoldenSet.class.getResourceAsStream("/retrieval/" + name)) {
            assertThat(stream).as(name).isNotNull();
            return stream.readAllBytes();
        }
    }
    static Corpus corpus() throws Exception {
        Corpus corpus = JSON.readValue(resource("office-search-golden.json"), Corpus.class);
        assertThat(corpus.notices()).hasSize(28);
        assertThat(corpus.cases()).hasSize(70);
        assertThat(corpus.cases()).extracting(Query::id).doesNotHaveDuplicates();
        assertThat(corpus.notices()).extracting(Document::id).doesNotHaveDuplicates();
        assertThat(corpus.cases()).extracting(Query::lang).containsAll(List.of("ko", "en", "ja", "zh", "vi", "de", "fr"));
        Set<Long> ids = new HashSet<>(corpus.notices().stream().map(Document::id).toList());
        for (Query q : corpus.cases()) {
            assertThat(q.relevant()).isNotEmpty().doesNotHaveDuplicates();
            assertThat(ids).containsAll(q.relevant());
            assertThat(NoticeLang.fromCode(q.lang())).isPresent();
        }
        assertThat(corpus.notices()).anySatisfy(doc -> assertThat(doc.texts().get("ko").body().length()).isGreaterThan(3000));
        return corpus;
    }
    static Notice notice(Document doc, Long office, Long member) {
        Text source = doc.texts().get("ko");
        Notice n = Notice.register(office, doc.category(), source.title(), source.body(), null, List.of(), "2027-1",
                TODAY.minusDays(1), TODAY.plusDays(60), member, NOW);
        n.approve(member.toString(), NOW, TODAY);
        NoticeFixtures.verify(n, NOW);
        doc.texts().forEach((code, text) -> {
            NoticeLang lang = NoticeLang.fromCode(code).orElseThrow();
            if (lang != NoticeLang.KO) n.putTranslation(lang, text.title(), text.body(), NOW);
        });
        return n;
    }
    static void evaluate(String adapter, NoticeSearchPort search, Long office, Map<Long, Notice> documents) throws Exception {
        Corpus corpus = corpus();
        Map<Long, Long> fixtureIds = new HashMap<>();
        documents.forEach((id, n) -> fixtureIds.put(n.getId(), id));
        List<Row> rows = new ArrayList<>();
        for (Query q : corpus.cases()) {
            List<Notice> found = search.findCandidates(office, NoticeLang.fromCode(q.lang()).orElseThrow(), q.searchQuestion(), TODAY, 8);
            assertThat(found).hasSizeLessThanOrEqualTo(8).allSatisfy(n -> assertThat(n.getOfficeId()).isEqualTo(office));
            List<Long> retrieved = found.stream().map(n -> fixtureIds.get(n.getId())).toList();
            assertThat(retrieved).doesNotContainNull().doesNotHaveDuplicates();
            Map<Long, Integer> ranks = new LinkedHashMap<>();
            for (Long id : q.relevant()) ranks.put(id, retrieved.indexOf(id) + 1); // 0 = not retrieved
            double recall = ranks.values().stream().filter(rank -> rank > 0).count() / (double) q.relevant().size();
            int firstRank = ranks.values().stream().filter(rank -> rank > 0).mapToInt(Integer::intValue).min().orElse(0);
            rows.add(new Row(q.id(), q.lang(), q.kind(), q.question(), q.searchQuestion(), q.relevant(), retrieved, ranks, recall, firstRank));
        }
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(resource("office-search-golden.json")));
        double recall = rows.stream().mapToDouble(Row::recallAt8).average().orElseThrow();
        Report report = new Report(adapter, hash, recall, groups(rows, false), groups(rows, true), rows);
        Path output = Path.of("build/reports/retrieval");
        Files.createDirectories(output);
        Files.writeString(output.resolve(adapter + ".json"), JSON.writerWithDefaultPrettyPrinter().writeValueAsString(report) + "\n");
        StringBuilder csv = new StringBuilder("case,language,kind,recallAt8,firstRank,relevantRanks,retrievedIds\n");
        for (Row row : rows) csv.append(row.id()).append(',').append(row.lang()).append(',').append(row.kind()).append(',')
                .append(row.recallAt8()).append(',').append(row.firstRank()).append(',').append(quote(row.ranks().toString())).append(',')
                .append(quote(row.retrieved().toString())).append('\n');
        Files.writeString(output.resolve(adapter + ".csv"), csv, StandardCharsets.UTF_8);
        var baselineTree = JSON.readTree(resource("office-search-baseline.json"));
        assertThat(baselineTree.has(adapter)).as("Record the first measured baseline after reviewing build/reports/retrieval/%s.json", adapter).isTrue();
        Baseline baseline = JSON.treeToValue(baselineTree.get(adapter), Baseline.class);
        assertThat(hash).as("Changing fixtures requires an explicitly reviewed new baseline").isEqualTo(baseline.fixtureSha256());
        assertThat(recall).as(adapter + " recall@8").isGreaterThanOrEqualTo(baseline.minimumRecallAt8());
        for (String id : baseline.mustRetrieve()) {
            assertThat(rows).filteredOn(row -> row.id().equals(id)).singleElement()
                    .satisfies(row -> assertThat(row.recallAt8()).as(id + " previously retrieved gold").isEqualTo(1.0));
        }
    }
    private static String quote(String value) { return "\"" + value.replace("\"", "\"\"") + "\""; }
    private static Map<String, Group> groups(List<Row> rows, boolean language) {
        Map<String, List<Row>> grouped = new TreeMap<>();
        for (Row row : rows) grouped.computeIfAbsent(language ? row.lang() : row.kind(), k -> new ArrayList<>()).add(row);
        Map<String, Group> result = new TreeMap<>();
        grouped.forEach((key, values) -> result.put(key, new Group(values.size(), (int) values.stream().filter(r -> r.recallAt8() < 1).count(),
                values.stream().mapToDouble(Row::recallAt8).average().orElseThrow())));
        return result;
    }
}
