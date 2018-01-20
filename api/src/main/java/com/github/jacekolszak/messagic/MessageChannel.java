package com.github.jacekolszak.messagic;

/***
 * Please note that sending messages or starting and stopping the channel is asynchronous, ie. does not block the current thread until it is done.
 * All message and events listeners are run asynchronously in sequential manner.
 */
public interface MessageChannel {

    ChannelEvents events();

    /**
     * Once started channel allows to send messages and accepts incoming ones. After successful start Started event is published.
     * Running this method when MessageChannel is already started has no effect. Running this method after channel was stopped will throw IllegalStateException
     */
    void start();

    void send(String textMessage);

    void send(byte[] binaryMessage);

    /**
     * Once stopped no new messages can be send and no incoming messages will be read. Implementation may close the socket, stream or whatever.
     * After successful stop Stopped event is published. Running this method when MessageChannel is already stopped or not yet started has no effect.
     */
    void stop();

}
