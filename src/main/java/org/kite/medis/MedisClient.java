package org.kite.medis;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;

public class MedisClient {

    private String host;

    private Integer port;

    private String password;

    private static final String DEFAULT_HOST = "127.0.0.1";

    private static final int DEFAULT_PORT = 6379;

    private static final int DEFAULT_TIMEOUT = 2000;


    private OutputStream outputStream;

    private InputStream inputStream;

    private MedisClient() {
        this(DEFAULT_HOST, DEFAULT_PORT);
    }

    public MedisClient(String host, Integer port) {
        this(host, port, null);
    }

    public MedisClient(String host, Integer port, String password) {
        this.host = host;
        this.port = port;
        this.password = password;
        try {
            createSocket();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Socket createSocket() throws IOException {
        Socket socket = null;
        try {
            socket = new Socket();
            socket.setReuseAddress(true);
            socket.setKeepAlive(true);
            socket.setTcpNoDelay(true);
            socket.setSoLinger(true, 0);

            socket.connect(new InetSocketAddress(host, port), DEFAULT_TIMEOUT);
            socket.setSoTimeout(DEFAULT_TIMEOUT);
            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();
            return socket;
        } catch (Exception ex) {
            if (socket != null) {
                socket.close();
            }
            throw ex;
        }
    }


    public String command(String cmd) throws Exception {
        outputStream.write((cmd + "\r\n").getBytes());
        outputStream.flush();
        while (inputStream.available() == 0) {
        }
        byte[] bytes = new byte[inputStream.available()];
        inputStream.read(bytes);
        String result = new String(bytes);
        System.out.println(String.format("%s => %s", "原始格式", result.replace("\r\n", "\\r\\n")));

        printPretty(result);
        return result;
    }

    private void printPretty(String raw) {
        byte type = raw.getBytes()[0];
        String result = raw.substring(1);
        switch (type) {
            case '+':
                System.out.println("状态回复：" + result.replace("\r\n", ""));
                break;
            case '-':
                System.out.println("错误回复：" + result.replace("\r\n", ""));
                break;
            case ':':
                System.out.println("整数回复：" + result.replace("\r\n", ""));
                break;
            case '$':
                System.out.println("批量回复：" + result.substring(1).replace("\r\n", ""));
                break;
            case '*':
                String[] strList = result.substring(5).split("\r\n");
                System.out.print("多条批量回复：");
                for (int i = 1; i < strList.length; i += 2) {
                    System.out.print(strList[i] + " ");
                }
                System.out.println();
                break;
            default:
                System.out.println("未知返回");
                break;
        }
    }

    public static void main(String[] args) {
        MedisClient medisClient = new MedisClient("127.0.0.1", 6379, "P@ssw0rd");
        try {
            while (true) {
                Scanner reader = new Scanner(System.in);
                if (reader.hasNextLine()) {
                    String cmd = reader.nextLine();
                    if (!cmd.isEmpty()) {
                        medisClient.command(cmd);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
