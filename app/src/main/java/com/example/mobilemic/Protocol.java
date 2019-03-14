package com.example.mobilemic;

public class Protocol extends Thread{
    private Client client;
    Protocol(Client client)
    {
        this.client = client;
    }

    public Client getClient()
    {
        return client;
    }
    public void run() {
        try {
            client.connectToServer();
        } catch (java.io.IOException e)
        {

        }
    }

    public void couldNotConnectHandle(Exception e)
    {
        e.printStackTrace();
        //handle error handling by updating ui

        System.out.println("Could not connect to host.");
    }
}
