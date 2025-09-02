package org.apache.syncope.core.provisioning.api.data;

import org.apache.syncope.common.lib.to.PasswordManagementTO;
import org.apache.syncope.core.persistence.api.entity.am.PasswordManagement;

public interface PasswordManagementDataBinder {

    PasswordManagement create(PasswordManagementTO passwordManagementTO);

    PasswordManagement update(PasswordManagement passwordManagement, PasswordManagementTO passwordManagementTO);

    PasswordManagementTO getPasswordManagementTO(PasswordManagement passwordManagement);
}
