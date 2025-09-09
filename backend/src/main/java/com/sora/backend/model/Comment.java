package com.sora.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "comment")
public class Comment extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    @NotNull
    private UserAccount author;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    @NotNull
    private Post post;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parentComment;
    
    @Column(name = "content", nullable = false)
    @NotBlank
    private String content;
    
    public Comment() {}
    
    public Comment(UserAccount author, Post post, String content) {
        this.author = author;
        this.post = post;
        this.content = content;
    }
    
    public Comment(UserAccount author, Post post, Comment parentComment, String content) {
        this.author = author;
        this.post = post;
        this.parentComment = parentComment;
        this.content = content;
    }
    
    public UserAccount getAuthor() {
        return author;
    }
    
    public void setAuthor(UserAccount author) {
        this.author = author;
    }
    
    public Post getPost() {
        return post;
    }
    
    public void setPost(Post post) {
        this.post = post;
    }
    
    public Comment getParentComment() {
        return parentComment;
    }
    
    public void setParentComment(Comment parentComment) {
        this.parentComment = parentComment;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
}