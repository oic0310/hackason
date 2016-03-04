package com.oec.android.myo.session;

import android.app.AlertDialog;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.neurosky.thinkgear.TGDevice;
import com.neurosky.thinkgear.TGRawMulti;

import com.oec.android.myo.thankyou.R;
import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.Arm;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.XDirection;
import com.thalmic.myo.scanner.ScanActivity;

import com.uxxu.konashi.lib.Konashi;
import com.uxxu.konashi.lib.KonashiManager;
import com.uxxu.konashi.lib.KonashiListener;
import com.uxxu.konashi.lib.util.KonashiUtils;

import android.support.v7.app.AppCompatActivity;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import info.izumin.android.bletia.BletiaException;
import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.android.volley.Request;

public class SIArmAndMindActivity extends AppCompatActivity implements TextToSpeech.OnInitListener{
    private TextToSpeech tts;
    final private String MYO_URL = "http://sessionimpossible.au-syd.mybluemix.net/myo";
    final private String MIND_URL = "http://sessionimpossible.au-syd.mybluemix.net/mind";

    private KonashiManager mKonashiManager;
    public final SIArmAndMindActivity self = this;
    private TextView mLockStateView;
    private TextView mTextView;
    private TextView message;
    final boolean rawEnabled = false;
    TGDevice tgDevice;
    BluetoothAdapter bluetoothAdapter;
    String IMEI;

    RequestQueue requestQueue;

    @Override
    protected void onPause() {
        mKonashiManager.removeListener(mKonashiListener);
        super.onPause();
    }

    @Override
    public void onInit(int status) {
        if (TextToSpeech.SUCCESS == status) {
            Locale locale = Locale.ENGLISH;
            if (tts.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE) {
                tts.setLanguage(locale);
            } else {
                Log.d("", "Error SetLocale");
            }
        } else {
            Log.d("", "Error Init");
        }
    }

