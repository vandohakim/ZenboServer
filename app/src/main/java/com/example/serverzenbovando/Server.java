package com.example.serverzenbovando;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class Server extends AppCompatActivity {

    EditText SocketServerPORT;

    TextView infoIp, infoPort, chatMsg, serverisOFF;
    Spinner spUsers;
    ArrayAdapter<ChatClient> spUsersAdapter;
    Button btnSentTo;
    EditText chattoclient;
    Button dscBtn;
    Button startBtn;
    LinearLayout startLayout;
    String msgLog = "";

    List<ChatClient> userList;
    boolean disconnect = false;
    ServerSocket serverSocket;
    ChatServerThread chatServerThread;
    private final Object lock = new Object();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        SocketServerPORT = (EditText) findViewById(R.id.SocketServerPORT);
        infoIp = (TextView) findViewById(R.id.infoip);
        serverisOFF = (TextView) findViewById(R.id.serverisOFF);
        infoPort = (TextView) findViewById(R.id.infoport);
        chatMsg = (TextView) findViewById(R.id.chatmsg);
        chattoclient = (EditText) findViewById(R.id.chattoclient);
        dscBtn = (Button) findViewById(R.id.dscBtn);
        startBtn = (Button) findViewById(R.id.startBtn);
        startLayout = (LinearLayout) findViewById(R.id.startLayout);
        spUsers = (Spinner) findViewById(R.id.spusers);
        userList = new ArrayList<ChatClient>();
        spUsersAdapter = new ArrayAdapter<ChatClient>(Server.this, android.R.layout.simple_spinner_item, userList);
        spUsersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spUsers.setAdapter(spUsersAdapter);

        btnSentTo = (Button)findViewById(R.id.sentto);
        btnSentTo.setOnClickListener(btnSentToOnClickListener);


        dscBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                for (int i = 0; i < userList.size(); i++){
//                    userList.get(i).chatThread.sendMsg("///Exit///");
//                    try {
//                        userList.get(i).chatThread.join(100);
//                    } catch (InterruptedException e){
//                        e.printStackT
//                        race();
//                    }
//                }
                broadcastMsg("///Exit///");

                startBtn.setVisibility(View.VISIBLE);
                SocketServerPORT.setVisibility(View.VISIBLE);
                serverisOFF.setVisibility(View.VISIBLE);
                startLayout.setVisibility(View.GONE);
                chatMsg.setVisibility(View.GONE);

                if (chatServerThread.isAlive()){
                    chatServerThread.interrupt();
                }

                infoPort.setText("");
                chatMsg.setText("");
                onRestart();
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                //chatServerThread.stop();
                //onDestroy();
                return;
                //Intent intent = getIntent();

                //startActivity(intent);
            }
        });
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String portcontent = SocketServerPORT.getText().toString();
                if (portcontent.equals("")) {
                    Toast.makeText(Server.this, "Insert PORT!",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                if (portcontent.length()!=4) {
                    Toast.makeText(Server.this, "PORT must be 4 digit!",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                chatMsg.setText("");
                startBtn.setVisibility(View.GONE);
                SocketServerPORT.setVisibility(View.GONE);
                serverisOFF.setVisibility(View.GONE);
                startLayout.setVisibility(View.VISIBLE);
                chatMsg.setVisibility(View.VISIBLE);
                chatServerThread = new ChatServerThread();
                chatServerThread.start();
            }
        });
        infoIp.setText(getIpAddress());

    }

    View.OnClickListener btnSentToOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ChatClient client = (ChatClient)spUsers.getSelectedItem();
            if(client != null){
                String dummyMsg = chattoclient.getText().toString()+"\n";
                client.chatThread.sendMsg("Server: "+dummyMsg);
                msgLog += "- Message sent to " + client.name + ": " + dummyMsg;
                chatMsg.setText(msgLog);


            }else{
                Toast.makeText(Server.this, "No user connected", Toast.LENGTH_LONG).show();
            }
        }
    };



    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private class ChatServerThread extends Thread {

        @Override
        public void run() {
            Socket socket = null;
            try {
                serverSocket = new ServerSocket(Integer.parseInt(SocketServerPORT.getText().toString()));
                Server.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        infoPort.setText("I'm waiting here: " + serverSocket.getLocalPort());
                    }
                });

                while (true) {
                    socket = serverSocket.accept();
                    ChatClient client = new ChatClient();
                    userList.add(client);
                    ConnectThread connectThread = new ConnectThread(client, socket);
                    connectThread.start();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            spUsersAdapter.notifyDataSetChanged();
                        }
                    });
                }


            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    try {
                        Log.d("", "run: interupted");
                        serverSocket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

            }

        }

    }

    private class ConnectThread extends Thread {

        Socket socket;
        ChatClient connectClient;
        String msgToSend = "";

        ConnectThread(ChatClient client, Socket socket){
            connectClient = client;
            this.socket= socket;
            client.socket = socket;
            client.chatThread = this;
        }

        @Override
        public void run() {
            DataInputStream dataInputStream = null;
            DataOutputStream dataOutputStream = null;

            try {
                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());

                String n = dataInputStream.readUTF();

                connectClient.name = n;

                msgLog += connectClient.name + " connected@" + connectClient.socket.getInetAddress() + ":" + connectClient.socket.getPort() + "\n";
                Server.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        chatMsg.setText(msgLog);
                    }
                });

                dataOutputStream.writeUTF("Welcome " + n + "\n");
                dataOutputStream.flush();

                broadcastMsg(n + " join our chat.\n");

                while (true) {
                    if (dataInputStream.available() > 0) {
                        String newMsg = dataInputStream.readUTF();

                        if (newMsg.contains("///Exit///")){
                            break;
                        }
                        msgLog += n + ": " + newMsg;
                        Server.this.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                chatMsg.setText(msgLog);
                            }
                        });

                        broadcastMsg(n + ": " + newMsg);
                    }

                    if(!msgToSend.equals("")){
                        dataOutputStream.writeUTF(msgToSend);
                        dataOutputStream.flush();
                        if (msgToSend.contains("///Exit///")){
                            break;
                        }
                        msgToSend = "";
                    }

                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (dataInputStream != null) {
                    try {

                        dataInputStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }


                Server.this.userList.remove(connectClient);


                Server.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        spUsersAdapter.notifyDataSetChanged();
                        Toast.makeText(Server.this,connectClient.name + " Disconnected.", Toast.LENGTH_LONG).show();

                        msgLog += "-- " + connectClient.name + " leaved\n";
                        Server.this.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                chatMsg.setText(msgLog);
                            }
                        });

                        broadcastMsg("-- " + connectClient.name + " leaved\n");


                    }
                });

            }

        }

        private void sendMsg(String msg){
            msgToSend = msg;
        }

    }

    private void broadcastMsg(String msg){
        for(int i=0; i<userList.size(); i++){
            userList.get(i).chatThread.sendMsg(msg);
            msgLog += "- send to " + userList.get(i).name + "\n";
        }

        Server.this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                chatMsg.setText(msgLog);
            }
        });
    }

    private String getIpAddress() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces.nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface.getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ip += "SiteLocalAddress: " + inetAddress.getHostAddress() + "\n";
                    }

                }

            }

        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ip += "Something Wrong! " + e.toString() + "\n";
        }

        return ip;
    }

    class ChatClient {
        String name;
        Socket socket;
        ConnectThread chatThread;

        @Override
        public String toString() {
            return name + ": " + socket.getInetAddress().getHostAddress();
        }
    }
}