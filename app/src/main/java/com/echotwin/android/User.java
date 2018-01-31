package com.echotwin.android;

/**
 * Created by kristo.prifti on 1/22/18.
 */

public class User {

    private String mUserName;
    private String mId;
    private String mUserAvatar;
    private String mUserVoice;

    User() {
    }

    String getUserName() {
        return mUserName;
    }

    void setUserName(String userName) {
        this.mUserName = userName;
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        this.mId = id;
    }

    String getUserAvatar() {
        return mUserAvatar;
    }

    void setUserAvatar(String userAvatar) {
        this.mUserAvatar = userAvatar;
    }

    public String getUserVoice() {
        return mUserVoice;
    }

    void setUserVoice(String userVoice) {
        this.mUserVoice = userVoice;
    }
}
