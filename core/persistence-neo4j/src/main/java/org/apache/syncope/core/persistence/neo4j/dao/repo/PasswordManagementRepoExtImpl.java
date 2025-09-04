package org.apache.syncope.core.persistence.neo4j.dao.repo;

import java.util.Map;
import org.apache.syncope.core.persistence.api.entity.am.PasswordManagement;
import org.apache.syncope.core.persistence.neo4j.entity.am.Neo4JPasswordManagement;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class PasswordManagementRepoExtImpl implements PasswordManagementRepoExt {

    protected final Neo4jTemplate neo4jTemplate;

    protected final Neo4jClient neo4jClient;

    protected final NodeValidator nodeValidator;

    public PasswordManagementRepoExtImpl(
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator) {
        this.neo4jTemplate = neo4jTemplate;
        this.neo4jClient = neo4jClient;
        this.nodeValidator = nodeValidator;
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    @Override
    public boolean isAnotherInstanceEnabled(final String key) {
        Long count = neo4jClient.query(
                        "MATCH (pm:" + Neo4JPasswordManagement.NODE + ") "
                                + "WHERE pm.enabled = 'true' AND pm.id <> $id "
                                + "RETURN count(pm) AS cnt")
                .bindAll(Map.of("id", key))
                .fetch()
                .one()
                .map(record -> ((Number) record.get("cnt")).longValue())
                .orElse(0L);

        return count > 0;
    }

    @Override
    public PasswordManagement save(final PasswordManagement passwordManagement) {
        PasswordManagement saved = neo4jTemplate.save(nodeValidator.validate(passwordManagement));
        return saved;
    }

    @Override
    public void delete(final PasswordManagement passwordManagement) {
        neo4jTemplate.deleteById(passwordManagement.getKey(), Neo4JPasswordManagement.class);
    }
}
