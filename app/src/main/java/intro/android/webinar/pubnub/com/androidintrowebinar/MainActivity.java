package intro.android.webinar.pubnub.com.androidintrowebinar;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.pubnub.api.PNConfiguration;
import com.pubnub.api.PubNub;
import com.pubnub.api.callbacks.PNCallback;
import com.pubnub.api.callbacks.SubscribeCallback;
import com.pubnub.api.enums.PNStatusCategory;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.PNPublishResult;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult;

import java.util.ArrayList;
import java.util.Arrays;

import static android.R.id.list;

public class MainActivity extends AppCompatActivity {
    final static public String TAG = "MainActivity";
    final static public String CHANNEL = "intro-webinar";

    private PubNub pubnub;

    private ArrayAdapter<String> listAdapter ;
    private ArrayList<PNMessageResult> listItems = new ArrayList<PNMessageResult>();
    private ListView messagesListView;
    private EditText messageEditText;
    private Button sendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        messagesListView = (ListView) findViewById( R.id.messagesListView );
        messageEditText = (EditText) findViewById( R.id.messageEditText );
        sendButton = (Button) findViewById( R.id.sendButton );

        initListView();

        initPubNub();
        addPubNubListener();
        joinChat();
    }

    private void initListView() {
//        listAdapter = new ArrayAdapter<String>(this, R.layout.message_row, listItems);
//        messagesListView.setAdapter(listAdapter);

        listAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_2, android.R.id.text1, listItems) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);

                PNMessageResult item = listItems.get(position);
                text1.setText(item.getMessage().get("text").asText());
                String when = new java.util.Date((long)item.getTimetoken()/10000).toString();
//                text2.setTextAlignment();
                text2.setText(item.getMessage().get("sender").asText() + " @ " + when);
                return view;
            }
        };
        messagesListView.setAdapter(listAdapter);
    }

    /** Called when the user clicks the Send button */
    public void sendMessage(View view) {
        Log.v(TAG, messageEditText.getText().toString());

        pubnub.publish()
            .channel(CHANNEL)
            .message(createMessage(messageEditText.getText().toString()))
            .async(new PNCallback<PNPublishResult>() {
                @Override
                public void onResponse(PNPublishResult result, PNStatus status) {
                    Log.v(TAG, "pub timetoken:   " + result.getTimetoken());
                    Log.v(TAG, "pub status code: " + status.getStatusCode());

                    if (!status.isError()) {
                        messageEditText.setText("");
                    }
                }
            });
    }

    private void addMessage(final PNMessageResult payload) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                listItems.add(payload.get("text").textValue());
                listItems.add(payload);
                listAdapter.notifyDataSetChanged();

                messagesListView.post(new Runnable() {
                    @Override
                    public void run() {
                        // Select the last row so it will scroll into view...
                        messagesListView.setSelection(listAdapter.getCount() - 1);
                    }
                });
            }
        });
    }

    private ObjectNode createMessage(String message) {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectNode payload = factory.objectNode();
        payload.put("text", message);
        payload.put("sender", pubnub.getConfiguration().getUuid());

        return payload;
    }

    // init the PubNub object
    public void initPubNub() {
        if (pubnub == null) {
            PNConfiguration pnConfiguration = new PNConfiguration();
            pnConfiguration.setSubscribeKey("demo-36");
            pnConfiguration.setPublishKey("demo-36");
            pnConfiguration.setSecure(false);
            pnConfiguration.setUuid("cvc-android-webinar");

            pubnub = new PubNub(pnConfiguration);
        }
    }

    public void addPubNubListener() {
        pubnub.addListener(new SubscribeCallback() {
            @Override
            public void status(PubNub pubnub, PNStatus status) {
                Log.v(TAG, status.getCategory().toString());

                if (status.getCategory() == PNStatusCategory.PNUnexpectedDisconnectCategory) {
                    // This event happens when radio / connectivity is lost
                }
                else if (status.getCategory() == PNStatusCategory.PNConnectedCategory) {
                    // Connect event. You can do stuff like publish, and know you'll get it.
                    // Or just use the connected event to confirm you are subscribed for
                    // UI / internal notifications, etc

                    if (status.getCategory() == PNStatusCategory.PNConnectedCategory) {

                    }
                }
                else if (status.getCategory() == PNStatusCategory.PNReconnectedCategory) {
                    // Happens as part of our regular operation. This event happens when
                    // radio / connectivity is lost, then regained.
                }
            }

            @Override
            public void message(PubNub pubnub, PNMessageResult message) {
                Log.v(TAG, message.getMessage().toString());
                addMessage(message);

//                if (message.getChannel() != null) {
//
//                }
//                else {
//                    // Message has been received on channel stored in
//                    // message.getSubscription()
//                }
            }

            @Override
            public void presence(PubNub pubnub, PNPresenceEventResult presence) {
                Log.v(TAG, presence.getEvent().toString());
            }
        });
    }

    public void joinChat() {
        pubnub.subscribe().channels(Arrays.asList(CHANNEL)).execute();
    }

    public void leaveChat() {
        pubnub.unsubscribe().channels(Arrays.asList(CHANNEL)).execute();
    }
}
