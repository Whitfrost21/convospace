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

  

  public void writetohistory(String Mesg) {
    try (BufferedWriter bf = new BufferedWriter(new FileWriter("histo.txt", true))) {
      
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
