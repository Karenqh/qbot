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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
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

public class QBotActivity extends IOIOActivity {
	private ToggleButton ledToggleButton_, toggleButtonForward_, toggleButtonBack_, toggleButtonLeft_, toggleButtonRight_;
	private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	private static final String TAG = "MainActivity";
	//private SharedPreferences mSharedPreferences;
	private String username;
	private String stdByChannel;
	private String userStdByChannel;
	private Pubnub mPubNub;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
				             WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
				             WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
				             WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

		ledToggleButton_ = (ToggleButton) findViewById(R.id.ledToggleButton);
		toggleButtonBack_= (ToggleButton) findViewById(R.id.toggleButtonBack);
		toggleButtonForward_= (ToggleButton) findViewById(R.id.toggleButtonForward);
		toggleButtonLeft_= (ToggleButton) findViewById(R.id.toggleButtonLeft);
		toggleButtonRight_= (ToggleButton) findViewById(R.id.toggleButtonRight);

		enableUi(false);

		//this.mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFS, MODE_PRIVATE);
		this.username     = Constants.USER_NAME;
		this.stdByChannel = this.username + Constants.STDBY_SUFFIX;
        this.userStdByChannel = Constants.CALL_USER  + Constants.STDBY_SUFFIX;
		initPubNub();

