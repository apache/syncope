package org.apache.syncope.common.lib.to;

import jakarta.ws.rs.PathParam;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.password.PasswordManagementConf;

public class PasswordManagementTO implements EntityTO {

    private static final long serialVersionUID = -7203295929825782174L;

    private String key;

    private String description;

    private String enabled = Boolean.FALSE.toString();

    private PasswordManagementConf conf;

    @Override
    public String getKey() {
        return key;
    }

    @PathParam("key")
    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getEnabled() {
        return StringUtils.isNotBlank(enabled)
                ? enabled
                : Boolean.FALSE.toString();
    }

    public void setEnabled(final String enabled) {
        this.enabled = enabled;
    }

    public PasswordManagementConf getConf() {
        return conf;
    }

    public void setConf(final PasswordManagementConf conf) {
        this.conf = conf;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PasswordManagementTO other = (PasswordManagementTO) obj;
        return new EqualsBuilder().
                append(key, other.key).
                append(description, other.description).
                append(enabled, other.enabled).
                append(conf, other.conf).
                build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(key).
                append(description).
                append(conf).
                build();
    }
}
