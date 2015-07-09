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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.net.InetAddress;
import java.net.UnknownHostException;

import communicator.BertCommunicator;
import communicator.bertMessages.BertMessage;
import communicator.bertMessages.MessageHandlerThread;
import communicator.bertMessages.BertMessageObserver;
import communicator.bertMessages.variants.FT_Message;
import communicator.commands.Command;
import communicator.commands.CommandSequences;
import communicator.utility.exceptions.InvalidCommandParameterException;

public class MainActivity extends ActionBarActivity implements BertMessageObserver {

    public static Handler TEMP_UPDATE_HANDLER;

    static {
        BertCommunicator.start();
    }

    private EditText ipEditText;

    private EditText setTempField;
    private EditText highTempField;
    private EditText lowTempField;

    private Button connectButton;
    private Button calibrateTemperatureButton;
    private RadioButton enableHighRadioButton;
    private RadioButton disableHighRadioButton;
    private RadioButton enableLowRadioButton;
    private RadioButton disableLowRadioButton;


    private TextView temperatureDisplay;
    private TextView updateDisplay;

    private CheckBox isHeatingDeviceCheckBox;

    private InetAddress connectedDevice;

    private int updateCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ipEditText = (EditText) findViewById(R.id.IP_FIELD);
        setTempField = (EditText) findViewById(R.id.tempSetpoint);
        highTempField = (EditText) findViewById(R.id.highTempField);
        lowTempField = (EditText) findViewById(R.id.lowTempField);

        temperatureDisplay = (TextView) findViewById(R.id.tempDisplay);
        updateDisplay = (TextView) findViewById(R.id.updateDisplay);

        isHeatingDeviceCheckBox = (CheckBox) findViewById(R.id.isHeatingDevice);

        connectButton = (Button) findViewById(R.id.BUTTON_CONNECT);
        calibrateTemperatureButton = (Button) findViewById(R.id.BUTTON_CALIBRATE);

        enableHighRadioButton = (RadioButton) findViewById(R.id.enableHigh);
        enableLowRadioButton = (RadioButton) findViewById(R.id.enableLow);

        connectButton.setOnClickListener(new View.OnClickListener() {//TODO connect button currently sends a temp request
            @Override
            public void onClick(View v) {
                try {
                    connectedDevice = InetAddress.getByName(ipEditText.getText().toString());
                    connectButton.setBackgroundColor(getResources().getColor(R.color.LightGreen));
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    Log.e("MainActiviy", "Invalid IP Entered");
                    connectedDevice = null;
                    connectButton.setBackgroundColor(getResources().getColor(R.color.LightBlue));
                }
            }
        });

        calibrateTemperatureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (connectedDevice == null) return;

                int highTemp = Integer.parseInt(highTempField.getText().toString());
                int lowTemp = Integer.parseInt(lowTempField.getText().toString());
                int setTemp = Integer.parseInt(setTempField.getText().toString());

                if (setTemp > 99 | setTemp < 32) return;
                if (highTemp > 99 | highTemp < 32) return;
                if (lowTemp < 32 | lowTemp > 99) return;

                if (highTemp <= lowTemp) {
                    highTempField.setText("");
                    lowTempField.setText("");
                    return;
                }

                boolean isHeatingDevice = isHeatingDeviceCheckBox.isChecked();
                boolean highEnable = enableHighRadioButton.isChecked();
                boolean lowEnable = enableLowRadioButton.isChecked();

                try {
                    Command c = CommandSequences.setTemperatureConfiguration(
                            setTemp,
                            highEnable,
                            highTemp,
                            lowEnable,
                            lowTemp,
                            isHeatingDevice
                    );
                    BertCommunicator.COMMAND_QUEUE_MAP.get(connectedDevice).addCommand(c);
                    Log.d("Activity", "Sent temp calibration command");
                } catch (InvalidCommandParameterException e) {
                    highTempField.setText("");
                    lowTempField.setText("");
                    setTempField.setText("");
                    return;
                }
            }
        });

        TEMP_UPDATE_HANDLER = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Bundle data = msg.getData();
                temperatureDisplay.setText(String.valueOf(data.getInt("Temp")));
                updateCounter++;
                updateDisplay.setText(String.valueOf(updateCounter));
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
        if (message.getOpCode().equals("FT") && message.deviceIP.equals(connectedDevice)) {
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
