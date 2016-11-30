package intro.android.webinar.pubnub.com.androidintrowebinar;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pubnub.api.PNConfiguration;
import com.pubnub.api.PubNub;
import com.pubnub.api.PubNubException;
import com.pubnub.api.callbacks.PNCallback;
import com.pubnub.api.callbacks.SubscribeCallback;
import com.pubnub.api.enums.PNStatusCategory;
import com.pubnub.api.models.consumer.PNPublishResult;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.history.PNHistoryItemResult;
import com.pubnub.api.models.consumer.history.PNHistoryResult;
import com.pubnub.api.models.consumer.presence.PNHereNowChannelData;
import com.pubnub.api.models.consumer.presence.PNHereNowOccupantData;
import com.pubnub.api.models.consumer.presence.PNHereNowResult;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    final static public String TAG = "MainActivity";
    final static public String APP_PREFS = "app-prefs";
    final static public String CHANNEL = "intro-webinar";

    private PubNub pubnub;

    private UserProfile profile;

    private ArrayAdapter<String> buddiesListAdapter;
    private ArrayList<UserProfile> buddiesListItems = new ArrayList<UserProfile>();
    private ListView buddiesListView;

    private ArrayAdapter<String> messagesListAdapter;
    private ArrayList<ChatMessage> messagesListItems = new ArrayList<ChatMessage>();
    private ListView messagesListView;

    private EditText messageEditText;
    private Button sendButton;
    private Button buddiesButton;
    private ToggleButton activeStatusToggle;
    private HashMap<String, UserProfile> buddyList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buddiesListView = (ListView) findViewById(R.id.buddiesListView);
        messagesListView = (ListView) findViewById( R.id.messagesListView );

        messageEditText = (EditText) findViewById( R.id.messageEditText );
        sendButton = (Button) findViewById( R.id.sendButton );
        activeStatusToggle = (ToggleButton) findViewById(R.id.statusToggleButton);
        buddiesButton = (Button) findViewById(R.id.buddiesButton);

        // buddyList: uuid:state
        buddyList = new HashMap<String, UserProfile>();

        initBuddiesListView();
        initMessagesListView();

        initPubNub();
        addPubNubListener();
    }

    private void initBuddiesListView() {
        buddiesListAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_2, android.R.id.text1, buddiesListItems) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);

                UserProfile item = buddiesListItems.get(position);
                text1.setGravity(Gravity.RIGHT);
                text2.setGravity(Gravity.RIGHT);
                text1.setText(item.getUuid());
                text2.setText(item.getLocation());

                return view;
            }
        };
        buddiesListView.setAdapter(buddiesListAdapter);
    }

    private void initMessagesListView() {
        messagesListAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_2, android.R.id.text1, messagesListItems) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);

                ChatMessage item = messagesListItems.get(position);
                JsonNode senderNode = item.getMessage().get("sender");

                text1.setText(item.getMessage().get("text").asText());

                // my messages are blue
                if (senderNode.asText().equals(pubnub.getConfiguration().getUuid()))
                    text1.setTextColor(Color.BLUE);
                    // everyone elses' are red
                else
                    text1.setTextColor(Color.RED);

                String when = getFormattedDateTime(new java.util.Date(
                        (long) item.getPublishTT() / 10000)).toString();

                text2.setGravity(Gravity.RIGHT);

                if (senderNode != null)
                    text2.setText(senderNode.asText() + "\n" + when);
                else
                    text2.setText("anonymous @ " + when);

                return view;
            }
        };
        messagesListView.setAdapter(messagesListAdapter);
    }

    private String getFormattedDateTime(Date date) {
        android.text.format.DateFormat df = new android.text.format.DateFormat();
        return df.format("MM-dd hh:mm:ss", date).toString();
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

    public void toggleActiveStatus(View v) {
        if (activeStatusToggle.isChecked()) {
            joinChat();
        } else {
            leaveChat();
        }
    }

    //    private void addMessage(final PNMessageResult payload) {
    private void addBuddy(final UserProfile buddy) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                buddiesListItems.add(buddy);
                buddiesListAdapter.notifyDataSetChanged();

                // Select the last row so it will scroll into view
                buddiesListView.post(new Runnable() {
                    @Override
                    public void run() {
                        buddiesListView.setSelection(buddiesListAdapter.getCount() - 1);
                    }
                });
            }
        });
    }

    //    private void addMessage(final PNMessageResult payload) {
    private void addMessage(final ChatMessage chatMessage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messagesListItems.add(chatMessage);
                messagesListAdapter.notifyDataSetChanged();

                // Select the last row so it will scroll into view
                messagesListView.post(new Runnable() {
                    @Override
                    public void run() {
                        messagesListView.setSelection(messagesListAdapter.getCount() - 1);
                    }
                });
            }
        });
    }

    private void clearBuddies() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                buddiesListItems.clear();
                buddiesListAdapter.notifyDataSetChanged();
            }
        });
    }

    private void clearMessages() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messagesListItems.clear();
                messagesListAdapter.notifyDataSetChanged();
            }
        });
    }

    private ObjectNode createState() {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectNode payload = factory.objectNode();

        if (profile != null) {
            payload.put("location", profile.getLocation());
            payload.put("fullname", profile.getFullname());
            payload.put("language", profile.getLanguage());
        }

        return payload;
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
            configurePubnubUUID(pnConfiguration);
            pubnub = new PubNub(pnConfiguration);
        }
    }

    private void configurePubnubUUID(PNConfiguration pnconfig) {
        SharedPreferences sharedPrefs = getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE);

        // get the current pn_uuid value (first time, it will be null)
        String uuid = sharedPrefs.getString("pubnub-uuid", null);

        // if uuid hasn’t been created/ persisted, then create
        // and persist to use for subsequent app loads/connections
        if (uuid == null || uuid.length() == 0) {
            // generate a UUID or use your own custom uuid, if required
            uuid = UUID.randomUUID().toString();
            sharedPrefs.edit().putString("pubnub-uuid", uuid);

//            profile = new UserProfile();
//            profile.setUuid(uuid);
//            profile.setFullname("Anonymous");
//            profile.setLanguage("en");
//            profile.setLocation("Somewhere");

            sharedPrefs.edit().putString("pubnub-location", profile.getLocation());
            sharedPrefs.edit().putString("pubnub-language", profile.getLanguage());
            sharedPrefs.edit().putString("pubnub-fullname", profile.getFullname());
            sharedPrefs.edit().commit();
        }

