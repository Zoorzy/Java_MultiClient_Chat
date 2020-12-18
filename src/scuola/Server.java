package scuola;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Toolkit;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class Server extends JFrame {

  static final long serialVersionUID = 1;

  // Socket client e server
  ServerSocket server = null;
  Socket socketClient = null;

  // Dimensione della finestra
  final int WIDTH = 500;
  final int HEIGHT = (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight() - 25;

  int porta = 5678;

  // Componenti grafiche di output del Server
  BorderLayout bl;
  JTextArea outputTextArea;
  JScrollPane scrollPane;

  // Array che contiene Socket e Username dei client loggati
  ArrayList<Connect> utenzeAttive;

  // Costruttore
  public Server() {

    // Impostazione della finestra di I/O (Grafica)
    bl = new BorderLayout();
    setLayout(bl);
    setTitle("Server screen");
    setSize(WIDTH, HEIGHT);
    // setLocation(0,0);

    outputTextArea = new JTextArea();
    outputTextArea.setBounds(0, 0, WIDTH - 200, HEIGHT);
    outputTextArea.setBackground(Color.BLACK);
    outputTextArea.setForeground(Color.WHITE);

    // Creazione della scrollbar
    scrollPane = new JScrollPane();
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

    scrollPane.setBounds(0, 0, WIDTH, HEIGHT);
    // scrollPane.getViewport().setBackground(Color.BLACK);
    // scrollPane.getViewport().setForeground(Color.WHITE);
    scrollPane.getViewport().add(outputTextArea);

    add(scrollPane);

    setVisible(true);
    outputTextArea.append("Attivazione del server...\n");

    // Array che contiene gli oggetti connect, che definiscono socket e
    // username dei client connessi
    utenzeAttive = new ArrayList<Connect>();

    // WELCOME stampato a schermo su più righe
    outputTextArea.append("                __        __  ___                 __    \n");
    outputTextArea.append("  \\\\       //  |__  |     |     |      |   |  \\  /  |   |__        \n");
    outputTextArea.append("   \\\\//\\\\//    |__  |__ |__ |___|   |        |   |__        \n\n");

    outputTextArea.append("Server attivo in attesa di connessioni\n");

    this.createConnections();

  } // FINE COSTRUTTORE SERVER ()

  // Ciclando all'infinito, aspetto e accetto nuove connessioni dei client
  public void createConnections() {

    try {

      server = new ServerSocket(porta, 5);

      while (true) {
        socketClient = server.accept();
        new Connect(this, socketClient);
      }

    }

    catch (IOException e) {
      e.printStackTrace();
    }

  } // FINE CREATE CONNECTIONS ()

  public static void main(String[] args) {

    new Server();

  } // FINE MAIN ()

} // FINE CLASSE SERVER
