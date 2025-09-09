package com.sora.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "post_media")
public class PostMedia extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
    
    @Column(name = "file_name", nullable = false)
    private String fileName;
    
    @Column(name = "cloudinary_public_id", nullable = false)
    private String cloudinaryPublicId;
    
    @Column(name = "cloudinary_url", nullable = false)
    private String cloudinaryUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    private MediaType mediaType = MediaType.IMAGE;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "width")
    private Integer width;
    
    @Column(name = "height")
    private Integer height;
    
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
    
    public PostMedia() {}
    
    public PostMedia(Post post, String fileName, String cloudinaryPublicId, String cloudinaryUrl, MediaType mediaType) {
        this.post = post;
        this.fileName = fileName;
        this.cloudinaryPublicId = cloudinaryPublicId;
        this.cloudinaryUrl = cloudinaryUrl;
        this.mediaType = mediaType;
        this.sortOrder = 0;
    }
    
    public Post getPost() {
        return post;
    }
    
    public void setPost(Post post) {
        this.post = post;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getCloudinaryPublicId() {
        return cloudinaryPublicId;
    }
    
    public void setCloudinaryPublicId(String cloudinaryPublicId) {
        this.cloudinaryPublicId = cloudinaryPublicId;
    }
    
    public String getCloudinaryUrl() {
        return cloudinaryUrl;
    }
    
    public void setCloudinaryUrl(String cloudinaryUrl) {
        this.cloudinaryUrl = cloudinaryUrl;
    }
    
    public MediaType getMediaType() {
        return mediaType;
    }
    
    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public Integer getWidth() {
        return width;
    }
    
    public void setWidth(Integer width) {
        this.width = width;
    }
    
    public Integer getHeight() {
        return height;
    }
    
    public void setHeight(Integer height) {
        this.height = height;
    }
    
    public Integer getSortOrder() {
        return sortOrder;
    }
    
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}