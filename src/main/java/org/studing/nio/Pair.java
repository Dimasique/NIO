package org.studing.nio;

public class Pair<K, U> {
    private final K key;
    private final U value;

    public Pair(K key, U value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() {
        return key;
    }

    public U getValue() {
        return value;
    }
}
