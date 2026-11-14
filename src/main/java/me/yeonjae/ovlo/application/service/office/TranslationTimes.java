package me.yeonjae.ovlo.application.service.office;

import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Clock times are compared separately from quantities. Ambiguous duration words are not inferred as times. */
final class TranslationTimes {
    private TranslationTimes() {}
    record Extracted(Map<LocalTime,Integer> times, String remainingText, boolean invalidTime) {}
    static Extracted extract(String text, NoticeLang language) {
        StringBuilder remaining = new StringBuilder(text);
        Map<LocalTime,Integer> times = new HashMap<>();
        boolean[] invalid = {false};
        collect(remaining, "(?<![\\p{L}\\d.,])(?:(?<ap>오전|오후)\\s*)?(?<h>\\d+)[:：](?<m>\\d+)(?::(?<s>\\d+))?\\s*(?<suffix>a\\.?m\\.?|p\\.?m\\.?)?(?!\\d)",
                m -> add(times, m.group("h"), m.group("m"), m.group("s"), m.group("ap") == null ? m.group("suffix") : m.group("ap")), invalid);
        if (language == NoticeLang.KO || language == NoticeLang.JA || language == NoticeLang.ZH) {
            collect(remaining, "(?<![\\d.,])(?:(?<ap>오전|오후|午前|午後|上午|下午)\\s*)?(?<h>\\d+)\\s*[-–~]\\s*(?<end>\\d+)\\s*[시時时](?![간間])",
                    m -> { add(times,m.group("h"),null,null,m.group("ap")); add(times,m.group("end"),null,null,m.group("ap")); }, invalid);
            collect(remaining, "(?<![\\d.,])(?:(?<ap>오전|오후|午前|午後|上午|下午)\\s*)?(?<h>\\d+)\\s*[시時时](?![간間])(?:\\s*(?<m>\\d+)\\s*[분分])?(?:\\s*(?<s>\\d+)\\s*[초秒])?",
                    m -> add(times,m.group("h"),m.group("m"),m.group("s"),m.group("ap")), invalid);
        }
        if (language == NoticeLang.EN) {
            collect(remaining,"(?<![\\p{L}\\d.,])(?<h>\\d+)\\s*(?<ap>a\\.?m\\.?|p\\.?m\\.?)(?!\\p{L})",
                    m -> add(times,m.group("h"),null,null,m.group("ap")),invalid);
        }
        if (language == NoticeLang.DE) {
            collect(remaining,"(?<![\\d.,])(?<h>\\d+)\\s*Uhr(?!\\p{L})",
                    m -> add(times,m.group("h"),null,null,null),invalid);
        }
        if (language == NoticeLang.FR) {
            collect(remaining,"(?<![\\d.,])(?<h>\\d+)\\s*h\\s*(?<m>\\d+)(?!\\d)",
                    m -> add(times,m.group("h"),m.group("m"),null,null),invalid);
        }
        return new Extracted(Map.copyOf(times),remaining.toString(),invalid[0]);
    }
    private static void add(Map<LocalTime,Integer> times,String h,String m,String s,String meridiem) {
        int hour=Integer.parseInt(h);
        if(meridiem!=null) {
            if(hour<1 || hour>12) throw new IllegalArgumentException("Invalid twelve-hour time");
            String ap=meridiem.toLowerCase(java.util.Locale.ROOT).replace(".","");
            hour=hour%12+(java.util.Set.of("pm","오후","午後","下午").contains(ap)?12:0);
        }
        times.merge(LocalTime.of(hour,m==null?0:Integer.parseInt(m),s==null?0:Integer.parseInt(s)),1,Integer::sum);
    }
    private static void collect(StringBuilder remaining,String regex,Consumer<Matcher> parse,boolean[] invalid) {
        Matcher matcher=Pattern.compile(regex,Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE).matcher(remaining.toString());
        while(matcher.find()) {
            try { parse.accept(matcher); }
            catch(java.time.DateTimeException|IllegalArgumentException e) { invalid[0]=true; }
            for(int i=matcher.start();i<matcher.end();i++) remaining.setCharAt(i,' ');
        }
    }
}
