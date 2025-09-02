package org.apache.syncope.core.persistence.jpa.entity.am;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.password.PasswordManagementConf;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.types.PasswordManagementState;
import org.apache.syncope.core.persistence.api.entity.am.PasswordManagement;
import org.apache.syncope.core.persistence.jpa.entity.AbstractProvidedKeyEntity;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;

@Entity
@Table(name = JPAPasswordManagement.TABLE)
public class JPAPasswordManagement extends AbstractProvidedKeyEntity implements PasswordManagement {

    private static final long serialVersionUID = 5457779846065079998L;

    public static final String TABLE = "PasswordManagement";

    protected static final TypeReference<List<Item>> TYPEREF = new TypeReference<List<Item>>() {
    };

    private String description;

    private boolean enabled;

    @Lob
    private String items;

    @Transient
    private final List<Item> itemList = new ArrayList<>();

    @Lob
    private String jsonConf;

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

    }

    @Override
    public List<Item> getItems() {
        return itemList;
    }

    @Override
    public PasswordManagementConf getConf() {
        PasswordManagementConf conf = null;
        if (!StringUtils.isBlank(jsonConf)) {
            conf = POJOHelper.deserialize(jsonConf, PasswordManagementConf.class);
        }

        return conf;
    }

    @Override
    public void setConf(final PasswordManagementConf conf) {
        jsonConf = POJOHelper.serialize(conf);
    }

    protected void json2list(final boolean clearFirst) {
        if (clearFirst) {
            getItems().clear();
        }
        if (items != null) {
            getItems().addAll(POJOHelper.deserialize(items, TYPEREF));
        }
    }

    @PostLoad
    public void postLoad() {
        json2list(false);
    }

    @PostPersist
    @PostUpdate
    public void postSave() {
        json2list(true);
    }

    @PrePersist
    @PreUpdate
    public void list2json() {
        items = POJOHelper.serialize(getItems());
    }
}
