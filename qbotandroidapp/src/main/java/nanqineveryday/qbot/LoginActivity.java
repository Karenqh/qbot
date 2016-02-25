package nanqineveryday.qbot;

        import android.app.Activity;
        import android.content.Intent;
        import android.content.SharedPreferences;
        import android.os.Bundle;
        import android.view.Menu;
        import android.view.MenuItem;
        import android.view.View;
        import android.widget.EditText;

        import nanqineveryday.qbot.util.Constants;

/**
 * Login Activity for the first time the app is opened, or when a user clicks the sign out button.
 * Saves the username in SharedPreferences.
 */
public class LoginActivity extends Activity {

    private EditText mUsername;
    private EditText mRobotname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mUsername = (EditText) findViewById(R.id.login_username);
        mRobotname = (EditText) findViewById(R.id.login_robotname);
        Bundle extras = getIntent().getExtras();
        if (extras != null){
            String lastUsername = extras.getString("oldUsername", "");
            mUsername.setText(lastUsername);
        }
    }

    @Override
    public void onBackPressed()
    {
        moveTaskToBack(true); // exit app
        stopService(new Intent(this, BgIOIOService.class));
        super.onBackPressed();
    }


    /**
     * Takes the username from the EditText, check its validity and saves it if valid.
     *   Then, redirects to the MainActivity.
     * @param view Button clicked to trigger call to joinChat
     */
    public void joinChat(View view){
        String username = mUsername.getText().toString().toLowerCase().replaceAll("\\s+","");
        if (!validUsername(username))
            return;
        String robotname = mRobotname.getText().toString().toLowerCase().replaceAll("\\s+","");
        if (!validUsername(robotname))
            return;
        SharedPreferences sp = getSharedPreferences(Constants.SHARED_PREFS, MODE_PRIVATE);
        sp.edit().putString(Constants.USER_NAME, username).apply();
        sp.edit().putString(Constants.ROBOT_NAME, robotname).apply();

        Intent intent = new Intent(this, QBotActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Optional function to specify what a username in your chat app can look like.
     * @param username The name entered by a user.
     * @return is username valid
     */
    private boolean validUsername(String username) {
        if (username.length() == 0) {
            mUsername.setError("input cannot be empty.");
            return false;
        }
        if (username.length() > 16) {
            mUsername.setError("input too long.");
            return false;
        }
        return true;
    }
}
