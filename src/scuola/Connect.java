package scuola;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

// Classe che gestisce le SINGOLE connessioni Client - Server
// 1 oggetto Connect => 1 connessione tra 1 client e 1 server
class Connect extends Thread {

  final Server server;
  Socket client;
  BufferedReader inputFromClient;
  DataOutputStream outputToClient;
  String username;
  String risposta;

  // COSTRUTTORE()
  public Connect(Server server, Socket client) throws IOException {

    this.server = server;
    this.client = client;

    serverDisplay("=== Il client " + client.getInetAddress() + ":" + client.getPort() + " si sta connettendo. ===");

    this.start();

  } // FINE COSTRUTTORE()

  private boolean usernameAlreadyExists(String username) throws IOException {

    // Username Pippo e PIPPO non possono esistere in contemporanea

    for (int i = 0; this.server.utenzeAttive.size() > i && this.server.utenzeAttive.size() != 0; i++) {
      if (this.server.utenzeAttive.get(i).username.equalsIgnoreCase(username)) {
        return true;
      }
    }

    return false;

  }

  public boolean setUsername() throws IOException {

    boolean chiudi; // Chiusura del ciclo while per settare l'username
    String usernameRicevuto; // Stringhe di I/O per settare l'username
    String risposta; // contiene il risultato dei passaggi per settare l'username

    if (!client.isClosed()) {

      chiudi = false; // Finchè il ciclo deve andare è false (ovvero finchè l'username non è settato)

      inputFromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
      outputToClient = new DataOutputStream(this.server.socketClient.getOutputStream());

      while (!chiudi) {

        try {

          usernameRicevuto = inputFromClient.readLine();

          if (usernameRicevuto == null) {

            // null lo ricevo se il client chiude la finestra

            risposta = "=== Chiusura connessione con " + client.getPort() + " ===";
            serverDisplay(risposta);

            destroyConnection();

            return false;

          } else if (usernameAlreadyExists(usernameRicevuto)) {

            risposta = "ERR_TAKEN_USER";

            serverDisplay("(" + client.getPort() + ") : " + risposta + " ==> " + usernameRicevuto);
            sendMessage(risposta);

          } else if (usernameRicevuto.contains(":")) {

            risposta = "ERR_:_USER";

            serverDisplay("(" + client.getPort() + ") : " + risposta + " ==> " + usernameRicevuto);
            sendMessage(risposta);

          } else if (usernameRicevuto.equalsIgnoreCase("QUIT")) {

            // QUIT vale come messaggio di uscita

            serverDisplay("(" + client.getPort() + ") : " + usernameRicevuto);
            risposta = "=== Chiusura connessione con " + client.getPort() + " ===";
            serverDisplay(risposta);

            risposta = "ERR_QUIT_USER";
            sendMessage(risposta);

            destroyConnection();

            return false;

          } else if ((usernameRicevuto.length() <= 0) || (usernameRicevuto.length() > 15)) {

            risposta = "ERR_LENGTH_USER";

            serverDisplay("(" + client.getPort() + ") : " + risposta + " ==> " + usernameRicevuto);
            sendMessage(risposta);

          } else if ((usernameRicevuto.contains("ERR_"))) {

            // Se l'username non va bene (piu in generale), perchè contiene "ERR_" ...
            risposta = "ERR_INVALID_USER";

            serverDisplay("(" + client.getPort() + ") : " + risposta + " ==> " + usernameRicevuto);
            sendMessage(risposta);

          } else {

            // Ok, l'username (teoricamente) va bene e posso uscire dal ciclo while che
            // setta l'username

            chiudi = true;

            // Completo la configurazione dell'oggetto connect

            this.username = usernameRicevuto;

            risposta = "Username per questa sessione settato a: " + this.username;
            serverDisplay("(" + client.getPort() + ") : " + risposta);
            sendMessage(risposta);

          }

        } catch (IOException e) {

          e.printStackTrace();
          return false;

        }

      }

    } else {

      return false;

    }

    return true;

  } // FINE SETUSERNAME()

  public void destroyConnection() throws IOException {

    inputFromClient.close();
    outputToClient.close();
    client.close();
    this.server.utenzeAttive.remove(this);

  }

  public void sendMessage(String messaggio) throws IOException {

    outputToClient.writeBytes(messaggio + "\n");

  }

