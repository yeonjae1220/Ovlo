package me.yeonjae.ovlo.application.port.out.follow;

import me.yeonjae.ovlo.domain.follow.model.Follow;

public interface SaveFollowPort {

    Follow save(Follow follow);

    void delete(Follow follow);
}
