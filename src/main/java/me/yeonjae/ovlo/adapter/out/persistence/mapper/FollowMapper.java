package me.yeonjae.ovlo.adapter.out.persistence.mapper;

import me.yeonjae.ovlo.adapter.out.persistence.entity.FollowJpaEntity;
import me.yeonjae.ovlo.domain.follow.model.Follow;
import me.yeonjae.ovlo.domain.follow.model.FollowId;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.springframework.stereotype.Component;

@Component
public class FollowMapper {

    public FollowJpaEntity toJpaEntity(Follow follow) {
        FollowJpaEntity entity = new FollowJpaEntity();
        if (follow.getId() != null) {
            entity.setId(follow.getId().value());
        }
        entity.setFollowerId(follow.getFollowerId().value());
        entity.setFolloweeId(follow.getFolloweeId().value());
        return entity;
    }

    public Follow toDomain(FollowJpaEntity entity) {
        return Follow.restore(
                new FollowId(entity.getId()),
                new MemberId(entity.getFollowerId()),
                new MemberId(entity.getFolloweeId())
        );
    }
}
