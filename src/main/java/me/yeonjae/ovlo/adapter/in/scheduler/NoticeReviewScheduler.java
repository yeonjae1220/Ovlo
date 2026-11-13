package me.yeonjae.ovlo.adapter.in.scheduler;
import me.yeonjae.ovlo.application.port.in.office.ReviewNoticeScheduleUseCase;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
@Component
public class NoticeReviewScheduler {
    private final ReviewNoticeScheduleUseCase reviews;
    public NoticeReviewScheduler(ReviewNoticeScheduleUseCase reviews) { this.reviews=reviews; }
    @Scheduled(cron="${ovlo.office.notice-review-cron:0 20 0 * * *}",zone="Asia/Seoul")
    public void run() { reviews.remind(); }
}
