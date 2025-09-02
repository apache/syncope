package org.apache.syncope.core.persistence.jpa.dao.repo;

import jakarta.persistence.EntityManager;
import org.apache.syncope.core.persistence.api.entity.am.PasswordManagement;
import org.apache.syncope.core.persistence.jpa.entity.am.JPAPasswordManagement;

public class PasswordManagementRepoExtImpl implements PasswordManagementRepoExt {

    protected final EntityManager entityManager;

    public PasswordManagementRepoExtImpl(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }


    @Override
    public PasswordManagement save(final PasswordManagement passwordManagement) {
        ((JPAPasswordManagement) passwordManagement).list2json();
        return entityManager.merge(passwordManagement);
    }

    @Override
    public void delete(final PasswordManagement passwordManagement) {
        entityManager.remove(passwordManagement);
    }
}
