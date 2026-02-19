/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * \"License\"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.persistence.jpa.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Strings;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.jpa.entity.JPARealm;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.springframework.data.domain.Pageable;

public class MySQLJPARealmSearchDAO extends AbstractJPARealmSearchDAO {

    public MySQLJPARealmSearchDAO(
            final EntityManager entityManager,
            final EntityManagerFactory entityManagerFactory,
            final PlainSchemaDAO plainSchemaDAO,
            final EntityFactory entityFactory,
            final PlainAttrValidationManager validator) {
        super(entityManager, entityManagerFactory, plainSchemaDAO, entityFactory, validator);
    }

    @Override
    protected AttrCondQuery getQuery(
            final AttrCond cond,
            final boolean not,
            final CheckResult<AttrCond> checked,
            final List<Object> parameters) {

        if (not) {
            if (cond.getType() == AttrCond.Type.ISNULL) {
                cond.setType(AttrCond.Type.ISNOTNULL);
            } else if (cond.getType() == AttrCond.Type.ISNOTNULL) {
                cond.setType(AttrCond.Type.ISNULL);
            }
        }

        switch (cond.getType()) {
            case ISNOTNULL -> {
                return new AttrCondQuery(true, new RealmSearchNode.Leaf(
                        "JSON_SEARCH("
                        + "e.plainAttrs, 'one', '" + checked.schema().getKey() + "', NULL, '$[*].schema'"
                        + ") IS NOT NULL"));
            }

            case ISNULL -> {
                return new AttrCondQuery(true, new RealmSearchNode.Leaf(
                        "JSON_SEARCH("
                        + "e.plainAttrs, 'one', '" + checked.schema().getKey() + "', NULL, '$[*].schema'"
                        + ") IS NULL"));
            }

            default -> {
                if (cond.getType() == AttrCond.Type.EQ || cond.getType() == AttrCond.Type.IEQ) {
                    PlainAttr container = new PlainAttr();
                    container.setPlainSchema(checked.schema());
                    if (checked.schema().isUniqueConstraint()) {
                        container.setUniqueValue(checked.value());
                    } else {
                        container.add(checked.value());
                    }

                    String jsonContains = "JSON_CONTAINS("
                            + "e.plainAttrs, '" + POJOHelper.serialize(List.of(container)).replace("'", "''")
                            + "')";

                    return new AttrCondQuery(true, new RealmSearchNode.Leaf((not ? "NOT " : "") + jsonContains));
                }

                return new AttrCondQuery(true, new RealmSearchNode.Leaf(ALWAYS_FALSE_CLAUSE));
            }
        }
    }

    @Override
    protected void visitNode(final RealmSearchNode node, final List<String> where) {
        node.asLeaf().ifPresentOrElse(
                leaf -> where.add(leaf.getClause()),
                () -> {
                    List<String> nodeWhere = new ArrayList<>();
                    node.getChildren().forEach(child -> visitNode(child, nodeWhere));
                    String op = " " + node.getType().name() + " ";
                    where.add(nodeWhere.stream().
                            map(w -> w.contains(" AND ") || w.contains(" OR ") ? "(" + w + ")" : w).
                            collect(Collectors.joining(op)));
                });
    }

    @Override
    protected StringBuilder buildDescendantsQuery(
            final Set<String> bases,
            final SearchCond searchCond,
            final List<Object> parameters) {

        String basesClause = bases.stream().
                map(base -> "e.fullpath=?" + setParameter(parameters, base)
                + " OR e.fullpath LIKE ?" + setParameter(
                        parameters, SyncopeConstants.ROOT_REALM.equals(base) ? "/%" : base + "/%")).
                collect(Collectors.joining(" OR "));

        StringBuilder queryString = new StringBuilder("SELECT e.* FROM ").
                append(JPARealm.TABLE).append(" e ").
                append("WHERE (").append(basesClause).append(')');

        getQuery(searchCond, parameters).ifPresent(condition ->
                queryString.append(" AND (").
                        append(buildPlainAttrQuery(condition, parameters, List.of())).
                        append(')'));

        return queryString;
    }

    @Override
    public long countDescendants(final Set<String> bases, final SearchCond searchCond) {
        List<Object> parameters = new ArrayList<>();

        StringBuilder queryString = buildDescendantsQuery(bases, searchCond, parameters);
        Query query = entityManager.createNativeQuery(Strings.CS.replaceOnce(
                queryString.toString(),
                "SELECT e.* ",
                "SELECT COUNT(e.id) "));

        for (int i = 1; i <= parameters.size(); i++) {
            query.setParameter(i, parameters.get(i - 1));
        }

        return ((Number) query.getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Realm> findDescendants(final Set<String> bases, final SearchCond searchCond, final Pageable pageable) {
        List<Object> parameters = new ArrayList<>();

        StringBuilder queryString = buildDescendantsQuery(bases, searchCond, parameters);
        Query query = entityManager.createNativeQuery(
                queryString.append(" ORDER BY e.fullpath").toString(), JPARealm.class);

        for (int i = 1; i <= parameters.size(); i++) {
            query.setParameter(i, parameters.get(i - 1));
        }

        if (pageable.isPaged()) {
            query.setFirstResult(pageable.getPageSize() * pageable.getPageNumber());
            query.setMaxResults(pageable.getPageSize());
        }

        return (List<Realm>) query.getResultList();
    }
}
