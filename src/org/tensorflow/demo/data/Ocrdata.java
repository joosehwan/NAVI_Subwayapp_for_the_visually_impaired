package org.tensorflow.demo.data;

import com.google.gson.annotations.SerializedName;

public class Ocrdata {
    @SerializedName("title")
    String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    @SerializedName("image")
    String image;
}
