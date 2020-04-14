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

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.CasCoreConfigurationUtils;
import org.apereo.cas.configuration.model.support.generic.AcceptAuthenticationProperties;
import org.apereo.cas.configuration.model.support.ldap.LdapAuthenticationProperties;
import org.apereo.cas.configuration.model.support.syncope.SyncopeAuthenticationProperties;

import org.apache.syncope.common.lib.auth.AuthModuleConf;
import org.apache.syncope.common.lib.auth.LDAPAuthModuleConf;
import org.apache.syncope.common.lib.auth.StaticAuthModuleConf;
import org.apache.syncope.common.lib.auth.SyncopeAuthModuleConf;
import org.apache.syncope.common.rest.api.service.AuthModuleService;
import org.apache.syncope.wa.WARestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import java.util.Map;
import java.util.stream.Collectors;

@Order
public class SyncopeWAPropertySourceLocator implements PropertySourceLocator {
    private static final Logger LOG = LoggerFactory.getLogger(SyncopeWABootstrapConfiguration.class);

    private final WARestClient waRestClient;

    public SyncopeWAPropertySourceLocator(final WARestClient waRestClient) {
        this.waRestClient = waRestClient;
    }

    private static void mapSyncopeAuthModuleConf(final CasConfigurationProperties casProperties,
                                                 final AuthModuleConf authConf) {
        SyncopeAuthModuleConf conf = SyncopeAuthModuleConf.class.cast(authConf);
        SyncopeAuthenticationProperties syncopeProps = new SyncopeAuthenticationProperties();
        syncopeProps.setName(conf.getName());
        syncopeProps.setDomain(conf.getDomain());
        syncopeProps.setUrl(conf.getUrl());
        casProperties.getAuthn().setSyncope(syncopeProps);
    }

    private static void mapStaticAuthModuleConf(final CasConfigurationProperties casProperties,
                                                final AuthModuleConf authConf) {
        StaticAuthModuleConf conf = StaticAuthModuleConf.class.cast(authConf);
        AcceptAuthenticationProperties staticProps = new AcceptAuthenticationProperties();
        staticProps.setName(conf.getName());
        String users = conf.getUsers().entrySet().stream().
            map(entry -> entry.getKey() + "::" + entry.getValue()).
            collect(Collectors.joining(","));
        staticProps.setUsers(users);
        casProperties.getAuthn().setAccept(staticProps);
    }

    private static void mapLdapAuthModuleConf(final CasConfigurationProperties casProperties,
                                              final AuthModuleConf authConf) {
        LDAPAuthModuleConf ldapConf = LDAPAuthModuleConf.class.cast(authConf);

        LdapAuthenticationProperties ldapProps = new LdapAuthenticationProperties();
        ldapProps.setName(ldapConf.getName());
        ldapProps.setBaseDn(ldapConf.getBaseDn());
        ldapProps.setBindCredential(ldapConf.getBindCredential());
        ldapProps.setSearchFilter(ldapConf.getSearchFilter());
        ldapProps.setPrincipalAttributeId(ldapConf.getUserIdAttribute());
        ldapProps.setLdapUrl(ldapConf.getLdapUrl());
        ldapProps.setSubtreeSearch(ldapConf.isSubtreeSearch());

        casProperties.getAuthn().getLdap().add(ldapProps);
    }

    @Override
    public PropertySource<?> locate(final Environment environment) {
        if (!WARestClient.isReady()) {
            LOG.warn("Application context is not ready to bootstrap WA configuration");
            return null;
        }
        LOG.info("Bootstrapping WA configuration");
        CasConfigurationProperties casProperties = new CasConfigurationProperties();
        AuthModuleService authService = waRestClient.getSyncopeClient().getService(AuthModuleService.class);
        authService.list().forEach(authModuleTO -> {

            AuthModuleConf authConf = authModuleTO.getConf();
            LOG.debug("Mapping auth module {}:{} as conf {}", authModuleTO.getKey(),
                authModuleTO.getName(), authConf.getName());
            if (authConf instanceof LDAPAuthModuleConf) {
                mapLdapAuthModuleConf(casProperties, authConf);
            }
            if (authConf instanceof StaticAuthModuleConf) {
                mapStaticAuthModuleConf(casProperties, authConf);
            }
            if (authConf instanceof SyncopeAuthModuleConf) {
                mapSyncopeAuthModuleConf(casProperties, authConf);
            }
        });
        Map<String, Object> properties = CasCoreConfigurationUtils.asMap(casProperties.withHolder());
        LOG.debug("Collected WA properties: {}", properties);
        return new MapPropertySource(getClass().getName(), properties);
    }
}
