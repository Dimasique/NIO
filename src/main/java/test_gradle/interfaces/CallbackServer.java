package test_gradle.interfaces;

public interface CallbackServer<T> {

    T callingback(T message);

}
