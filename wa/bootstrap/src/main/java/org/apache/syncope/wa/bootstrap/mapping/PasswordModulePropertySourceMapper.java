package org.apache.syncope.wa.bootstrap.mapping;

import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.password.PasswordModuleConf;
import org.apache.syncope.common.lib.password.SyncopePasswordModuleConf;
import org.apache.syncope.common.lib.to.PasswordModuleTO;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.apereo.cas.configuration.model.support.pm.SyncopePasswordManagementProperties;

public class PasswordModulePropertySourceMapper extends PropertySourceMapper implements PasswordModuleConf.Mapper {

    protected final WARestClient waRestClient;

    public PasswordModulePropertySourceMapper(final WARestClient waRestClient) {
        this.waRestClient = waRestClient;
    }

    @Override public Map<String, Object> map(PasswordModuleTO passwordModuleTO, SyncopePasswordModuleConf conf) {
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
}
