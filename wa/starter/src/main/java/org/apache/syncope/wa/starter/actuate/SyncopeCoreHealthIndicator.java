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
package org.apache.syncope.wa.starter.actuate;

import org.apache.syncope.common.rest.api.service.UserSelfService;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;

public class SyncopeCoreHealthIndicator implements HealthIndicator {

    protected static final Logger LOG = LoggerFactory.getLogger(SyncopeCoreHealthIndicator.class);

    protected final WARestClient waRestClient;

    public SyncopeCoreHealthIndicator(final WARestClient waRestClient) {
        this.waRestClient = waRestClient;
    }

    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();

        try {
            waRestClient.getSyncopeClient().getService(UserSelfService.class).read();
            builder.status(Status.UP);
        } catch (Exception e) {
            LOG.debug("When attempting to connect to Syncope Core", e);
            builder.status(Status.DOWN);
        }

        return builder.build();
    }
}
