package me.yeonjae.ovlo.application.port.out.oauth;

import me.yeonjae.ovlo.application.dto.result.GoogleUserProfile;

public interface GoogleOAuthPort {
    GoogleUserProfile getUserProfile(String authCode, String redirectUri);
}
