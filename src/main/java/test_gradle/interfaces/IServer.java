package test_gradle.interfaces;

import java.io.IOException;

public interface IServer<T> {

    void start();

    void close() throws IOException;

}
