/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.sra;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.security.cert.X509Certificate;
import java.util.Objects;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.config.HttpClientProperties;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.tcp.ProxyProvider;

@PropertySource("classpath:sra.properties")
@PropertySource(value = "file:${conf.directory}/sra.properties", ignoreResourceNotFound = true)
@EnableWebFluxSecurity
@SpringBootApplication
public class SyncopeSRAApplication implements EnvironmentAware {

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeSRAApplication.class);

    public static void main(final String[] args) {
        SpringApplication.run(SyncopeSRAApplication.class, args);
    }

    @Autowired
    private RouteProvider provider;

    private Environment env;

    @Override
    public void setEnvironment(final Environment env) {
        this.env = env;
    }

    @Bean
    public RouteLocator routes(final RouteLocatorBuilder builder) {
        return () -> Flux.fromIterable(provider.fetch()).map(Route.AbstractBuilder::build);
    }

    @Bean
    public SecurityWebFilterChain actuatorSecurityFilterChain(final ServerHttpSecurity http) {
        ServerWebExchangeMatcher actuatorMatcher = EndpointRequest.toAnyEndpoint();
        return http.securityMatcher(actuatorMatcher).
                authorizeExchange().anyExchange().authenticated().
                and().httpBasic().
                and().csrf().requireCsrfProtectionMatcher(new NegatedServerWebExchangeMatcher(actuatorMatcher)).
                and().build();
    }

    @Bean
    public MapReactiveUserDetailsService userDetailsService() {
        UserDetails user = User.builder().
                username(Objects.requireNonNull(env.getProperty("anonymousUser"))).
                password("{noop}" + env.getProperty("anonymousKey")).
                roles(IdRepoEntitlement.ANONYMOUS).
                build();
        return new MapReactiveUserDetailsService(user);
    }

    /**
     * Temp work around https://github.com/spring-cloud/spring-cloud-gateway/issues/1532 .
     *
     * @param properties Gateway HTTP Client properties
     * @return Gateway HTTP Client instance
     */
    @Bean
    public HttpClient gatewayHttpClient(final HttpClientProperties properties) {
        // configure pool resources
        HttpClientProperties.Pool pool = properties.getPool();

        ConnectionProvider connectionProvider;
        if (pool.getType() == HttpClientProperties.Pool.PoolType.DISABLED) {
            connectionProvider = ConnectionProvider.newConnection();
        } else if (pool.getType() == HttpClientProperties.Pool.PoolType.FIXED) {
            connectionProvider = ConnectionProvider.fixed(
                    pool.getName(), pool.getMaxConnections(), pool.getAcquireTimeout(), pool.getMaxIdleTime(), null);
        } else {
            connectionProvider = ConnectionProvider.elastic(pool.getName(), pool.getMaxIdleTime(), null);
        }

        HttpClient httpClient = HttpClient.create(connectionProvider).tcpConfiguration(tcpClient -> {
            if (properties.getConnectTimeout() != null) {
                tcpClient = tcpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getConnectTimeout());
            }

            // configure proxy if proxy host is set.
            HttpClientProperties.Proxy proxy = properties.getProxy();

            if (StringUtils.hasText(proxy.getHost())) {
                tcpClient = tcpClient.proxy(proxySpec -> {
                    ProxyProvider.Builder builder = proxySpec.type(ProxyProvider.Proxy.HTTP).host(proxy.getHost());

                    PropertyMapper map = PropertyMapper.get();

                    map.from(proxy::getPort).whenNonNull().to(builder::port);
                    map.from(proxy::getUsername).whenHasText().to(builder::username);
                    map.from(proxy::getPassword).whenHasText().to(password -> builder.password(s -> password));
                    map.from(proxy::getNonProxyHostsPattern).whenHasText().to(builder::nonProxyHosts);
                });
            }
            return tcpClient;
        });

        HttpClientProperties.Ssl ssl = properties.getSsl();
        if ((ssl.getKeyStore() != null && ssl.getKeyStore().length() > 0)
                || ssl.getTrustedX509CertificatesForTrustManager().length > 0
                || ssl.isUseInsecureTrustManager()) {

            httpClient = httpClient.secure(sslContextSpec -> {
                // configure ssl
                SslContextBuilder sslContextBuilder = SslContextBuilder.forClient();

                X509Certificate[] trustedX509Certificates = ssl.getTrustedX509CertificatesForTrustManager();
                if (trustedX509Certificates.length > 0) {
                    sslContextBuilder = sslContextBuilder.trustManager(trustedX509Certificates);
                } else if (ssl.isUseInsecureTrustManager()) {
                    sslContextBuilder = sslContextBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE);
                }

                try {
                    sslContextBuilder = sslContextBuilder.keyManager(ssl.getKeyManagerFactory());
                } catch (Exception e) {
                    LOG.error("During SSL configuration", e);
                }

                sslContextSpec.sslContext(sslContextBuilder).
                        defaultConfiguration(ssl.getDefaultConfigurationType()).
                        handshakeTimeout(ssl.getHandshakeTimeout()).
                        closeNotifyFlushTimeout(ssl.getCloseNotifyFlushTimeout()).
                        closeNotifyReadTimeout(ssl.getCloseNotifyReadTimeout());
            });
        }

        if (properties.isWiretap()) {
            httpClient = httpClient.wiretap(true);
        }

        return httpClient;
    }
}
