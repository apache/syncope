package org.apache.syncope.core.persistence.api.entity.am;

import org.apache.syncope.common.lib.password.PasswordManagementConf;
import org.apache.syncope.core.persistence.api.entity.ProvidedKeyEntity;

public interface PasswordManagement extends ProvidedKeyEntity {

    String getDescription();

    void setDescription(String description);

    String getEnabled();

    void setEnabled(String enabled);

    PasswordManagementConf getConf();

    void setConf(PasswordManagementConf conf);
}
