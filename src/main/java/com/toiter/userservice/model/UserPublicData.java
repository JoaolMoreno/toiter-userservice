package com.toiter.userservice.model;

public class UserPublicData {
    private String username;

    private String bio;

    private Long profileImageId;

    private Long headerImageId;

    private Integer followersCount;

    private Integer followingCount;

    private Boolean isFollowing;

    private Boolean isFollowingMe;

    private Integer postsCount;

    public UserPublicData() {
    }

    public UserPublicData(String username, String bio, Long profileImageId, Long headerImageId, Integer followersCount, Integer followingCount, Boolean isFollowing, Boolean isFollowingMe, Integer postsCount) {
        this.username = username;
        this.bio = bio;
        this.profileImageId = profileImageId;
        this.headerImageId = headerImageId;
        this.followersCount = followersCount;
        this.followingCount = followingCount;
        this.isFollowing = isFollowing;
        this.isFollowingMe = isFollowingMe;
        this.postsCount = postsCount;
    }

    public UserPublicData(String username, String bio, Long profileImageId, Long headerImageId, Integer followersCount, Integer followingCount) {
        this.username = username;
        this.bio = bio;
        this.profileImageId = profileImageId;
        this.headerImageId = headerImageId;
        this.followersCount = followersCount;
        this.followingCount = followingCount;
    }

    public Boolean getFollowingMe() {
        return isFollowingMe;
    }

    public void setFollowingMe(Boolean followingMe) {
        isFollowingMe = followingMe;
    }

    public Integer getPostsCount() {
        return postsCount;
    }

    public void setPostsCount(Integer postsCount) {
        this.postsCount = postsCount;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public Long getProfileImageId() {
        return profileImageId;
    }

    public void setProfileImageId(Long profileImageId) {
        this.profileImageId = profileImageId;
    }

    public Long getHeaderImageId() {
        return headerImageId;
    }

    public void setHeaderImageId(Long headerImageId) {
        this.headerImageId = headerImageId;
    }

    public Integer getFollowersCount() {
        return followersCount;
    }

    public void setFollowersCount(Integer followersCount) {
        this.followersCount = followersCount;
    }

    public Integer getFollowingCount() {
        return followingCount;
    }

    public void setFollowingCount(Integer followingCount) {
        this.followingCount = followingCount;
    }

    public Boolean getFollowing() {
        return isFollowing;
    }

    public void setFollowing(Boolean following) {
        isFollowing = following;
    }

    public Boolean getIsFollowingMe() {
        return isFollowingMe;
    }

    public void setIsFollowingMe(Boolean isFollowingMe) {
        this.isFollowingMe = isFollowingMe;
    }
}
