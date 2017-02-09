package client;

import java.io.IOException;

import shared.GameSocket;
import shared.Connection;

public class ServerConnection extends Connection {
    public ServerConnection(GameSocket gameSocket, SnakeTest snakeTest) 
            throws IOException {
        super(gameSocket, snakeTest.getPlayerName());
        send(getPlayerName());
    }
}
