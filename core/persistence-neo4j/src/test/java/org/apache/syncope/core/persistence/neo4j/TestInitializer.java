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
package org.apache.syncope.core.persistence.neo4j;

import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.persistence.api.content.ContentLoader;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;

public class TestInitializer implements InitializingBean {

    private final StartupDomainLoader domainLoader;

    private final DomainHolder<Driver> domainHolder;

    private final ContentLoader contentLoader;

    private final ConfigurableApplicationContext ctx;

    public TestInitializer(
            final StartupDomainLoader domainLoader,
            final DomainHolder<Driver> domainHolder,
            final ContentLoader contentLoader,
            final ConfigurableApplicationContext ctx) {

        this.domainLoader = domainLoader;
        this.domainHolder = domainHolder;
        this.contentLoader = contentLoader;
        this.ctx = ctx;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ApplicationContextProvider.setApplicationContext(ctx);
        ApplicationContextProvider.setBeanFactory((DefaultListableBeanFactory) ctx.getBeanFactory());

        domainLoader.load();

        contentLoader.load(SyncopeConstants.MASTER_DOMAIN);

        if (domainHolder.getDomains().containsKey("Two")) {
            AuthContextUtils.runAsAdmin("Two", () -> contentLoader.load("Two"));
        }
    }
}
