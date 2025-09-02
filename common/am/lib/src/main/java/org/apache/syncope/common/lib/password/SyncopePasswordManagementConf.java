package org.apache.syncope.common.lib.password;

import java.util.HashMap;
import java.util.Map;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.PasswordManagementTO;

public class SyncopePasswordManagementConf implements PasswordManagementConf {

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

    @Override public Map<String, Object> map(final PasswordManagementTO passwordManagementTO, final Mapper mapper) {
        return mapper.map(passwordManagementTO, this);
    }
}
