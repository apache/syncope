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
package org.apache.syncope.wa.bootstrap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

@Configuration(value = "restfulCloudConfigBootstrapConfiguration", proxyBeanMethods = false)
public class RestfulCloudConfigBootstrapConfiguration implements PropertySourceLocator {

    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    @Override
    public PropertySource<?> locate(final Environment environment) {
        try {
            String content = WebClient.create(URI.create("https://demo5926981.mockable.io/casproperties")).
                    accept(MediaType.APPLICATION_JSON_TYPE).
                    get().
                    readEntity(String.class);

            Map<String, Object> payload = MAPPER.readValue(content, new TypeReference<Map<String, Object>>() {
            });
            return new MapPropertySource(getClass().getName(), payload);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to fetch settings", e);
        }
    }
}
