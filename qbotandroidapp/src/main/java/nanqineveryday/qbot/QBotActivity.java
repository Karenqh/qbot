package nanqineveryday.qbot;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import nanqineveryday.qbot.gcm.RegistrationIntentService;
import nanqineveryday.qbot.gcm.gcmPreferences;
import nanqineveryday.qbot.util.Constants;
import nanqineveryday.qbot.util.IOIOCommands;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.content.SharedPreferences;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.pubnub.api.Callback;
import com.pubnub.api.PnGcmMessage;
import com.pubnub.api.PnMessage;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;
import org.json.JSONException;
import org.json.JSONObject;

public class QBotActivity extends Activity {
	private ToggleButton ledToggleButton_, toggleButtonForward_, toggleButtonBack_, toggleButtonLeft_, toggleButtonRight_;
    private TextView usernameText_, robotnameText_;
	private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	private static final String TAG = "MainActivity";
	private SharedPreferences mSharedPreferences;
	private String robotname;
    private String username;
	private String stdByChannel;
	private Pubnub mPubNub;
	//service bounding
	BgIOIOService mService;
	boolean mBound = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Register with GCM
		if (checkPlayServices()) {
			// Start IntentService to register this application with GCM.
			Intent intent = new Intent(QBotActivity.this, RegistrationIntentService.class);
			startService(intent);
		}

