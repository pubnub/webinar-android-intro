package intro.android.webinar.pubnub.com.androidintrowebinar;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.pubnub.api.PNConfiguration;
import com.pubnub.api.PubNub;
import com.pubnub.api.callbacks.SubscribeCallback;
import com.pubnub.api.enums.PNLogVerbosity;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult;

public class PubNubService extends Service {
    public static final String TAG = "PubNubService";
    public static final String ACTION_PN_MESSAGE = "PUBNUB_MESSAGE";
    public static final String ACTION_PN_STATUS = "PUBNUB_STATUS";
    public static final String ACTION_PN_PRESENCE = "PUBNUB_PRESENCE";

    public static final String PN_DATA = "PUBNUB_DATA";

    private PubNub pubnub = null;
    private Binder binder;

    public PubNubService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        binder = new Binder();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        pubnub = getPubNub();
        return super.onStartCommand(intent, flags, startId);
    }

    // init the PubNub object
    public PubNub getPubNub() {
        if (pubnub == null) {
            PNConfiguration pnConfiguration = new PNConfiguration();
            pnConfiguration.setSubscribeKey("demo-36");
            pnConfiguration.setPublishKey("demo-36");
            pnConfiguration.setSecure(false);
            pnConfiguration.setLogVerbosity(PNLogVerbosity.BODY);

            pubnub = new PubNub(pnConfiguration);
            addPubNubListener();
        }

        return pubnub;
    }

    public void addPubNubListener() {
        pubnub.addListener(new SubscribeCallback() {
            // MESSAGES
            @Override
            public void message(PubNub pubnub, PNMessageResult message) {
                Log.v(TAG, message.getMessage().toString());

                Intent intent = new Intent();
                intent.setAction(ACTION_PN_MESSAGE);
                intent.putExtra(PN_DATA, new ChatMessage(message));

                sendBroadcast(intent);
            }

            // STATUS EVENTS
            @Override
            public void status(PubNub pubnub, PNStatus status) {
                Log.v(TAG, status.getCategory().toString());

                Intent intent = new Intent();
                intent.setAction(ACTION_PN_STATUS);
                intent.putExtra(PN_DATA, status.getCategory());

                sendBroadcast(intent);
            }

            // PRESENCE EVENTS
            @Override
            public void presence(PubNub pubnub, PNPresenceEventResult presence) {
                Log.d(TAG, "begin presence: " + presence.getEvent() + ", " + presence.getUuid());

                Intent intent = new Intent();
                intent.setAction(ACTION_PN_PRESENCE);
                intent.putExtra(PN_DATA, new PresenceEvent(presence));

                sendBroadcast(intent);
            }
        });
    }

    public class Binder extends android.os.Binder {
        public PubNubService getService() {
            return PubNubService.this;
        }
    }
}