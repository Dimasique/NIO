package test_gradle.interfaces;

public interface CallbackClient<T> {

    void callingback(T message);
}
