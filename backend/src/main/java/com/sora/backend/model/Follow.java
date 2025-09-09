package com.sora.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "follow", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"follower_id", "following_id"}, name = "uk_follow_follower_following")
})
public class Follow extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id", nullable = false)
    private UserAccount follower;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "following_id", nullable = false)
    private UserAccount following;
    
    public Follow() {}
    
    public Follow(UserAccount follower, UserAccount following) {
        this.follower = follower;
        this.following = following;
    }
    
    public UserAccount getFollower() {
        return follower;
    }
    
    public void setFollower(UserAccount follower) {
        this.follower = follower;
    }
    
    public UserAccount getFollowing() {
        return following;
    }
    
    public void setFollowing(UserAccount following) {
        this.following = following;
    }
}