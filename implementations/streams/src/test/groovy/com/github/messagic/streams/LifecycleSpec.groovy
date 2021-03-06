/*
 * Copyright 2018 The Messagic Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.messagic.streams

import com.github.messagic.Event
import com.github.messagic.Started
import com.github.messagic.Stopped
import spock.lang.Specification
import spock.lang.Subject

final class LifecycleSpec extends Specification {

    private final BlockingQueueInputStream inputStream = new BlockingQueueInputStream()
    private final BlockingQueueOutputStream outputStream = new BlockingQueueOutputStream()

    @Subject
    private StreamsMessageChannel channel = new StreamsMessageChannel(inputStream, outputStream)

    void cleanup() {
        channel.stop()
    }

    void 'after start should notify StartedEvent listener'() {
        given:
            ConsumeOneMessage listener = new ConsumeOneMessage()
            channel.addListener(Started, listener)
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
            channel.addListener(Started, first)
            channel.addListener(Started, last)
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
            channel.addListener(Started, first)
            channel.addListener(Started, last)
        when:
            channel.start()
        then:
            first.waitUntilExecuted()
            last.waitUntilExecuted()
    }

    void 'after stop should notify StoppedEvent listener'() {
        given:
            ConsumeOneMessage listener = new ConsumeOneMessage()
            channel.addListener(Stopped, listener)
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
            channel.addListener(Stopped, first)
            channel.addListener(Stopped, last)
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
            channel.addListener(Stopped, first)
            channel.addListener(Stopped, last)
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
            channel.addListener(Started, first)
            channel.addListener(Started, last)
        when:
            channel.removeListener(Started, first)
            channel.start()
        then:
            last.waitUntilExecuted()
            !first.messageReceived()
    }

    void 'removed StoppedEvent listeners does not receive notifications'() {
        given:
            ConsumeOneMessage first = new ConsumeOneMessage()
            AwaitingConsumer last = new AwaitingConsumer()
            channel.addListener(Stopped, first)
            channel.addListener(Stopped, last)
            channel.start()
        when:
            channel.removeListener(Stopped, first)
            channel.stop()
        then:
            last.waitUntilExecuted()
            !first.messageReceived()
    }

    void 'cant start channel when it was stopped'() {
        given:
            channel.start()
            channel.stop()
        when:
            channel.start()
        then:
            thrown(IllegalStateException)
    }

    void 'should close the channel when InputStream is closed'() {
        given:
            AwaitingConsumer stoppedListener = new AwaitingConsumer()
            channel.addListener(Stopped, stoppedListener)
            channel.start()
        when:
            inputStream.close()
        then:
            stoppedListener.waitUntilExecuted()
    }

    void 'should close the channel when OutputStream is closed'() {
        given:
            AwaitingConsumer stoppedListener = new AwaitingConsumer()
            channel.addListener(Stopped, stoppedListener)
            channel.start()
        when:
            outputStream.close()
            channel.send('a')
        then:
            stoppedListener.waitUntilExecuted()
    }

    void 'should throw RuntimeException when adding listener for invalid event type'() {
        when:
            channel.addListener(Event, {})
        then:
            thrown(RuntimeException)
    }

    void 'should throw RuntimeException when removinglistener for invalid event type'() {
        when:
            channel.removeListener(Event, {})
        then:
            thrown(RuntimeException)
    }

}
