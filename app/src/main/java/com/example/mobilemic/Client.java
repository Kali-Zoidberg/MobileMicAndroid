package com.example.mobilemic;

import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Client {
    private Socket serverSocket = null;
    private DatagramSocket udpSocket = null;
    private int portNumber;
    private String hostName;
    private PrintWriter outputStream;
    private BufferedReader inputStream;

    private Object ouputLock = new Object();
    private Object inputLock = new Object();

    Client(String hostName, int port) {
        this.setPortNumber(port);
        this.setHostName(hostName);
    }

    public Socket getSocket() {
        return serverSocket;
    }

    /**
     * @return
     */

    public int getPortNumber() {
        return portNumber;
    }

    /**
     * Sets the port number to the specified value
     *
     * @param port The port number to connect to.
     * @return Returns false if the port number is invalid (less than 0).
     */

    public boolean setPortNumber(int port) {
        if (port < 0) {
            System.out.println("Error, port is an invalid value: " + port);
            return false;
        } else {
            portNumber = port;
            return true;
        }
    }


    /**
     * Returns the host name the client is trying to connect to.
     *
     * @return
     */

    public String getHostName() {
        return hostName;
    }


    /**
     * Sets the host name of the server that the client is to connect to.
     *
     * @param hostName
     */

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    /**
     * Receives incoming bytes from a udp server.
     *
     * @param buf The buffer to store the bytes in
     * @return Returns the packet's data
     * @throws IOException
     */

    public byte[] recieveBytesFromUDP(byte[] buf) throws IOException {
        if (udpSocket != null) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            udpSocket.receive(packet);
            return packet.getData();
        } else {
            return null;
        }
    }

    /**
     * Sends specified bytes to the udp server the client is connected to.
     *
     * @param buffer The byte array to send to the server.
     * @throws IOException You should handle this exception yourself in case the bytes cannot be accepted.
     */

    public void sendBytesToUDP(byte[] buffer) throws IOException {
        if (udpSocket != null) {
            System.out.println("sending bytes to udp socket");
            for (byte b: buffer)
            {
                System.out.print (b + " ");
            }
            System.out.println();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, this.serverSocket.getLocalAddress(), this.portNumber);
            udpSocket.send(packet);
        }
    }


    public void sendPacketToServer(DatagramPacket packet) throws IOException{
        if (udpSocket != null)
        {
            System.out.println("Sending packet to server" );
            udpSocket.send(packet);
        }
    }

    /**
     * Starts the udp socket on the server.
     *
     * @throws SocketException
     */
    public void connectToUDPServer() throws SocketException {
        this.sendDataToServer("S.udp");
        udpSocket = new DatagramSocket();
    }



    /**
     * Connects the client to the specified server
     *
     * @throws UnknownHostException
     * @throws IOException
     */

    public void connectToServer() throws UnknownHostException, IOException {
        System.out.println("creating server socket");
        serverSocket = new Socket(InetAddress.getByName(hostName), portNumber);
        System.out.println("Setingup input stream");
        this.setupInputStream();
        this.setupOutputStream();
        System.out.println("connected to server");
    }


    /**
     * Sets up the output stream so long as the client has already been connected to the server.
     *
     * @return
     */

    public PrintWriter setupOutputStream() {
        if (serverSocket == null)
            return null;
        else {
            try {
                System.out.println("output stream created");
                outputStream = new PrintWriter(serverSocket.getOutputStream(), true);
            } catch (IOException e) {
                System.out.println("Error creating output stream.");
                e.printStackTrace();

                return null;
            }
            return outputStream;
        }
    }


    /**
     * Sets up the input stream for the client.
     *
     * @return Returns null if the client is not connected or there was an error creating the input stream.
     */

    public BufferedReader setupInputStream() {
        if (serverSocket == null)
            return null;
        else {

            try {
                inputStream = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
                return inputStream;
            } catch (IOException e) {
                System.out.println("Error creating input stream");
                e.printStackTrace();
                return null;
            }


        }
    }


    /**
     * Reads a message form the server.
     *
     * @return if the server closes unexplectedly, the method will return null. Otherwise, it returns a mesage from the server.
     */

    public String readMessageFromServer() {
        try {
            return inputStream.readLine();
        } catch (IOException e) {

            e.printStackTrace();
            return null;
        }
    }


    /**
     * Sends data to a server.
     *
     * @param data
     */

    public void sendDataToServer(char[] data) {
        System.out.println("sending: " + data.toString());
        if (outputStream == null)
            System.out.println("output stream is null");
        outputStream.print(data);

        outputStream.flush();
        System.out.println("Sent data");

    }

    /**
     * Disconnects the client from the server
     */

    public void disconnectFromServer() {
        //put any messages that need to be sent to server here.
        try {

            if (this.inputStream != null)
                this.inputStream.close();

            if (this.outputStream != null)
                this.outputStream.close();
            if (this.serverSocket != null)
                this.serverSocket.close();
            if (this.udpSocket != null)
                this.udpSocket.close();
        } catch (IOException e) {
            //do something
        }
    }

    /**
     * Sends data to a server
     *
     * @param data
     */
    public void sendDataToServer(String data) {
        System.out.println("Sending: " + data + " to server");
        if (outputStream != null) {
            outputStream.println(data);
        }
    }



    /**
     * Reads one message from the server in a background task and returns it as a string.
     */
    class ReadDatafromServerTask extends AsyncTask<Void, Boolean, String>
    {
        /**
         * Reads data from the server.
         * @param voids this don't do nothing.
         * @return
         */
        protected String doInBackground(Void... voids)
        {
            publishProgress(new Boolean(true));
            return readMessageFromServer();
        }

    }



    class VerifyUIDTask extends AsyncTask<String, Integer, Boolean>
    {
        protected Boolean doInBackground(String... strings)
        {
            int len = strings.length;
            int count = 0;
            String[] messages = new String[len];
            for (int i = 0; i < strings.length; ++i)
            {
                messages[i] = readMessageFromServer();
                if (messages[i].equals("VALID_UID"))
                    return new Boolean(false);
            }

            return new Boolean(true);
        }

        protected void onPostExecution(Boolean... bools)
        {
            System.out.println("Valid UID??? Should be more to this");
        }
    }

    class SendBytesToServerTask extends AsyncTask<DatagramPacket, Integer, Integer>
    {
        /**
         * Sends packets to the server via the client object.
         * @param packets The packets of data to send to the server via the UDP socket.
         * @return Returns the amount of packets sent.
         */
        protected Integer doInBackground(DatagramPacket... packets)
        {
            int len = packets.length;
            int count = 0;
            for (int i = 0; i < len; ++i)
            {
                try {
                    sendPacketToServer(packets[i]);
                } catch (IOException e)
                {
                    System.out.println("Error sending packet: " + i);
                    e.printStackTrace();
                }
                count = i;
                publishProgress((int) ((i / (float) len) * 100));

                if (isCancelled()) break;
            }
            return new Integer(count);
        }

        /**
         * Executed after SendMessageToServerTask exits. Prints that the messages all sent
         * @param messagesSent
         */
        public void onPostExecute(Integer messagesSent)
        {
            System.out.println("Sent: " + messagesSent.toString());

        }
    }

    class SendMessageToServerTask extends AsyncTask<String, Integer, Integer>
    {

        /**
         * Sends strings to the server via the client object.
         * @param strings
         * @return
         */
        protected Integer doInBackground(String ... strings)
        {
            int len = strings.length;
            int count = 0;
            for (int i = 0; i < len; ++i)
            {
                count = i;
                sendDataToServer(strings[i]);
                publishProgress((int) ((i / (float) len) * 100));

                if (isCancelled()) break;
            }
            return new Integer(count);
        }

        /**
         * Executed after SendMessageToServerTask exits. Prints that the messages all sent
         * @param messagesSent
         */
        public void onPostExecute(Integer messagesSent)
        {
            System.out.println("Sent: " + messagesSent.toString());

        }
    }
}
class ClientThread extends Thread{
    private Client client;
    ClientThread(Client client) {
        this.client = client;
    }
    @Override
    public void run() {
        try {
            this.client.connectToServer();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}