package org.apache.syncope.core.persistence.neo4j.dao.repo;

import org.apache.syncope.core.persistence.api.entity.am.PasswordManagement;
import org.apache.syncope.core.persistence.neo4j.entity.am.Neo4JPasswordManagement;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.springframework.data.neo4j.core.Neo4jTemplate;

public class PasswordManagementRepoExtImpl implements PasswordManagementRepoExt {

    protected final Neo4jTemplate neo4jTemplate;

    protected final NodeValidator nodeValidator;

    public PasswordManagementRepoExtImpl(
            final Neo4jTemplate neo4jTemplate,
            final NodeValidator nodeValidator) {
        this.neo4jTemplate = neo4jTemplate;
        this.nodeValidator = nodeValidator;
    }

    @Override
    public PasswordManagement save(final PasswordManagement passwordManagement) {
        ((Neo4JPasswordManagement) passwordManagement).list2json();
        PasswordManagement saved = neo4jTemplate.save(nodeValidator.validate(passwordManagement));
        ((Neo4JPasswordManagement) saved).postSave();
        return saved;
    }

    @Override
    public void delete(final PasswordManagement passwordManagement) {
        neo4jTemplate.deleteById(passwordManagement.getKey(), Neo4JPasswordManagement.class);
    }
}
