package com.sora.backend.service;

import com.sora.backend.exception.ServiceException;
import com.sora.backend.model.Follow;
import com.sora.backend.model.UserAccount;
import com.sora.backend.repository.FollowRepository;
import com.sora.backend.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import com.sora.backend.util.MessageUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class FollowService {

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private NotificationService notificationService;


    public Follow followUser(UserAccount follower, Long followingUserId) {
        UserAccount following = userAccountRepository.findById(followingUserId)
                .orElseThrow(() -> new ServiceException(MessageUtil.getMessage("user.not.found")));

        if (follower.getId().equals(following.getId()))
            throw new ServiceException(MessageUtil.getMessage("follow.cannot.follow.self"));

        if (followRepository.existsByFollowerIdAndFollowingId(follower.getId(), following.getId()))
            throw new ServiceException(MessageUtil.getMessage("follow.already.following"));

        Follow follow = new Follow();
        follow.setFollower(follower);
        follow.setFollowing(following);
        follow.setCreatedAt(LocalDateTime.now());

        Follow savedFollow = followRepository.save(follow);
        notificationService.createFollowNotification(following, follower);
        
        return savedFollow;
    }

    public void unfollowUser(UserAccount follower, Long followingUserId) {
        UserAccount following = userAccountRepository.findById(followingUserId)
                .orElseThrow(() -> new ServiceException(MessageUtil.getMessage("user.not.found")));
        Optional<Follow> followOpt = followRepository.findByFollowerIdAndFollowingId(follower.getId(), following.getId());

        if (followOpt.isEmpty())
            throw new ServiceException(MessageUtil.getMessage("follow.not.following"));

        followRepository.deleteByFollowerIdAndFollowingId(follower.getId(), following.getId());
    }

    @Transactional(readOnly = true)
    public boolean isFollowing(UserAccount follower, UserAccount following) {
        return followRepository.existsByFollowerIdAndFollowingId(follower.getId(), following.getId());
    }

    @Transactional(readOnly = true)
    public Page<Follow> getFollowers(UserAccount user, Pageable pageable) {
        return followRepository.findByFollowingId(user.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<Follow> getFollowing(UserAccount user, Pageable pageable) {
        return followRepository.findByFollowerId(user.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public long getFollowersCount(UserAccount user) {
        return followRepository.countByFollowingId(user.getId());
    }

    @Transactional(readOnly = true)
    public long getFollowingCount(UserAccount user) {
        return followRepository.countByFollowerId(user.getId());
    }

    @Transactional(readOnly = true)
    public List<UserAccount> getFollowingUsers(UserAccount follower) {
        return followRepository.findFollowingByFollowerId(follower.getId());
    }

    @Transactional(readOnly = true)
    public Page<Follow> getUserFollowers(Long userId, Pageable pageable) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ServiceException(MessageUtil.getMessage("user.not.found")));
        
        return getFollowers(user, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Follow> getUserFollowing(Long userId, Pageable pageable) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ServiceException(MessageUtil.getMessage("user.not.found")));
        
        return getFollowing(user, pageable);
    }

    @Transactional(readOnly = true)
    public boolean isUserFollowing(UserAccount currentUser, Long targetUserId) {
        UserAccount targetUser = userAccountRepository.findById(targetUserId)
                .orElseThrow(() -> new ServiceException(MessageUtil.getMessage("user.not.found")));

        return isFollowing(currentUser, targetUser);
    }
}