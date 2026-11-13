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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.transports.http.configuration.ConnectionType;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.junit.jupiter.api.Test;
import tools.jackson.jakarta.rs.json.JacksonJsonProvider;

class SyncopeClientFactoryBeanTest {

    private static final String ADDRESS = "http://localhost";

    private static final String DOMAIN = "Master";

    @Test
    void defaults() {
        SyncopeClientFactoryBean factory = new SyncopeClientFactoryBean();
        factory.setAddress(ADDRESS);

        assertNotNull(factory.getJsonProvider());
        assertInstanceOf(JacksonJsonProvider.class, factory.getJsonProvider());
        assertNotNull(factory.getExceptionMapper());
        assertInstanceOf(RestClientExceptionMapper.class, factory.getExceptionMapper());
        assertNull(factory.getDomain());
        assertFalse(factory.isUseCompression());
        assertNotNull(factory.getHttpClientPolicy());
        assertEquals(ConnectionType.CLOSE, factory.getHttpClientPolicy().getConnection());
        assertNull(factory.getTlsClientParameters());
        assertNotNull(factory.getRestClientFactoryBean());
    }

    @Test
    void settersAndGetters() {
        SyncopeClientFactoryBean factory = new SyncopeClientFactoryBean();

        JacksonJsonProvider jsonProvider = new JacksonJsonProvider();
        factory.setJsonProvider(jsonProvider);
        assertSame(jsonProvider, factory.getJsonProvider());

        RestClientExceptionMapper exceptionMapper = new RestClientExceptionMapper();
        factory.setExceptionMapper(exceptionMapper);
        assertSame(exceptionMapper, factory.getExceptionMapper());

        factory.setAddress(ADDRESS);
        assertEquals(ADDRESS, factory.getAddress());

        factory.setDomain(DOMAIN);
        assertEquals(DOMAIN, factory.getDomain());

        factory.setUseCompression(true);
        assertTrue(factory.isUseCompression());

        HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();
        factory.setHttpClientPolicy(httpClientPolicy);
        assertSame(httpClientPolicy, factory.getHttpClientPolicy());

        TLSClientParameters tlsClientParameters = new TLSClientParameters();
        factory.setTlsClientParameters(tlsClientParameters);
        assertSame(tlsClientParameters, factory.getTlsClientParameters());

        JAXRSClientFactoryBean restClientFactoryBean = new JAXRSClientFactoryBean();
        factory.setRestClientFactoryBean(restClientFactoryBean);
        assertSame(restClientFactoryBean, factory.getRestClientFactoryBean());
    }

    @Test
    void defaultRestClientFactoryBeanRequiresAddress() {
        SyncopeClientFactoryBean factory = new SyncopeClientFactoryBean();

        assertThrows(IllegalArgumentException.class, factory::getRestClientFactoryBean);
    }

    @Test
    void defaultRestClientFactoryBean() {
        SyncopeClientFactoryBean factory = new SyncopeClientFactoryBean();
        factory.setAddress(ADDRESS);
        factory.setDomain(DOMAIN);

        JAXRSClientFactoryBean bean = factory.getRestClientFactoryBean();

        assertNotNull(bean);
        assertEquals(ADDRESS, bean.getAddress());
        assertNotNull(bean.getHeaders());
        assertEquals(DOMAIN, bean.getHeaders().get(org.apache.syncope.common.rest.api.RESTHeaders.DOMAIN).
                getFirst());
        assertNotNull(bean.getFeatures());
        assertEquals(1, bean.getFeatures().size());
        assertInstanceOf(LoggingFeature.class, bean.getFeatures().get(0));
        assertNotNull(bean.getProviders());
    }

    @Test
    void createBasicAuthenticationHandler() {
        SyncopeClientFactoryBean factory = new SyncopeClientFactoryBean();
        factory.setAddress(ADDRESS);

        SyncopeClient client = factory.create(new BasicAuthenticationHandler("user", "password"));

        assertNotNull(client);
        assertEquals(ADDRESS, client.getAddress());
    }

    @Test
    void createJWT() {
        SyncopeClientFactoryBean factory = new SyncopeClientFactoryBean();
        factory.setAddress(ADDRESS);

        SyncopeClient client = factory.create("token");

        assertNotNull(client);
        assertEquals(ADDRESS, client.getAddress());
    }

    @Test
    void createAnonymous() {
        SyncopeClientFactoryBean factory = new SyncopeClientFactoryBean();
        factory.setAddress(ADDRESS);

        SyncopeAnonymousClient client = factory.createAnonymous("user", "password");

        assertNotNull(client);
        assertEquals(ADDRESS, client.getAddress());
    }
}
