package nanqineveryday.qbot.ioio;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import nanqineveryday.qbot.ioio.util.Constants;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.content.SharedPreferences;

import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;
import org.json.JSONException;
import org.json.JSONObject;

public class QBotActivity extends IOIOActivity {
	private ToggleButton ledToggleButton_, toggleButtonForward_, toggleButtonBack_, toggleButtonLeft_, toggleButtonRight_;
	//private SharedPreferences mSharedPreferences;
	private String username;
	private String stdByChannel;
	private Pubnub mPubNub;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		ledToggleButton_ = (ToggleButton) findViewById(R.id.ledToggleButton);
		toggleButtonBack_= (ToggleButton) findViewById(R.id.toggleButtonBack);
		toggleButtonForward_= (ToggleButton) findViewById(R.id.toggleButtonForward);
		toggleButtonLeft_= (ToggleButton) findViewById(R.id.toggleButtonLeft);
		toggleButtonRight_= (ToggleButton) findViewById(R.id.toggleButtonRight);

		enableUi(false);

		//this.mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFS, MODE_PRIVATE);
		this.username     = Constants.USER_NAME;
		this.stdByChannel = this.username + Constants.STDBY_SUFFIX;

		initPubNub();
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
						if (!jsonMsg.has(Constants.JSON_CALL_USER)) return;     //Ignore Signaling messages.
						String user = jsonMsg.getString(Constants.JSON_CALL_USER);
						dispatchIncomingCall(user);
					} catch (JSONException e){
						e.printStackTrace();
					}
				}

				@Override
				public void connectCallback(String channel, Object message) {
					Log.d("MA-iPN", "CONNECTED: " + message.toString());
					setUserStatus(Constants.STATUS_AVAILABLE);
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

	@Override
	protected void onRestart() {
		super.onRestart();
		if(this.mPubNub==null){
			initPubNub();
		} else {
			subscribeStdBy();
		}
	}
}