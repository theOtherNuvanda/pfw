package shared;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.Scanner;

import shared.GameSocket;
import shared.MessageHandler;

public abstract class Connection extends Thread {
    private final String playerName;
    private final GameSocket gameSocket;

    private final Map<String, MessageHandler> handlers = new HashMap<>();

    public Connection(GameSocket gameSocket, String playerName) 
            throws IOException {
        this.gameSocket = gameSocket;
        this.playerName = playerName;
    }

    @Override
    public void run() {
        String line;
        try {
            while ((line = receive()) != null) {
                handle(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send(String msg) {
        gameSocket.getOutput().println(msg);
        gameSocket.getOutput().flush();
    }

    public void close() {
        try {
            gameSocket.getSocket().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String receive() throws IOException {
        return gameSocket.getInput().readLine();
    }

    public String getPlayerName() {
        return playerName;
    }

    public void handle(String msg) {
        Scanner scanner = new Scanner(msg);
        scanner.useLocale(Locale.US);
        String type = scanner.next();
        MessageHandler handler = handlers.get(type);
        if (handler != null) {
            handler.handle(scanner, this);
        } else {
            System.out.println("Received an unhandled message of type " + type);
        }
        scanner.close();
    }

    public MessageHandler
            putMessageHandler(String type, MessageHandler handler) {
        return handlers.put(type, handler);
    }

    public MessageHandler getMessageHandler(String type) {
        return handlers.get(type);
    }
}
