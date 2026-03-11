package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.result.MemberResult;
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
    public List<MemberResult> searchByNickname(String keyword) {
        return loadMemberPort.searchByNickname(keyword).stream()
                .map(MemberResult::from)
                .toList();
    }
}
