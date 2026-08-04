package com.sergio.planix.profile;

import com.sergio.planix.auth.User;
import com.sergio.planix.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_social_links")
@Getter
@Setter
public class SocialLink extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SocialPlatform platform;

    @Column(nullable = false, length = 255)
    private String url;

    protected SocialLink() {}

    public SocialLink(User user, SocialPlatform platform, String url) {
        this.user = user;
        this.platform = platform;
        this.url = url;
    }
}