//        profile = new UserProfile();
//        profile.setUuid(uuid);
//        profile.setFullname(sharedPrefs.getString("pubnub-fullname", "Anonymous"));
//        profile.setLanguage(sharedPrefs.getString("pubnub-fullname", "en"));
//        profile.setLocation(sharedPrefs.getString("pubnub-fullname", "Somewhere"));

        pnconfig.setUuid(uuid);
    }

    public void addPubNubListener() {
        pubnub.addListener(new SubscribeCallback() {
            @Override
            public void status(PubNub pubnub, PNStatus status) {
                Log.v(TAG, status.getCategory().toString());

                if (status.getCategory() == PNStatusCategory.PNConnectedCategory) {
                    // doing this in join event
                    // showToast("You have joined the chat room.");
                    showToast("You have joined");
                    fetchHistory();
                    fetchBuddies();
                } else if (status.getCategory() == PNStatusCategory.PNUnexpectedDisconnectCategory) {
                    showToast("Internet connectivity has been lost.");
                }
                else if (status.getCategory() == PNStatusCategory.PNReconnectedCategory) {
                    showToast("You have rejoined the chat room.");
                    fetchBuddies();
                    fetchHistory();
                }
            }

            @Override
            public void message(PubNub pubnub, PNMessageResult message) {
                Log.v(TAG, message.getMessage().toString());
                addMessage(new ChatMessage(message.getMessage(), message.getTimetoken()));
            }

            @Override
            public void presence(PubNub pubnub, PNPresenceEventResult presence) {
                Log.v(TAG, presence.toString());

                String action = presence.getEvent();
                String uuid = presence.getUuid();
                String channel = presence.getChannel();

                if (action.equalsIgnoreCase("join")) {
                    if (!uuid.equalsIgnoreCase(pubnub.getConfiguration().getUuid())) {
                        showToast(uuid + " has joined");
                    }

                    UserProfile buddy = new UserProfile(uuid, (JsonNode) presence.getState());
//                    addBuddy(buddy);

                    try {
                        pubnub.setPresenceState().channels(Arrays.asList(CHANNEL))
                                .state((JsonNode) createState()).sync();
                    } catch (PubNubException e) {
                        e.printStackTrace();
                    }
                } else if (action.equalsIgnoreCase("leave") || action.equalsIgnoreCase("timeout")) {
                    showToast(uuid + " has left");
                } else if (action.equalsIgnoreCase("state-change")) {
                    // update state of buddy in list
                    UserProfile buddy = new UserProfile(uuid, presence.getState());
                    addBuddy(buddy);
                } else {
                    // interval mode; occupancy exceeded max announce
                    // can use join/leave delta if enabled
                }
            }
        });
    }

    private void fetchHistory() {
        pubnub.history().channel(CHANNEL).includeTimetoken(true).async(new PNCallback<PNHistoryResult>() {
            @Override
            public void onResponse(PNHistoryResult result, PNStatus status) {
                clearMessages();

                List<PNHistoryItemResult> results = result.getMessages();

                for (PNHistoryItemResult item : results) {
                    addMessage(new ChatMessage(item.getEntry(), item.getTimetoken()));
                }
            }
        });
    }


    private void fetchBuddies() {
        pubnub.hereNow().channels(Arrays.asList(CHANNEL)).includeUUIDs(true).includeState(true).async(
                new PNCallback<PNHereNowResult>() {
                    @Override
                    public void onResponse(PNHereNowResult result, PNStatus status) {
//                    clearBuddies();

                        if (status.isError()) {
                            // handle error
                            return;
                        }

                        for (PNHereNowChannelData channelData : result.getChannels().values()) {
                            System.out.println("---");
                            System.out.println("channel:" + channelData.getChannelName());
                            System.out.println("occupancy: " + channelData.getOccupancy());
                            System.out.println("occupants:");

                            for (PNHereNowOccupantData occupant : channelData.getOccupants()) {
                                System.out.println("uuid: " + occupant.getUuid() + " state: " + occupant.getState());
//                            addBuddy(occupant);
                                //!!! state is a LinkedHashMap not JsonNode ????
                                addBuddy(new UserProfile(occupant.getUuid(), (JsonNode) occupant.getState()));
                            }
                        }
                    }
                });
    }

    private void joinChat() {
        pubnub.subscribe().channels(Arrays.asList(CHANNEL)).withPresence().execute();
        sendButton.setEnabled(true);
        messageEditText.setEnabled(true);
    }

    private void leaveChat() {
        pubnub.unsubscribe().channels(Arrays.asList(CHANNEL)).execute();
        sendButton.setEnabled(false);
        messageEditText.setEnabled(false);
        clearBuddies();
    }

    private void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    class ChatMessage {
        private JsonNode message;
        private Long publishTT;

        public ChatMessage(JsonNode message, Long timetoken) {
            this.setMessage(message);
            this.setPublishTT(timetoken);
        }

        public JsonNode getMessage() {
            return message;
        }

        public void setMessage(JsonNode message) {
            this.message = message;
        }

        public Long getPublishTT() {
            return publishTT;
        }

        public void setPublishTT(Long publishTT) {
            this.publishTT = publishTT;
        }
    }

    class UserProfile {
        private String fullname;
        private String uuid;
        private String location;
        private String language;

        public UserProfile() {
        }

        public UserProfile(String uuid, JsonNode state) {
            this.uuid = uuid;

            if (state != null) {
                if (state.get("fullname") != null)
                    this.fullname = state.get("fullname").asText();
                else
                    this.location = "Anonymous";

                if (state.get("location") != null)
                    this.location = state.get("location").asText();
                else
                    this.location = "Somewhere";

                if (state.get("language") != null)
                    this.language = state.get("language").asText();
                else
                    this.language = "en";
            } else {
                this.location = "Somewhere";
                this.fullname = "Anonymous";
                this.language = "en";
            }
        }

//        public UserProfile(String uuid, LinkedHashMap state) {
//            this.uuid = uuid;
//
//            if (state != null) {
//                if (state.get("fullname") != null)
//                    this.fullname = state.get("fullname").asText();
//                else
//                    this.location = "Anonymous";
//
//                if (state.get("location") != null)
//                    this.location = state.get("location").asText();
//                else
//                    this.location = "Somewhere";
//
//                if (state.get("language") != null)
//                    this.language = state.get("language").asText();
//                else
//                    this.language = "en";
//            }
//            else {
//                this.location = "Somewhere";
//                this.fullname = "Anonymous";
//                this.language = "en";
//            }
//        }

        public String getFullname() {
            return fullname;
        }

        public void setFullname(String fullname) {
            this.fullname = fullname;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }
    }

//    static public class CustomDialogFragment extends DialogFragment {
//        /** The system calls this to get the DialogFragment's layout, regardless
//         of whether it's being displayed as a dialog or an embedded fragment. */
//        @Override
//        public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                                 Bundle savedInstanceState) {
//            // Inflate the layout to use as dialog or embedded fragment
//            return inflater.inflate(R.layout.buddies, container, false);
//        }
//
//        /** The system calls this only when creating the layout in a dialog. */
//        @Override
//        public Dialog onCreateDialog(Bundle savedInstanceState) {
//            // The only reason you might override this method when using onCreateView() is
//            // to modify any dialog characteristics. For example, the dialog includes a
//            // title by default, but your custom layout might not need it. So here you can
//            // remove the dialog title, but you must call the superclass to get the Dialog.
//            Dialog dialog = super.onCreateDialog(savedInstanceState);
//            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
//            return dialog;
//        }
//    }
}
