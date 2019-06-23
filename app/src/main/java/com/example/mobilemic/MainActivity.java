package com.example.mobilemic;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.annotation.NonNull;

import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mobilemic.rtp.RtpPacket;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;

import javax.media.MediaLocator;

public class MainActivity extends AppCompatActivity {
    private String serverIP = "";
    private int port = -1;
    private AudioRecord recorder;
    private Client client = new Client();
    private RTPClient rtpClient = null;
    private static final String LOG_TAG = "AudioRecordTest";
    private boolean connected = false;
    private MicRecordThread micRecordThread;
    private final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private final int REQUEST_INTERNET_PERMISSION = 404;
    private final int REQUEST_NETWORK_ACCESS_PERMISSION = 520;
    private String [] recordPermissions = {Manifest.permission.RECORD_AUDIO};
    private String [] internetPermissions = {Manifest.permission.INTERNET};
    private String [] networkPermissions = {Manifest.permission.ACCESS_NETWORK_STATE};
    private boolean recording = true;

    private boolean permissionToRecordAccepted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.testlayout);
        System.out.println(this.getApplicationContext().getFilesDir().getAbsolutePath());


        int hertz = 44100;
        int channels = 2;
        int bufferSize = 8096;

        ActivityCompat.requestPermissions(this, internetPermissions, REQUEST_INTERNET_PERMISSION);
       // ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
        MediaRecorder mediaRec = new MediaRecorder();
        try {
            mediaRec.setAudioSource(MediaRecorder.AudioSource.MIC);

            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    AudioRecord.getMinBufferSize(hertz, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT));
        } catch (Exception e)
        {
            e.printStackTrace();
            errorClose(e, "Error starting Mobile Mic");

            //create a toast
        }
    }

    public void errorClose(Exception e, String message)
    {
        Toast toaster  = Toast.makeText(this.getApplicationContext(), "Error starting application.\n Please contact support.\n error message:" + message, Toast.LENGTH_LONG);
        toaster.show();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        } finally {
            this.finish();
        }

    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        System.out.println("requesting permission results");
        switch (requestCode){
            case REQUEST_INTERNET_PERMISSION:
                permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                ActivityCompat.requestPermissions(this, networkPermissions, REQUEST_NETWORK_ACCESS_PERMISSION);

                System.out.println("permission granted");
            break;
            case REQUEST_NETWORK_ACCESS_PERMISSION:
                ActivityCompat.requestPermissions(this, recordPermissions, REQUEST_RECORD_AUDIO_PERMISSION);
                System.out.println("Permission to network access granted.");
            break;
            case REQUEST_RECORD_AUDIO_PERMISSION:

                System.out.println("Permission to Record Audio granted.");
            break;
        }
        if (!permissionToRecordAccepted ) finish();

    }

    /**
     * Connects the client to the server.
     */

    public void connectToServer(View view)
    {
        TextView errorTextView = (TextView) findViewById(R.id.connectServerErrorTextView);
        String audioFile = "file://sdcard/sup.wav";
        if (!connected) {

            TextInputEditText serverIPTextInput = (TextInputEditText) findViewById(R.id.serverIPTextInput);
            TextInputEditText portTextInput = (TextInputEditText) findViewById(R.id.portTextInput).findViewById(R.id.portTextInputEditText);
            this.serverIP = serverIPTextInput.getText().toString();
            this.port = Integer.parseInt(portTextInput.getText().toString());

            errorTextView.setText("Connected to server.");
            errorTextView.setVisibility(View.VISIBLE);
            client.setHostName(this.serverIP);
            client.setPortNumber(this.port);
            ClientThread clientThread = new ClientThread(client);
            ConnectToServerTask connectToServer = new ConnectToServerTask();
            VerifyUIDTask verifyUIDTask = new VerifyUIDTask("98231");
            connectToServer.execute(client);
            verifyUIDTask.execute(client);
            connected = true;

            try {

                micRecordThread = new MicRecordThread(this.recorder, client, rtpClient);



            } catch (Exception e) {
                errorTextView.setText("Error connecting to server " + e.getMessage());
                e.printStackTrace();
            }
        } else if (connected)
        {
            //what if the user is also connected to the udp server? we need to end the udp as well
            if(client != null)
            {
                micRecordThread.setRecording(false);
                DisconnectFromServerTask disconnectFromServerTask = new DisconnectFromServerTask();
                disconnectFromServerTask.execute(client);
                connected = false;
                errorTextView.setText("Disconnected from server");
            }
        }
    }


    public void streamAudio(View view)
    {
        if(connected && !micRecordThread.isRecording()) {
            micRecordThread.setRecording(true);
            micRecordThread.start();
        } else
        {
            micRecordThread.setRecording(false);
        }

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
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    static class MicRecordThread extends Thread
    {
        private AudioRecord audioRec;
        private Client client;
        private boolean recording = false;
        private int bufSize = 256;
        private final Object bufSizeLock = new Object();
        private Object recordingLock = new Object();
        private int packetsSent = 0;
        private RTPClient rtpClient;
        MicRecordThread(AudioRecord audioRecord, Client client, RTPClient rtpClient)
        {
            this.audioRec = audioRecord;
            this.client = client;
            this.rtpClient = rtpClient;
            this.setBufSize(AudioRecord.getMinBufferSize(audioRec.getSampleRate(), audioRec.getChannelCount(), audioRec.getAudioFormat()));
        }

        public void run()
        {
            audioRec.startRecording();
            int payloadType = 17; // dvi4
            int seqNum = 0;
            try {
                client.connectToMultiSocket();
                int bytesRead = 0;
                while(this.isRecording())
                {
                    int bufSize = this.getBufSize();

                    byte[] audioData = new byte[bufSize];
                    bytesRead = audioRec.read(audioData, 0, bufSize);
                    RtpPacket rtpPacket = new RtpPacket(payloadType, seqNum++, (int) System.currentTimeMillis(), audioData);
                    try {
                        client.sendRTP(rtpPacket.getPacket());
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
                this.client.closeUDPSocket();

            } catch (IOException e)
            {
                e.printStackTrace();
            }


        }


        /**
         * Returns whether or not the mic record thread is streaming to the server.
         * @return Returns true if it is streaming to the UDP socket, false otherwise.
         */
        public synchronized boolean isRecording()
        {
            synchronized(recordingLock)
            {
                return recording;
            }

        }

        /**
         * Enables or disables microphone streaming to the UDP socket.
         * @param recording
         */

        public synchronized void setRecording(boolean recording)
        {
            synchronized (recordingLock) {
                this.recording = recording;
            }
        }

        public int getBufSize()
        {
            synchronized(bufSizeLock)
            {
                return this.bufSize;
            }
        }

        public void setBufSize(int bufSize)
        {
            synchronized (bufSizeLock)
            {
                this.bufSize = bufSize;
            }
        }


    }
}

