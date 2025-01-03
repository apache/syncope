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
package org.apache.syncope.client.lib;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.syncope.common.lib.info.NumbersInfo;
import org.apache.syncope.common.lib.info.PlatformInfo;
import org.apache.syncope.common.lib.info.SystemInfo;
import org.apache.syncope.common.rest.api.RESTHeaders;

public class SyncopeAnonymousClient extends SyncopeClient {

    protected final AnonymousAuthenticationHandler anonymousAuthHandler;

    public SyncopeAnonymousClient(
            final MediaType mediaType,
            final JAXRSClientFactoryBean restClientFactory,
            final RestClientExceptionMapper exceptionMapper,
            final AnonymousAuthenticationHandler anonymousAuthHandler,
            final boolean useCompression,
            final HTTPClientPolicy httpClientPolicy,
            final TLSClientParameters tlsClientParameters) {

        super(
                mediaType,
                restClientFactory,
                exceptionMapper,
                anonymousAuthHandler,
                useCompression,
                httpClientPolicy,
                tlsClientParameters);
        this.anonymousAuthHandler = anonymousAuthHandler;
    }

    public JsonNode info() throws IOException {
        WebClient webClient = WebClientBuilder.build(
                StringUtils.removeEnd(restClientFactory.getAddress().replace("/rest", "/actuator/info"), "/")).
                accept(MediaType.APPLICATION_JSON_TYPE).
                header(RESTHeaders.DOMAIN, getDomain()).
                header(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString(
                        (anonymousAuthHandler.getUsername() + ":" + anonymousAuthHandler.getPassword()).getBytes()));

        return MAPPER.readTree((InputStream) webClient.get().getEntity());
    }

    public Pair<String, String> gitAndBuildInfo() {
        try {
            JsonNode info = info();
            return Pair.of(
                    info.has("git") && info.get("git").has("commit") && info.get("git").get("commit").has("id")
                    ? info.get("git").get("commit").get("id").asText()
                    : StringUtils.EMPTY,
                    info.get("build").get("version").asText());
        } catch (IOException e) {
            throw new RuntimeException("While getting build and git Info", e);
        }
    }

    public PlatformInfo platform() {
        try {
            return MAPPER.treeToValue(info().get("platform"), PlatformInfo.class);
        } catch (IOException e) {
            throw new RuntimeException("While getting Platform Info", e);
        }
    }

    public SystemInfo system() {
        try {
            return MAPPER.treeToValue(info().get("system"), SystemInfo.class);
        } catch (IOException e) {
            throw new RuntimeException("While getting System Info", e);
        }
    }

    public NumbersInfo numbers() {
        try {
            return MAPPER.treeToValue(info().get("numbers"), NumbersInfo.class);
        } catch (IOException e) {
            throw new RuntimeException("While getting Numbers Info", e);
        }
    }
}
