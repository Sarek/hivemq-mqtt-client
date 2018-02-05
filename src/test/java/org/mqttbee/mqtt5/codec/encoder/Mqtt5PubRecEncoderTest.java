package org.mqttbee.mqtt5.codec.encoder;

import com.google.common.collect.ImmutableList;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.EncoderException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mqttbee.mqtt5.codec.Mqtt5DataTypes;
import org.mqttbee.mqtt5.message.Mqtt5UTF8StringImpl;
import org.mqttbee.mqtt5.message.Mqtt5UserPropertiesImpl;
import org.mqttbee.mqtt5.message.Mqtt5UserPropertyImpl;
import org.mqttbee.mqtt5.message.pubrec.Mqtt5PubRecImpl;
import org.mqttbee.mqtt5.message.pubrec.Mqtt5PubRecReasonCode;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.mqttbee.mqtt5.message.pubrec.Mqtt5PubRecReasonCode.SUCCESS;

/**
 * @author David Katz
 */
class Mqtt5PubRecEncoderTest extends AbstractMqtt5EncoderTest {

    Mqtt5PubRecEncoderTest() {
        super(true);
    }

    @Test
    void encode_simple() {
        final byte[] expected = {
                // fixed header
                //   type, flags
                0b0101_0000,
                //   remaining length
                3,
                // variable header
                //   packet identifier
                0, 5,
                // reason code
                (byte) 0x90
        };

        final Mqtt5PubRecReasonCode reasonCode = Mqtt5PubRecReasonCode.TOPIC_NAME_INVALID;
        final Mqtt5UTF8StringImpl reasonString = null;
        final Mqtt5UserPropertiesImpl userProperties = Mqtt5UserPropertiesImpl.NO_USER_PROPERTIES;
        final Mqtt5PubRecImpl pubRec = new Mqtt5PubRecImpl(5, reasonCode, reasonString, userProperties);

        encode(expected, pubRec);
    }

    @Test
    void encode_reasonCodeOmittedWhenSuccessWithoutProperties() {
        final byte[] expected = {
                // fixed header
                //   type, flags
                0b0101_0000,
                //   remaining length
                2,
                // variable header
                //   packet identifier
                0, 5
        };

        final Mqtt5PubRecImpl pubRec =
                new Mqtt5PubRecImpl(5, SUCCESS, null, Mqtt5UserPropertiesImpl.NO_USER_PROPERTIES);

        encode(expected, pubRec);
    }

    @ParameterizedTest
    @EnumSource(value = Mqtt5PubRecReasonCode.class, mode = EXCLUDE, names = {"SUCCESS"})
    void encode_reasonCodes(final Mqtt5PubRecReasonCode reasonCode) {
        final byte[] expected = {
                // fixed header
                //   type, flags
                0b0101_0000,
                //   remaining length
                3,
                // variable header
                //   packet identifier
                6, 5,
                //   reason code placeholder
                (byte) 0xFF
        };

        expected[4] = (byte) reasonCode.getCode();
        final Mqtt5PubRecImpl pubRec =
                new Mqtt5PubRecImpl(0x0605, reasonCode, null, Mqtt5UserPropertiesImpl.NO_USER_PROPERTIES);

        encode(expected, pubRec);
    }

    @Test
    void encode_reasonString() {
        final byte[] expected = {
                // fixed header
                //   type, flags
                0b0101_0000,
                //   remaining length
                13,
                // variable header
                //   packet identifier
                0, 9,
                //   reason code
                (byte) 0x90,
                //   properties
                9,
                // reason string
                0x1F, 0, 6, 'r', 'e', 'a', 's', 'o', 'n'
        };

        final Mqtt5PubRecReasonCode reasonCode = Mqtt5PubRecReasonCode.TOPIC_NAME_INVALID;
        final Mqtt5UTF8StringImpl reasonString = Mqtt5UTF8StringImpl.from("reason");
        final Mqtt5UserPropertiesImpl userProperties = Mqtt5UserPropertiesImpl.NO_USER_PROPERTIES;
        final Mqtt5PubRecImpl pubRec = new Mqtt5PubRecImpl(9, reasonCode, reasonString, userProperties);

        encode(expected, pubRec);
    }

