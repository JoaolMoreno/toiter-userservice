package com.toiter.postservice.model;

import com.toiter.postservice.model.Post;

public class PostDeletedEvent implements PostEvent {

    private Post post;

    public PostDeletedEvent(Post post) {
        this.post = post;
    }

    public PostDeletedEvent() {
    }

    @Override
    public Post getPost() {
        return post;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    @Override
    public String toString() {
        return "PostDeletedEvent{" +
                "post=" + post +
                '}';
    }
}