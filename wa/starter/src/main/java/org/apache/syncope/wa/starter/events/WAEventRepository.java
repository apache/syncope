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
package org.apache.syncope.wa.starter.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.to.AuditEventTO;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.syncope.common.rest.api.service.AuditService;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.apereo.cas.support.events.CasEventRepositoryFilter;
import org.apereo.cas.support.events.dao.AbstractCasEventRepository;
import org.apereo.cas.support.events.dao.CasEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WAEventRepository extends AbstractCasEventRepository {

    private static final Logger LOG = LoggerFactory.getLogger(WAEventRepository.class);

    private static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    private final WARestClient waRestClient;

    public WAEventRepository(
            final CasEventRepositoryFilter eventRepositoryFilter,
            final WARestClient restClient) {

        super(eventRepositoryFilter);
        this.waRestClient = restClient;
    }

    public void put(final Map<String, String> properties, final String key, final String value) {
        if (StringUtils.isNotBlank(value)) {
            properties.put(key, value);
        }
    }

    @Override
    public CasEvent saveInternal(final CasEvent event) {
        if (!waRestClient.isReady()) {
            LOG.debug("Syncope client is not yet ready to store audit record");
            return null;
        }

        LOG.debug("Saving WA events");
        try {
            Map<String, String> properties = new HashMap<>();
            if (event.getGeoLocation() != null) {
                put(properties, "geoLatitude", event.getGeoLocation().getLatitude());
                put(properties, "geoLongitude", event.getGeoLocation().getLongitude());
                put(properties, "geoAccuracy", event.getGeoLocation().getAccuracy());
                put(properties, "geoTimestamp", event.getGeoLocation().getTimestamp());
            }
            put(properties, "clientIpAddress", event.getClientIpAddress());
            put(properties, "serverIpAddress", event.getServerIpAddress());

            String output = MAPPER.writeValueAsString(properties);

            AuditEventTO auditEvent = new AuditEventTO();
            auditEvent.setWho(event.getPrincipalId());
            if (event.getTimestamp() != null) {
                auditEvent.setWhen(OffsetDateTime.ofInstant(
                        Instant.ofEpochMilli(event.getTimestamp()), ZoneId.systemDefault()));
            }
            auditEvent.setOutput(output);
            OpEvent opEvent = new OpEvent(
                    OpEvent.CategoryType.WA,
                    null,
                    event.getType().toUpperCase(),
                    String.valueOf(event.getId()),
                    OpEvent.Outcome.SUCCESS);
            auditEvent.setOpEvent(opEvent);
            waRestClient.getService(AuditService.class).create(auditEvent);
        } catch (JsonProcessingException e) {
            LOG.error("During serialization", e);
        }
        return event;
    }

    @Override
    public Stream<? extends CasEvent> load() {
        throw new UnsupportedOperationException("Fetching authentication events from WA is not supported");
    }

    @Override
    public void removeAll() {
        throw new UnsupportedOperationException("Removing authentication events from WA is not supported");
    }
}
