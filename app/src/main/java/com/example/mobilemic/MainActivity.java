package com.example.mobilemic;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;

import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mobilemic.Network.Protocol;
import com.example.mobilemic.rtp.RtpPacket;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Random;

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
        ActivityCompat.requestPermissions(this, internetPermissions, REQUEST_INTERNET_PERMISSION);
//        ActivityCompat.requestPermissions(this, recordPermissions, REQUEST_RECORD_AUDIO_PERMISSION);
        setContentView(R.layout.testlayout);

        
        System.out.println(this.getApplicationContext().getFilesDir().getAbsolutePath());


        int hertz = 44100;
        int channels = 2;
        int bufferSize = 512;

        /**
         * TODO
         *
         *
         * Note very big
         * Need to set the requestPermissions to an onAccessGranted Event Invokation.
         * This is because the app will minimize if no permission granted before setting content view and stuff.
         */

        MediaRecorder mediaRec = new MediaRecorder();
        try {
            mediaRec.setAudioSource(MediaRecorder.AudioSource.MIC);

            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT,
                    AudioRecord.getMinBufferSize(hertz, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT));
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

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
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

    private void parseClumpSize(EditText clumpSizeView)
    {
        int clumpSize = 16;
        String clumpMessage = "S.clumpSize";
        if (clumpSizeView != null)
        {
            String clumpStr = clumpSizeView.getText().toString();
            //If user did not input a clump size, set it to 64.
            if (clumpStr != null)
                clumpSize = Integer.parseInt(clumpStr);

            System.out.println("User's clump size: " + clumpSize);
            if (clumpSize < 4) {
                System.out.println("Invalid clump Size.");
            }

        }

        this.client.setClumpSize(clumpSize);

        if (connected)
        {
            SendMessageTask sendClumpSize = new SendMessageTask(clumpMessage + " " + clumpSize);
            sendClumpSize.execute(client);
        }
    }

    public void setClumpSize(View view)
    {
        EditText clumpSizeText = findViewById(R.id.clumpView);

        parseClumpSize(clumpSizeText);

    }

    /**
     * Connects the client to the server.
     */

    public void connectToServer(View view)
    {
        TextView errorTextView = (TextView) findViewById(R.id.connectServerErrorTextView);
        String audioFile = "file://sdcard/sup.wav";
        if (!connected) {

            TextInputEditText serverIPTextInput =  findViewById(R.id.serverIPTextInput);
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

                micRecordThread = new MicRecordThread(this.recorder, client, this.getApplicationContext());

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
        private int bufSize = 812;
        private final Object bufSizeLock = new Object();
        private Context mainContext;
        private Object recordingLock = new Object();

        MicRecordThread(AudioRecord audioRecord, Client client, Context context)
        {
            this.audioRec = audioRecord;
            this.client = client;
            this.mainContext = context;

        }

        public void run()
        {
            ByteArrayOutputStream byteOutpuStream = new ByteArrayOutputStream();

            try {
                //Start udp connection.
                client.connectToUDPServer();
                if (client.isConnectedToUDP())
                    System.out.println("Client has connected to UDP...");

                else
                    System.out.println("Client was unable to connect to the UDP port.");
            } catch (IOException e) {
                e.printStackTrace();
            }


            audioRec.startRecording();

            Random rand = new Random(System.currentTimeMillis());
            int payloadType = 17; // dvi4 requied as specified by rtp format.
            int seqNum = 0;
            int clumpSize = this.client.getClumpSize();
            long numBytes = 0;
            long numBytesRead = 0;
            long rtpBytesRead = 0;
            long numOrderBytesBefore = 0;
            long numOrderBytesAfter = 0;
            long numBytesArray =0 ;

            RtpPacket[] tempStorage = new RtpPacket[clumpSize];
            byte[][] reorderPayloads = new byte[clumpSize][];
            byte[][] unorderPayloads = new byte[clumpSize][];
            byte[][] tempAudio = new byte[clumpSize][];
            try {
                int bytesRead = 0;
                OutputStreamWriter audioFile = new OutputStreamWriter(mainContext.openFileOutput("micAudio.txt", Context.MODE_PRIVATE));
                int curIter = 0;
                while(this.isRecording())
                {
                    int bufSize = this.getBufSize();

                    byte[] audioData = new byte[bufSize];

                    //Write here
                    bytesRead = audioRec.read(audioData, 0, bufSize);
                    numBytesRead +=bytesRead;
                    numBytes += bytesRead;
//                    System.out.println("PRocessing: " + curIter++);
//                    System.out.println("Audio num bytes: " + numBytes);
                    audioFile.write(this.bytesToString(audioData));
                    //Write here
                    RtpPacket rtpPacket = new RtpPacket(payloadType, seqNum, (int) System.currentTimeMillis(), audioData);
                    rtpBytesRead += rtpPacket.getPayloadLength();
//                    System.out.println("rtpBytes read: " + rtpBytesRead);
                    //Transmit RTP packets to server.
                    if (seqNum != 0 && (seqNum % clumpSize == 0)) {

                        //Write here
                        //Reorder bytes
                        numOrderBytesBefore += reorderPayloads.length;
                        reorderPayloads = Protocol.order(reorderPayloads, clumpSize);

                        numOrderBytesAfter += reorderPayloads.length;
                        //Transmit
                        for (int i = 0; i < clumpSize; ++i) {
                            //Write here -- tempStorage
                            tempStorage[i].setPayload(reorderPayloads[i]);
                            this.client.sendBytesToUDP(tempStorage[i].getPacket());
                            //Write here -- tempStorage
                        }
//                        unorderPayloads = Protocol.reorder(tempStorage, clumpSize);
//                        for (int i = 0; i < tempAudio.length; ++i){
//                            for (int j = 0; j < tempAudio[i].length; ++j) {
//                                if (tempAudio[i][j] != unorderPayloads[i][j]) {
//                                    System.out.println("alignment error at: (" + i + "," + j+")");
//                                    System.out.println(tempAudio[i][j] + " != " + unorderPayloads[i][j]);
//                                }
//                            }
//                        }
                    }
                    tempAudio[seqNum % clumpSize] = audioData;
                    //Write here
                    //Store rtppacket bytes in a temporary array
                    tempStorage[seqNum % clumpSize] = rtpPacket;
                    reorderPayloads[seqNum % clumpSize] = rtpPacket.getPayload();
                    numBytesArray += rtpPacket.getPayload().length;
//                    System.out.println("num Bytes Array: "  + numBytesArray);


                    //Increment seqNum
                    ++seqNum;
//                    System.out.println("numBytes: " + numBytes);
                }

                //Finished recording, close UDP socket.
                this.client.closeUDPSocket();

            } catch (Exception e)
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
         * Creates a string from an array of bytes separated by a space
         * @param someBytes
         * @return
         */
        private String bytesToString(byte[] someBytes) {
            String retStr = "";

            for (int i = 0; i < someBytes.length; ++i) {
                retStr += " " + someBytes[i];
            }

            return retStr;
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

