package org.apache.syncope.common.lib.password;

import java.util.Map;
import org.apache.syncope.common.lib.AbstractLDAPConf;
import org.apache.syncope.common.lib.to.PasswordModuleTO;

public class LDAPPasswordModuleConf extends AbstractLDAPConf implements PasswordModuleConf {

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

    @Override public Map<String, Object> map(final PasswordModuleTO passwordModuleTO, final Mapper mapper) {
        return mapper.map(passwordModuleTO, this);
    }
}
