package com.github.jacekolszak.messagic.streams

import com.github.jacekolszak.messagic.Started
import com.github.jacekolszak.messagic.Stopped
import spock.lang.Specification
import spock.lang.Subject

final class LifecycleEventsSpec extends Specification {

    private final StreamsPipedOutputStream inputPipe = new StreamsPipedOutputStream()
    private final PipedInputStream input = inputPipe.inputStream()
    private final StreamsPipedOutputStream output = new StreamsPipedOutputStream()

    @Subject
    private StreamsMessageChannel channel = new StreamsMessageChannel(input, output)

    void cleanup() {
        channel.stop()
    }

    void 'after start should notify StartedEvent listener'() {
        given:
            ConsumeOneMessage listener = new ConsumeOneMessage()
            channel.eventBus().addListener(Started, listener)
        when:
            channel.start()
        then:
            listener.message() instanceof Started
    }

    void 'should notify StartedEvent listeners in sequence based on the order they were registered'() {
        given:
            List<AwaitingConsumer> executionOrder = []
            AwaitingConsumer first = new AwaitingConsumer({ executionOrder << it })
            AwaitingConsumer last = new AwaitingConsumer({ executionOrder << it })
            channel.eventBus().addListener(Started, first)
            channel.eventBus().addListener(Started, last)
        when:
            channel.start()
        then:
            first.waitUntilExecuted()
            last.waitUntilExecuted()
            executionOrder == [first, last]
    }

    void 'all StartedEvent listeners should be executed even though some listener thrown exception'() {
        given:
            AwaitingConsumer first = new AwaitingConsumer({ throw new RuntimeException('Deliberate exception') })
            AwaitingConsumer last = new AwaitingConsumer()
            channel.eventBus().addListener(Started, first)
            channel.eventBus().addListener(Started, last)
        when:
            channel.start()
        then:
            first.waitUntilExecuted()
            last.waitUntilExecuted()
    }


    void 'after stop should notify StoppedEvent listener'() {
        given:
            ConsumeOneMessage listener = new ConsumeOneMessage()
            channel.eventBus().addListener(Stopped, listener)
            channel.start()
        when:
            channel.stop()
        then:
            listener.message() instanceof Stopped
    }

    void 'should notify StoppedEvent listeners in sequence based on the order they were registered'() {
        given:
            List<AwaitingConsumer> executionOrder = []
            AwaitingConsumer first = new AwaitingConsumer({ executionOrder << it })
            AwaitingConsumer last = new AwaitingConsumer({ executionOrder << it })
            channel.eventBus().addListener(Stopped, first)
            channel.eventBus().addListener(Stopped, last)
            channel.start()
        when:
            channel.stop()
        then:
            first.waitUntilExecuted()
            last.waitUntilExecuted()
            executionOrder == [first, last]
    }

    void 'all StoppedEvent listeners should be executed even though some listener thrown exception'() {
        given:
            AwaitingConsumer first = new AwaitingConsumer({ throw new RuntimeException('Deliberate exception') })
            AwaitingConsumer last = new AwaitingConsumer()
            channel.eventBus().addListener(Stopped, first)
            channel.eventBus().addListener(Stopped, last)
            channel.start()
        when:
            channel.stop()
        then:
            first.waitUntilExecuted()
            last.waitUntilExecuted()
    }


    void 'removed StartedEvent listeners does not receive notifications'() {
        given:
            ConsumeOneMessage first = new ConsumeOneMessage()
            AwaitingConsumer last = new AwaitingConsumer()
            channel.eventBus().addListener(Started, first)
            channel.eventBus().addListener(Started, last)
        when:
            channel.eventBus().removeListener(Started, first)
            channel.start()
        then:
            last.waitUntilExecuted()
            !first.messageReceived()
    }

    void 'removed StoppedEvent listeners does not receive notifications'() {
        given:
            ConsumeOneMessage first = new ConsumeOneMessage()
            AwaitingConsumer last = new AwaitingConsumer()
            channel.eventBus().addListener(Stopped, first)
            channel.eventBus().addListener(Stopped, last)
            channel.start()
        when:
            channel.eventBus().removeListener(Stopped, first)
            channel.stop()
        then:
            last.waitUntilExecuted()
            !first.messageReceived()
    }

}
