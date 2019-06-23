package com.example.mobilemic;

import android.net.rtp.AudioCodec;
import android.net.rtp.AudioGroup;
import android.net.rtp.AudioStream;
import android.os.Build;

import com.sun.media.rtp.RTPSessionMgr;

import javax.media.*;
import javax.media.datasink.DataSinkListener;
import javax.media.protocol.*;
import javax.media.protocol.DataSource;
import javax.media.format.*;
import javax.media.control.TrackControl;
import javax.media.control.QualityControl;
import javax.media.rtp.RTPManager;
import javax.media.rtp.RTPPushDataSource;

import java.io.*;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class RTPClient {

    private String hostname;
    private int port;
    private AudioStream audioStream;
    private AudioGroup audioGroup;
    public RTPClient(String hostname, int port)
    {
        this.hostname = hostname;
        this.port = port;

    }

    public AudioGroup initAudioGroup()
    {
        audioGroup = new AudioGroup();
        audioGroup.setMode(AudioGroup.MODE_NORMAL);

        return audioGroup;
    }

    public AudioCodec initAudioCodec()
    {
        return AudioCodec.AMR;
    }

    public void connect() throws UnknownHostException, SocketException {
        InetAddress remoteAddress = InetAddress.getByName(hostname);
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.out.println("Associating rtp with " + remoteAddress.getHostAddress() + ":" + this.port);

        audioStream = new AudioStream(InetAddress.getLocalHost());
        audioGroup = initAudioGroup();
        audioStream.setMode(AudioStream.MODE_SEND_ONLY);
        audioStream.setCodec(initAudioCodec());
        audioStream.associate(remoteAddress, this.port);

        audioStream.join(audioGroup);

    }

}