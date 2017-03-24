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
package org.apache.syncope.client.enduser;

import java.security.AccessControlException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.lib.AnonymousAuthenticationHandler;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.info.PlatformInfo;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.rest.api.beans.SchemaQuery;
import org.apache.syncope.common.rest.api.service.SchemaService;
import org.apache.syncope.common.rest.api.service.SyncopeService;
import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.Request;
import org.apache.wicket.util.cookies.CookieUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom Syncope Enduser Session class.
 */
public class SyncopeEnduserSession extends WebSession {

    private static final long serialVersionUID = 1284946129513378647L;

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeEnduserSession.class);

    private final SyncopeClient anonymousClient;

    private SyncopeClient client;

    private final PlatformInfo platformInfo;

    private final List<PlainSchemaTO> datePlainSchemas;

    private UserTO selfTO;

    private final CookieUtils cookieUtils;

    private boolean xsrfTokenGenerated = false;

    public static SyncopeEnduserSession get() {
        return (SyncopeEnduserSession) Session.get();
    }

    public SyncopeEnduserSession(final Request request) {
        super(request);
        // define cookie utility to manage application cookies
        cookieUtils = new CookieUtils();

        anonymousClient = SyncopeEnduserApplication.get().getClientFactory().
                create(new AnonymousAuthenticationHandler(
                        SyncopeEnduserApplication.get().getAnonymousUser(),
                        SyncopeEnduserApplication.get().getAnonymousKey()));
        platformInfo = anonymousClient.getService(SyncopeService.class).platform();

        datePlainSchemas = anonymousClient.getService(SchemaService.class).
                list(new SchemaQuery.Builder().type(SchemaType.PLAIN).build());
        CollectionUtils.filter(datePlainSchemas, new Predicate<PlainSchemaTO>() {

            @Override
            public boolean evaluate(final PlainSchemaTO object) {
                return object.getType() == AttrSchemaType.Date;
            }
        });
    }

    private void afterAuthentication() {
        Pair<Map<String, Set<String>>, UserTO> self = client.self();
        selfTO = self.getValue();

        // bind explicitly this session to have a stateful behavior during http requests, unless session will
        // expire for every request
        this.bind();
    }

    public boolean authenticate(final String username, final String password) {
        boolean authenticated = false;

        try {
            client = SyncopeEnduserApplication.get().getClientFactory().
                    setDomain(SyncopeEnduserApplication.get().getDomain()).
                    create(username, password);

            afterAuthentication();

            authenticated = true;
        } catch (Exception e) {
            LOG.error("Authentication failed", e);
        }

        return authenticated;
    }

    public boolean authenticate(final String jwt) {
        boolean authenticated = false;

        try {
            client = SyncopeEnduserApplication.get().getClientFactory().
                    setDomain(SyncopeEnduserApplication.get().getDomain()).create(jwt);

            afterAuthentication();

            authenticated = true;
        } catch (Exception e) {
            LOG.error("Authentication failed", e);
        }

        return authenticated;
    }

    @Override
    public void invalidate() {
        if (isAuthenticated()) {
            try {
                client.logout();
            } catch (AccessControlException e) {
                LOG.debug("Unexpected exception while logging out", e);
            } finally {
                client = null;
                selfTO = null;
            }
        }
        super.invalidate();
    }

    public <T> T getService(final Class<T> serviceClass) {
        return (client == null || !isAuthenticated())
                ? anonymousClient.getService(serviceClass)
                : client.getService(serviceClass);
    }

    public <T> T getService(final String etag, final Class<T> serviceClass) {
        T serviceInstance = getService(serviceClass);
        WebClient.client(serviceInstance).match(new EntityTag(etag), false).
                type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON);

        return serviceInstance;
    }

    public PlatformInfo getPlatformInfo() {
        return platformInfo;
    }

    public List<PlainSchemaTO> getDatePlainSchemas() {
        return datePlainSchemas;
    }

    public UserTO getSelfTO() {
        if (selfTO == null) {
            throw new IllegalArgumentException("User not authenticated");
        }
        return selfTO;
    }

    public boolean isAuthenticated() {
        return client != null && client.getJWT() != null;
    }

    public CookieUtils getCookieUtils() {
        return cookieUtils;
    }

    public boolean isXsrfTokenGenerated() {
        return xsrfTokenGenerated;
    }

    public void setXsrfTokenGenerated(final boolean xsrfTokenGenerated) {
        this.xsrfTokenGenerated = xsrfTokenGenerated;
    }
}
