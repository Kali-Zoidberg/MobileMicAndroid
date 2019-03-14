package com.example.mobilemic;

import android.os.AsyncTask;

import java.io.IOException;

public class ClientTasks {
}

class ConnectToServerTask extends AsyncTask<Client, Boolean, Integer>
{
    protected Integer doInBackground(Client... clients)
    {
        int len = clients.length;
        int count = 0;
        for (int i = 0; i < len; ++i)
        {
            count = i;
            try {
                clients[i].connectToServer();
                publishProgress(new Boolean(true));
            } catch (IOException e)
            {
                e.printStackTrace();
                break;
            }
        }
        return new Integer(count);
    }

    protected void onPostExecution(Integer Integer)
    {
        System.out.println("Connected to client. Make sure to update UI");
    }
}


class VerifyUIDTask extends AsyncTask<Client, Boolean, Integer> {
    private String uid = "";
    private Object uidLock = new Object();

    public synchronized void setUID(String uid) {
        synchronized (uidLock) {
            this.uid = uid;
        }
    }

    public synchronized String getUID()
    {
        synchronized(uidLock)
        {
            return uid;
        }

    }

    protected Integer doInBackground(Client... clients)
    {
        int len = clients.length;
        int count = 0;
        for (int i = 0; i < len; ++i)
        {
            count = i;
            clients[i].sendDataToServer("UID." + this.getUID());
            if (!clients[i].readMessageFromServer().equals("VALID_UID"))
            {
                publishProgress(new Boolean(false));
                count = 0;
                break;
            }
            publishProgress(new Boolean(true));
        }
        return new Integer(count);
    }

    protected void onPostExecution(Integer Integer)
    {
        System.out.println("valid uid");
    }
}