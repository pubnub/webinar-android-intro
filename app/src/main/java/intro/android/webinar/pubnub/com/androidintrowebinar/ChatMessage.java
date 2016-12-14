package intro.android.webinar.pubnub.com.androidintrowebinar;

import com.google.gson.JsonElement;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;

import java.io.Serializable;

/**
 * Created by pubnubcvconover on 12/6/16.
 */

public class ChatMessage implements Serializable {
    private String channel;
    private Long publishTT;
    private String sender;
    private String text;


    public ChatMessage(String channel, Long publishTT, JsonElement json) {
        this.setChannel(channel);
        this.setPublishTT(publishTT);

        if (json != null) {
            if (json.getAsJsonObject().get("sender") != null) {
                this.setSender(json.getAsJsonObject().get("sender").getAsString());
            }

            if (json.getAsJsonObject().get("text") != null) {
                this.setText(json.getAsJsonObject().get("text").getAsString());
            }
        }
    }

    public ChatMessage(PNMessageResult message) {
        this.setChannel(message.getChannel());
        this.setPublishTT(message.getTimetoken());
        this.setSender(message.getMessage().getAsJsonObject().get("sender").getAsString());
        this.setText(message.getMessage().getAsJsonObject().get("text").getAsString());
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public Long getPublishTT() {
        return publishTT;
    }

    public void setPublishTT(Long publishTT) {
        this.publishTT = publishTT;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
