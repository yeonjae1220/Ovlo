package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.result.MemberResult;
import me.yeonjae.ovlo.application.dto.result.MemberSummaryResult;
import me.yeonjae.ovlo.application.port.in.member.GetMemberQuery;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.domain.member.exception.MemberException;
import me.yeonjae.ovlo.domain.member.model.Member;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MemberQueryService implements GetMemberQuery {

    /** 부분 일치 검색이라 짧은 키워드는 회원 대부분에 걸린다 — 한 번에 돌려주는 수를 묶어 둔다. */
    private static final int SEARCH_RESULT_LIMIT = 20;

    private final LoadMemberPort loadMemberPort;

    public MemberQueryService(LoadMemberPort loadMemberPort) {
        this.loadMemberPort = loadMemberPort;
    }

    @Override
    public MemberResult getById(MemberId memberId) {
        Member member = loadMemberPort.findById(memberId)
                .orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다: " + memberId.value(), MemberException.ErrorType.NOT_FOUND));
        return MemberResult.from(member);
    }

    @Override
    public MemberResult getProfile(MemberId target, MemberId viewer) {
        MemberResult profile = getById(target);
        return target.equals(viewer) ? profile : profile.withoutPrivateFields();
    }

    @Override
    public List<MemberSummaryResult> searchByNickname(String keyword) {
        return loadMemberPort.searchByNickname(keyword, SEARCH_RESULT_LIMIT).stream()
                .map(MemberSummaryResult::from)
                .toList();
    }

    @Override
    public boolean isNicknameAvailable(String nickname) {
        return !loadMemberPort.existsByNickname(nickname);
    }
}
