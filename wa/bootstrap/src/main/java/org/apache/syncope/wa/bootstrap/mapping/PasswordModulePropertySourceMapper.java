package org.apache.syncope.wa.bootstrap.mapping;

import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.password.JDBCPasswordModuleConf;
import org.apache.syncope.common.lib.password.LDAPPasswordModuleConf;
import org.apache.syncope.common.lib.password.PasswordModuleConf;
import org.apache.syncope.common.lib.password.SyncopePasswordModuleConf;
import org.apache.syncope.common.lib.to.PasswordModuleTO;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.apereo.cas.configuration.model.support.ldap.AbstractLdapProperties;
import org.apereo.cas.configuration.model.support.pm.JdbcPasswordManagementProperties;
import org.apereo.cas.configuration.model.support.pm.LdapPasswordManagementProperties;
import org.apereo.cas.configuration.model.support.pm.SyncopePasswordManagementProperties;

public class PasswordModulePropertySourceMapper extends PropertySourceMapper implements PasswordModuleConf.Mapper {

    protected final WARestClient waRestClient;

    public PasswordModulePropertySourceMapper(final WARestClient waRestClient) {
        this.waRestClient = waRestClient;
    }

    @Override
    public Map<String, Object> map(final PasswordModuleTO passwordModuleTO, final SyncopePasswordModuleConf conf) {
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

        return prefix("cas.authn.pm.syncope.", WAConfUtils.asMap(props));
    }

    @Override
    public Map<String, Object> map(final PasswordModuleTO passwordModuleTO, final LDAPPasswordModuleConf conf) {
        LdapPasswordManagementProperties props = new LdapPasswordManagementProperties();
        props.setName(passwordModuleTO.getKey());
        props.setType(AbstractLdapProperties.LdapType.valueOf(conf.getLdapType().name()));
        props.setUsernameAttribute(conf.getUsernameAttribute());

        fill(props, conf);

        return prefix("cas.authn.pm.ldap[].", WAConfUtils.asMap(props));
    }

    @Override
    public Map<String, Object> map(final PasswordModuleTO passwordModuleTO, final JDBCPasswordModuleConf conf) {
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

        return prefix("cas.authn.pm.jdbc.", WAConfUtils.asMap(props));
    }
}
