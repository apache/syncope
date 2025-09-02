package org.apache.syncope.core.persistence.neo4j.dao.repo;

import org.apache.syncope.core.persistence.api.dao.PasswordManagementDAO;
import org.apache.syncope.core.persistence.neo4j.entity.am.Neo4JPasswordManagement;
import org.springframework.data.repository.ListCrudRepository;

public interface PasswordManagementRepo
        extends ListCrudRepository<Neo4JPasswordManagement, String>, PasswordManagementRepoExt, PasswordManagementDAO {
}
