package org.apache.syncope.common.lib.to;

import jakarta.ws.rs.PathParam;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.password.PasswordModuleConf;

public class PasswordModuleTO implements EntityTO {

    private String key;

    private String description;

    private int order = 0;

    private final List<Item> items = new ArrayList<>();

    private PasswordModuleConf conf;

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

    public int getOrder() {
        return order;
    }

    public void setOrder(final int order) {
        this.order = order;
    }

    public List<Item> getItems() {
        return items;
    }

    public PasswordModuleConf getConf() {
        return conf;
    }

    public void setConf(final PasswordModuleConf conf) {
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
        PasswordModuleTO other = (PasswordModuleTO) obj;
        return new EqualsBuilder().
                append(key, other.key).
                append(description, other.description).
                append(order, other.order).
                append(items, other.items).
                append(conf, other.conf).
                build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(key).
                append(description).
                append(items).
                append(conf).
                build();
    }
}
