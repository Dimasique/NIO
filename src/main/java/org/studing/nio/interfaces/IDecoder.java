package org.studing.nio.interfaces;

import org.studing.nio.Pair;

import java.nio.ByteBuffer;

public interface IDecoder<T> {

   ByteBuffer encode(T message);

    Pair<T, Integer> decode(ByteBuffer bytes, int begin, int end);
}
