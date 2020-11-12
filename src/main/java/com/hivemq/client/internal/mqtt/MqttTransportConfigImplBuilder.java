/*
 * Copyright 2018-present HiveMQ and the HiveMQ Community
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

package com.hivemq.client.internal.mqtt;

import com.hivemq.client.internal.util.Checks;
import com.hivemq.client.internal.util.InetSocketAddressUtil;
import com.hivemq.client.mqtt.MqttProxyConfig;
import com.hivemq.client.mqtt.MqttTlsConfig;
import com.hivemq.client.mqtt.MqttTransportConfigBuilder;
import com.hivemq.client.mqtt.MqttWebSocketConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @author Silvio Giebl
 */
public abstract class MqttTransportConfigImplBuilder<B extends MqttTransportConfigImplBuilder<B>> {

    private @Nullable InetSocketAddress serverAddress;
    private @NotNull Object serverHost = MqttTransportConfigImpl.DEFAULT_SERVER_HOST; // String or InetAddress
    private int serverPort = -1;
    private @Nullable InetSocketAddress localAddress;
    private @Nullable MqttTlsConfigImpl tlsConfig;
    private @Nullable MqttWebSocketConfigImpl webSocketConfig;
    private @Nullable MqttProxyConfigImpl proxyConfig;
    private int socketConnectTimeoutMs = MqttTransportConfigImpl.DEFAULT_SOCKET_CONNECT_TIMEOUT_MS;
    private int mqttConnectTimeoutMs = MqttTransportConfigImpl.DEFAULT_MQTT_CONNECT_TIMEOUT_MS;

    MqttTransportConfigImplBuilder() {}

    MqttTransportConfigImplBuilder(final @NotNull MqttTransportConfigImpl transportConfig) {
        set(transportConfig);
    }

    MqttTransportConfigImplBuilder(final @NotNull MqttTransportConfigImplBuilder<?> builder) {
        serverAddress = builder.serverAddress;
        serverHost = builder.serverHost;
        serverPort = builder.serverPort;
        localAddress = builder.localAddress;
        tlsConfig = builder.tlsConfig;
        webSocketConfig = builder.webSocketConfig;
        proxyConfig = builder.proxyConfig;
        socketConnectTimeoutMs = builder.socketConnectTimeoutMs;
        mqttConnectTimeoutMs = builder.mqttConnectTimeoutMs;
    }

    void set(final @NotNull MqttTransportConfigImpl transportConfig) {
        serverAddress = transportConfig.getServerAddress();
        localAddress = transportConfig.getRawLocalAddress();
        tlsConfig = transportConfig.getRawTlsConfig();
        webSocketConfig = transportConfig.getRawWebSocketConfig();
        proxyConfig = transportConfig.getRawProxyConfig();
        socketConnectTimeoutMs = transportConfig.getSocketConnectTimeoutMs();
        mqttConnectTimeoutMs = transportConfig.getMqttConnectTimeoutMs();
    }

    abstract @NotNull B self();

    public @NotNull B serverAddress(final @Nullable InetSocketAddress address) {
        this.serverAddress = Checks.notNull(address, "Server address");
        return self();
    }

    public @NotNull B serverHost(final @Nullable String host) {
        setServerHost(Checks.notEmpty(host, "Server host"));
        return self();
    }

    public @NotNull B serverHost(final @Nullable InetAddress host) {
        setServerHost(Checks.notNull(host, "Server host"));
        return self();
    }

    private void setServerHost(final @NotNull Object host) {
        serverHost = host;
        if (serverAddress != null) {
            serverPort = serverAddress.getPort();
            serverAddress = null;
        }
    }

    public @NotNull B serverPort(final int port) {
        this.serverPort = Checks.unsignedShort(port, "Server port");
        if (serverAddress != null) {
            final InetAddress inetAddress = serverAddress.getAddress();
            if (inetAddress != null) {
                serverHost = inetAddress;
            } else {
                serverHost = serverAddress.getHostString();
            }
            serverAddress = null;
        }
        return self();
    }

    public @NotNull B localAddress(final @Nullable InetSocketAddress address) {
        if (address == null) {
            localAddress = null;
        } else {
            localAddress = checkLocalAddress(address);
        }
        return self();
    }

    public @NotNull B localAddress(final @Nullable String address) {
        if (address == null) {
            removeLocalAddress();
        } else {
            localAddress = checkLocalAddress(new InetSocketAddress(address, getLocalPort()));
        }
        return self();
    }

    public @NotNull B localAddress(final @Nullable InetAddress address) {
        if (address == null) {
            removeLocalAddress();
        } else {
            localAddress = new InetSocketAddress(address, getLocalPort());
        }
        return self();
    }

    private @NotNull InetSocketAddress checkLocalAddress(final @NotNull InetSocketAddress address) {
        if (address.isUnresolved()) {
            throw new IllegalArgumentException("Local bind address must not be unresolved.");
        }
        return address;
    }

    private void removeLocalAddress() {
        if ((localAddress != null) && (localAddress.getAddress() != null)) {
            if (localAddress.getPort() == 0) {
                localAddress = null;
            } else {
                localAddress = new InetSocketAddress(localAddress.getPort());
            }
        }
    }

    private int getLocalPort() {
        return (localAddress == null) ? 0 : localAddress.getPort();
    }

