/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.persistence.dao.impl;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;
import org.apache.commons.jexl2.parser.Parser;
import org.apache.commons.jexl2.parser.ParserConstants;
import org.apache.commons.jexl2.parser.Token;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import org.syncope.core.persistence.beans.AbstractAttrValue;
import org.syncope.core.persistence.beans.AbstractVirAttr;
import org.syncope.core.persistence.beans.PropagationTask;
import org.syncope.core.persistence.beans.membership.Membership;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.beans.user.UAttrUniqueValue;
import org.syncope.core.persistence.beans.user.UAttrValue;
import org.syncope.core.persistence.beans.user.UDerSchema;
import org.syncope.core.persistence.beans.user.USchema;
import org.syncope.core.persistence.dao.DerSchemaDAO;
import org.syncope.core.persistence.dao.RoleDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.TaskDAO;
import org.syncope.core.persistence.dao.UserDAO;
import org.syncope.core.rest.controller.InvalidSearchConditionException;

@Repository
public class UserDAOImpl extends AbstractDAOImpl
        implements UserDAO {

    @Autowired
    private SchemaDAO schemaDAO;

    @Autowired
    private DerSchemaDAO derSchemaDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private TaskDAO taskDAO;

    @Override
    public SyncopeUser find(final Long id) {
        TypedQuery<SyncopeUser> query = entityManager.createQuery(
                "SELECT e FROM " + SyncopeUser.class.getSimpleName() + " e "
                + "WHERE e.id = :id", SyncopeUser.class);
        query.setParameter("id", id);

        SyncopeUser result = null;
        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
        }

        return result;
    }

    @Override
    public SyncopeUser find(final String username) {
        TypedQuery<SyncopeUser> query = entityManager.createQuery(
                "SELECT e FROM " + SyncopeUser.class.getSimpleName() + " e "
                + "WHERE e.username = :username", SyncopeUser.class);
        query.setParameter("username", username);

        SyncopeUser result = null;
        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
        }

        return result;
    }

    @Override
    public SyncopeUser findByWorkflowId(final String workflowId) {
        TypedQuery<SyncopeUser> query = entityManager.createQuery(
                "SELECT e FROM " + SyncopeUser.class.getSimpleName() + " e "
                + "WHERE e.workflowId = :workflowId", SyncopeUser.class);
        query.setParameter("workflowId", workflowId);

        return query.getSingleResult();
    }

    /**
     * Find users by derived attribute value. This method could fail if one or
     * more string literals contained into the derived attribute value provided
     * derive from identifier (schema name) replacement. When you are going to
     * specify a derived attribute expression you must be quite sure that string
     * literals used to build the expression cannot be found into the attribute
     * values used to replace attribute schema names used as identifiers.
     *
     * @param schemaName derived schema name.
     * @param value derived attribute value.
     * @return list of users.
     * @throws InvalidSearchConditionException in case of errors retrieving
     * schema names used to buid the derived schema expression.
     */
    @Override
    public List<SyncopeUser> findByDerAttrValue(
            final String schemaName, final String value)
            throws InvalidSearchConditionException {

        UDerSchema schema = derSchemaDAO.find(schemaName, UDerSchema.class);
        if (schema == null) {
            LOG.error("Invalid schema name '{}'", schemaName);
            return Collections.EMPTY_LIST;
        }

        // query string
        final StringBuilder querystring = new StringBuilder();

        boolean subquery = false;
        for (String clause : getWhereClause(schema.getExpression(), value)) {
            if (querystring.length() > 0) {
                subquery = true;
                querystring.append(" AND a.owner_id IN ( ");
            }

            querystring.append("SELECT a.owner_id ").
                    append("FROM UAttr a, UAttrValue v, USchema s ").
                    append("WHERE ").append(clause);

            if (subquery) {
                querystring.append(')');
            }
        }

        LOG.debug("Execute query {}", querystring);

        final Query query = entityManager.createNativeQuery(
                querystring.toString());

        final List<SyncopeUser> result = new ArrayList<SyncopeUser>();

        SyncopeUser user;
        for (Object userId : query.getResultList()) {
            user = find(Long.parseLong(userId.toString()));
            if (!result.contains(user)) {
                result.add(user);
            }
        }

        return result;
    }

    @Override
    public List<SyncopeUser> findByAttrValue(final String schemaName,
            final UAttrValue attrValue) {

        USchema schema = schemaDAO.find(schemaName, USchema.class);
        if (schema == null) {
            LOG.error("Invalid schema name '{}'", schemaName);
            return Collections.EMPTY_LIST;
        }

        final String entityName = schema.isUniqueConstraint()
                ? UAttrUniqueValue.class.getName() : UAttrValue.class.getName();

        Query query = entityManager.createQuery(
                "SELECT e FROM " + entityName + " e"
                + " WHERE e.attribute.schema.name = :schemaName "
                + " AND (e.stringValue IS NOT NULL"
                + " AND e.stringValue = :stringValue)"
                + " OR (e.booleanValue IS NOT NULL"
                + " AND e.booleanValue = :booleanValue)"
                + " OR (e.dateValue IS NOT NULL"
                + " AND e.dateValue = :dateValue)"
                + " OR (e.longValue IS NOT NULL"
                + " AND e.longValue = :longValue)"
                + " OR (e.doubleValue IS NOT NULL"
                + " AND e.doubleValue = :doubleValue)");

        query.setParameter("schemaName", schemaName);
        query.setParameter("stringValue", attrValue.getStringValue());
        query.setParameter("booleanValue",
                attrValue.getBooleanValue() == null ? null
                : attrValue.getBooleanAsInteger(attrValue.getBooleanValue()));
        if (attrValue.getDateValue() != null) {
            query.setParameter("dateValue",
                    attrValue.getDateValue(), TemporalType.TIMESTAMP);
        } else {
            query.setParameter("dateValue", null);
        }
        query.setParameter("longValue", attrValue.getLongValue());
        query.setParameter("doubleValue", attrValue.getDoubleValue());

        List<SyncopeUser> result = new ArrayList<SyncopeUser>();
        SyncopeUser user;
        for (AbstractAttrValue value :
                (List<AbstractAttrValue>) query.getResultList()) {

            user = (SyncopeUser) value.getAttribute().getOwner();
            if (!result.contains(user)) {
                result.add(user);
            }
        }

        return result;
    }

    @Override
    public SyncopeUser findByAttrUniqueValue(final String schemaName,
            final UAttrValue attrUniqueValue) {

        USchema schema = schemaDAO.find(schemaName, USchema.class);
        if (schema == null) {
            LOG.error("Invalid schema name '{}'", schemaName);
            return null;
        }
        if (!schema.isUniqueConstraint()) {
            LOG.error("This schema has not unique constraint: '{}'",
                    schemaName);
            return null;
        }

        List<SyncopeUser> result = findByAttrValue(schemaName, attrUniqueValue);
        return result.isEmpty() ? null : result.iterator().next();
    }

    private StringBuilder getFindAllQuery(final Set<Long> adminRoles) {
        final StringBuilder queryString = new StringBuilder(
                "SELECT id FROM SyncopeUser WHERE id NOT IN (");

        if (adminRoles == null || adminRoles.isEmpty()) {
            queryString.append("SELECT syncopeUser_id AS id FROM Membership");
        } else {
            queryString.append("SELECT syncopeUser_id FROM Membership M1 ").
                    append("WHERE syncopeRole_id IN (");
            queryString.append("SELECT syncopeRole_id FROM Membership M2 ").
                    append("WHERE M2.syncopeUser_id=M1.syncopeUser_id ").
                    append("AND syncopeRole_id NOT IN (");

            queryString.append("SELECT id AS syncopeRole_id FROM SyncopeRole");
            boolean firstRole = true;
            for (Long adminRoleId : adminRoles) {
                if (firstRole) {
                    queryString.append(" WHERE");
                    firstRole = false;
                } else {
                    queryString.append(" OR");
                }

                queryString.append(" id=").append(adminRoleId);
            }

            queryString.append("))");
        }
        queryString.append(")");

        return queryString;
    }

    @Override
    public final List<SyncopeUser> findAll(final Set<Long> adminRoles) {
        return findAll(adminRoles, -1, -1);
    }

    @Override
    public final List<SyncopeUser> findAll(final Set<Long> adminRoles,
            final int page, final int itemsPerPage) {

        final Query query = entityManager.createNativeQuery(
                getFindAllQuery(adminRoles).toString());

        query.setFirstResult(itemsPerPage * (page <= 0 ? 0 : page - 1));

        if (itemsPerPage > 0) {
            query.setMaxResults(itemsPerPage);
        }

        List<Number> userIds = new ArrayList<Number>();
        List resultList = query.getResultList();

        //fix for HHH-5902 - bug hibernate
        if (resultList != null) {
            for (Object userId : resultList) {
                if (userId instanceof Object[]) {
                    userIds.add((Number) ((Object[]) userId)[0]);
                } else {
                    userIds.add((Number) userId);
                }
            }
        }

        List<SyncopeUser> result =
                new ArrayList<SyncopeUser>(userIds.size());

        SyncopeUser user;
        for (Object userId : userIds) {
            user = find(((Number) userId).longValue());
            if (user == null) {
                LOG.error("Could not find user with id {}, "
                        + "even though returned by the native query", userId);
            } else {
                result.add(user);
            }
        }

        return result;
    }

    @Override
    public final Integer count(final Set<Long> adminRoles) {
        StringBuilder queryString = getFindAllQuery(adminRoles);
        queryString.insert(0, "SELECT COUNT(id) FROM (");
        queryString.append(") count_user_id");

        Query countQuery =
                entityManager.createNativeQuery(queryString.toString());

        return ((Number) countQuery.getSingleResult()).intValue();
    }

    @Override
    public SyncopeUser save(final SyncopeUser user) {
        SyncopeUser merged = entityManager.merge(user);

        for (AbstractVirAttr virtual : merged.getVirtualAttributes()) {
            virtual.setValues(user.getVirtualAttribute(
                    virtual.getVirtualSchema().getName()).getValues());
        }

        return merged;
    }

    @Override
    public void delete(final Long id) {
        SyncopeUser user = find(id);
        if (user == null) {
            return;
        }

        delete(user);
    }

    @Override
    public void delete(final SyncopeUser user) {
        // Not calling membershipDAO.delete() here because it would try
        // to save this user as well, thus going into
        // ConcurrentModificationException
        for (Membership membership : user.getMemberships()) {
            membership.setSyncopeUser(null);

            roleDAO.save(membership.getSyncopeRole());
            membership.setSyncopeRole(null);

            entityManager.remove(membership);
        }
        user.getMemberships().clear();

        for (PropagationTask task : taskDAO.findAll(user)) {
            task.setSyncopeUser(null);
        }

        entityManager.remove(user);
    }

    /**
     * Generate one where clause for each different attribute schema into the
     * derived schema expression provided.
     *
     * @param expression derived schema expression.
     * @param value derived attribute value.
     * @return where clauses to use to build the query.
     * @throws InvalidSearchConditionException in case of errors retrieving
     * identifiers.
     */
    private Set<String> getWhereClause(
            final String expression, final String value)
            throws InvalidSearchConditionException {
        final Parser parser = new Parser(new StringReader(expression));

        // Schema names
        final List<String> identifiers = new ArrayList<String>();

        // Literals
        final List<String> literals = new ArrayList<String>();

        // Get schema names and literals
        Token token;
        while ((token = parser.getNextToken()) != null
                && StringUtils.hasText(token.toString())) {

            if (token.kind == ParserConstants.STRING_LITERAL) {
                literals.add(token.toString().
                        substring(1, token.toString().length() - 1));
            }

            if (token.kind == ParserConstants.IDENTIFIER) {
                identifiers.add(token.toString());
            }
        }

        // Sort literals in order to process later literals included into others
        Collections.sort(literals, new Comparator<String>() {

            @Override
            public int compare(String t, String t1) {
                if (t == null && t1 == null) {
                    return 0;
                }

                if (t != null && t1 == null) {
                    return -1;
                }

                if (t == null && t1 != null) {
                    return 1;
                }

                if (t.length() == t1.length()) {
                    return 0;
                }

                if (t.length() > t1.length()) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });

        // Split value on provided literals
        final List<String> attrValues = split(value, literals);

        if (attrValues.size() != identifiers.size()) {
            LOG.error("Ambiguous jexl expression resolution.");
            throw new InvalidSearchConditionException(
                    "literals and values have different size");
        }

        // clauses to be used with INTERSECTed queries
        final Set<String> clauses = new HashSet<String>();

        // builder to build the clauses
        final StringBuilder bld = new StringBuilder();

        // Contains used identifiers in order to avoid replications
        final Set<String> used = new HashSet<String>();

        USchema schema;

        // Create several clauses: one for eanch identifiers
        for (int i = 0; i < identifiers.size(); i++) {
            if (!used.contains(identifiers.get(i))) {

                // verify schema existence and get schema type
                schema = schemaDAO.find(identifiers.get(i), USchema.class);
                if (schema == null) {
                    LOG.error("Invalid schema name '{}'", identifiers.get(i));
                    throw new InvalidSearchConditionException(
                            "Invalid schema name " + identifiers.get(i));
                }

                // clear builder
                bld.delete(0, bld.length());

                bld.append("(");

                // set schema name
                bld.append("s.name = '").
                        append(identifiers.get(i)).append("'");

                bld.append(" AND ");

                bld.append("s.name = a.schema_name").append(" AND ");

                bld.append("a.id = v.attribute_id");

                bld.append(" AND ");

                // use a value clause different for eanch different schema type
                switch (schema.getType()) {
                    case Boolean:
                        bld.append("v.booleanValue = '").
                                append(attrValues.get(i)).append("'");
                        break;
                    case Long:
                        bld.append("v.longValue = ").
                                append(attrValues.get(i));
                        break;
                    case Double:
                        bld.append("v.doubleValue = ").
                                append(attrValues.get(i));
                        break;
                    case Date:
                        bld.append("v.dateValue = '").
                                append(attrValues.get(i)).append("'");
                        break;
                    default:
                        bld.append("v.stringValue = '").
                                append(attrValues.get(i)).append("'");
                }

                bld.append(")");

                used.add(identifiers.get(i));

                clauses.add(bld.toString());
            }
        }

        LOG.debug("Generated where clauses {}", clauses);

        return clauses;
    }

    /**
     * Split an attribute value recurring on provided literals/tokens.
     *
     * @param attrValue value to be splitted.
     * @param literals literals/tokens.
     * @return
     */
    private List<String> split(
            final String attrValue,
            final List<String> literals) {

        final List<String> attrValues = new ArrayList<String>();

        if (literals.isEmpty()) {
            attrValues.add(attrValue);
        } else {

            for (String token :
                    attrValue.split(Pattern.quote(literals.get(0)))) {

                attrValues.addAll(
                        split(token, literals.subList(1, literals.size())));
            }
        }

        return attrValues;
    }
}
