package me.yeonjae.ovlo.application.dto.command;
public record NoticeActionCommand(Long officeId, Long noticeId, String actor, Long revision) {
    public NoticeActionCommand(Long officeId,Long noticeId,String actor) { this(officeId,noticeId,actor,null); }
}
