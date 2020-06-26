package org.studing.nio.implementations;

import org.studing.nio.Pair;
import org.studing.nio.interfaces.IDecoder;

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
        return new Pair<>(new String(Arrays.copyOfRange(bytes.array(), begin, end), StandardCharsets.UTF_8 ), end);
    }
}
