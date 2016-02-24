package nanqineveryday.qbot;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOService;
import nanqineveryday.qbot.util.IOIOCommands;

public class BgIOIOService extends IOIOService {
    private NotificationManager mNM;
    private static String ioioCommand = IOIOCommands.STANDBY;
    private boolean ioioStatus = false;

    @Override
    protected IOIOLooper createIOIOLooper() {
        return new BaseIOIOLooper() {
            public DigitalOutput led_, D1A, D1B, D2A, D2B;
            public PwmOutput PWM1, PWM2;

            @Override
            protected void setup() throws ConnectionLostException,
                    InterruptedException {
                led_ = ioio_.openDigitalOutput(IOIO.LED_PIN, false);
                D1A = ioio_.openDigitalOutput(41, false);  //left side motor
                D1B = ioio_.openDigitalOutput(42, false);
                D2A = ioio_.openDigitalOutput(43, false);
                D2B = ioio_.openDigitalOutput(44, false);
                PWM1 = ioio_.openPwmOutput(40, 100); //left side motor
                PWM1.setDutyCycle(0);
                PWM2 = ioio_.openPwmOutput(45, 100);
                PWM2.setDutyCycle(0);
                ioioStatus = true;
            }

            @Override
            public void loop() throws ConnectionLostException,
                    InterruptedException {
                if (ioioCommand.equals(IOIOCommands.STOP)) {
                    PWM1.setDutyCycle(0);
                    PWM2.setDutyCycle(0);
                    D1A.write(false);
                    D1B.write(false);
                    D2A.write(false);
                    D2B.write(false);
                    led_.write(false);
                    ioioCommand=IOIOCommands.STANDBY;
                }else if (ioioCommand.equals(IOIOCommands.LED)) {
                    led_.write(false);
                    Thread.sleep(200);
                    led_.write(true);
                    Thread.sleep(200);
                } else if (ioioCommand.equals(IOIOCommands.FORWARD)){
                    PWM1.setDutyCycle((float) 1);
                    PWM2.setDutyCycle((float) 1);
                    D1A.write(true);
                    D1B.write(false);
                    D2A.write(true);
                    D2B.write(false);
                } else if (ioioCommand.equals(IOIOCommands.BACK)){
                    PWM1.setDutyCycle((float) 1);
                    PWM2.setDutyCycle((float) 1);
                    D1A.write(false);
                    D1B.write(true);
                    D2A.write(false);
                    D2B.write(true);
                } else if (ioioCommand.equals(IOIOCommands.LEFT)){
                    PWM1.setDutyCycle((float)1);
                    PWM2.setDutyCycle((float)1);
                    D1A.write(false);
                    D1B.write(true);
                    D2A.write(true);
                    D2B.write(false);
                }
                else if (ioioCommand.equals(IOIOCommands.RIGHT)){
                    PWM1.setDutyCycle((float)1);
                    PWM2.setDutyCycle((float)1);
                    D1A.write(true);
                    D1B.write(false);
                    D2A.write(false);
                    D2B.write(true);
                }
                Thread.sleep(20);
            }

            @Override
            public void disconnected() {
                ioioStatus = false;
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int result = super.onStartCommand(intent, flags, startId);
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        showNotification();

        if (intent != null && intent.getAction() != null
                && intent.getAction().equals("stop")) {
            // User clicked the notification. Need to stop the service.
            mNM.cancel(R.string.local_service_started);
            stopSelf();
        } else {
            showNotification();
        }
        return result;
    }

    public void onDestroy() {
        // Cancel the persistent notification.
        super.onDestroy();
        mNM.cancel(R.string.local_service_started);
    }
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.local_service_started);

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getService(this, 0, new Intent(
                "stop", null, this, this.getClass()), 0);

        // Set the info for the views that show in the notification panel.
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)  // the status icon
                .setTicker(text)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle("Qbot")  // the label of the entry
                .setContentText(text+" Click to stop!")  // the contents of the entry
                .setContentIntent(contentIntent) // The intent to send when the entry is clicked
                .setOngoing(true);

        // Send the notification.
        mNM.notify(R.string.local_service_started, notificationBuilder.build());
    }

    public void setIOIOCommand(String command) {
        ioioCommand = command;
        Log.i("IOIOService", ioioCommand);
    }

    public boolean isIOIOConnected() {
        return ioioStatus;
    }

    private final IBinder mBinder = new LocalBinder();
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    public class LocalBinder extends Binder {
        public BgIOIOService getServiceInstance(){
            return BgIOIOService.this;
        }
    }
}
