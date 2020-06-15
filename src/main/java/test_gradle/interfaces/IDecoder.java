package test_gradle.interfaces;

public interface IDecoder<T> {

    byte[] encode(T message);

    T decode(byte[] bytes);
}
