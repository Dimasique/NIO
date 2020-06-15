package test_gradle.interfaces;

public interface IClient<T> {

    void connect(String IP, int port);

    void close();

    void send(T message);
}
