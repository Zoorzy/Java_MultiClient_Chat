package scuola;

import java.io.IOException;

public class listenForMessage extends Thread {
  Client client;
  String msg;

  public listenForMessage(Client client) {

    this.client = client;
    this.start();
  
  }

  @Override
  public void run() {

    // LISTEN
    while (!(client.mySocket.isClosed())) {

      try {

        msg = "";

        if (!(client.mySocket.isClosed()) && (msg = client.inputDataStream.readLine()) != null) {

          client.outputTextArea.append(msg + "\n");

        } else if (msg == null || msg.equalsIgnoreCase("QUIT")) {

          client.outputTextArea.append("=== Chiusura connessione con il server ===\n");
          break;

        }

      }

      catch (IOException e) {

        // e.printStackTrace();
        client.outputTextArea.append("=== Chiusura connessione con il server ===\n");

      }
    }
  }
}
