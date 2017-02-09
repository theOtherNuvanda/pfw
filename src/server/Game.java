package server;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import processing.core.PVector;
import shared.Connection;
import shared.Food;
import shared.MessageHandler;
import shared.Snake;

public class Game extends Thread{

    class Player {

        final Connection connection;
        final Snake snake;

        public Player(Connection connection, Snake snake) {
            this.connection = connection;
            this.snake = snake;
        }

        public void move() {
            snake.moveBy(TICK_DURATION);
            // close connection if snake reaches the canvas border
            if (borderCollision()) { unregisterClient(connection); }
        }

        public void eat() {
            for (int i = 0; i < foods.size(); i++) {
                Food food = foods.get(i);
                if (snake.head().dist(food)
                        < Food.SIZE + Snake.SPEED * TICK_DURATION) {
                    snake.grow(Food.GROWTH_FACTOR);
                    foods.remove(i);
                    broadcast("eat "
                            + connection.getPlayerName() + " "
                            + Integer.toString(i));
                }
            }
        }

        public String position() {
            return "pos "+ connection.getPlayerName()+" "+snake.head().x+" "+snake.head().y;
        }

        public String direction() {
            return "dir "
                + connection.getPlayerName() + " "
                + snake.getDirection().x + " "
                + snake.getDirection().y;
        }

        public String score() {
            return "score "
                + connection.getPlayerName() + " "
                + snake.getParts().size();
        }

        public String death() {
            return "die " + connection.getPlayerName();
        }

        /**
         * To determine whether snake touches the border of screen
         * @return      true when snake reaches the canvas border
         */
        public boolean borderCollision() {
            return snake.head().x < 0
                || snake.head().y < 0
                || snake.head().x > FIELD_X
                || snake.head().y > FIELD_Y;
        }
    }

    /** Width of the game-window. */
    public static final int FIELD_X = 1024;

    /** Height of the game-window. */
    public static final int FIELD_Y = 768;

    /** Maximum number of Food on the screen. */
    public static final int MAX_FOOD = 30;

    public static final float TICK_DURATION = 0.1f;

    static final Random random = new Random();

    private final Map<String, Player> players = new HashMap<>();

    /** List of the currently drawn food. */
    final List<Food> foods = new LinkedList<>();

    public synchronized void registerClient(Connection connection) {
        System.out.println("Client registered: "+ connection.getPlayerName());
        players.put(
                connection.getPlayerName(),
                new Player(connection, new Snake(connection.getPlayerName()))
        );
        connection.putMessageHandler("dir", new DirMessageHandler());
        for (Food food : foods) { connection.send(food.getMessage()); }
        for (Player player : players.values()) {
            connection.send(player.position() + "\n"
                    + player.direction() + "\n"
                    + player.score());
        }
    }

    public synchronized void unregisterClient(Connection connection) {
        System.out.println("Client unregistered: "+ connection.getPlayerName());
        broadcast(players.get(connection.getPlayerName()).death());
        connection.close();
        players.remove(connection.getPlayerName());
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep((long) (TICK_DURATION * 1000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            for (Player player : players.values()) {
                player.move();
                player.eat();
            }

            for (Player player : players.values()) {
                broadcast(player.position());
            }
            // if the list of food is not full create new food an add it to the list
            if (foods.size() <= random.nextInt(MAX_FOOD)) {
                Food food = Food.randomFood(FIELD_X, FIELD_Y);
                foods.add(food);
                broadcast(food.getMessage());
            }
        }
    }

    private void broadcast(String message) {
        for (Player p : players.values()) {
            p.connection.send(message);
        }
    }

    public synchronized void setDirection(String playerName, float x, float y) {
        players.get(playerName).snake.setDirection(new PVector(x, y));
    }

    public class DirMessageHandler implements MessageHandler {
        @Override
        public void handle(Scanner scanner, Connection connection) {
            setDirection(
                    connection.getPlayerName(),
                    scanner.nextFloat(),
                    scanner.nextFloat());
            broadcast(players.get(connection.getPlayerName()).direction());
        }
    }
}