    // Classes that inherit from AbstractDeviceListener can be used to receive events from Myo devices.
    // If you do not override an event, the default behavior is to do nothing.
    private DeviceListener mListener = new AbstractDeviceListener() {

        // onConnect() is called whenever a Myo has been connected.
        @Override
        public void onConnect(Myo myo, long timestamp) {
            // Set the text color of the text view to cyan when a Myo connects.
            mTextView.setTextColor(Color.CYAN);
        }

        // onDisconnect() is called whenever a Myo has been disconnected.
        @Override
        public void onDisconnect(Myo myo, long timestamp) {
            // Set the text color of the text view to red when a Myo disconnects.
            mTextView.setTextColor(Color.RED);
        }

        // onArmSync() is called whenever Myo has recognized a Sync Gesture after someone has put it on their
        // arm. This lets Myo know which arm it's on and which way it's facing.
        @Override
        public void onArmSync(Myo myo, long timestamp, Arm arm, XDirection xDirection) {
            mTextView.setText(myo.getArm() == Arm.LEFT ? R.string.arm_left : R.string.arm_right);
        }

        // onArmUnsync() is called whenever Myo has detected that it was moved from a stable position on a person's arm after
        // it recognized the arm. Typically this happens when someone takes Myo off of their arm, but it can also happen
        // when Myo is moved around on the arm.
        @Override
        public void onArmUnsync(Myo myo, long timestamp) {
            mTextView.setText(R.string.bar);
        }

        // onUnlock() is called whenever a synced Myo has been unlocked. Under the standard locking
        // policy, that means poses will now be delivered to the listener.
        @Override
        public void onUnlock(Myo myo, long timestamp) {
            mLockStateView.setText(R.string.unlocked);
        }

        // onLock() is called whenever a synced Myo has been locked. Under the standard locking
        // policy, that means poses will no longer be delivered to the listener.
        @Override
        public void onLock(Myo myo, long timestamp) {
            mLockStateView.setText(R.string.locked);
        }

        private boolean yoo = true;
        // onOrientationData() is called whenever a Myo provides its current orientation,
        // represented as a quaternion.
        @Override
        public void onOrientationData(Myo myo, long timestamp, Quaternion rotation) {
            // Calculate Euler angles (roll, pitch, and yaw) from the quaternion.
            float roll = (float) Math.toDegrees(Quaternion.roll(rotation));
            float pitch = (float) Math.toDegrees(Quaternion.pitch(rotation));
            float yaw = (float) Math.toDegrees(Quaternion.yaw(rotation));


            // Adjust roll and pitch for the orientation of the Myo on the arm.
            if (myo.getXDirection() == XDirection.TOWARD_ELBOW) {
                roll *= -1;
                pitch *= -1;
            }


            // Next, we apply a rotation to the text view using the roll, pitch, and yaw.
            JSONObject jsonRequest = new JSONObject();
            try {
                jsonRequest.put("imei", IMEI);
                jsonRequest.put("roll", roll);
                jsonRequest.put("pitch", pitch);
                jsonRequest.put("yaw", yaw);
            } catch (JSONException e) {
                Log.e(MIND_TAG, e.getMessage());
            }
            sendRequet(jsonRequest,MYO_URL);

            mTextView.setRotation(roll);
            mTextView.setRotationX(pitch);
            mTextView.setRotationY(yaw);
        }
        // 読み上げのスピード
        private void setSpeechRate(float rate){
            if (null != tts) {
                tts.setSpeechRate(rate);
            }
        }

        // 読み上げのピッチ
        private void setSpeechPitch(float pitch){
            if (null != tts) {
                tts.setPitch(pitch);
            }
        }
        // onPose() is called whenever a Myo provides a new pose.
        @Override
        public void onPose(Myo myo, long timestamp, Pose pose) {

            // Handle the cases of the Pose enumeration, and change the text of the text view
            // based on the pose we receive.
            switch (pose) {
                case UNKNOWN:
                    mTextView.setText(getString(R.string.bar));
                    break;
                case REST:

                case DOUBLE_TAP:
                    int restTextId = R.string.bar;
                    setSpeechRate(0.8f);
                    setSpeechPitch(0.4f);
                    message.setText("I'm perfect human");
                    tts.speak("I'm perfect human", TextToSpeech.QUEUE_FLUSH, null);
                    switch (myo.getArm()) {
                        case LEFT:
                            restTextId = R.string.arm_left;
                            break;
                        case RIGHT:
                            restTextId = R.string.arm_right;
                            break;
                    }
                    mTextView.setText(getString(restTextId));
                    break;
                case FIST:
                    mTextView.setText(getString(R.string.pose_fist));

                    break;
                case WAVE_IN:

                    mTextView.setText(getString(R.string.pose_wavein));
                    break;
                case WAVE_OUT:
                    mTextView.setText(getString(R.string.pose_waveout));
                    break;
                case FINGERS_SPREAD:
                    mTextView.setText(getString(R.string.pose_fingersspread));
                    break;
            }

            if (pose != Pose.UNKNOWN && pose != Pose.REST) {
                // Tell the Myo to stay unlocked until told otherwise. We do that here so you can
                // hold the poses without the Myo becoming locked.
                myo.unlock(Myo.UnlockType.HOLD);

                // Notify the Myo that the pose has resulted in an action, in this case changing
                // the text on the screen. The Myo will vibrate.
                myo.notifyUserAction();
            } else {
                // Tell the Myo to stay unlocked only for a short period. This allows the Myo to
                // stay unlocked while poses are being performed, but lock after inactivity.
                myo.unlock(Myo.UnlockType.TIMED);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thankyou);
        requestQueue = Volley.newRequestQueue(this);
        mLockStateView = (TextView) findViewById(R.id.lock_state);
        mTextView = (TextView) findViewById(R.id.text);
        message = (TextView) findViewById(R.id.message);
        tts = new TextToSpeech(this, this);
        mKonashiManager = new KonashiManager(getApplicationContext());

        // First, we initialize the Hub singleton with an application identifier.
        Hub hub = Hub.getInstance();
        if (!hub.init(this, getPackageName())) {
            // We can't do anything with the Myo device if the Hub can't be initialized, so exit.
            Toast.makeText(this, "Couldn't initialize Hub", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null) {
            // Alert user that Bluetooth is not available
            Toast.makeText(this, "Bluetooth not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }else {
        	/* create the TGDevice */
            tgDevice = new TGDevice(bluetoothAdapter, handler);
        }
        // Next, register for DeviceListener callbacks.
        hub.addListener(mListener);
        mKonashiManager.addListener(mKonashiListener);

        IMEI = getIMEI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // We don't want any callbacks when the Activity is gone, so unregister the listener.
        Hub.getInstance().removeListener(mListener);

        if (isFinishing()) {
            // The Activity is finishing, so shutdown the Hub. This will disconnect from the Myo.
            Hub.getInstance().shutdown();
        }
        if (mKonashiManager != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (mKonashiManager.isConnected()) {
                        mKonashiManager.reset();
                        mKonashiManager.disconnect();
                        mKonashiManager = null;
                    }
                }
            }).start();
        }
        if (null != tts) {
            // TextToSpeechのリソースを解放する
            tts.shutdown();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        mKonashiManager.addListener(mKonashiListener);

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (R.id.action_scan == id) {
            onScanActionSelected();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    final String MIND_TAG = "#MindWave";
    HashMap<Integer,String> tgDataMap = new HashMap<Integer,String>();
    private static final Map<Integer,String> tgConstMap;
    static {
        HashMap<Integer,String> map = new HashMap<Integer,String>();
        map.put(2, "poorSignal");
        map.put(3, "heartRate");
        map.put(4, "attention");
        map.put(5, "meditation");
        map.put(22, "blink");
        map.put(131, "eegPower");
        map.put(19, "rawCount");

        tgConstMap = Collections.unmodifiableMap(map);
    }

    /**
     * Handles messages from TGDevice
     */
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == TGDevice.MSG_STATE_CHANGE) {
                if(msg.arg1 == TGDevice.STATE_CONNECTED){
                    tgDevice.start();
                }
            }else{
                if(!tgDataMap.containsKey(new Integer(msg.what))) {
                    tgDataMap.put(new Integer(msg.what), new Integer(msg.arg1).toString());
                }else{
                    JSONObject jsonRequest = new JSONObject();
                    try {
                        jsonRequest.put("imei", IMEI);
                        for (Integer itemId : tgDataMap.keySet()) {
                            Log.d(MIND_TAG, "tgDataMap.values(): " + tgConstMap.get(itemId) + " itemId:" + itemId);
                            jsonRequest.put(tgConstMap.get(itemId), tgDataMap.get(itemId));
                        }
                    } catch (JSONException e) {
                        Log.e(MIND_TAG, e.getMessage());
                    }
                    sendRequet(jsonRequest,MIND_URL);
                    tgDataMap.clear();
                }
            }
        }
    };
    private void onScanActionSelected() {
        // Launch the ScanActivity to scan for Myos to connect to.
        Intent intent = new Intent(this, ScanActivity.class);
        startActivity(intent);
        if(tgDevice.getState() != TGDevice.STATE_CONNECTING && tgDevice.getState() != TGDevice.STATE_CONNECTED)
            tgDevice.connect(rawEnabled);
        //tgDevice.ena
    }

