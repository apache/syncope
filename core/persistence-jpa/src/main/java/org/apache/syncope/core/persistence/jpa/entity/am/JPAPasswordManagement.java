package org.apache.syncope.core.persistence.jpa.entity.am;

import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.password.PasswordManagementConf;
import org.apache.syncope.core.persistence.api.entity.am.PasswordManagement;
import org.apache.syncope.core.persistence.common.validation.PasswordManagementCheck;
import org.apache.syncope.core.persistence.jpa.entity.AbstractProvidedKeyEntity;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;

@Entity
@Table(name = JPAPasswordManagement.TABLE)
@PasswordManagementCheck
public class JPAPasswordManagement extends AbstractProvidedKeyEntity implements PasswordManagement {

    private static final long serialVersionUID = 5457779846065079998L;

    public static final String TABLE = "PasswordManagement";

    private String description;

    @NotNull
    private String enabled;

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
    public String getEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(final String enabled) {
        this.enabled = enabled;

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
}
