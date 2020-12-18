package scuola;

import java.awt.event.ActionEvent;
import java.io.IOException;
import javax.swing.AbstractAction;
import javax.swing.Action;

public class sendMessage extends Thread {

  Client client;

  public sendMessage(Client client) throws IOException {

    this.client = client;
    this.start();
    
  }

  @Override
  public void run() {

    Action invioMessaggi = new AbstractAction() {
      static final long serialVersionUID = 1;

      @Override
      public void actionPerformed(ActionEvent e) {

        final Runnable sendMessage;

        sendMessage = new Runnable() {

          @Override
          public void run() {

            if (!(client.mySocket.isClosed())) {

              // READ
              client.messaggio = client.inputTextField.getText();
              client.outputTextArea.append("Tu (" + client.mySocket.getLocalPort() + "): " + client.messaggio + "\n");

              try {

                // SEND
                client.outputDataStream.writeBytes(client.messaggio + "\n");
                client.inputTextField.setText("");

              } 
              
              catch (IOException e) {

                e.printStackTrace();

              }

            }

          }

        };

        sendMessage.run();

      }

    };

    this.client.inputTextField.addActionListener(invioMessaggi);

    

  }
}
