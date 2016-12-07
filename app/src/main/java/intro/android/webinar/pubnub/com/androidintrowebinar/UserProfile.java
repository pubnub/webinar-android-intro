package intro.android.webinar.pubnub.com.androidintrowebinar;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.Serializable;
import java.util.LinkedHashMap;

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

    UserProfile(String uuid, LinkedHashMap state) {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectNode payload = factory.objectNode();

        this.uuid = uuid;

        if (state != null) {
            this.location = state.get("location") == null ? DEFAULT_LOCATION : state.get("location").toString();
            this.fullname = state.get("fullname") == null ? DEFAULT_LOCATION : state.get("fullname").toString();
            this.language = state.get("language") == null ? DEFAULT_LOCATION : state.get("language").toString();
        }
    }

    UserProfile(String uuid, JsonNode state) {
        this.uuid = uuid;

        if (state != null) {
            if (state.get("fullname") != null)
                this.fullname = state.get("fullname").asText();
            else
                this.fullname = DEFAULT_FULLNAME;

            if (state.get("location") != null)
                this.location = state.get("location").asText();
            else
                this.location = DEFAULT_LOCATION;

            if (state.get("language") != null)
                this.language = state.get("language").asText();
            else
                this.language = DEFAULT_LANGUAGE;
        }
        else {
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