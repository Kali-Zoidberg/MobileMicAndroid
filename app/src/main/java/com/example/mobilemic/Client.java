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
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Client {
    private Socket serverSocket = null;
    private DatagramSocket udpSocket = null;
    private InetAddress group = null;
    private MulticastSocket multiSock = null;
    private int portNumber;
    private String hostName;
    private PrintWriter outputStream;
    private BufferedReader inputStream;
    private boolean connectedToUDP = false;
    private final String DISCONNECT_CMD = "S.disconnect";
    private final byte[] CLOSE_UDP_CMD = "end".getBytes();
    private int clumpSize = 64;

    private Object ouputLock = new Object();
    private Object inputLock = new Object();

    Client(String hostName, int port) {
        this.setPortNumber(port);
        this.setHostName(hostName);

    }

    Client()
    {
        this.hostName = "10.0.2.2";
        this.setPortNumber(7000);
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
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(this.hostName), this.portNumber);
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

    public void sendRTP(byte[] buffer) throws IOException
    {
        if (multiSock != null)
        {
            System.out.println("Sending packet to server.");
            for (byte b: buffer)
            {
                System.out.print (b + " ");
            }
            System.out.println();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(this.hostName), this.portNumber);
            multiSock.send(packet);

        } else
            System.out.println("Packet sent to server.");
    }


    public void connectToMultiSocket() throws SocketException
    {
        try {
            multiSock = new MulticastSocket(this.portNumber);
            group = InetAddress.getByName(this.hostName);
            multiSock.joinGroup(group);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Starts the udp socket on the server.
     *
     * @throws SocketException
     */
    public void connectToUDPServer() throws SocketException {
        this.sendDataToServer("S.udp");
        try {
            udpSocket = new DatagramSocket();
            this.connectedToUDP = true;

        } catch (SocketException e) {
            System.out.println("Could not connect to the UDP socket");
            e.printStackTrace();

        }
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
     * 1s the client from the server
     */

    public void disconnectFromServer() {
        //put any messages that need to be sent to server here.

        try {
            if (this.connectedToUDP)
                this.sendBytesToUDP("end".getBytes());

        } catch (IOException e) {
            //do something
        } finally {
            this.sendDataToServer(DISCONNECT_CMD);
            try {
                if (this.inputStream != null)
                    this.inputStream.close();

                if (this.outputStream != null)
                    this.outputStream.close();
                if (this.serverSocket != null)
                    this.serverSocket.close();
                if (this.udpSocket != null)
                    this.udpSocket.close();
            } catch (IOException e)
            {
                e.printStackTrace();
                System.out.println("Error closing one or more client file descriptors.");
            }
        }
    }


    /**
     * Closes the client's UDP socket if it is open.
     */

    public void closeUDPSocket()
    {
        if (this.isConnectedToUDP() || this.udpSocket != null)
        {
            try {
                this.sendBytesToUDP("end".getBytes());
                this.udpSocket.close();
            } catch (IOException e)
            {
                e.printStackTrace();
                System.out.println("Error closing udp socket or sending the end message to the udp server.");
            }
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
     * Method to determine if the client is connected to the UDP socket of the server.
     * @return Returns true if the client is connected to the UDP socket and returns false otherwise.
     */
    public boolean isConnectedToUDP()
    {
        return this.connectedToUDP;
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

    public int getClumpSize() {
        return clumpSize;
    }

    public void setClumpSize(int clumpSize) {
        this.clumpSize = clumpSize;
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