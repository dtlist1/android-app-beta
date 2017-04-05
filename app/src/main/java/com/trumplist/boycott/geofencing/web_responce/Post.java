
package com.trumplist.boycott.geofencing.web_responce;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import javax.annotation.Generated;

@Generated("org.jsonschema2pojo")
public class Post {

    @SerializedName("post")
    @Expose
    private Post_ post;

    /**
     * 
     * @return
     *     The post
     */
    public Post_ getPost() {
        return post;
    }

    /**
     * 
     * @param post
     *     The post
     */
    public void setPost(Post_ post) {
        this.post = post;
    }

}
