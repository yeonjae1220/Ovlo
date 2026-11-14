package me.yeonjae.ovlo.application.service.office;
import me.yeonjae.ovlo.application.port.out.office.TranslationPort;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.*;
class NoticeTranslationTimeTest {
 @ParameterizedTest @CsvSource(delimiter='|',value={
  "EN|18시|18:00", "EN|오후 6시 30분|6:30 PM", "EN|오전 12시|12 a.m.", "EN|오후 12시|12 PM",
  "DE|18시|18 Uhr", "FR|18시 30분|18 h 30", "JA|18시 30분|18時30分", "ZH|18시|18时",
  "VI|18시|18:00", "EN|14-17시|14:00–17:00", "EN|18시 30분 45초|18:30:45",
  "EN|교육 1.5시간, 시작 18시|Training 1.5 hours, starts 18:00"
 }) void acceptsEquivalentClockTimes(NoticeLang to,String source,String translated) { assertThat(check(to,source,translated)).isTrue(); }
 @ParameterizedTest @CsvSource(delimiter='|',value={
  "EN|18시|18:59", "EN|오후 6시|6 AM", "EN|18:30:45|18:30:46", "EN|18시|18:00 and 19:00",
  "EN|18시, 18시|18:00", "EN|18시|25:00", "EN|18시|18:60", "EN|18시|18:123",
  "DE|18시|19 Uhr", "FR|18시|18 h 59", "EN|마감 18시, 번호 18과 59|Deadline 18:59, references 18 and 59"
 }) void rejectsChangedMissingOrInvalidClockTimes(NoticeLang to,String source,String translated) { assertThat(check(to,source,translated)).isFalse(); }
 boolean check(NoticeLang to,String source,String translated) {
  return new NoticeTranslationGate().check(new TranslationPort.Request(NoticeLang.KO,to,"안내",source),
      new TranslationPort.Result("Notice",translated,"test")).passed();
 }
}
