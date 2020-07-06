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
package org.apache.syncope.sra.filters;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory.NameConfig;
import org.springframework.http.server.reactive.ServerHttpRequest;

public class ClientCertsToRequestHeaderFilterFactory extends AbstractGatewayFilterFactory<NameConfig> {

    private static final Logger LOG = LoggerFactory.getLogger(ClientCertsToRequestHeaderFilterFactory.class);

    public ClientCertsToRequestHeaderFilterFactory() {
        super(NameConfig.class);
    }

    @Override
    public GatewayFilter apply(final NameConfig config) {
        return (exchange, chain) -> {
            ServerHttpRequest originalRequest = exchange.getRequest();

            ServerHttpRequest mutatedRequest;
            if (originalRequest.getSslInfo() != null
                    && ArrayUtils.isNotEmpty(originalRequest.getSslInfo().getPeerCertificates())) {

                LOG.debug("Client certificates found in original request: {}",
                        originalRequest.getSslInfo().getPeerCertificates().length);

                List<String> certs = new ArrayList<>();
                for (X509Certificate cert : originalRequest.getSslInfo().getPeerCertificates()) {
                    try {
                        certs.add(Base64.getEncoder().encodeToString(cert.getEncoded()));
                    } catch (CertificateEncodingException e) {
                        LOG.error("Could not encode one of client certificates", e);
                    }
                }

                mutatedRequest = originalRequest.mutate().
                        headers(headers -> headers.addAll(config.getName(), certs)).
                        sslInfo(null).
                        build();
            } else {
                mutatedRequest = originalRequest;
            }

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }
}
