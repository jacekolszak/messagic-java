package com.github.messagic.streams.input;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.function.Consumer;
import java.util.logging.Logger;

import com.github.messagic.Event;
import com.github.messagic.streams.StreamsMessageChannelException;

public final class InputPipeThread {

    private static final Logger logger = Logger.getLogger(InputPipeThread.class.getName());

    private final Thread thread;
    private volatile boolean stopped;

    public InputPipeThread(MessageStream messageStream, Consumer<Event> onMessage, Consumer<Exception> onError) {
        thread = new Thread(() -> {
            try {
                while (!stopped) {
                    messageStream.readNextMessage();
                    Message message = messageStream.message();
                    DecodedMessage decodedMessage = message.decodedMessage();
                    onMessage.accept(decodedMessage.event());
                }
            } catch (InterruptedIOException e) {
                logger.info("Reading message stream interrupted");
            } catch (IOException e) {
                onError.accept(new StreamsMessageChannelException("Problem during reading input stream", e));
            }
        });
    }

    public void start() {
        thread.start();
    }

    public void stop() {
        stopped = true;
        if (thread != null) {
            thread.interrupt();
        }
    }

}
