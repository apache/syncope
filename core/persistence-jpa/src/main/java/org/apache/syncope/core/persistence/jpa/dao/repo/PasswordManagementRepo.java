package org.apache.syncope.core.persistence.jpa.dao.repo;

import org.apache.syncope.core.persistence.api.dao.PasswordManagementDAO;
import org.apache.syncope.core.persistence.jpa.entity.am.JPAPasswordManagement;
import org.springframework.data.repository.ListCrudRepository;

public interface PasswordManagementRepo
        extends ListCrudRepository<JPAPasswordManagement, String>, PasswordManagementRepoExt, PasswordManagementDAO {

}
