package com.sergio.planix.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SocialLinkRepository extends JpaRepository<SocialLink, Long> {

    List<SocialLink> findByUserIdOrderByPlatformAsc(Long userId);

    Optional<SocialLink> findByUserIdAndPlatform(Long userId, SocialPlatform platform);

    void deleteByUserIdAndPlatform(Long userId, SocialPlatform platform);
}
