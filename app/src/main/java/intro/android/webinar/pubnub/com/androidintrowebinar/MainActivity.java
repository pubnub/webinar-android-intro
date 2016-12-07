package intro.android.webinar.pubnub.com.androidintrowebinar;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
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
import com.pubnub.api.PubNubException;
import com.pubnub.api.callbacks.PNCallback;
import com.pubnub.api.enums.PNStatusCategory;
import com.pubnub.api.models.consumer.PNPublishResult;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.history.PNHistoryItemResult;
import com.pubnub.api.models.consumer.history.PNHistoryResult;
import com.pubnub.api.models.consumer.presence.PNHereNowChannelData;
import com.pubnub.api.models.consumer.presence.PNHereNowOccupantData;
import com.pubnub.api.models.consumer.presence.PNHereNowResult;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    final static public String TAG = "MainActivity";
    final static public String APP_PREFS = "app-prefs";
    final static public String CHANNEL = "intro-webinar";

    private final static int ACTION_REMOVE = -1;
    private final static int ACTION_UPDATE = 0;
    private final static int ACTION_ADD = 1;

    private UserProfile profile;
    private PNDataReceiver pnDataReceiver;

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

    private PubNubService pubnubService;
    public ServiceConnection pubnubServiceConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            Log.d("ServiceConnection", "connected");
            pubnubService = ((PubNubService.Binder)binder).getService();
            configurePubnubUUID();
        }
        //binder comes from server to communicate with method's of

        public void onServiceDisconnected(ComponentName className) {
            Log.d("ServiceConnection", "disconnected");
            pubnubService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buddiesListView = (ListView) findViewById(R.id.buddiesListView);
        messagesListView = (ListView) findViewById(R.id.messagesListView);

        messageEditText = (EditText) findViewById(R.id.messageEditText);
        sendButton = (Button) findViewById(R.id.sendButton);
        activeStatusToggle = (ToggleButton) findViewById(R.id.statusToggleButton);
        buddiesButton = (Button) findViewById(R.id.buddiesButton);

        // buddyList: uuid:state
        buddyList = new HashMap<String, UserProfile>();

        // set uuid on PubNub instance in PubNubService

        initBuddiesListView();
        initMessagesListView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        pnDataReceiver = new PNDataReceiver();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PubNubService.ACTION_PN_MESSAGE);
        intentFilter.addAction(PubNubService.ACTION_PN_STATUS);
        intentFilter.addAction(PubNubService.ACTION_PN_PRESENCE);
        registerReceiver(pnDataReceiver, intentFilter);

        Intent intent = new Intent(MainActivity.this, PubNubService.class);
        bindService(intent);
        startService(intent);
    }

    @Override
    public void onResume() {
        super.onResume();

        Intent intent = new Intent(MainActivity.this, PubNubService.class);
        bindService(intent);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (pubnubServiceConn != null) {
            unbindService(pubnubServiceConn);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(pubnubServiceConn);
    }

    private void bindService(Intent intent) {
        if (pubnubServiceConn != null) {
            bindService(intent, pubnubServiceConn, Context.BIND_AUTO_CREATE);
        }
    }

    private void unbindService() {
        if (pubnubServiceConn != null) {
            unbindService(pubnubServiceConn);
        }
    }

    private void handleStatusEvent(PNStatusCategory statusCategory) {
        if (statusCategory == PNStatusCategory.PNConnectedCategory) {
            showToast("You have joined");
            fetchHistory();
            fetchBuddies();
            updateBuddyList(ACTION_ADD, profile.getUuid(), profile);
        }
        else if (statusCategory == PNStatusCategory.PNUnexpectedDisconnectCategory) {
            showToast("Internet connectivity has been lost.");
        }
        else if (statusCategory == PNStatusCategory.PNReconnectedCategory) {
            showToast("You have rejoined the chat room.");
            fetchBuddies();
            fetchHistory();
        }
    }

    private void handlePresenceEvent(PresenceEvent presence) {
        String action = presence.getAction();
        String uuid = presence.getUuid();
        String channel = presence.getChannel();

        // JOIN event
        if (action.equalsIgnoreCase("join")) {
            if (!uuid.equalsIgnoreCase(pubnubService.getPubNub().getConfiguration().getUuid())) {
                showToast(uuid + " has joined");


//                    UserProfile buddy = new UserProfile(uuid, (JsonNode) presence.getState());
//                    addBuddy(buddy);

                try {
                    pubnubService.getPubNub().setPresenceState().channels(Arrays.asList(CHANNEL))
                            .state((JsonNode) createState()).sync();
                }
                catch (PubNubException e) {
                    e.printStackTrace();
                }
            }
        }
        // LEAVE or TIMEOUT event
        else if (action.equalsIgnoreCase("leave") || action.equalsIgnoreCase("timeout")) {
            showToast(uuid + " has left");

            updateBuddyList(ACTION_REMOVE, uuid, null);
        }
        // STATE CHANGE event
        else if (action.equalsIgnoreCase("state-change")) {
            // update state of buddy in list
            UserProfile buddy = new UserProfile(uuid, presence.getState());
            updateBuddyList(ACTION_UPDATE, uuid, buddy);
        }
        else {
            // interval mode; occupancy exceeded max announce
            // can use join/leave delta if enabled
            // Andvanced Feature
        }
    }

    /**
     * Called when the user clicks the Send button
     */
    public void sendMessage(View view) {
        Log.v(TAG, messageEditText.getText().toString());

        pubnubService.getPubNub().publish()
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

    private ObjectNode createMessage(String message) {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectNode payload = factory.objectNode();
        payload.put("text", message);
        payload.put("sender", pubnubService.getPubNub().getConfiguration().getUuid());

        return payload;
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

    private void fetchHistory() {
        pubnubService.getPubNub().history().channel(CHANNEL).includeTimetoken(true).async(new PNCallback<PNHistoryResult>() {
            @Override
            public void onResponse(PNHistoryResult result, PNStatus status) {
                clearMessages();

                List<PNHistoryItemResult> results = result.getMessages();

                for (PNHistoryItemResult item : results) {
                    addMessage(new ChatMessage(CHANNEL, item.getTimetoken(), item.getEntry()));
                }
            }
        });
    }

    private void updateBuddyList(final int action, final String uuid, final UserProfile buddy) {
        Log.d(TAG, "begin updateBuddyList: " + uuid);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                UserProfile tbuddy = null;
                int pos = -1;

                for (int i = 0; i < buddiesListItems.size(); i++) {
                    UserProfile curbuddy = buddiesListItems.get(i);

                    if (curbuddy != null && curbuddy.getUuid().equals(uuid)) {
                        tbuddy = curbuddy;
                        pos = i;
                        break;
                    }
                }

                if (action == ACTION_REMOVE) {
                    buddiesListItems.remove(pos);
                    buddyList.remove(uuid);
                }
                else if (action == ACTION_ADD) {
                    buddiesListItems.add(buddy);
                    buddyList.put(uuid, buddy);
                }
                else {
                    if (tbuddy == null) {
                        // buddy was not found in current buddy list, so add to list
                        buddiesListItems.add(buddy);
                        buddyList.put(uuid, buddy);
                    }
                    else {
                        // buddy was already found in the list so just update the element in place
                        buddiesListItems.set(pos, buddy);
                    }
                }

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

    private void fetchBuddies() {
        Log.d(TAG, "begin fetchBuddies");

        pubnubService.getPubNub().hereNow().channels(Arrays.asList(CHANNEL)).includeUUIDs(true).includeState(true).async(
                new PNCallback<PNHereNowResult>() {
                    @Override
                    public void onResponse(PNHereNowResult result, PNStatus status) {
//                    clearBuddies();

                        if (status.isError()) {
                            Log.e(TAG, "ERROR: " + status.getErrorData());
                            return;
                        }

                        for (PNHereNowChannelData channelData : result.getChannels().values()) {
                            System.out.println("---");
                            System.out.println("channel:" + channelData.getChannelName());
                            System.out.println("occupancy: " + channelData.getOccupancy());
                            System.out.println("occupants:");

                            for (PNHereNowOccupantData occupant : channelData.getOccupants()) {
                                Log.d(TAG, "uuid: " + occupant.getUuid() + " state: " + occupant.getState());

                                if (!occupant.getUuid().equals(pubnubService.getPubNub().getConfiguration().getUuid())) {
                                    updateBuddyList(ACTION_ADD, occupant.getUuid(),
                                            new UserProfile(occupant.getUuid(), (LinkedHashMap) occupant.getState()));
                                }
                            }
                        }
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

    public void toggleActiveStatus(View v) {
        if (activeStatusToggle.isChecked()) {
            joinChat();
        }
        else {
            leaveChat();
        }
    }

    private void joinChat() {
        pubnubService.getPubNub().subscribe().channels(Arrays.asList(CHANNEL)).withPresence().execute();
        sendButton.setEnabled(true);
        messageEditText.setEnabled(true);
    }

    private void leaveChat() {
        pubnubService.getPubNub().unsubscribe().channels(Arrays.asList(CHANNEL)).execute();
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

    private void configurePubnubUUID() {
        SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = defaultPrefs.edit();

        // get the current pn_uuid value (first time, it will be null)
        String uuid = defaultPrefs.getString("uuid", null);

        // if uuid hasn’t been created/ persisted, then create
        // and persist to use for subsequent app loads/connections
        if (uuid == null || uuid.length() == 0) {
            // generate a UUID or use your own custom uuid, if required
            uuid = UUID.randomUUID().toString();
            editor.putString("uuid", uuid);
            editor.putString("location", profile == null ? UserProfile.DEFAULT_LOCATION : profile.getLocation());
            editor.putString("language", profile == null ? UserProfile.DEFAULT_LANGUAGE : profile.getLanguage());
            editor.putString("fullname", profile == null ? UserProfile.DEFAULT_FULLNAME : profile.getFullname());
            editor.commit();
        }

        profile = new UserProfile();
        profile.setUuid(uuid);
        profile.setFullname(defaultPrefs.getString("fullname", UserProfile.DEFAULT_FULLNAME));
        profile.setLanguage(defaultPrefs.getString("language", UserProfile.DEFAULT_LANGUAGE));
        profile.setLocation(defaultPrefs.getString("location", UserProfile.DEFAULT_LOCATION));

        // set the uuid for pnconfig
        pubnubService.getPubNub().getConfiguration().setUuid(uuid);
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

                String name = item.getFullname();
                String uuid = item.getUuid();

                if (name != null && name.equals("anon") && uuid != null) {
                    name = truncateUserIdentity(uuid);
                }

                text1.setText(name);
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
                String sender = item.getSender();
                String text = item.getText();

                text1.setText(text);

                // my messages are blue
                if (sender != null && sender.equals(pubnubService.getPubNub().getConfiguration().getUuid()))
                    text1.setTextColor(Color.BLUE);
                    // everyone elses' are red
                else
                    text1.setTextColor(Color.RED);

                String when = getFormattedDateTime(new java.util.Date(
                        (long) item.getPublishTT() / 10000)).toString();

                text2.setGravity(Gravity.RIGHT);

                if (sender != null) {
                    String who = sender;
                    UserProfile buddy = buddyList.get(sender);

                    if (buddy != null) {
                        who = buddy.getFullname();
                    }
                    else who = truncateUserIdentity(who);

                    text2.setText(who + "\n" + when);
                }
                else
                    text2.setText("unknown \n" + when);

                return view;
            }
        };

        messagesListView.setAdapter(messagesListAdapter);
    }

    public void displayProfile(View view) {
        Intent intent = new Intent(this, ProfileActivity.class);
        startActivity(intent);
    }

    private String getFormattedDateTime(Date date) {
        android.text.format.DateFormat df = new android.text.format.DateFormat();
        return df.format("MM-dd hh:mm:ss", date).toString();
    }

    private String truncateUserIdentity(String uuid) {
        int length = uuid.length() > 12 ? 12 : uuid.length();
        return uuid.substring(0, length);
    }

    private class PNDataReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Serializable data = intent.getSerializableExtra(PubNubService.PN_DATA);

            // MESSAGES
            if (intent.getAction().equals(PubNubService.ACTION_PN_MESSAGE)) {
                addMessage((ChatMessage) data);
            }
            // STATUS EVENTS
            else if (intent.getAction().equals(PubNubService.ACTION_PN_STATUS)) {
                handleStatusEvent((PNStatusCategory)data);
            }
            // PRESENCE EVENTS
            else {
                handlePresenceEvent((PresenceEvent)data);
            }

        }
    }

}
