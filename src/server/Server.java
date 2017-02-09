package server;

import java.io.IOException;
import java.net.ServerSocket;
import shared.GameSocket;
import shared.Connection;

public class Server {
    public static final int FIELD_X = 1024;
    public static final int FIELD_Y = 768;

    private static Game game = new Game();
	private static ServerSocket server;

    public static void main(String[] args) throws IOException {
        server = new ServerSocket(3000);
        game.start();

        while (true) {
            GameSocket client = new GameSocket(server.accept());
            System.out.println("Client connected");
            Connection connection = new PlayerConnection(client, game);
            game.registerClient(connection);
            connection.start();
        }
    }
}
