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
package org.apache.syncope.wa.starter.events;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.ObjectUtils;
import org.apereo.cas.config.CasConfigurationModifiedEvent;
import org.apereo.cas.configuration.CasConfigurationPropertiesEnvironmentManager;
import org.apereo.cas.support.events.listener.CasConfigurationEventListener;
import org.apereo.cas.util.function.FunctionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindingPostProcessor;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.ApplicationContext;

/**
 * CAS configuration listener variant that avoids manually re-initializing web servlets without ServletConfig.
 */
public class WACasConfigurationEventListener implements CasConfigurationEventListener {

    protected static final Logger LOG = LoggerFactory.getLogger(WACasConfigurationEventListener.class);

    private final CasConfigurationPropertiesEnvironmentManager configurationPropertiesEnvironmentManager;

    private final ConfigurationPropertiesBindingPostProcessor binder;

    private final ContextRefresher contextRefresher;

    private final ApplicationContext applicationContext;

    public WACasConfigurationEventListener(
            final CasConfigurationPropertiesEnvironmentManager configurationPropertiesEnvironmentManager,
            final ConfigurationPropertiesBindingPostProcessor binder,
            final ContextRefresher contextRefresher,
            final ApplicationContext applicationContext) {

        this.configurationPropertiesEnvironmentManager = configurationPropertiesEnvironmentManager;
        this.binder = binder;
        this.contextRefresher = contextRefresher;
        this.applicationContext = applicationContext;
    }

    @Override
    public void onRefreshScopeRefreshed(final RefreshScopeRefreshedEvent event) {
        LOG.info("Refreshing application context beans eagerly...");
        initializeBeansEagerly();
    }

    @Override
    public void onEnvironmentChangedEvent(final EnvironmentChangeEvent event) {
        LOG.trace("Received event [{}]", event);
        rebind();
    }

    @Override
    public void handleConfigurationModifiedEvent(final CasConfigurationModifiedEvent event) {
        if (event.isEligibleForContextRefresh()) {
            LOG.info("Received event [{}]. Refreshing CAS configuration...", event);
            final Set<String> keys = contextRefresher.refresh();
            LOG.info("Refreshed the following settings: [{}].", keys);
            rebind();
            LOG.info("CAS finished rebinding configuration with new settings [{}]",
                    ObjectUtils.getIfNull(keys, new ArrayList<>()));
        }
    }

    private void initializeBeansEagerly() {
        FunctionUtils.doAndHandle(unused -> {
            for (final String beanName : applicationContext.getBeanDefinitionNames()) {
                Objects.requireNonNull(applicationContext.getBean(beanName).getClass());
            }
        });
    }

    private void rebind() {
        LOG.info("Refreshing CAS configuration. Stand by...");
        final Object ctx = FunctionUtils.doIfNotNull(configurationPropertiesEnvironmentManager,
                () -> configurationPropertiesEnvironmentManager.rebindCasConfigurationProperties(applicationContext),
                () -> CasConfigurationPropertiesEnvironmentManager.rebindCasConfigurationProperties(
                        binder, applicationContext)).
                get();
        Objects.requireNonNull(ctx);
        initializeBeansEagerly();
    }
}
