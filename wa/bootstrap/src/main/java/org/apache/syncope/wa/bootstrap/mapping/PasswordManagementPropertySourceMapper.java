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
package org.apache.syncope.wa.bootstrap.mapping;

import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.password.JDBCPasswordManagementConf;
import org.apache.syncope.common.lib.password.LDAPPasswordManagementConf;
import org.apache.syncope.common.lib.password.PasswordManagementConf;
import org.apache.syncope.common.lib.password.RESTPasswordManagementConf;
import org.apache.syncope.common.lib.password.SyncopePasswordManagementConf;
import org.apache.syncope.common.lib.to.PasswordManagementTO;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.apereo.cas.configuration.model.support.ldap.AbstractLdapProperties;
import org.apereo.cas.configuration.model.support.pm.JdbcPasswordManagementProperties;
import org.apereo.cas.configuration.model.support.pm.LdapPasswordManagementProperties;
import org.apereo.cas.configuration.model.support.pm.RestfulPasswordManagementProperties;
import org.apereo.cas.configuration.model.support.pm.SyncopePasswordManagementProperties;

public class PasswordManagementPropertySourceMapper extends PropertySourceMapper
        implements PasswordManagementConf.Mapper {

    protected final WARestClient waRestClient;

    public PasswordManagementPropertySourceMapper(final WARestClient waRestClient) {
        this.waRestClient = waRestClient;
    }

    @Override
    public Map<String, Object> map(
            final PasswordManagementTO passwordManagementTO,
            final SyncopePasswordManagementConf conf) {

        SyncopeClient syncopeClient = waRestClient.getSyncopeClient();
        if (syncopeClient == null) {
            LOG.warn("Application context is not ready to bootstrap WA configuration");
            return Map.of();
        }

        SyncopePasswordManagementProperties props = new SyncopePasswordManagementProperties();
        props.setDomain(conf.getDomain());
        props.setUrl(StringUtils.substringBefore(syncopeClient.getAddress(), "/rest"));
        props.setBasicAuthUsername(conf.getBasicAuthUsername());
        props.setBasicAuthPassword(conf.getBasicAuthPassword());
        props.setSearchFilter(conf.getSearchFilter());
        props.setHeaders(conf.getHeaders());

        Map<String, Object> mapped = prefix("cas.authn.pm.syncope.", WAConfUtils.asMap(props));
        mapped.put("cas.authn.pm.syncope.enabled", passwordManagementTO.isEnabled());
        return mapped;
    }

    @Override
    public Map<String, Object> map(
            final PasswordManagementTO passwordManagementTO,
            final LDAPPasswordManagementConf conf) {

        LdapPasswordManagementProperties props = new LdapPasswordManagementProperties();
        props.setName(passwordManagementTO.getKey());
        props.setType(AbstractLdapProperties.LdapType.valueOf(conf.getLdapType().name()));
        props.setUsernameAttribute(conf.getUsernameAttribute());

        fill(props, conf);

        Map<String, Object> mapped = prefix("cas.authn.pm.ldap[].", WAConfUtils.asMap(props));
        mapped.put("cas.authn.pm.ldap.enabled", passwordManagementTO.isEnabled());
        return mapped;
    }

    @Override
    public Map<String, Object> map(
            final PasswordManagementTO passwordManagementTO,
            final JDBCPasswordManagementConf conf) {

        JdbcPasswordManagementProperties props = new JdbcPasswordManagementProperties();
        props.setSqlChangePassword(conf.getSqlChangePassword());
        props.setSqlFindEmail(conf.getSqlFindEmail());
        props.setSqlFindPhone(conf.getSqlFindPhone());
        props.setSqlFindUser(conf.getSqlFindUser());
        props.setSqlGetSecurityQuestions(conf.getSqlGetSecurityQuestions());
        props.setSqlUpdateSecurityQuestions(conf.getSqlUpdateSecurityQuestions());
        props.setSqlDeleteSecurityQuestions(conf.getSqlDeleteSecurityQuestions());
        props.setSqlUnlockAccount(conf.getSqlUnlockAccount());
        fill(props, conf);

        Map<String, Object> mapped = prefix("cas.authn.pm.jdbc.", WAConfUtils.asMap(props));
        mapped.put("cas.authn.pm.jdbc.enabled", passwordManagementTO.isEnabled());
        mapped.put("management.health.db.enabled", "false");
        return mapped;
    }

    @Override
    public Map<String, Object> map(
            final PasswordManagementTO passwordManagementTO,
            final RESTPasswordManagementConf conf) {

        RestfulPasswordManagementProperties props = new RestfulPasswordManagementProperties();
        props.setEndpointPassword(conf.getEndpointPassword());
        props.setEndpointUrlAccountUnlock(conf.getEndpointUrlAccountUnlock());
        props.setEndpointUrlChange(conf.getEndpointUrlChange());
        props.setEndpointUrlEmail(conf.getEndpointUrlEmail());
        props.setEndpointUrlPhone(conf.getEndpointUrlPhone());
        props.setEndpointUrlSecurityQuestions(conf.getEndpointUrlSecurityQuestions());
        props.setEndpointUrlUser(conf.getEndpointUrlUser());
        props.setEndpointUsername(conf.getEndpointUsername());
        props.setFieldNamePasswordOld(conf.getFieldNamePasswordOld());
        props.setFieldNamePassword(conf.getFieldNamePassword());
        props.setFieldNameUser(conf.getFieldNameUser());
        props.setHeaders(conf.getHeaders());

        Map<String, Object> mapped = prefix("cas.authn.pm.rest.", WAConfUtils.asMap(props));
        mapped.put("cas.authn.pm.rest.enabled", passwordManagementTO.isEnabled());
        return mapped;
    }
}
