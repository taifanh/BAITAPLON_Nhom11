package controllers;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class MessageBus {

    private static final MessageBus INSTANCE = new MessageBus();
    // CopyOnWriteArrayList an toàn khi nhiều luồng subscribe/unsubscribe
    private final List<Consumer<String>> subscribers = new CopyOnWriteArrayList<>();

    private MessageBus() {}

    public static MessageBus getInstance() {
        return INSTANCE;
    }

    // Controller gọi hàm này khi mở màn hình
    public void subscribe(Consumer<String> handler) {
        subscribers.add(handler);
    }

    // Controller gọi hàm này khi đóng màn hình (tránh memory leak)
    public void unsubscribe(Consumer<String> handler) {
        subscribers.remove(handler);
    }

    // Listener thread gọi hàm này với mỗi dòng nhận được từ server
    public void dispatch(String rawJson) {
        System.out.println("[MessageBus] Dispatching message: " + rawJson);
        for (Consumer<String> sub : subscribers) {
            System.out.println("[MessageBus] Sending to subscriber: " + sub);
            sub.accept(rawJson);
        }
    }
}