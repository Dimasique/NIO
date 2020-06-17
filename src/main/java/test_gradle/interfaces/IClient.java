package test_gradle.interfaces;

import java.io.IOException;

public interface IClient<T> {

    void connect(String IP, int port) throws IOException;

    void registration() throws IOException;

    void close() throws IOException;

    void send(T message);

    void start();
}
