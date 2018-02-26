package org.mqttbee.mqtt.codec.encoder.provider;

import org.mqttbee.annotations.NotNull;
import org.mqttbee.mqtt.codec.encoder.MqttMessageEncoder;
import org.mqttbee.mqtt.message.MqttMessage;
import org.mqttbee.mqtt5.netty.NettyThreadLocals;

import java.util.function.Supplier;

/**
 * Provider for a {@link MqttMessageEncoderApplier}.
 *
 * @param <M> the type of the MQTT message.
 * @author Silvio Giebl
 */
public interface MqttMessageEncoderProvider<M extends MqttMessage> extends Supplier<MqttMessageEncoderApplier<M>> {

    /**
     * Provider for a thread local {@link MqttMessageEncoderApplier}.
     *
     * @param <M> the type of the MQTT message.
     */
    class ThreadLocalMqttMessageEncoderProvider<M extends MqttMessage>
            implements MqttMessageEncoderProvider<M>, MqttMessageEncoderApplier<M> {

        private final ThreadLocal<MqttMessageEncoderApplier<M>> threadLocal;

        public ThreadLocalMqttMessageEncoderProvider(
                @NotNull final Supplier<MqttMessageEncoderApplier<M>> supplier) {

            threadLocal = ThreadLocal.withInitial(supplier);
            NettyThreadLocals.register(threadLocal);
        }

        @Override
        public MqttMessageEncoderApplier<M> get() {
            return this;
        }

        @NotNull
        @Override
        public MqttMessageEncoder apply(@NotNull final M message) {
            return threadLocal.get().apply(message);
        }

    }

}