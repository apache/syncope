package org.apache.syncope.core.persistence.api.dao;

import org.apache.syncope.core.persistence.api.entity.am.PasswordManagement;

public interface PasswordManagementDAO extends DAO<PasswordManagement> {

    boolean isAnotherInstanceEnabled(String key);
}