    public @NotNull B localPort(final int port) {
        if (port == 0) {
            if ((localAddress != null) && (localAddress.getPort() != 0)) {
                if (localAddress.getAddress() == null) {
                    localAddress = null;
                } else {
                    localAddress = new InetSocketAddress(localAddress.getAddress(), 0);
                }
            }
        } else {
            localAddress = new InetSocketAddress((localAddress == null) ? null : localAddress.getAddress(), port);
        }
        return self();
    }

    public @NotNull B tlsWithDefaultConfig() {
        this.tlsConfig = MqttTlsConfigImpl.DEFAULT;
        return self();
    }

    public @NotNull B tlsConfig(final @Nullable MqttTlsConfig tlsConfig) {
        this.tlsConfig = Checks.notImplementedOrNull(tlsConfig, MqttTlsConfigImpl.class, "TLS config");
        return self();
    }

    public MqttTlsConfigImplBuilder.@NotNull Nested<B> tlsConfig() {
        return new MqttTlsConfigImplBuilder.Nested<>(tlsConfig, this::tlsConfig);
    }

    public @NotNull B webSocketWithDefaultConfig() {
        this.webSocketConfig = MqttWebSocketConfigImpl.DEFAULT;
        return self();
    }

    public @NotNull B webSocketConfig(final @Nullable MqttWebSocketConfig webSocketConfig) {
        this.webSocketConfig =
                Checks.notImplementedOrNull(webSocketConfig, MqttWebSocketConfigImpl.class, "WebSocket config");
        return self();
    }

    public MqttWebSocketConfigImplBuilder.@NotNull Nested<B> webSocketConfig() {
        return new MqttWebSocketConfigImplBuilder.Nested<>(webSocketConfig, this::webSocketConfig);
    }

    public @NotNull B proxyConfig(final @Nullable MqttProxyConfig proxyConfig) {
        this.proxyConfig = Checks.notImplementedOrNull(proxyConfig, MqttProxyConfigImpl.class, "Proxy config");
        return self();
    }

    public MqttProxyConfigImplBuilder.@NotNull Nested<B> proxyConfig() {
        return new MqttProxyConfigImplBuilder.Nested<>(proxyConfig, this::proxyConfig);
    }

    public @NotNull B socketConnectTimeout(final long timeout, final @Nullable TimeUnit timeUnit) {
        Checks.notNull(timeUnit, "Time unit");
        this.socketConnectTimeoutMs = (int) Checks.range(timeUnit.toMillis(timeout), 0, Integer.MAX_VALUE,
                "Socket connect timeout in milliseconds");
        return self();
    }

    public @NotNull B mqttConnectTimeout(final long timeout, final @Nullable TimeUnit timeUnit) {
        Checks.notNull(timeUnit, "Time unit");
        this.mqttConnectTimeoutMs = (int) Checks.range(timeUnit.toMillis(timeout), 0, Integer.MAX_VALUE,
                "MQTT connect timeout in milliseconds");
        return self();
    }

    private @NotNull InetSocketAddress getServerAddress() {
        if (serverAddress != null) {
            return serverAddress;
        }
        if (serverHost instanceof InetAddress) {
            return new InetSocketAddress((InetAddress) serverHost, getServerPort());
        }
        return InetSocketAddressUtil.create((String) serverHost, getServerPort());
    }

    private int getServerPort() {
        if (serverPort != -1) {
            return serverPort;
        }
        if (tlsConfig == null) {
            if (webSocketConfig == null) {
                return MqttTransportConfigImpl.DEFAULT_SERVER_PORT;
            }
            return MqttTransportConfigImpl.DEFAULT_SERVER_PORT_WEBSOCKET;
        }
        if (webSocketConfig == null) {
            return MqttTransportConfigImpl.DEFAULT_SERVER_PORT_TLS;
        }
        return MqttTransportConfigImpl.DEFAULT_SERVER_PORT_WEBSOCKET_TLS;
    }

    @NotNull MqttTransportConfigImpl buildTransportConfig() {
        return new MqttTransportConfigImpl(getServerAddress(), localAddress, tlsConfig, webSocketConfig, proxyConfig,
                socketConnectTimeoutMs, mqttConnectTimeoutMs);
    }

    public static class Default extends MqttTransportConfigImplBuilder<Default> implements MqttTransportConfigBuilder {

        public Default() {}

        Default(final @NotNull MqttTransportConfigImpl transportConfig) {
            super(transportConfig);
        }

        @Override
        @NotNull Default self() {
            return this;
        }

        @Override
        public @NotNull MqttTransportConfigImpl build() {
            return buildTransportConfig();
        }
    }

    public static class Nested<P> extends MqttTransportConfigImplBuilder<Nested<P>>
            implements MqttTransportConfigBuilder.Nested<P> {

        private final @NotNull Function<? super MqttTransportConfigImpl, P> parentConsumer;

        public Nested(
                final @NotNull MqttTransportConfigImpl transportConfig,
                final @NotNull Function<? super MqttTransportConfigImpl, P> parentConsumer) {

            super(transportConfig);
            this.parentConsumer = parentConsumer;
        }

        Nested(
                final @NotNull MqttTransportConfigImplBuilder<?> builder,
                final @NotNull Function<? super MqttTransportConfigImpl, P> parentConsumer) {

            super(builder);
            this.parentConsumer = parentConsumer;
        }

        @Override
        @NotNull Nested<P> self() {
            return this;
        }

        @Override
        public @NotNull P applyTransportConfig() {
            return parentConsumer.apply(buildTransportConfig());
        }
    }
}
