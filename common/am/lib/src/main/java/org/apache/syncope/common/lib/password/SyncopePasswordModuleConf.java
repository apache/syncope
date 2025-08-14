package org.apache.syncope.common.lib.password;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.PasswordModuleTO;

public class SyncopePasswordModuleConf implements PasswordModuleConf{

    private static final long serialVersionUID = -8203692328056109683L;

    private String domain = SyncopeConstants.MASTER_DOMAIN;

    /**
     * User FIQL filter to use for searching.
     */
    protected String searchFilter;

    /**
     * Specify the username for REST authentication.
     */
    private String basicAuthUsername;

    /**
     * Specify the password for REST authentication.
     */
    private String basicAuthPassword;

    /**
     * Headers, defined as a Map, to include in the request when making the REST call.
     * Will overwrite any header that CAS is pre-defined to
     * send and include in the request. Key in the map should be the header name
     * and the value in the map should be the header value.
     */
    private Map<String, String> headers = new HashMap<>();

    /**
     * Map of attributes that optionally may be used to control the names
     * of the collected attributes from Syncope. If an attribute is provided by Syncope,
     * it can be listed here as the key of the map with a value that should be the name
     * of that attribute as collected and recorded by CAS.
     * For example, the convention {@code lastLoginDate->lastDate} will process the
     * Syncope attribute {@code lastLoginDate} and will internally rename that to {@code lastDate}.
     * If no mapping is specified, CAS defaults will be used instead.
     */
    private Map<String, String> attributeMappings = new LinkedHashMap<>();

    public String getDomain() {
        return domain;
    }

    public void setDomain(final String domain) {
        this.domain = domain;
    }

    public String getSearchFilter() {
        return searchFilter;
    }

    public void setSearchFilter(final String searchFilter) {
        this.searchFilter = searchFilter;
    }

    public String getBasicAuthUsername() {
        return basicAuthUsername;
    }

    public void setBasicAuthUsername(final String basicAuthUsername) {
        this.basicAuthUsername = basicAuthUsername;
    }

    public String getBasicAuthPassword() {
        return basicAuthPassword;
    }

    public void setBasicAuthPassword(final String basicAuthPassword) {
        this.basicAuthPassword = basicAuthPassword;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(final Map<String, String> headers) {
        this.headers = headers;
    }

    public Map<String, String> getAttributeMappings() {
        return attributeMappings;
    }

    public void setAttributeMappings(Map<String, String> attributeMappings) {
        this.attributeMappings = attributeMappings;
    }

    @Override public Map<String, Object> map(final PasswordModuleTO passwordModuleTO, final Mapper mapper) {
        return mapper.map(passwordModuleTO, this);
    }
}