  public void serverDisplay(String messaggio) {

    this.server.outputTextArea.append(messaggio + "\n");

  }

  // RUN GESTISCE I MESSAGGI DI INPUT E OUTPUT LATO SERVER
  public void run() {

    try {

      String messaggioDaClient = "";
      String risposta = "";
      String componentiMessaggio[]; // Conterrà le varie parti di un messaggio presumibilmente privato

      if (!client.isClosed() && this.setUsername()) {

        this.server.utenzeAttive.add(this);

        // Il client invia un 1 (inteso come true) quando si connette
        if ((inputFromClient.read() == 1)) {

          risposta = "=== Il client " + this.username + " ( " + client.getPort() + " ) si è connesso ===";
          broadcastMessage(risposta);
          serverDisplay(risposta);

        }

        serverDisplay(getUtenzeAttive());

        // Attesa dei messaggi
        while (!client.isClosed()) {

          // Lettura del messaggio dal client
          messaggioDaClient = inputFromClient.readLine();

          // null lo ricevo se chiudo la finestra client
          // QUIT è la stringa inviata dal client per cui vuole disconnettersi

          if ((messaggioDaClient == null) || (messaggioDaClient.equalsIgnoreCase("QUIT"))) {

            risposta = "=== Il client " + this.username + " ( " + client.getPort() + " ) si è disconnesso ===";

            broadcastMessage(risposta);
            serverDisplay(risposta);

            destroyConnection();
            return;

          }

          else {

            // Il contenuto ricevuto è un messaggio valido
            // System.out.println(messaggioDaClient);
            componentiMessaggio = messaggioDaClient.split(":");

            serverDisplay(this.username + " (" + client.getPort() + ") : " + messaggioDaClient);

            if (componentiMessaggio.length == 1) {

              // Se l'array è composto da 1 valore --> messaggio Broadcast
              broadcastMessage(this.username + " (" + this.client.getPort() + "): " + messaggioDaClient);

            }

            else if (componentiMessaggio.length == 2) {

              // Se l'array è composto da 2 valori --> messaggio privato
              if (!privateMessage(componentiMessaggio[0], componentiMessaggio[1])) {
                sendMessage("=== L'utente a cui hai provato a mandare il messaggio non esiste! ===");
                serverDisplay("=== L'utente a cui hai provato a mandare il messaggio non esiste! ===");
              }

            }

            else {

              sendMessage("Errore nella formazione del messaggio privato");
              serverDisplay("Errore nella formazione del messaggio privato");

            }

          }

        }
      }

    } catch (Exception e) {

      e.printStackTrace();

    }

  } // FINE RUN()

  public String getUtenzeAttive() {

    String stampa = "\n=== Utenze Attive : ===\n";

    for (int i = 0; i < this.server.utenzeAttive.size(); i++) {
      stampa += this.server.utenzeAttive.get(i).username + " sta attualmente comunicando tramite la porta "
          + this.server.utenzeAttive.get(i).client.getPort();

      if (i < (this.server.utenzeAttive.size() - 1))
        stampa += ",";
      else
        stampa += ".";

      stampa += "\n";
    }

    stampa += "==================\n";

    return stampa;

  } // FINE GETUTENZEATTIVE

  public void broadcastMessage(String message) throws IOException {

    for (int i = 0; i < this.server.utenzeAttive.size(); i++) {

      if (this.server.utenzeAttive.indexOf(this) != i) {

        this.server.utenzeAttive.get(i).sendMessage(message);

      }

    }

  } // FINE BROADCAST MESSAGE

  public boolean privateMessage(String destinatario, String message) throws IOException {

    // per non dover cercare l'indice di questo client tutte le volte
    int indiceClient = this.server.utenzeAttive.indexOf(this);

    // Ricerca del client
    for (int i = 0; i < this.server.utenzeAttive.size(); i++) {

      if (this.server.utenzeAttive.get(i).username.equalsIgnoreCase(destinatario)) {

        if (i != indiceClient) {

          this.server.utenzeAttive.get(i)
              .sendMessage(this.username + "(" + this.client.getPort() + ") ti sussurra : " + message);
          return true;

        } else {

          return false;

        }

      }

    }
    return false;

  } // FINE BROADCAST MESSAGE

} // FINE CLASSE CONNECT