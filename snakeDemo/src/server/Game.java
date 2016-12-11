package server;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.LinkedList;
import java.util.Random;

import processing.core.PVector;

import shared.Connection;
import shared.Snake;
import shared.Food;

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

        public String score() {
            return "score "
                + connection.getPlayerName() + " "
                + snake.getParts().size();
        }

        public String death() {
            return "die " + connection.getPlayerName();
        }

        public boolean borderCollision() {
            // FIXME NYI
            return false;
        }

    }

    public static final int FIELD_X = 1024;
    public static final int FIELD_Y = 768;
    public static final int MAX_FOOD = 30;
    public static final float TICK_DURATION = 0.1f;

    static final Random random = new Random();

    private final Map<String, Player> players = new HashMap<>();
    final List<Food> foods = new LinkedList<>();

    public synchronized void registerClient(Connection connection) {
        System.out.println("Client registered: "+ connection.getPlayerName());
        players.put(
                connection.getPlayerName(),
                new Player(connection, new Snake(connection.getPlayerName()))
        );
        for (Food food : foods) { connection.send(food.getMessage()); }
        for (Player player : players.values()) {
            connection.send(player.position() + "\n" + player.score());
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
}
