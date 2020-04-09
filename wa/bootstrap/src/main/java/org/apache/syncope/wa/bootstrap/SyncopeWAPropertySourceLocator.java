/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.syncope.wa.bootstrap;

import org.apache.syncope.wa.WARestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import java.util.HashMap;
import java.util.Map;

@Order
public class SyncopeWAPropertySourceLocator implements PropertySourceLocator {
    private static final Logger LOG = LoggerFactory.getLogger(SyncopeWABootstrapConfiguration.class);

    private final WARestClient waRestClient;

    public SyncopeWAPropertySourceLocator(final WARestClient waRestClient) {
        this.waRestClient = waRestClient;
    }

    @Override
    public PropertySource<?> locate(final Environment environment) {
        try {
            Map<String, Object> properties = new HashMap<>();
            if (WARestClient.isReady()) {
                LOG.info("Bootstrapping WA configuration");
                return new MapPropertySource(getClass().getName(), properties);
            }

            LOG.warn("Application context is not ready to bootstrap WA configuration");
            return null;
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to fetch settings", e);
        }
    }
}
