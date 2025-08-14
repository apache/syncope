package org.apache.syncope.core.persistence.jpa.dao.repo;

import org.apache.syncope.core.persistence.api.entity.am.PasswordModule;

public interface PasswordModuleRepoExt {

    PasswordModule save(PasswordModule passwordModule);

    void delete(PasswordModule passwordModule);
}