    @Test
    void encode_userProperty() {
        final byte[] expected = {
                // fixed header
                //   type, flags
                0b0101_0000,
                //   remaining length
                17,
                // variable header
                //   packet identifier
                0, 5,
                //   reason code
                (byte) 0x90,
                //   properties
                13,
                // user Property
                0x26, 0, 3, 'k', 'e', 'y', 0, 5, 'v', 'a', 'l', 'u', 'e'
        };

        final Mqtt5PubRecReasonCode reasonCode = Mqtt5PubRecReasonCode.TOPIC_NAME_INVALID;
        final Mqtt5UserPropertiesImpl userProperties =
                Mqtt5UserPropertiesImpl.of(ImmutableList.of(new Mqtt5UserPropertyImpl(
                        requireNonNull(Mqtt5UTF8StringImpl.from("key")),
                        requireNonNull(Mqtt5UTF8StringImpl.from("value")))));
        final Mqtt5PubRecImpl pubRec = new Mqtt5PubRecImpl(5, reasonCode, null, userProperties);

        encode(expected, pubRec);
    }

    @Test
    @Disabled
    void encode_maximumPacketSizeExceeded_throwsEncoderException() {
        final MaximumPacketBuilder maxPacket = new MaximumPacketBuilder().build();
        final Mqtt5PubRecImpl pubRec = new Mqtt5PubRecImpl(1, SUCCESS, maxPacket.getMaxPaddedReasonString("a"),
                maxPacket.getMaxPossibleUserProperties());

        final Throwable exception = assertThrows(EncoderException.class, () -> channel.writeOutbound(pubRec));
        assertTrue(exception.getMessage().contains("variable byte integer size exceeded for remaining length"));
    }

    @Test
    @Disabled
    void encode_propertyLengthExceedsMax_throwsEncoderException() {
        final MaximumPacketBuilder maxPacket = new MaximumPacketBuilder().build();
        final Mqtt5PubRecImpl pubRec = new Mqtt5PubRecImpl(1, SUCCESS, maxPacket.getMaxPaddedReasonString(),
                maxPacket.getMaxPossibleUserProperties(1));

        final Throwable exception = assertThrows(EncoderException.class, () -> channel.writeOutbound(pubRec));
        assertTrue(exception.getMessage().contains("variable byte integer size exceeded for property length"));
    }


    private void encode(final byte[] expected, final Mqtt5PubRecImpl pubRec) {
        channel.writeOutbound(pubRec);
        final ByteBuf byteBuf = channel.readOutbound();

        final byte[] actual = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(actual);
        byteBuf.release();

        assertArrayEquals(expected, actual);
    }

    private class MaximumPacketBuilder {

        private StringBuilder reasonStringBuilder;
        private ImmutableList.Builder<Mqtt5UserPropertyImpl> userPropertiesBuilder;
        final Mqtt5UTF8StringImpl user = requireNonNull(Mqtt5UTF8StringImpl.from("user"));
        final Mqtt5UTF8StringImpl property = requireNonNull(Mqtt5UTF8StringImpl.from("property"));

        MaximumPacketBuilder build() {
            final int maxPropertyLength = Mqtt5DataTypes.MAXIMUM_PACKET_SIZE_LIMIT - 1  // type, reserved
                    - 4  // remaining length
                    - 4  // property length
                    - 2  // packet identifier
                    - 1; // reason code

            final int remainingBytes = maxPropertyLength - 3; // reason string identifier and length
            final int userPropertyBytes = 1 // identifier
                    + 2 // key length
                    + 4 // bytes to encode "user"
                    + 2 // value length
                    + 8; // bytes to encode "property"
            final int reasonStringBytes = remainingBytes % userPropertyBytes;

            reasonStringBuilder = new StringBuilder();
            for (int i = 0; i < reasonStringBytes; i++) {
                reasonStringBuilder.append(i);
            }

            final int numberOfUserProperties = remainingBytes / userPropertyBytes;
            final Mqtt5UserPropertyImpl userProperty = new Mqtt5UserPropertyImpl(user, property);
            userPropertiesBuilder = new ImmutableList.Builder<>();
            for (int i = 0; i < numberOfUserProperties; i++) {
                userPropertiesBuilder.add(userProperty);
            }
            return this;
        }

        Mqtt5UTF8StringImpl getMaxPaddedReasonString() {
            return getMaxPaddedReasonString("");
        }

        Mqtt5UTF8StringImpl getMaxPaddedReasonString(final String withSuffix) {
            return Mqtt5UTF8StringImpl.from(reasonStringBuilder.toString() + withSuffix);
        }

        Mqtt5UserPropertiesImpl getMaxPossibleUserProperties() {
            return getMaxPossibleUserProperties(0);
        }

        Mqtt5UserPropertiesImpl getMaxPossibleUserProperties(final int withExtraUserProperties) {
            for (int i = 0; i < withExtraUserProperties; i++) {
                userPropertiesBuilder.add(new Mqtt5UserPropertyImpl(user, property));
            }
            return Mqtt5UserPropertiesImpl.of(userPropertiesBuilder.build());
        }
    }
}