package org.apache.syncope.common.lib.password;

import java.util.Map;
import org.apache.syncope.common.lib.AbstractLDAPConf;
import org.apache.syncope.common.lib.to.PasswordManagementTO;

public class LDAPPasswordManagementConf extends AbstractLDAPConf implements PasswordManagementConf {

    private static final long serialVersionUID = 3721321025567652223L;

    /**
     * Username attribute required by LDAP.
     */
    private String usernameAttribute = "uid";

    public String getUsernameAttribute() {
        return usernameAttribute;
    }

    public void setUsernameAttribute(final String usernameAttribute) {
        this.usernameAttribute = usernameAttribute;
    }

    @Override public Map<String, Object> map(final PasswordManagementTO passwordManagementTO, final Mapper mapper) {
        return mapper.map(passwordManagementTO, this);
    }
}
