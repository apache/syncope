package org.apache.syncope.core.persistence.jpa.dao.repo;

import org.apache.syncope.core.persistence.api.entity.am.PasswordManagement;

public interface PasswordManagementRepoExt {

    boolean isAnotherInstanceEnabled(String key);

    PasswordManagement save(PasswordManagement passwordManagement);

    void delete(PasswordManagement passwordManagement);
}
