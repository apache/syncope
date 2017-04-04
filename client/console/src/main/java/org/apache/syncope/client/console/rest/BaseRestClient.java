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

import java.net.URI;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.search.OrderByClauseBuilder;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.JAXRSService;
import org.apache.syncope.common.rest.api.service.SyncopeService;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseRestClient implements RestClient {

    protected static final Logger LOG = LoggerFactory.getLogger(BaseRestClient.class);

    private static final long serialVersionUID = 1523999867826481989L;

    public static SyncopeService getSyncopeService() {
        return getService(SyncopeService.class);
    }

    protected static <T> T getService(final Class<T> serviceClass) {
        return SyncopeConsoleSession.get().getService(serviceClass);
    }

    protected static <T> T getService(final String etag, final Class<T> serviceClass) {
        return SyncopeConsoleSession.get().getService(etag, serviceClass);
    }

    protected static <T> void resetClient(final Class<T> serviceClass) {
        SyncopeConsoleSession.get().resetClient(serviceClass);
    }

    protected static String toOrderBy(final SortParam<String> sort) {
        OrderByClauseBuilder builder = SyncopeClient.getOrderByClauseBuilder();

        String property = sort.getProperty();
        if (property.indexOf('#') != -1) {
            property = property.substring(property.indexOf('#') + 1);
        }

        if (sort.isAscending()) {
            builder.asc(property);
        } else {
            builder.desc(property);
        }

        return builder.build();
    }

    protected static <E extends JAXRSService, T> T getObject(
            final E service, final URI location, final Class<T> resultClass) {

        WebClient webClient = WebClient.fromClient(WebClient.client(service));
        webClient.accept(SyncopeConsoleSession.get().getMediaType()).to(location.toASCIIString(), false);
        return webClient.
                header(RESTHeaders.DOMAIN, SyncopeConsoleSession.get().getDomain()).
                header(RESTHeaders.TOKEN, SyncopeConsoleSession.get().getJWT()).
                get(resultClass);
    }
}
