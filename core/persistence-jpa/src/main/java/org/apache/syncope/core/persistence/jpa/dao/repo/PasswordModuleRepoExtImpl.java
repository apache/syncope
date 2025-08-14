package org.apache.syncope.core.persistence.jpa.dao.repo;

import jakarta.persistence.EntityManager;
import org.apache.syncope.core.persistence.api.entity.am.PasswordModule;
import org.apache.syncope.core.persistence.jpa.entity.am.JPAPasswordModule;

public class PasswordModuleRepoExtImpl implements  PasswordModuleRepoExt {

    protected final EntityManager entityManager;

    public PasswordModuleRepoExtImpl(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }


    @Override public PasswordModule save(PasswordModule passwordModule) {
        ((JPAPasswordModule) passwordModule).list2json();
        return entityManager.merge(passwordModule);
    }

    @Override public void delete(PasswordModule passwordModule) {
        entityManager.remove(passwordModule);
    }
}
