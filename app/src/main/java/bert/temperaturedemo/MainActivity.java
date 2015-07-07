package bert.temperaturedemo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity {

    public static Handler timeUpdateHandler;

    EditText ipEditText;
    Button connectButton;
    TextView temperatureDisplay;
    TextView timeDisplay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //FIXME new BertCommunicator();

        setContentView(R.layout.activity_main);
        ipEditText = (EditText) findViewById(R.id.IP_FIELD);
        connectButton = (Button) findViewById(R.id.BUTTON_CONNECT);
        temperatureDisplay = (TextView) findViewById(R.id.tempDisplay);
        timeDisplay = (TextView) findViewById(R.id.timeDisplay);

        timeUpdateHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                timeDisplay.setText(msg.getData().getString("time"));
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
