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
import javax.persistence.ValidationMode;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.core.persistence.api.DomainsHolder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;

/**
 * Container for common configuration options among all EntityManagerFactory entities (one for each domain).
 * Acts as a commodity place for fetching each domain's {@link DataSource}..
 */
public class CommonEntityManagerFactoryConf implements DomainsHolder, InitializingBean, ApplicationContextAware {

    private ApplicationContext ctx;

    private final Map<String, DataSource> domains = new HashMap<>();

    private String[] packagesToScan;

    private ValidationMode validationMode;

    private PersistenceUnitPostProcessor[] postProcessors;

    private final Map<String, Object> jpaPropertyMap = new HashMap<>();

    @Override
    public void setApplicationContext(final ApplicationContext ctx) throws BeansException {
        this.ctx = ctx;
    }

    @Override
    public void afterPropertiesSet() {
        for (Map.Entry<String, DataSource> entry : ctx.getBeansOfType(DataSource.class).entrySet()) {
            if (!entry.getKey().startsWith("local")) {
                this.domains.put(
                        StringUtils.substringBefore(entry.getKey(), DataSource.class.getSimpleName()),
                        entry.getValue());
            }
        }
    }

    @Override
    public Map<String, DataSource> getDomains() {
        return domains;
    }

    public String[] getPackagesToScan() {
        return packagesToScan;
    }

    public void setPackagesToScan(final String... packagesToScan) {
        this.packagesToScan = packagesToScan;
    }

    public ValidationMode getValidationMode() {
        return validationMode;
    }

    public void setValidationMode(final ValidationMode validationMode) {
        this.validationMode = validationMode;
    }

    public PersistenceUnitPostProcessor[] getPersistenceUnitPostProcessors() {
        return postProcessors;
    }

    public void setPersistenceUnitPostProcessors(final PersistenceUnitPostProcessor... postProcessors) {
        this.postProcessors = postProcessors;
    }

    public Map<String, ?> getJpaPropertyMap() {
        return jpaPropertyMap;
    }

    public void setJpaPropertyMap(final Map<String, ?> jpaProperties) {
        if (jpaProperties != null) {
            this.jpaPropertyMap.putAll(jpaProperties);
        }
    }

}
