package com.sora.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "like_comment", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "comment_id"}, name = "uk_like_comment_user_comment")
})
public class LikeComment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    public LikeComment() {}

    public LikeComment(UserAccount user, Comment comment) {
        this.user = user;
        this.comment = comment;
    }

    public UserAccount getUser() {
        return user;
    }

    public void setUser(UserAccount user) {
        this.user = user;
    }

    public Comment getComment() {
        return comment;
    }

    public void setComment(Comment comment) {
        this.comment = comment;
    }
}
