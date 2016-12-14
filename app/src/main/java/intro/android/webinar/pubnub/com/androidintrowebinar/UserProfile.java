package intro.android.webinar.pubnub.com.androidintrowebinar;

import com.google.gson.JsonElement;

import java.io.Serializable;

/**
 * Created by pubnubcvconover on 12/6/16.
 */

public class UserProfile implements Serializable {
    public static final String DEFAULT_LOCATION = "unknown";
    public static final String DEFAULT_FULLNAME = "anon";
    public static final String DEFAULT_LANGUAGE = "en";

    private String fullname;
    private String uuid;
    private String location;
    private String language;

    UserProfile() {
    }

    UserProfile(String uuid, JsonElement state) {
        this.uuid = uuid;

        if (state != null) {
            this.location = state.getAsJsonObject().get("location") == null ? DEFAULT_LOCATION : state.getAsJsonObject().get("location").toString();
            this.fullname = state.getAsJsonObject().get("fullname") == null ? DEFAULT_FULLNAME : state.getAsJsonObject().get("fullname").toString();
            this.language = state.getAsJsonObject().get("language") == null ? DEFAULT_LANGUAGE : state.getAsJsonObject().get("language").toString();
        } else {
            this.location = DEFAULT_LOCATION;
            this.fullname = DEFAULT_FULLNAME;
            this.language = DEFAULT_LANGUAGE;
        }

    }

    String getFullname() {
        return fullname;
    }

    void setFullname(String fullname) {
        this.fullname = fullname;
    }

    String getUuid() {
        return uuid;
    }

    void setUuid(String uuid) {
        this.uuid = uuid;
    }

    String getLocation() {
        return location;
    }

    void setLocation(String location) {
        this.location = location;
    }

    String getLanguage() {
        return language;
    }

    void setLanguage(String language) {
        this.language = language;
    }
}