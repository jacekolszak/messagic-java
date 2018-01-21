package com.github.jacekolszak.messagic.streams

import com.github.jacekolszak.messagic.Error
import com.github.jacekolszak.messagic.Stopped
import spock.lang.Specification
import spock.lang.Subject

final class LimitsSpec extends Specification {

    private final StreamsPipedOutputStream inputPipe = new StreamsPipedOutputStream()
    private final PipedInputStream input = inputPipe.inputStream()
    private final StreamsPipedOutputStream output = new StreamsPipedOutputStream()
    private final ConsumeOneMessage<Error> errorListener = new ConsumeOneMessage()
    private final AwaitingConsumer stoppedListener = new AwaitingConsumer()

    @Subject
    private StreamsMessageChannel channel

    void cleanup() {
        channel.stop()
    }

    void 'should close the channel when incoming text message is too big'() {
        given:
            Limits limits = new Limits(textMessageMaximumSize: 1)
            channel = new StreamsMessageChannel(input, output, limits)
            channel.events().addListener(Error, errorListener)
            channel.events().addListener(Stopped, stoppedListener)
            channel.start()
        when:
            inputPipe.writeTextMessage('ab')
        then:
            errorListener.message().exception() instanceof StreamsMessageChannelException
            stoppedListener.waitUntilExecuted()
    }

    void 'should close the channel when incoming binary message is too big'() {
        given:
            Limits limits = new Limits(binaryMessageMaximumSize: 1)
            channel = new StreamsMessageChannel(input, output, limits)
            channel.events().addListener(Error, errorListener)
            channel.events().addListener(Stopped, stoppedListener)
            channel.start()
        when:
            inputPipe.writeBinaryMessage('AQI=')
        then:
            errorListener.message().exception() instanceof StreamsMessageChannelException
            stoppedListener.waitUntilExecuted()
    }

    void 'should close the channel when outgoing text message is too big'() {
        given:
            Limits limits = new Limits(textMessageMaximumSize: 1)
            channel = new StreamsMessageChannel(input, output, limits)
            channel.events().addListener(Error, errorListener)
            channel.events().addListener(Stopped, stoppedListener)
            channel.start()
        when:
            channel.send('ab')
        then:
            errorListener.message().exception() instanceof StreamsMessageChannelException
            stoppedListener.waitUntilExecuted()
    }

    void 'should close the channel when outgoing binary message is too big'() {
        given:
            Limits limits = new Limits(binaryMessageMaximumSize: 1)
            channel = new StreamsMessageChannel(input, output, limits)
            channel.events().addListener(Error, errorListener)
            channel.events().addListener(Stopped, stoppedListener)
            channel.start()
        when:
            channel.send([1, 2] as byte[])
        then:
            errorListener.message().exception() instanceof StreamsMessageChannelException
            stoppedListener.waitUntilExecuted()
    }

}