    private final KonashiListener mKonashiListener = new KonashiListener() {

        @Override
        public void onConnect(KonashiManager manager) {
            KonashiUtils.log("onReady");
            Log.d("onReady>>>", manager.toString());
            mKonashiManager.pinMode(Konashi.PIO1, Konashi.INPUT);
            mKonashiManager.digitalWrite(Konashi.LED2, Konashi.HIGH);
        }



        @Override
        public void onDisconnect(KonashiManager manager) {
            KonashiUtils.log("onDisconnected");
            Log.d("onDisconnect>>>", manager.toString());

        }


        public void onError(KonashiManager manager, final BletiaException e) {
            new AlertDialog.Builder(SIArmAndMindActivity.this)
                    .setTitle("Error")
                    .setMessage(e.getMessage())
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .show();
        }

        @Override
        public void onUpdatePioOutput(KonashiManager manager, int value) {

        }

        @Override
        public void onUpdateUartRx(KonashiManager manager, byte[] value) {
        }

        @Override
        public void onUpdateBatteryLevel(KonashiManager manager, int level) {

        }

        @Override
        public void onUpdateSpiMiso(KonashiManager manager, byte[] value) {

        }
    };
    private byte[] mValue;

    final String JSON_TAG = "#JSON";

    private void sendRequet(JSONObject jsonRequest,String url){
        // サーバからのレスポンス時の挙動を定義
        Log.e(JSON_TAG, jsonRequest.toString());
        Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.e(JSON_TAG, "onResponse ok =" + response);
            }
        };
        // エラー時の挙動を定義
        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(JSON_TAG, "onResponse error=" + error);
            }
        };

        // サーバへデータ送信
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,url, jsonRequest, listener, errorListener);
        requestQueue.add(jsonObjectRequest);
    }



    ///端末のIDを取得
    private String getIMEI(){
        TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        // IMEI
        return tm.getDeviceId();
    }
}
