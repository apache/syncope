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
package org.apache.syncope.sra.actuate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.AMSession;
import org.apache.syncope.sra.SessionConfig;
import org.apache.syncope.sra.security.cas.CASAuthenticationToken;
import org.apache.syncope.sra.security.saml2.SAML2AuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.session.MapSession;

@Endpoint(id = "sraSessions")
public class SRASessions {

    private static final Logger LOG = LoggerFactory.getLogger(SRASessions.class);

    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    private final CacheManager cacheManager;

    public SRASessions(final CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    private static AMSession map(final MapSession mapSession) {
        SecurityContext ctx = mapSession.getAttribute(
                WebSessionServerSecurityContextRepository.DEFAULT_SPRING_SECURITY_CONTEXT_ATTR_NAME);
        if (ctx == null) {
            return null;
        }

        AMSession session = new AMSession();
        session.setKey(mapSession.getId());
        session.setAuthenticationDate(new Date(mapSession.getCreationTime().toEpochMilli()));

        String principal;
        if (ctx.getAuthentication() instanceof SAML2AuthenticationToken) {
            principal = ((SAML2AuthenticationToken) ctx.getAuthentication()).getPrincipal().getNameId().getValue();
        } else if (ctx.getAuthentication() instanceof CASAuthenticationToken) {
            principal = ((CASAuthenticationToken) ctx.getAuthentication()).getPrincipal().getPrincipal().getName();
        } else if (ctx.getAuthentication() instanceof OAuth2AuthenticationToken) {
            principal = ((OAuth2AuthenticationToken) ctx.getAuthentication()).getPrincipal().getName();
        } else {
            principal = ctx.getAuthentication().getPrincipal().toString();
        }
        session.setPrincipal(principal);

        try {
            session.setJson(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(ctx.getAuthentication()));
        } catch (JsonProcessingException e) {
            LOG.error("While serializing session {}", mapSession.getId(), e);
        }

        return session;
    }

    @ReadOperation
    @SuppressWarnings("unchecked")
    public List<AMSession> list() {
        return ((ConcurrentMap<Object, Object>) cacheManager.getCache(SessionConfig.DEFAULT_CACHE).getNativeCache()).
                values().stream().map(MapSession.class::cast).map(SRASessions::map).
                filter(Objects::nonNull).collect(Collectors.toList());
    }

    @ReadOperation
    public AMSession read(@Selector final String id) {
        Cache.ValueWrapper value = cacheManager.getCache(SessionConfig.DEFAULT_CACHE).get(id);
        if (value == null || !(value.get() instanceof MapSession)) {
            return null;
        }

        return map((MapSession) value.get());
    }

    @DeleteOperation
    public void delete(@Selector final String id) {
        cacheManager.getCache(SessionConfig.DEFAULT_CACHE).evict(id);
    }
}
