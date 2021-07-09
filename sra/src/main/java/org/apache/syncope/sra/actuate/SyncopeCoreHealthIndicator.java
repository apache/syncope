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

import org.apache.syncope.client.lib.AnonymousAuthenticationHandler;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.rest.api.service.SRARouteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;

public class SyncopeCoreHealthIndicator implements HealthIndicator {

    protected static final Logger LOG = LoggerFactory.getLogger(SyncopeCoreHealthIndicator.class);

    @Autowired
    protected ServiceOps serviceOps;

    @Value("${anonymousUser}")
    protected String anonymousUser;

    @Value("${anonymousKey}")
    protected String anonymousKey;

    @Value("${useGZIPCompression:false}")
    protected boolean useGZIPCompression;

    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();

        try {
            new SyncopeClientFactoryBean().
                    setAddress(serviceOps.get(NetworkService.Type.CORE).getAddress()).
                    setUseCompression(useGZIPCompression).
                    create(new AnonymousAuthenticationHandler(anonymousUser, anonymousKey)).
                    getService(SRARouteService.class).list();
            builder.status(Status.UP);
        } catch (Exception e) {
            LOG.debug("When attempting to connect to Syncope Core", e);
            builder.status(Status.DOWN);
        }

        return builder.build();
    }
}
