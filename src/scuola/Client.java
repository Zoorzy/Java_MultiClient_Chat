package scuola;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.TimeUnit;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class Client extends JFrame {

  static final long serialVersionUID = 1;

  Socket mySocket = null;

  int porta = 5678;

  final int WIDTH = 500;
  final int HEIGHT = 400;

  DataOutputStream outputDataStream;
  BufferedReader inputDataStream;

  BorderLayout bl;
  JTextField inputTextField;
  JTextArea outputTextArea;
  JScrollPane scrollPane;

  String messaggio = ""; // Stringa che contiene i messaggi in uscita dal client verso il server
  String rispostaDalServer = ""; // Stringa che contiene la risposte in entrata nel client dal server
  String username = ""; // Stringa che contiene l'username inviato al momento del login nella chat

  public Client() {

    try {

      bl = new BorderLayout();
      setLayout(bl);
      setTitle("Client screen");
      setSize(WIDTH, HEIGHT);
      setLocation((((int) Toolkit.getDefaultToolkit().getScreenSize().getWidth() / 2) - (WIDTH / 2)),
          ((int) Toolkit.getDefaultToolkit().getScreenSize().getHeight() / 2) - (HEIGHT / 2));

      outputTextArea = new JTextArea();
      outputTextArea.setBounds(0, 0, WIDTH - 20, HEIGHT - 20);
      outputTextArea.setBackground(Color.BLACK);
      outputTextArea.setForeground(Color.WHITE);

      // Creazione della scrollbar
      scrollPane = new JScrollPane();
      scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

      scrollPane.setBounds(20, 20, WIDTH, HEIGHT);
      // scrollPane.getViewport().setBackground(Color.BLACK);
      // scrollPane.getViewport().setForeground(Color.WHITE);
      scrollPane.getViewport().add(outputTextArea);

      add(scrollPane);

      inputTextField = new JTextField(10);
      add(inputTextField, BorderLayout.SOUTH);

      clientDisplay("########################################");
      clientDisplay("#                           --- Istruzioni ---\t#");
      clientDisplay("#                       Scegli un username\t#");
      clientDisplay("#                        Chatta con gli altri\t#");
      clientDisplay("#     Messaggi privati {username}:{messaggio}\t#");
      clientDisplay("#                           QUIT per uscire\t#");
      clientDisplay("########################################");

      setVisible(true);

      int secondi = 1;

      // Tento di connettermi con il server finchè non si instaura una connessione
      mySocket = this.connetti();
      while (mySocket == null) {
        clientDisplay("Nuovo tentativo tra " + secondi * 3 + " secondi");
        TimeUnit.SECONDS.sleep(secondi);
        clientDisplay("-");
        TimeUnit.SECONDS.sleep(secondi);
        clientDisplay("-");
        TimeUnit.SECONDS.sleep(secondi);
        clientDisplay("-");
        mySocket = this.connetti();
      }

      this.sendUsername();

    }

    catch (Exception e) {

      System.out.println("\n=== Errore di connessione ===\n");
      e.printStackTrace();

    }

  } // FINE COSTRUTTORE

  // Metodo che consente la connessione da parte del client verso un ipotetico
  // server, se esiste
  public Socket connetti() {

    try {

      clientDisplay("Tentativo di connessione in corso");
      mySocket = new Socket("127.0.0.1", porta);
      clientDisplay("Connesso con successo!");
      setTitle("Client Screen " + mySocket.getInetAddress() + ":" + mySocket.getLocalPort());

      inputDataStream = new BufferedReader(new InputStreamReader(mySocket.getInputStream()));
      outputDataStream = new DataOutputStream(mySocket.getOutputStream());

      // Chiusura della socket e dei flussi I/O in caso di chiusura della finestra
      this.addWindowListener(new WindowAdapter() {

        @Override
        public void windowClosing(WindowEvent e) {

          System.out.print("This client (" + mySocket.getLocalPort() + ") ...");

          try {

            if (!mySocket.isClosed()) {

              mySocket.close();
              outputDataStream.close();
              inputDataStream.close();

            }

          }

          catch (IOException e1) {

            e1.printStackTrace();

          }

          System.out.println(" has been closed");

        }

      });

    }

    catch (UnknownHostException e) {

      clientDisplay("=== Host Sconosciuto ===");
      e.printStackTrace();
      return null;

    }

    catch (Exception e) {

      clientDisplay("=== Impossibile stabilire la connessione, probabilmente i server sono in down... ===");
      e.printStackTrace();
      return null;

    }

    return mySocket;

  } // FINE CONNETTI()

  // Metodo che si occupa di inviare l'username all'avvio del client
  // e di intercettare eventuali messaggi di errore di ritorno dal server
  // verificatisi durante l'invio dell'username

  // Essendo questo un processo sequenziale (sincrono con l'andare del programma)
  // non ho bisogno di creare un nuovo Thread per gestire questo processo
  // Anzi è più corretto NON gestire affatto questa operazione tramite un Thread
  // apposito, essendo qualcosa di sequenziale

  private void sendUsername() {

    outputTextArea.append("Inserisci username > ");

    Action convalidaUsername = new AbstractAction() {
      static final long serialVersionUID = 1;

      @Override
      public void actionPerformed(ActionEvent e) {

        try {

          // Invio l'username al server per la convalida
          username = inputTextField.getText();
          outputDataStream.writeBytes(username + "\n");

          // Mi aspetto una risposta dal Server che dica se l'username va bene o se devo
          // inserirne un altro
          rispostaDalServer = inputDataStream.readLine();
          clientDisplay(username);

          // Check del messaggio in arrivo dal server riguardo l'username
          if (rispostaDalServer.equalsIgnoreCase("ERR_INVALID_USER")) {

            // L'username ha generato un errore generico
            clientDisplay("ERR_INVALID_USER = Inserisci un username valido");
            outputTextArea.append("Inserisci username > ");

          }

          else if (rispostaDalServer.equalsIgnoreCase("ERR_TAKEN_USER")) {

            // L'username è già in uso da altri sul server
            clientDisplay("ERR_TAKEN_USER = Username già in uso da qualcun altro");
            outputTextArea.append("Inserisci username > ");

          }

          else if (rispostaDalServer.equalsIgnoreCase("ERR_QUIT_USER")) {

            // L'username 'QUIT' non è valido, e chiude la connessione
            clientDisplay("ERR_QUIT_USER = L'username non può essere la parola per chiudere la conversazione");

            // Chiusura connessione
            outputDataStream.close();
            inputDataStream.close();
            mySocket.close();

            clientDisplay("=== Chiusura connessione con il server ===");

          }

          else if (rispostaDalServer.equalsIgnoreCase("ERR_:_USER")) {

            // L'username contiene il carattere ':', usato per inviare messaggi diretti
            // privati
            clientDisplay("ERR_:_USER = L'username non può contenere il carattere per i messaggi privati ':'");
            outputTextArea.append("Inserisci username > ");

          }

          else if (rispostaDalServer.equalsIgnoreCase("ERR_LENGTH_USER")) {

            // L'username deve avere una certa lunghezza di caratteri
            clientDisplay("ERR_LENGTH_USER = L'username deve essere lungo da 1 a 15 caratteri (15 compreso)");
            outputTextArea.append("Inserisci username > ");

          }

          else {

            // L'username va bene
            inputTextField.setText("");
            inputTextField.removeActionListener(this);
            setTitle("Connesso come: " + username + " (porta: " + mySocket.getLocalPort() + ")");

            // Appena mi loggo invio un true (inteso come 1:int) al server (classe Connect)
            outputDataStream.writeBoolean(true);

            comunica();

            return;

          }

          inputTextField.setText("");

        }

        catch (IOException e1) {

          // Intercetta Exception dal outputDataStream.writeBoolean(true)
          clientDisplay("=== Errore di connessione nella login ===");
          e1.printStackTrace();

        }

        catch (Exception a) {

          clientDisplay("=== C'è stato un errore nell'invio del messaggio ===");
          a.printStackTrace();

        }

        rispostaDalServer = "";

      }

    };

    // Richiamo all'invio dell'username quando l'utente genera un evento tramite la
    // pressione del tasto invio nel campo input (appunto come inviasse un
    // messaggio)
    inputTextField.addActionListener(convalidaUsername);

  } // FINE SEND USERNAME ()

  // Metodo che contiene la creazione dei 2 Thread di I/O per messaggiare
  public void comunica() throws IOException {

    inputTextField.setText("");
    clientDisplay("\n=== Ora puoi chattare '" + username + "' ! ===\n");

    new sendMessage(this);
    new listenForMessage(this);

  } // FINE COMUNICA ()

  public void clientDisplay(String messaggio) {

    outputTextArea.append(messaggio + "\n");

  }

  public static void main(String[] args) {

    new Client();

  } // FINE MAIN ()

}
