package test_gradle.interfaces;

import javafx.util.Pair;

import java.nio.ByteBuffer;

public interface IDecoder<T> {

   ByteBuffer encode(T message);

    Pair<T, Integer> decode(ByteBuffer bytes, int begin, int end);
}
