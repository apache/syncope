package org.apache.syncope.core.persistence.neo4j.dao.repo;

import org.apache.syncope.core.persistence.api.entity.am.PasswordModule;
import org.apache.syncope.core.persistence.neo4j.entity.am.Neo4jPasswordModule;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.springframework.data.neo4j.core.Neo4jTemplate;

public class PasswordModuleRepoExtImpl implements PasswordModuleRepoExt {

    protected final Neo4jTemplate neo4jTemplate;

    protected final NodeValidator nodeValidator;

    public PasswordModuleRepoExtImpl(
            final Neo4jTemplate neo4jTemplate,
            final NodeValidator nodeValidator) {
        this.neo4jTemplate = neo4jTemplate;
        this.nodeValidator = nodeValidator;
    }

    @Override
    public PasswordModule save(final PasswordModule passwordModule) {
        ((Neo4jPasswordModule) passwordModule).list2json();
        PasswordModule saved = neo4jTemplate.save(nodeValidator.validate(passwordModule));
        ((Neo4jPasswordModule) saved).postSave();
        return saved;
    }

    @Override
    public void delete(final PasswordModule passwordModule) {
        neo4jTemplate.deleteById(passwordModule.getKey(), Neo4jPasswordModule.class);
    }
}
