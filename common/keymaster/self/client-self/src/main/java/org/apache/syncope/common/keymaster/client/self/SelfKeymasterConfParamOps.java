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
package org.apache.syncope.common.keymaster.client.self;

import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.keymaster.client.api.KeymasterException;
import org.apache.syncope.common.keymaster.rest.api.service.ConfParamService;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SelfKeymasterConfParamOps extends SelfKeymasterOps implements ConfParamOps {

    private static final Logger LOG = LoggerFactory.getLogger(ConfParamOps.class);

    private static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    public SelfKeymasterConfParamOps(final JAXRSClientFactoryBean clientFactory) {
        super(clientFactory);
    }

    @Override
    public Map<String, Object> list(final String domain) {
        return client(ConfParamService.class, Map.of(RESTHeaders.DOMAIN, domain)).list();
    }

    @Override
    public <T> T get(final String domain, final String key, final T defaultValue, final Class<T> reference) {
        Response response = client(ConfParamService.class, Map.of(RESTHeaders.DOMAIN, domain)).get(key);
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            return defaultValue;
        }
        try {
            return MAPPER.readValue(response.readEntity(InputStream.class), reference);
        } catch (IOException e) {
            LOG.error("Could not deserialize response", e);
            return defaultValue;
        }
    }

    @Override
    public <T> void set(final String domain, final String key, final T value) {
        if (value == null) {
            remove(domain, key);
        } else {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                MAPPER.writeValue(baos, value);

                client(ConfParamService.class, Map.of(RESTHeaders.DOMAIN, domain)).
                        set(key, new ByteArrayInputStream(baos.toByteArray()));
            } catch (IOException e) {
                throw new KeymasterException("Could not serialize " + value, e);
            }
        }
    }

    @Override
    public void remove(final String domain, final String key) {
        client(ConfParamService.class, Map.of(RESTHeaders.DOMAIN, domain)).remove(key);
    }
}
