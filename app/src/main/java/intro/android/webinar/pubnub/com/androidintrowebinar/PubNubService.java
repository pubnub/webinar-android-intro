package intro.android.webinar.pubnub.com.androidintrowebinar;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class PubNubService extends Service {

    public PubNubService() {
    }


    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Auto-generated method stub

        MyThread myThread = new MyThread();
        myThread.start();

        return super.onStartCommand(intent, flags, startId);
    }

    public class MyThread extends Thread {

        @Override
        public void run() {
            try {
                Thread.sleep(5000);
                Intent intent = new Intent();
                intent.setAction("foo-action");

                intent.putExtra("DATAPASSED", "foo message");
                sendBroadcast(intent);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }
}