package me.yeonjae.ovlo.adapter.out.office;
import me.yeonjae.ovlo.application.port.out.office.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.List;
@Component
@ConditionalOnProperty(name="ovlo.llm.provider", havingValue="fake", matchIfMissing=true)
public class FakeInquiryGuardAdapter implements InquiryGuardPort {
    public Verdict screen(String question, List<AnswerGenerationPort.Turn> history) {
        return new Verdict(true, "fake-guard", 0, 0);
    }
    public Verdict verify(AnswerGenerationPort.Request request, String answer) {
        return new Verdict(true, "fake-guard", 0, 0);
    }
}
