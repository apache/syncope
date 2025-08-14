package org.apache.syncope.core.persistence.neo4j.dao.repo;

import org.apache.syncope.core.persistence.api.entity.am.PasswordModule;

public interface PasswordModuleRepoExt {

    PasswordModule save(PasswordModule passwordModule);

    void delete(PasswordModule passwordModule);
}