		if (checkPlayServices()) {
			// Start IntentService to register this application with GCM.
			Intent intent = new Intent(QBotActivity.this, RegistrationIntentService.class);
			startService(intent);
		}
	}

	/**
	 * Subscribe to standby channel so that it doesn't interfere with the WebRTC Signaling.
	 */
	public void initPubNub(){
		this.mPubNub  = new Pubnub(Constants.PUB_KEY, Constants.SUB_KEY);
		this.mPubNub.setUUID(this.username);
		subscribeStdBy();
	}

	/**
	 * Subscribe to standby channel
	 */
	private void subscribeStdBy(){
		try {
			this.mPubNub.subscribe(this.stdByChannel, new Callback() {
				@Override
				public void successCallback(String channel, Object message) {
					Log.d("MA-iPN", "MESSAGE: " + message.toString());
					if (!(message instanceof JSONObject)) return; // Ignore if not JSONObject
					JSONObject jsonMsg = (JSONObject) message;
					try {
						if (jsonMsg.has(Constants.JSON_CALL_USER)) {
							String user = jsonMsg.getString(Constants.JSON_CALL_USER);
							dispatchIncomingCall(user);
						} else if (jsonMsg.has(Constants.JSON_POWER)){ //turn off application
							if (jsonMsg.getString(Constants.JSON_POWER).equals(Constants.JSON_POWER_OFF)) {
								JSONObject jso = new JSONObject();
								try {
									jso.put("status", "Offline");
								} catch (JSONException e) { }
								publishMessage(userStdByChannel, jso);
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
					setUserStatus(Constants.STATUS_AVAILABLE);
					JSONObject jso = new JSONObject();
					try {
						jso.put("status", "Available");
					} catch (JSONException e) { }
					publishMessage(userStdByChannel, jso);
				}

				@Override
				public void errorCallback(String channel, PubnubError error) {
					Log.d("MA-iPN","ERROR: " + error.toString());
				}
			});
		} catch (PubnubException e){
			Log.d("HERE", "HEREEEE");
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

	class Looper extends BaseIOIOLooper {
		public DigitalOutput led_, D1A, D1B, D2A, D2B;
		public PwmOutput PWM1, PWM2;

		@Override
		public void setup() throws ConnectionLostException {
			led_ = ioio_.openDigitalOutput(IOIO.LED_PIN, true);
			D1A = ioio_.openDigitalOutput(41, false);  //left side motor
			D1B = ioio_.openDigitalOutput(42, false);
			D2A = ioio_.openDigitalOutput(43, false);
			D2B = ioio_.openDigitalOutput(44, false);
			PWM1 = ioio_.openPwmOutput(40, 100); //left side motor
			PWM1.setDutyCycle(0);
			PWM2 = ioio_.openPwmOutput(45, 100);
			PWM2.setDutyCycle(0);

			ledToggleButton_.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					try {
						if (isChecked) {
							led_.write(!ledToggleButton_.isChecked());
						} else {
							led_.write(!ledToggleButton_.isChecked());
						}
					} catch (ConnectionLostException e) {
						e.printStackTrace();
					}
				}
			});
			toggleButtonBack_.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					try {
						if (isChecked) {
							toggleButtonRight_.setEnabled(false);
							toggleButtonLeft_.setEnabled(false);
							toggleButtonForward_.setEnabled(false);
							PWM1.setDutyCycle((float) 0.8);
							PWM2.setDutyCycle((float) 0.8);
							D1A.write(false);
							D1B.write(true);
							D2A.write(false);
							D2B.write(true);
						} else {
							toggleButtonRight_.setEnabled(true);
							toggleButtonLeft_.setEnabled(true);
							toggleButtonForward_.setEnabled(true);
							PWM1.setDutyCycle(0);
							PWM2.setDutyCycle(0);
						}
					} catch (ConnectionLostException e) {
						e.printStackTrace();
					}
				}
			});
			toggleButtonForward_.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					try {
						if (isChecked) {
							toggleButtonRight_.setEnabled(false);
							toggleButtonLeft_.setEnabled(false);
							toggleButtonBack_.setEnabled(false);
							PWM1.setDutyCycle((float) 1);
							PWM2.setDutyCycle((float) 1);
							D1A.write(true);
							D1B.write(false);
							D2A.write(true);
							D2B.write(false);
						} else {
							toggleButtonRight_.setEnabled(true);
							toggleButtonLeft_.setEnabled(true);
							toggleButtonBack_.setEnabled(true);
							PWM1.setDutyCycle(0);
							PWM2.setDutyCycle(0);
						}
					} catch (ConnectionLostException e) {
						e.printStackTrace();
					}
				}
			});
			toggleButtonLeft_.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					try {
						if (isChecked) {
							toggleButtonRight_.setEnabled(false);
							toggleButtonBack_.setEnabled(false);
							toggleButtonForward_.setEnabled(false);
							PWM1.setDutyCycle(1);
							PWM2.setDutyCycle(1);
							D1A.write(false);
							D1B.write(true);
							D2A.write(true);
							D2B.write(false);
						} else {
							toggleButtonRight_.setEnabled(true);
							toggleButtonBack_.setEnabled(true);
							toggleButtonForward_.setEnabled(true);
							PWM1.setDutyCycle(0);
							PWM2.setDutyCycle(0);
						}
					} catch (ConnectionLostException e) {
						e.printStackTrace();
					}
				}
			});
			toggleButtonRight_.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					try {
						if (isChecked) {
							toggleButtonBack_.setEnabled(false);
							toggleButtonLeft_.setEnabled(false);
							toggleButtonForward_.setEnabled(false);
							PWM1.setDutyCycle(1);
							PWM2.setDutyCycle(1);
							D1A.write(true);
							D1B.write(false);
							D2A.write(false);
							D2B.write(true);
						} else {
							toggleButtonBack_.setEnabled(true);
							toggleButtonLeft_.setEnabled(true);
							toggleButtonForward_.setEnabled(true);
							PWM1.setDutyCycle(0);
							PWM2.setDutyCycle(0);
						}
					} catch (ConnectionLostException e) {
						e.printStackTrace();
					}
				}
			});
			enableUi(true);
		}

		@Override
		public void loop() throws ConnectionLostException, InterruptedException {
//			led_.write(!ledToggleButton_.isChecked());
//			PWM1.setDutyCycle(1);
//			PWM2.setDutyCycle(1);
//			D1A.write(true);
//			D1B.write(false);
//			D2A.write(true);
//			D2B.write(false);

			Thread.sleep(20);
		}

		@Override
		public void disconnected() {
			enableUi(false);
		}
	}

	@Override
	protected IOIOLooper createIOIOLooper() {
		return new Looper();
	}

	private void enableUi(final boolean enable) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				ledToggleButton_.setEnabled(enable);
				toggleButtonRight_.setEnabled(enable);
				toggleButtonBack_.setEnabled(enable);
				toggleButtonLeft_.setEnabled(enable);
				toggleButtonForward_.setEnabled(enable);
			}
		});
	}

	/**
	 * Handle incoming calls. TODO: Implement an accept/reject functionality.
	 * @param userId
	 */
	private void dispatchIncomingCall(String userId){
		Intent intent = new Intent(QBotActivity.this, VideoChatActivity.class);
		intent.putExtra(Constants.USER_NAME, username);
		intent.putExtra(Constants.CALL_USER, userId);
		startActivity(intent);
	}

	private void setUserStatus(String status){
		try {
			JSONObject state = new JSONObject();
			state.put(Constants.JSON_STATUS, status);
			this.mPubNub.setState(this.stdByChannel, this.username, state, new Callback() {
				@Override
				public void successCallback(String channel, Object message) {
					Log.d("MA-sUS","State Set: " + message.toString());
				}
			});
		} catch (JSONException e){
			e.printStackTrace();
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		if(this.mPubNub!=null){
			this.mPubNub.unsubscribeAll();
		}
	}
	protected void onDestroy(){
		super.onDestroy();
		if(this.mPubNub!=null){
			this.mPubNub.unsubscribeAll();
		}
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
	public void sendNotification(View view) {
		PnGcmMessage gcmMessage = new PnGcmMessage();
		JSONObject jso = new JSONObject();
		try {
			jso.put("GCMSays", "hi");
		} catch (JSONException e) { }
		gcmMessage.setData(jso);
		PnMessage message = new PnMessage(
				this.mPubNub,
				"GCMPush",
				gcmsendcallback,
				gcmMessage);
		try {
			message.publish();
		} catch (PubnubException e) {
			e.printStackTrace();
		}
	}
	public static Callback gcmsendcallback = new Callback() {
		@Override
		public void successCallback(String channel, Object message) {
			Log.i(TAG, "Success on Channel GCMPush : " + message);
		}
		@Override
		public void errorCallback(String channel, PubnubError error) {
			Log.i(TAG, "Error On Channel GCMPush : " + error);
		}
	};
}