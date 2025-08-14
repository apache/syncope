package org.apache.syncope.core.persistence.neo4j.dao.repo;

import org.apache.syncope.core.persistence.api.dao.PasswordModuleDAO;
import org.apache.syncope.core.persistence.neo4j.entity.am.Neo4jPasswordModule;
import org.springframework.data.repository.ListCrudRepository;

public interface PasswordModuleRepo
        extends ListCrudRepository<Neo4jPasswordModule, String>, PasswordModuleRepoExt, PasswordModuleDAO {
}
