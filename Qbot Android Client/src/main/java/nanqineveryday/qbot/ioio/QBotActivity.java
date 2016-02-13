package nanqineveryday.qbot.ioio;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

public class QBotActivity extends IOIOActivity {
	private ToggleButton ledToggleButton_, toggleButtonForward_, toggleButtonBack_, toggleButtonLeft_, toggleButtonRight_;

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
}