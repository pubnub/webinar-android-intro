package intro.android.webinar.pubnub.com.androidintrowebinar;

import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult;

import java.io.Serializable;

/**
 * Created by pubnubcvconover on 12/6/16.
 */

public class PresenceEvent implements Serializable {
    private String action;
    private String uuid;
    private String channel;
    private UserProfile profile;


    public PresenceEvent(PNPresenceEventResult presence) {
        this.setAction(presence.getEvent());
        this.setUuid(presence.getUuid());
        this.setChannel(presence.getChannel());
        this.setProfile(new UserProfile(uuid, presence.getState()));
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public UserProfile getProfile() {
        return profile;
    }

    public void setProfile(UserProfile profile) {
        this.profile = profile;
    }
}
