package org.apache.syncope.core.persistence.jpa.dao.repo;

import org.apache.syncope.core.persistence.api.entity.am.PasswordManagement;

public interface PasswordManagementRepoExt {

    PasswordManagement save(PasswordManagement passwordManagement);

    void delete(PasswordManagement passwordManagement);
}
