package com.github.jacekolszak.messagic.streams;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.logging.Level;
import java.util.logging.Logger;

class InputPipe {

    private static final Logger logger = Logger.getLogger(InputPipe.class.getName());

    private final MessageStream messageStream;
    private Thread thread;
    private volatile boolean stopped;

    InputPipe(InputStream input, MessagePublisher messagePublisher) {
        this.messageStream = new MessageStream(input, 1024, 1024, messagePublisher);
    }

    void start() {
        thread = new Thread(() -> {
            try {
                while (!stopped) {
                    messageStream.readMessage();
                }
            } catch (InterruptedIOException e) {
                logger.info("Reading message stream interrupted");
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Problem during reading message stream", e);
            }
        });
        thread.start();
    }

    public void stop() {
        stopped = true;
        thread.interrupt();
    }

}