        //check robotname or launch login activity
        this.mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFS, MODE_PRIVATE);
        if ((!this.mSharedPreferences.contains(Constants.ROBOT_NAME))||(!this.mSharedPreferences.contains(Constants.USER_NAME))){
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        this.robotname = this.mSharedPreferences.getString(Constants.ROBOT_NAME, "");
        this.username = this.mSharedPreferences.getString(Constants.USER_NAME, "");
        this.stdByChannel = this.robotname + Constants.STDBY_SUFFIX;

		//Start IOIO Background Service
		startService(new Intent(this, BgIOIOService.class));

        //PubNub Initialize
        initPubNub();
		//Set layout
		setContentView(R.layout.main);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        //show usernames
        this.usernameText_  = (TextView) findViewById(R.id.username_text);
        this.usernameText_.setText(this.username);
        this.robotnameText_  = (TextView) findViewById(R.id.robotname_text);
        this.robotnameText_.setText(this.robotname);

		//find buttons
		ledToggleButton_ = (ToggleButton) findViewById(R.id.ledToggleButton);
		toggleButtonBack_= (ToggleButton) findViewById(R.id.toggleButtonBack);
		toggleButtonForward_= (ToggleButton) findViewById(R.id.toggleButtonForward);
		toggleButtonLeft_= (ToggleButton) findViewById(R.id.toggleButtonLeft);
		toggleButtonRight_= (ToggleButton) findViewById(R.id.toggleButtonRight);
		//button listeners
		ledToggleButton_.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(mBound) {if (isChecked) {
					mService.setIOIOCommand(IOIOCommands.LED);
				} else {
					mService.setIOIOCommand(IOIOCommands.STOP);
				}}
			}
		});
		toggleButtonBack_.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(mBound) {if (isChecked) {
					mService.setIOIOCommand(IOIOCommands.BACK);
					toggleButtonRight_.setEnabled(false);
					toggleButtonLeft_.setEnabled(false);
					toggleButtonForward_.setEnabled(false);
				} else {
					mService.setIOIOCommand(IOIOCommands.STOP);
					toggleButtonRight_.setEnabled(true);
					toggleButtonLeft_.setEnabled(true);
					toggleButtonForward_.setEnabled(true);
				}}
			}
		});
		toggleButtonForward_.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(mBound) {if (isChecked) {
					mService.setIOIOCommand(IOIOCommands.FORWARD);
					toggleButtonRight_.setEnabled(false);
					toggleButtonLeft_.setEnabled(false);
					toggleButtonBack_.setEnabled(false);
				} else {
					mService.setIOIOCommand(IOIOCommands.STOP);
					toggleButtonRight_.setEnabled(true);
					toggleButtonLeft_.setEnabled(true);
					toggleButtonBack_.setEnabled(true);
				}}
			}
		});
		toggleButtonLeft_.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(mBound) {if (isChecked) {
					mService.setIOIOCommand(IOIOCommands.LEFT);
					toggleButtonRight_.setEnabled(false);
					toggleButtonBack_.setEnabled(false);
					toggleButtonForward_.setEnabled(false);
				} else {
					mService.setIOIOCommand(IOIOCommands.STOP);
					toggleButtonRight_.setEnabled(true);
					toggleButtonBack_.setEnabled(true);
					toggleButtonForward_.setEnabled(true);
				}}
			}
		});
		toggleButtonRight_.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(mBound) {if (isChecked) {
					mService.setIOIOCommand(IOIOCommands.RIGHT);
					toggleButtonBack_.setEnabled(false);
					toggleButtonLeft_.setEnabled(false);
					toggleButtonForward_.setEnabled(false);
				} else {
					mService.setIOIOCommand(IOIOCommands.STOP);
					toggleButtonBack_.setEnabled(true);
					toggleButtonLeft_.setEnabled(true);
					toggleButtonForward_.setEnabled(true);
				}}
			}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();
		// Bind to LocalService
		Intent intent = new Intent(this, BgIOIOService.class);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch(id){
            case R.id.action_sign_out:
                signOut();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        if(this.mPubNub!=null){ //no need to unsubscribe?
            this.mPubNub.unsubscribeAll();
        }
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
        super.onStop();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if(this.mPubNub==null){
            initPubNub();
        } else {
            subscribeStdBy();
        }
    }
    protected void onDestroy(){
        Log.i(TAG, "onDestroy()");
        if(this.mPubNub!=null){
            setUserStatus(Constants.STATUS_OFFLINE);
            this.mPubNub.unsubscribeAll();
        }
        stopService(new Intent(this, BgIOIOService.class));
        super.onDestroy();
    }
	/**
	 * Subscribe to standby channel so that it doesn't interfere with the WebRTC Signaling.
	 */
	public void initPubNub(){
		this.mPubNub  = new Pubnub(Constants.PUB_KEY, Constants.SUB_KEY);
		this.mPubNub.setUUID(this.robotname);
		subscribeStdBy();
	}

	/**
	 * Subscribe to standby channel
	 */
	private void subscribeStdBy(){
        setUserStatus(Constants.STATUS_AVAILABLE);
		try {
			this.mPubNub.subscribe(this.stdByChannel, new Callback() {
				@Override
				public void successCallback(String channel, Object message) {
					Log.d("MA-iPN", "MESSAGE: " + message.toString());
					if (!(message instanceof JSONObject)) return; // Ignore if not JSONObject
					JSONObject jsonMsg = (JSONObject) message;
					try {
						if (jsonMsg.has(Constants.JSON_USER_CALL)) {
							if (username.equals(jsonMsg.getString(Constants.JSON_USER_CALL))){
							dispatchIncomingCall(username);}
						} else if (jsonMsg.has(Constants.JSON_POWER)){ //turn off application
							if (jsonMsg.getString(Constants.JSON_POWER).equals(Constants.JSON_POWER_OFF)) {
								Log.i(TAG, "Exit application on request by the user");
								finish();
							}
						} else return;     //Ignore Signaling messages.}
					} catch (JSONException e){
						e.printStackTrace();
					}
				}

				@Override
				public void connectCallback(String channel, Object message) {
                    Log.d("MA-iPN", "CONNECTED: " + message.toString());
                }
				@Override
				public void errorCallback(String channel, PubnubError error) {
					Log.d("MA-iPN","ERROR: " + error.toString());
				}
			});
		} catch (PubnubException e){
			Log.d("Pubnub", "subscribe error");
			e.printStackTrace();
		}
	}

	private void publishMessage(String channel, JSONObject jsomessage){
		Callback callback = new Callback() {
			public void successCallback(String channel, Object response) {
				Log.i(TAG, "Success publishing to: "+ channel);
			}
			public void errorCallback(String channel, PubnubError error) {
				Log.i(TAG, "Error publishing to : " + channel);
			}
		};
		mPubNub.publish(channel, jsomessage, callback);
	}


	/**
	 * Handle incoming calls. TODO: Implement an accept/reject functionality.
	 * @param userId
	 */
	private void dispatchIncomingCall(String userId){
		Intent intent = new Intent(QBotActivity.this, VideoChatActivity.class);
		intent.putExtra(Constants.USER_NAME, userId);
		intent.putExtra(Constants.ROBOT_NAME, robotname);
		startActivity(intent);
	}

	private void setUserStatus(String status){
        Log.i(TAG, "set user status " + status);
		try {
			JSONObject state = new JSONObject();
			state.put(Constants.JSON_STATUS, status);
			this.mPubNub.setState(this.stdByChannel, this.robotname, state, new Callback() {
				@Override
				public void successCallback(String channel, Object message) {
					Log.d("MA-sUS","State Set: " + message.toString());
				}
			});
		} catch (JSONException e){
			e.printStackTrace();
		}
	}

    //clear credentials and goto login page
    public void signOut(){
        this.mPubNub.unsubscribeAll();
        this.mSharedPreferences.edit().remove(Constants.USER_NAME).apply();
        this.mSharedPreferences.edit().remove(Constants.ROBOT_NAME).apply();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("oldUsername", this.username);
        startActivity(intent);
    }


	/**
	 * Check the device to make sure it has the Google Play Services APK. If
	 * it doesn't, display a dialog that allows users to download the APK from
	 * the Google Play Store or enable it in the device's system settings.
	 */
	private boolean checkPlayServices() {
		GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
		int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
		if (resultCode != ConnectionResult.SUCCESS) {
			if (apiAvailability.isUserResolvableError(resultCode)) {
				apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
						.show();
			} else {
				Log.i(TAG, "This device is not supported.");
				finish();
			}
			return false;
		}
		return true;
	}

	/** Defines callbacks for service binding, passed to bindService() */
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className,
									   IBinder service) {
			// We've bound to LocalService, cast the IBinder and get LocalService instance
			BgIOIOService.LocalBinder binder = (BgIOIOService.LocalBinder) service;
			mService = binder.getServiceInstance();
			mBound = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mBound = false;
		}
	};
}