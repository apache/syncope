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

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.rest.api.service.AttrRepoService;
import org.apache.syncope.common.rest.api.service.AuthModuleService;
import org.apache.syncope.common.rest.api.service.wa.WAConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

@Order
public class WAPropertySourceLocator implements PropertySourceLocator {

    protected static final Logger LOG = LoggerFactory.getLogger(WAPropertySourceLocator.class);

    protected final WARestClient waRestClient;

    public WAPropertySourceLocator(final WARestClient waRestClient) {
        this.waRestClient = waRestClient;
    }

    @Override
    public PropertySource<?> locate(final Environment environment) {
        SyncopeClient syncopeClient = waRestClient.getSyncopeClient();
        if (syncopeClient == null) {
            LOG.warn("Application context is not ready to bootstrap WA configuration");
            return null;
        }

        LOG.info("Bootstrapping WA configuration");
        Map<String, Object> properties = new TreeMap<>();

        syncopeClient.getService(AuthModuleService.class).list().forEach(authModuleTO -> {
            LOG.debug("Mapping auth module {} ", authModuleTO.getKey());

            properties.putAll(authModuleTO.getConf().map(
                    new AuthModulePropertySourceMapper(syncopeClient.getAddress(), authModuleTO)));
        });

        syncopeClient.getService(AttrRepoService.class).list().forEach(attrRepoTO -> {
            LOG.debug("Mapping attr repo {} ", attrRepoTO.getKey());

            properties.putAll(attrRepoTO.getConf().map(
                    new AttrRepoPropertySourceMapper(syncopeClient.getAddress(), attrRepoTO)));
        });

        syncopeClient.getService(WAConfigService.class).list().forEach(attr -> properties.put(
                attr.getSchema(), attr.getValues().stream().collect(Collectors.joining(","))));

        LOG.debug("Collected WA properties: {}", properties);
        return new MapPropertySource(getClass().getName(), properties);
    }
}
