import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.Base64;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.swing.*;

public class Client extends JFrame {
  private Socket socket;
  private BufferedReader reader;
  private BufferedWriter writer;
  private String Username;
  private JLabel header = new JLabel("GroupTalk");
  private JTextArea mesgArea = new JTextArea();
  private JTextField mesgInput = new JTextField(20);
  private Font font = new Font("Roboto", Font.BOLD, 25);
  private JButton jbut = new JButton("send");

  // private static final String BASE64_KEY = "AQIDBAUGBwgJCgsMDQ4PEA==";

  public Client(Socket socket, String Username) {
    try {
      this.socket = socket;
      this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
      this.Username = Username;
      writer.write(Username);
      writer.newLine();
      writer.flush();

    } catch (IOException e) {
      CloseEverything(socket, reader, writer);
    }
  }

  public void CreateFrame() {
    // design the jframe
    this.setTitle("GroupChatAPI");
    this.setSize(600, 600);
    this.setLocationRelativeTo(null);
    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    // set fonts
    header.setBackground(Color.cyan);
    header.setFont(font);
    mesgInput.setFont(font);
    mesgArea.setFont(font);

    // design header
    header.setHorizontalTextPosition(SwingConstants.CENTER);
    header.setVerticalTextPosition(SwingConstants.BOTTOM);
    header.setHorizontalAlignment(SwingConstants.CENTER);
    header.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    // set area uneditable and move to center
    mesgArea.setEditable(false);
    mesgInput.setHorizontalAlignment(SwingConstants.CENTER);

    // jbut
    jbut.setHorizontalAlignment(SwingConstants.RIGHT);
    jbut.setSize(40, 40);
    JPanel jp = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    jp.add(mesgInput);
    jp.add(jbut);

    // frame
    this.setLayout(new BorderLayout());

    // add components
    this.add(header, BorderLayout.NORTH);
    JScrollPane js = new JScrollPane(mesgArea);
    Dimension viewportSize = js.getViewport().getSize();
    Dimension documentSize = mesgArea.getPreferredSize();
    int yPosition = documentSize.height - viewportSize.height;
    js.getViewport().setViewPosition(new Point(0, yPosition));
    this.add(js, BorderLayout.CENTER);
    this.add(jp, BorderLayout.SOUTH);
    // this.add(mesgInput,BorderLayout.SOUTH);

    this.setVisible(true);
  }

  public void handleIO() {

    jbut.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent a) {
            SendMessage();
          }
        });
    mesgInput.addKeyListener(
        new KeyListener() {
          @Override
          public void keyPressed(KeyEvent e) {}

          public void keyTyped(KeyEvent e) {}

          public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == 10) {
              SendMessage();
              listen();
            }
          }
        });
  }

  public void SendMessage() {
    try {
      String messagefromclient = mesgInput.getText();
      if (!messagefromclient.isEmpty()) {
        writer.write(Username + ": " + messagefromclient);
        writer.newLine();
        writer.flush();
        mesgInput.setText(null);
        mesgInput.requestFocus();
        if (messagefromclient.equals("quit")) {
          writer.write(Username + " just left the chat");
          writer.newLine();
          writer.flush();
          blockframe();
        }
        mesgArea.append(Username + ":" + messagefromclient + "\n");
      }
    } catch (Exception e) {
      CloseEverything(socket, reader, writer);
    }
  }

  public void listen() {

    new Thread(
            new Runnable() {
              @Override
              public void run() {
                while (socket.isConnected()) {
                  try {
                    String messagefromchat = reader.readLine();
                    SwingUtilities.invokeLater(
                        () -> {
                          mesgArea.append(messagefromchat + "\n");
                          mesgArea.setCaretPosition(mesgArea.getDocument().getLength());
                        });
                  } catch (IOException e) {
                    CloseEverything(socket, reader, writer);
                  }
                }
              }
            })
        .start();
  }

  public void loaddata() {
    try (BufferedReader br = new BufferedReader(new FileReader("histo.txt"))) {
      String line;
      while ((line = br.readLine()) != null) {
        mesgArea.append(line + "\n");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void blockframe() {
    this.dispose();
  }

  public void CloseEverything(Socket socket, BufferedReader reader, BufferedWriter writer) {
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
      System.out.println("error at closeeverything");
    }
  }

  //// public static SecretKey loadkey() throws Exception {
  //    try (FileInputStream fis = new FileInputStream("pass.txt");
  // ObjectInputStream ois = new ObjectInputStream(fis)) {
  // return (SecretKey) ois.readObject();
  // } catch (Exception e) {
  //      throw new Exception("Failed to load the secret key", e);
  //  }
  //  }

  public String decrypt(String enc, SecretKey key) throws Exception {
    String[] parts = enc.split(":");
    byte[] iv = Base64.getDecoder().decode(parts[0]);
    byte[] encryptedBytes = Base64.getDecoder().decode(parts[1]);

    Cipher cip = Cipher.getInstance("AES/CBC/PKCS5Padding");
    IvParameterSpec ivSpec = new IvParameterSpec(iv);
    cip.init(Cipher.DECRYPT_MODE, key, ivSpec);
    byte[] decryptedBytes = cip.doFinal(encryptedBytes);
    return new String(decryptedBytes);
  }

  public static void main(String[] args) throws Exception {
    Socket socket = new Socket("127.0.0.1", 3233);

    String Username =
        JOptionPane.showInputDialog(null, "enter username", "ok", JOptionPane.QUESTION_MESSAGE);
    Client c = new Client(socket, Username);
    // byte[] decodedKey = Base64.getDecoder().decode(BASE64_KEY);
    // SecretKey key = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
    c.CreateFrame();
    c.loaddata();
    c.listen();
    c.SendMessage();
    new Thread(
            new Runnable() {
              public void run() {
                c.handleIO();
              }
            })
        .start();
  }
}
