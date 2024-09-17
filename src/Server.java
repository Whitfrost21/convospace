import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.ArrayList;

public class Server {
  private ServerSocket server;
  private Socket socket;

  public Server(ServerSocket server) {
    this.server = server;
  }

  public void startserver() {
    try {
      while (!server.isClosed()) {
        socket = server.accept();
        System.out.println("A new Client is connected");
        new Thread(new CLHandler(socket)).start();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void closeserver() {
    try {
      if (server != null) {
        server.close();
      }
    } catch (Exception e) {
      e.getStackTrace();
    }
  }

  public static void main(String[] args) throws IOException {
    ServerSocket server = new ServerSocket(3233);
    Server s = new Server(server);
    s.startserver();
  }
}

class CLHandler implements Runnable {
  public static ArrayList<CLHandler> clienthandler = new ArrayList<>();
  private Socket socket;
  private BufferedReader reader;
  private BufferedWriter writer;
  private String Username;

  // private static final byte[] KEY_BYTES =
  //  new byte[] {
  //  0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
  // 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10
  // };

  public CLHandler(Socket socket) {
    try {
      this.socket = socket;
      this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
      this.Username = reader.readLine();
      clienthandler.add(this);
      Broadcast("SERVER:" + Username + " just enterd the chat");

    } catch (IOException e) {
      CloseEverything(socket, reader, writer);
    }
  }

  @Override
  public void run() {
    String messagefromClient;
    try {
      // SecretKey key = new SecretKeySpec(KEY_BYTES, "AES");
      while (socket.isConnected()) {
        messagefromClient = reader.readLine();
        Broadcast(messagefromClient);
        writetohistory(messagefromClient);
      }
    } catch (Exception e) {
      CloseEverything(socket, reader, writer);
      System.out.println(Username + " got disconnected");
    }
  }

  public void Broadcast(String message) {
    System.out.println("broadcasting...");
    try {
      for (CLHandler handle : clienthandler) {
        if (!handle.Username.equals(Username)) {
          handle.writer.write(message);
          handle.writer.newLine();
          handle.writer.flush();
        }
      }

    } catch (IOException e) {
      CloseEverything(socket, reader, writer);
    }
  }

  // public SecretKey generatekey() throws Exception {
  // KeyGenerator keygen = KeyGenerator.getInstance("AES");
  // keygen.init(128);
  // return keygen.generateKey();
  // }

  // public void savekey(SecretKey key) {
  // try (FileOutputStream fos = new FileOutputStream("pass.txt");
  //  ObjectOutputStream oos = new ObjectOutputStream(fos)) {
  // oos.writeObject(key);
  // oos.flush();
  // } catch (Exception e) {
  // e.printStackTrace();
  // }
  // }

  // public String encrypt(String mesg, SecretKey key) throws Exception {
  // Cipher cip = Cipher.getInstance("AES/CBC/PKCS5Padding");
  // byte[] iv = new byte[16]; // Generate a random IV
  // IvParameterSpec ivSpec = new IvParameterSpec(iv);
  // cip.init(Cipher.ENCRYPT_MODE, key, ivSpec);
  // byte[] encryptedBytes = cip.doFinal(mesg.getBytes());
  // return Base64.getEncoder().encodeToString(iv)
  // + ":"
  // + Base64.getEncoder().encodeToString(encryptedBytes);
  // }

  public void writetohistory(String Mesg) {
    try (BufferedWriter bf = new BufferedWriter(new FileWriter("histo.txt", true))) {
      // String enc = encrypt(Mesg, key);
      bf.write(Mesg);
      bf.newLine();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void removehandler() {
    clienthandler.remove(this);
    Broadcast("SERVER:" + Username + "just left the chat!");
  }

  public void CloseEverything(Socket socket, BufferedReader reader, BufferedWriter writer) {
    removehandler();
    try {
      if (reader != null) {
        reader.close();
      }
      if (writer != null) {
        writer.close();
      }
      if (socket != null) {
        socket.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
