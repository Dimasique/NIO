package test_gradle;

import test_gradle.interfaces.IDecoder;

class Coder implements IDecoder<String> {

    @Override
    public byte[] encode(String message) {
        return message.getBytes();
    }

    @Override
    public String decode(byte[] bytes) {
        return new String(bytes);
    }
}
