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
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.AbstractJDBCConf;
import org.apache.syncope.common.lib.AbstractLDAPConf;
import org.apache.syncope.common.lib.AbstractLDAPConf.LdapTrustManager;
import org.apereo.cas.configuration.model.support.ConnectionPoolingProperties;
import org.apereo.cas.configuration.model.support.jpa.AbstractJpaProperties;
import org.apereo.cas.configuration.model.support.ldap.AbstractLdapProperties.LdapConnectionPoolPassivator;
import org.apereo.cas.configuration.model.support.ldap.AbstractLdapProperties.LdapHostnameVerifierOptions;
import org.apereo.cas.configuration.model.support.ldap.AbstractLdapSearchProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PropertySourceMapper {

    protected static final Logger LOG = LoggerFactory.getLogger(PropertySourceMapper.class);

    protected static Map<String, Object> prefix(final String prefix, final Map<String, Object> map) {
        return map.entrySet().stream().
                map(e -> Pair.of(prefix + e.getKey(), e.getValue())).
                collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    }

    protected void fill(final AbstractLdapSearchProperties props, final AbstractLDAPConf conf) {
        props.setLdapUrl(conf.getLdapUrl());
        props.setBaseDn(conf.getBaseDn());
        props.setSearchFilter(conf.getSearchFilter());
        props.setSubtreeSearch(conf.isSubtreeSearch());
        props.setPageSize(conf.getPageSize());
        props.setBindDn(conf.getBindDn());
        props.setBindCredential(conf.getBindCredential());
        props.setDisablePooling(conf.isDisablePooling());
        props.setMinPoolSize(conf.getMinPoolSize());
        props.setMaxPoolSize(conf.getMaxPoolSize());
        props.setPoolPassivator(LdapConnectionPoolPassivator.valueOf(conf.getPoolPassivator().name()).name());
        props.setHostnameVerifier(LdapHostnameVerifierOptions.valueOf(conf.getHostnameVerifier().name()));
        props.setTrustManager(Optional.ofNullable(conf.getTrustManager()).map(LdapTrustManager::name).orElse(null));
        props.setValidateOnCheckout(conf.isValidateOnCheckout());
        props.setValidatePeriodically(conf.isValidatePeriodically());
        props.setValidateTimeout(conf.getValidateTimeout().toString());
        props.setValidatePeriod(conf.getValidatePeriod().toString());
        props.setFailFast(conf.isFailFast());
        props.setIdleTime(conf.getIdleTime().toString());
        props.setPrunePeriod(conf.getPrunePeriod().toString());
        props.setBlockWaitTime(conf.getBlockWaitTime().toString());
        props.setUseStartTls(conf.isUseStartTls());
        props.setConnectTimeout(conf.getConnectTimeout().toString());
        props.setResponseTimeout(conf.getResponseTimeout().toString());
        props.setAllowMultipleDns(conf.isAllowMultipleDns());
        props.setAllowMultipleEntries(conf.isAllowMultipleEntries());
        props.setFollowReferrals(conf.isFollowReferrals());
        props.setBinaryAttributes(conf.getBinaryAttributes());
    }

    protected void fill(final AbstractJpaProperties props, final AbstractJDBCConf conf) {
        props.setDialect(conf.getDialect());
        props.setDriverClass(conf.getDriverClass());
        props.setUrl(conf.getUrl());
        props.setUser(conf.getUser());
        props.setPassword(conf.getPassword());
        props.setDefaultCatalog(conf.getDefaultCatalog());
        props.setDefaultSchema(conf.getDefaultSchema());
        props.setHealthQuery(conf.getHealthQuery());
        props.setIdleTimeout(conf.getIdleTimeout().toString());
        props.setDataSourceName(conf.getDataSourceName());
        props.setLeakThreshold(conf.getPoolLeakThreshold().toString());

        ConnectionPoolingProperties connProps = new ConnectionPoolingProperties();
        connProps.setMinSize(conf.getMinPoolSize());
        connProps.setMaxSize(conf.getMaxPoolSize());
        connProps.setMaxWait(conf.getMaxPoolWait().toString());
        connProps.setSuspension(conf.isPoolSuspension());
        connProps.setTimeoutMillis(conf.getPoolTimeoutMillis());
        props.setPool(connProps);
    }
}
