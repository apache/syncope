package org.apache.syncope.common.lib.to;

import jakarta.ws.rs.PathParam;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.password.PasswordManagementConf;
import org.apache.syncope.common.lib.types.PasswordManagementState;

public class PasswordManagementTO implements EntityTO {

    private static final long serialVersionUID = -7203295929825782174L;

    private String key;

    private String description;

    private boolean enabled = false;

//    private final List<Item> items = new ArrayList<>();

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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

//    public List<Item> getItems() {
//        return items;
//    }

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
//                append(items, other.items).
                append(conf, other.conf).
                build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(key).
                append(description).
//                append(items).
                append(conf).
                build();
    }
}
