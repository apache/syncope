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
package org.apache.syncope.wa.bootstrap;

import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.attr.AttrRepoConf;
import org.apache.syncope.common.lib.attr.JDBCAttrRepoConf;
import org.apache.syncope.common.lib.attr.LDAPAttrRepoConf;
import org.apache.syncope.common.lib.attr.StubAttrRepoConf;
import org.apache.syncope.common.lib.attr.SyncopeAttrRepoConf;
import org.apache.syncope.common.lib.to.AttrRepoTO;
import org.apereo.cas.configuration.CasCoreConfigurationUtils;
import org.apereo.cas.configuration.model.core.authentication.AttributeRepositoryStates;
import org.apereo.cas.configuration.model.core.authentication.StubPrincipalAttributesProperties;
import org.apereo.cas.configuration.model.support.jdbc.JdbcPrincipalAttributesProperties;
import org.apereo.cas.configuration.model.support.ldap.LdapPrincipalAttributesProperties;
import org.apereo.cas.configuration.model.support.syncope.SyncopePrincipalAttributesProperties;

public class AttrRepoPropertySourceMapper extends PropertySourceMapper implements AttrRepoConf.Mapper {

    protected final WARestClient waRestClient;

    public AttrRepoPropertySourceMapper(final WARestClient waRestClient) {
        this.waRestClient = waRestClient;
    }

    @Override
    public Map<String, Object> map(final AttrRepoTO attrRepoTO, final StubAttrRepoConf conf) {
        StubPrincipalAttributesProperties props = new StubPrincipalAttributesProperties();
        props.setId(attrRepoTO.getKey());
        props.setState(AttributeRepositoryStates.valueOf(attrRepoTO.getState().name()));
        props.setOrder(attrRepoTO.getOrder());
        props.setAttributes(conf.getAttributes());

        return prefix("cas.authn.attribute-repository.stub.", CasCoreConfigurationUtils.asMap(props));
    }

    @Override
    public Map<String, Object> map(final AttrRepoTO attrRepoTO, final LDAPAttrRepoConf conf) {
        LdapPrincipalAttributesProperties props = new LdapPrincipalAttributesProperties();
        props.setId(attrRepoTO.getKey());
        props.setState(AttributeRepositoryStates.valueOf(attrRepoTO.getState().name()));
        props.setOrder(attrRepoTO.getOrder());
        props.setAttributes(conf.getAttributes());
        props.setUseAllQueryAttributes(conf.isUseAllQueryAttributes());
        props.setQueryAttributes(conf.getQueryAttributes());
        fill(props, conf);

        return prefix("cas.authn.attribute-repository.ldap[].", CasCoreConfigurationUtils.asMap(props));
    }

    @Override
    public Map<String, Object> map(final AttrRepoTO attrRepoTO, final JDBCAttrRepoConf conf) {
        JdbcPrincipalAttributesProperties props = new JdbcPrincipalAttributesProperties();
        props.setId(attrRepoTO.getKey());
        props.setState(AttributeRepositoryStates.valueOf(attrRepoTO.getState().name()));
        props.setOrder(attrRepoTO.getOrder());
        props.setSql(conf.getSql());
        props.setSingleRow(conf.isSingleRow());
        props.setRequireAllAttributes(conf.isRequireAllAttributes());
        props.setCaseCanonicalization(conf.getCaseCanonicalization().name());
        props.setQueryType(conf.getQueryType().name());
        props.setColumnMappings(conf.getColumnMappings());
        props.setUsername(conf.getUsername());
        props.setAttributes(conf.getAttributes());
        props.setCaseInsensitiveQueryAttributes(conf.getCaseInsensitiveQueryAttributes());
        props.setQueryAttributes(conf.getQueryAttributes());
        fill(props, conf);

        return prefix("cas.authn.attribute-repository.jdbc[].", CasCoreConfigurationUtils.asMap(props));
    }

    @Override
    public Map<String, Object> map(final AttrRepoTO attrRepoTO, final SyncopeAttrRepoConf conf) {
        SyncopeClient syncopeClient = waRestClient.getSyncopeClient();
        if (syncopeClient == null) {
            LOG.warn("Application context is not ready to bootstrap WA configuration");
            return Map.of();
        }

        SyncopePrincipalAttributesProperties props = new SyncopePrincipalAttributesProperties();
        props.setId(attrRepoTO.getKey());
        props.setState(AttributeRepositoryStates.valueOf(attrRepoTO.getState().name()));
        props.setOrder(attrRepoTO.getOrder());
        props.setDomain(conf.getDomain());
        props.setUrl(StringUtils.substringBefore(syncopeClient.getAddress(), "/rest"));
        props.setSearchFilter(conf.getSearchFilter());
        props.setBasicAuthUsername(conf.getBasicAuthUsername());
        props.setBasicAuthPassword(conf.getBasicAuthPassword());
        props.setHeaders(props.getHeaders());

        return prefix("cas.authn.attribute-repository.syncope.", CasCoreConfigurationUtils.asMap(props));
    }
}
