package com.sora.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

@Entity
@Table(name = "post")
public class Post extends BaseEntity {

    // Who actually created this post content
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    @NotNull
    private UserAccount author;

    // Whose country collection this post appears in
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_owner_id", nullable = false)
    @NotNull
    private UserAccount profileOwner;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id", nullable = false)
    @NotNull
    private Country country;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id", nullable = false)
    @NotNull
    private Collection collection;

    @Column(name = "city_name", nullable = false)
    @NotBlank
    private String cityName;
    
    @Column(name = "city_latitude")
    private Double cityLatitude;
    
    @Column(name = "city_longitude")
    private Double cityLongitude;
    
    @Column(name = "caption")
    private String caption;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility_type", nullable = false)
    private PostVisibilityType visibilityType = PostVisibilityType.PERSONAL;

    @Column(name = "shared_post_group_id", length = 36)
    private String sharedPostGroupId;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("sortOrder")
    private List<PostMedia> media;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LikePost> likes;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Comment> comments;
    
    public Post() {}
    
    public Post(UserAccount author, UserAccount profileOwner, Country country, Collection collection, String cityName, String caption) {
        this.author = author;
        this.profileOwner = profileOwner;
        this.country = country;
        this.collection = collection;
        this.cityName = cityName;
        this.caption = caption;
        this.visibilityType = author.equals(profileOwner) ? PostVisibilityType.PERSONAL : PostVisibilityType.SHARED;
    }
    
    public UserAccount getAuthor() {
        return author;
    }
    
    public void setAuthor(UserAccount author) {
        this.author = author;
    }
    
    public UserAccount getProfileOwner() {
        return profileOwner;
    }
    
    public void setProfileOwner(UserAccount profileOwner) {
        this.profileOwner = profileOwner;
    }
    
    public Country getCountry() {
        return country;
    }
    
    public void setCountry(Country country) {
        this.country = country;
    }
    
    public Collection getCollection() {
        return collection;
    }
    
    public void setCollection(Collection collection) {
        this.collection = collection;
    }
    
    public String getCityName() {
        return cityName;
    }
    
    public void setCityName(String cityName) {
        this.cityName = cityName;
    }
    
    public Double getCityLatitude() {
        return cityLatitude;
    }
    
    public void setCityLatitude(Double cityLatitude) {
        this.cityLatitude = cityLatitude;
    }
    
    public Double getCityLongitude() {
        return cityLongitude;
    }
    
    public void setCityLongitude(Double cityLongitude) {
        this.cityLongitude = cityLongitude;
    }
    
    public String getCaption() {
        return caption;
    }
    
    public void setCaption(String caption) {
        this.caption = caption;
    }
    
    public PostVisibilityType getVisibilityType() {
        return visibilityType;
    }
    
    public void setVisibilityType(PostVisibilityType visibilityType) {
        this.visibilityType = visibilityType;
    }
    
    public String getSharedPostGroupId() {
        return sharedPostGroupId;
    }
    
    public void setSharedPostGroupId(String sharedPostGroupId) {
        this.sharedPostGroupId = sharedPostGroupId;
    }
    
    public boolean isSharedPost() {
        return visibilityType == PostVisibilityType.SHARED;
    }
    
    public boolean isOwnedByAuthor() {
        return author.equals(profileOwner);
    }

    public List<PostMedia> getMedia() {
        return media != null ? media : java.util.List.of();
    }

    public void setMedia(List<PostMedia> media) {
        this.media = media;
    }

    public List<LikePost> getLikes() {
        return likes != null ? likes : java.util.List.of();
    }

    public void setLikes(List<LikePost> likes) {
        this.likes = likes;
    }

    public List<Comment> getComments() {
        return comments != null ? comments : java.util.List.of();
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public Integer getLikesCount() {
        return likes != null ? likes.size() : 0;
    }

    public Integer getCommentsCount() {
        return comments != null ? comments.size() : 0;
    }
}