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
package org.apache.syncope.core.persistence.jpa.spring;

import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class DomainRoutingDataSource extends AbstractRoutingDataSource {

    private Map<Object, Object> targetDataSources;

    @Override
    protected Object determineCurrentLookupKey() {
        return AuthContextUtils.getDomain();
    }

    @Override
    public void setTargetDataSources(final Map<Object, Object> targetDataSources) {
        super.setTargetDataSources(targetDataSources);
        this.targetDataSources = targetDataSources;
    }

    public void add(final String domain, final DataSource dataSource) {
        if (targetDataSources == null) {
            targetDataSources = new HashMap<>();
        }
        targetDataSources.put(domain, dataSource);
        super.setTargetDataSources(targetDataSources);
        initialize();
    }

    public void remove(final String domain) {
        if (targetDataSources != null) {
            targetDataSources.remove(domain);
            super.setTargetDataSources(targetDataSources);
            initialize();
        }
    }
}
