package test_gradle.implementations;

import javafx.util.Pair;
import test_gradle.interfaces.IDecoder;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Coder implements IDecoder<String> {


    @Override
    public ByteBuffer encode(String message) {
        return ByteBuffer.wrap(message.getBytes());
    }

    @Override
    public Pair<String, Integer> decode(ByteBuffer bytes, int begin, int end) {
        if (begin > end)
            return null;
        String result = new String(Arrays.copyOfRange(bytes.array(), begin, end + 1), StandardCharsets.UTF_8 );

        return new Pair<>(new String(result), end);
    }
}
