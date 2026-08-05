package com.sergio.planix.profile;

import com.sergio.planix.auth.CurrentUser;
import com.sergio.planix.profile.dto.SocialLinkResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SocialLinkService {

    private final SocialLinkRepository repo;
    private final CurrentUser currentUser;

    public SocialLinkService(SocialLinkRepository repo, CurrentUser currentUser) {
        this.repo = repo;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<SocialLinkResponse> list() {
        return repo.findByUserIdOrderByPlatformAsc(currentUser.id())
                .stream().map(SocialLinkResponse::from).toList();
    }

    public SocialLinkResponse upsert(SocialPlatform platform, String url) {
        SocialLink link = repo.findByUserIdAndPlatform(currentUser.id(), platform)
                .orElseGet(() -> repo.save(new SocialLink(currentUser.reference(), platform, url)));
        link.setUrl(url.trim());
        return SocialLinkResponse.from(link);
    }

    public void delete(SocialPlatform platform) {
        repo.deleteByUserIdAndPlatform(currentUser.id(), platform);
    }
}
