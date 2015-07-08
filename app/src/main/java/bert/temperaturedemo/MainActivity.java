package bert.temperaturedemo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.net.InetAddress;
import java.net.UnknownHostException;

import communicator.BertCommunicator;
import communicator.bertMessages.BertMessage;
import communicator.bertMessages.MessageHandlerThread;
import communicator.bertMessages.BertMessageObserver;
import communicator.bertMessages.variants.FT_Message;
import communicator.commands.CommandSequences;

public class MainActivity extends ActionBarActivity implements BertMessageObserver {

    public static Handler TEMP_UPDATE_HANDLER;

    public static BertCommunicator bertCommunicator = new BertCommunicator();

    private EditText ipEditText;
    private Button connectButton;
    private TextView temperatureDisplay;
    private TextView timeDisplay;

    private InetAddress connectedDevice;

    private int updateCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ipEditText = (EditText) findViewById(R.id.IP_FIELD);
        connectButton = (Button) findViewById(R.id.BUTTON_CONNECT);
        temperatureDisplay = (TextView) findViewById(R.id.tempDisplay);
        timeDisplay = (TextView) findViewById(R.id.timeDisplay);

        connectButton.setOnClickListener(new View.OnClickListener() {//TODO connect button currently sends a temp request
            @Override
            public void onClick(View v) {
                try {
                    connectedDevice = InetAddress.getByName(ipEditText.getText().toString());
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    Log.e("MainActiviy", "Invalid IP Entered");
                }
            }
        });

        TEMP_UPDATE_HANDLER = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Bundle data = msg.getData();
                temperatureDisplay.setText(String.valueOf(data.getInt("Temp")));
                updateCounter++;
                timeDisplay.setText(String.valueOf(updateCounter));
            }
        };

        FT_Message.addObserver(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle dialogue bar item clicks here. The dialogue bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBertMessage(BertMessage message) {
        if (message.getOpCode().equals("FT")) {
            Bundle bundle = new Bundle();

            FT_Message ftm = (FT_Message) message;
            bundle.putInt("Temp", ftm.currentTemp);
            bundle.putInt("HighT", ftm.highThreshold);
            bundle.putInt("LowT", ftm.lowThreshold);
            bundle.putBoolean("onAtHigh", ftm.onAtHigh);
            bundle.putBoolean("onAtLow", ftm.onAtLow);
            bundle.putBoolean("isHeatingDevice", ftm.isHeatingDevice);

            Message msg = new Message();
            msg.setData(bundle);
            TEMP_UPDATE_HANDLER.sendMessage(msg);
        } else if (message.getOpCode().equals("HB")) {
            if (connectedDevice != null) {
                MessageHandlerThread.addCommandToQueue(connectedDevice, CommandSequences.getTemperature());
            }
        }
    }
}
