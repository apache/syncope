package org.apache.syncope.core.persistence.jpa.dao.repo;

import org.apache.syncope.core.persistence.api.dao.PasswordModuleDAO;
import org.apache.syncope.core.persistence.jpa.entity.am.JPAPasswordModule;
import org.springframework.data.repository.ListCrudRepository;

public interface PasswordModuleRepo
        extends ListCrudRepository<JPAPasswordModule, String>, PasswordModuleRepoExt, PasswordModuleDAO {

}
