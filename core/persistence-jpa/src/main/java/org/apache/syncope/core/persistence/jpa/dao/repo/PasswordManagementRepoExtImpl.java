package org.apache.syncope.core.persistence.jpa.dao.repo;

import jakarta.persistence.EntityManager;
import org.apache.syncope.core.persistence.api.entity.am.PasswordManagement;
import org.apache.syncope.core.persistence.jpa.entity.am.JPAPasswordManagement;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class PasswordManagementRepoExtImpl implements PasswordManagementRepoExt {

    protected final EntityManager entityManager;

    public PasswordManagementRepoExtImpl(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    @Override
    public boolean isAnotherInstanceEnabled(final String key) {
        Long count = entityManager.createQuery(
                "SELECT COUNT(pm) FROM "
                        + JPAPasswordManagement.class.getSimpleName()
                        + " pm WHERE pm.enabled = 'true' AND pm.id <> :id",
                Long.class)
                .setParameter("id", key)
                .getSingleResult();

        return count > 0;
    }

    @Override
    public PasswordManagement save(final PasswordManagement passwordManagement) {
        return entityManager.merge(passwordManagement);
    }

    @Override
    public void delete(final PasswordManagement passwordManagement) {
        entityManager.remove(passwordManagement);
    }
}
