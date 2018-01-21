package com.github.jacekolszak.messagic.streams.input;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

import com.github.jacekolszak.messagic.streams.StreamsMessageChannelException;

final class DecodingBuffer {

    private final Utf8TextBuffer textBuffer;
    private final int binaryMessageMaximumSize;
    private final int textMessageMaximumSize;

    DecodingBuffer(InputStream input, int textMessageMaximumSize, int binaryMessageMaximumSize) {
        this.textBuffer = new Utf8TextBuffer(input);
        this.binaryMessageMaximumSize = binaryMessageMaximumSize;
        this.textMessageMaximumSize = textMessageMaximumSize;
    }

    byte[] nextBinaryMessage() throws IOException {
        String message = textBuffer.nextMessage(binaryMessageMaximumSize);
        byte[] decoded = decode(message);
        if (decoded.length > binaryMessageMaximumSize) {
            String encodedMessageFragment = message.substring(0, Math.min(binaryMessageMaximumSize, 256));
            String error = String.format("Incoming binary message \"%s...\" is bigger than allowed %s bytes", encodedMessageFragment, binaryMessageMaximumSize);
            throw new StreamsMessageChannelException(error);
        }
        return decoded;
    }

    private byte[] decode(String message) throws IOException {
        try {
            return Base64.getDecoder().decode(message);
        } catch (IllegalArgumentException e) {
            throw new IOException("Problem during decoding binary message", e);
        }
    }

    String nextTextMessage() throws IOException {
        String text = textBuffer.nextMessage(textMessageMaximumSize);
        if (text.length() > textMessageMaximumSize) {
            String error = String.format("Incoming text message \"%s...\" is bigger than allowed %s characters", text.substring(0, textMessageMaximumSize), textMessageMaximumSize);
            throw new StreamsMessageChannelException(error);
        }
        return text;
    }

    char nextChar() throws IOException {
        return textBuffer.nextChar();
    }

}
