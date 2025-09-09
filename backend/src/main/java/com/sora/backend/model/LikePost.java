package com.sora.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "like_post", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "post_id"}, name = "uk_like_post_user_post")
})
public class LikePost extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
    
    public LikePost() {}
    
    public LikePost(UserAccount user, Post post) {
        this.user = user;
        this.post = post;
    }
    
    public UserAccount getUser() {
        return user;
    }
    
    public void setUser(UserAccount user) {
        this.user = user;
    }
    
    public Post getPost() {
        return post;
    }
    
    public void setPost(Post post) {
        this.post = post;
    }
}