package me.yeonjae.ovlo.adapter.out.persistence.entity;
import java.io.Serializable;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor
public class CalendarMembershipKey implements Serializable {
    private Long memberId;
    private Long universityId;
}
