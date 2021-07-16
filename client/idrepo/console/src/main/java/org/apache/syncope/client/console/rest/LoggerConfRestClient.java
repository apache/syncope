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
package org.apache.syncope.client.console.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.ui.commons.rest.RestClient;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LogLevel;

public class LoggerConfRestClient implements RestClient, LoggerConfOp {

    private static final long serialVersionUID = 16051907544728L;

    private static final Logger LOG = LoggerFactory.getLogger(LoggerConfRestClient.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final List<NetworkService> instances;

    private final List<String> domains;

    public LoggerConfRestClient(final List<NetworkService> instances, final List<Domain> domains) {
        this.instances = instances;
        this.domains = Stream.concat(
                Stream.of(SyncopeConstants.MASTER_DOMAIN), domains.stream().map(Domain::getKey)).
                collect(Collectors.toList());
    }

    private String getActuatorEndpoint(final NetworkService instance) {
        String address = instance.getAddress();
        if (address.contains("/rest")) {
            address = address.replace("/rest", "");
        }
        return address + "actuator/loggers";
    }

    private WebClient webClient(final NetworkService instance) {
        return WebClient.create(
                getActuatorEndpoint(instance),
                SyncopeWebApplication.get().getAnonymousUser(),
                SyncopeWebApplication.get().getAnonymousKey(),
                null).
                accept(MediaType.APPLICATION_JSON_TYPE).
                type(MediaType.APPLICATION_JSON_TYPE);
    }

    @Override
    public List<LoggerConf> list() {
        List<LoggerConf> loggerConfs = new ArrayList<>();

        try {
            Response response = webClient(instances.get(0)).get();
            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                JsonNode node = MAPPER.readTree((InputStream) response.getEntity());
                if (node.has("loggers")) {
                    for (Iterator<Map.Entry<String, JsonNode>> itor = node.get("loggers").fields(); itor.hasNext();) {
                        Map.Entry<String, JsonNode> entry = itor.next();

                        LoggerConf loggerConf = new LoggerConf();
                        loggerConf.setKey(entry.getKey());
                        if (entry.getValue().has("effectiveLevel")) {
                            loggerConf.setLevel(LogLevel.valueOf(entry.getValue().get("effectiveLevel").asText()));
                        } else {
                            loggerConf.setLevel(LogLevel.OFF);
                        }

                        if (!loggerConf.getKey().startsWith(AuditLoggerName.AUDIT_PREFIX)
                                && domains.stream().noneMatch(domain -> loggerConf.getKey().startsWith(domain))) {

                            loggerConfs.add(loggerConf);
                        }
                    }
                }
            } else {
                LOG.error("Unexpected response for loggers from {}: {}",
                        getActuatorEndpoint(instances.get(0)), response.getStatus());
            }
        } catch (Exception e) {
            LOG.error("Could not fetch loggers from {}", getActuatorEndpoint(instances.get(0)), e);
        }

        loggerConfs.sort(Comparator.comparing(LoggerConf::getKey));
        return loggerConfs;
    }

    @Override
    public void setLevel(final String key, final LogLevel level) {
        instances.forEach(i -> webClient(i).path(key).post("{\"configuredLevel\": \"" + level.name() + "\"}"));
    }
}
