package com.github.jacekolszak.messagic.streams.eventbus;

import java.util.Set;
import java.util.function.Consumer;

import com.github.jacekolszak.messagic.Event;

public final class EventBus implements Consumer<Event> {

    private final ChannelDispatchThread dispatchThread;
    private final ListenersSet listenersSet;

    public EventBus() {
        this.dispatchThread = new ChannelDispatchThread();
        this.listenersSet = new ListenersSet();
    }

    public void start() {
        dispatchThread.start();
    }

    public void stop() {
        dispatchThread.stop();
    }

    public <T extends Event> void addListener(Class<T> eventClass, Consumer<T> listener) {
        try {
            listenersSet.add(eventClass, listener);
        } catch (RuntimeException e) {
            throw new RuntimeException("Can't add listener", e);
        }
    }

    public <T extends Event> void removeListener(Class<T> eventClass, Consumer<T> listener) {
        try {
            listenersSet.remove(eventClass, listener);
        } catch (RuntimeException e) {
            throw new RuntimeException("Can't remove listener", e);
        }
    }

    @Override
    public void accept(Event event) {
        Set<Consumer<Event>> listeners = listenersSet.listenersForEvent(event);
        listeners.forEach(listener ->
                dispatchThread.execute(() -> listener.accept(event))
        );
    }

}