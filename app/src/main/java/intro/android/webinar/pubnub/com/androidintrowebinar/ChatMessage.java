package intro.android.webinar.pubnub.com.androidintrowebinar;

import com.fasterxml.jackson.databind.JsonNode;
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


    public ChatMessage(String channel, Long publishTT, JsonNode json) {
        this.setChannel(channel);
        this.setPublishTT(publishTT);

        if (json != null) {
            if (json.get("sender") != null) {
                this.setSender(json.get("sender").asText());
            }

            if (json.get("text") != null) {
                this.setText(json.get("text").asText());
            }
        }
    }

    public ChatMessage(PNMessageResult message) {
        this.setChannel(message.getChannel());
        this.setPublishTT(message.getTimetoken());
        this.setSender(((JsonNode)message.getMessage()).get("sender").asText());
        this.setText(((JsonNode)message.getMessage()).get("text").asText());
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
