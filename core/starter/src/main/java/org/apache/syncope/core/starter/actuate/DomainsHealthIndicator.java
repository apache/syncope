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
package org.apache.syncope.core.starter.actuate;

import java.sql.Connection;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.jdbc.datasource.DataSourceUtils;

public class DomainsHealthIndicator implements HealthIndicator {

    protected static final Logger LOG = LoggerFactory.getLogger(DomainsHealthIndicator.class);

    @Autowired
    protected DomainHolder domainHolder;

    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();

        AtomicReference<Boolean> anyDown = new AtomicReference<>(Boolean.FALSE);

        domainHolder.getDomains().forEach((key, ds) -> {
            Status status;

            Connection conn = null;
            try {
                conn = DataSourceUtils.getConnection(ds);
                status = conn.isValid(0) ? Status.UP : Status.OUT_OF_SERVICE;
            } catch (Exception e) {
                status = Status.DOWN;
                LOG.debug("When attempting to connect to Domain {}", key, e);
            } finally {
                if (conn != null) {
                    DataSourceUtils.releaseConnection(conn, ds);
                }
            }

            builder.withDetail(key, status);
            if (status != Status.UP) {
                anyDown.set(true);
            }
        });

        builder.status(anyDown.get() ? Status.DOWN : Status.UP);

        return builder.build();
    }
}
