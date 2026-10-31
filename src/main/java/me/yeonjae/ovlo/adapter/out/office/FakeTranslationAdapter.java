package me.yeonjae.ovlo.adapter.out.office;

import me.yeonjae.ovlo.application.port.out.office.TranslationPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 결정적 가짜 번역기. {@code ovlo.llm.provider} 가 비어 있거나 {@code fake} 면 활성.
 * 원문 앞에 "[lang]" 을 붙여 돌려주므로 숫자·날짜가 그대로 남아 번역 게이트를 통과한다.
 * ⚠️ 운영에서는 S3 에서 추가하는 LLM 어댑터(ovlo.llm.provider=anthropic)를 써야 한다.
 */
@Component
@ConditionalOnProperty(name = "ovlo.llm.provider", havingValue = "fake", matchIfMissing = true)
public class FakeTranslationAdapter implements TranslationPort {

    public static final String MODEL_ID = "fake-translator";

    @Override
    public Result translate(Request request) {
        String tag = "[" + request.to().code() + "] ";
        return new Result(tag + request.title(), tag + request.body(), MODEL_ID);
    }
}
