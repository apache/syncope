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

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.syncope.client.search.AttributeCond;
import org.syncope.client.search.NodeCond;
import org.syncope.core.persistence.beans.AbstractAttributeValue;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.beans.user.UserAttributeValue;
import org.syncope.core.persistence.beans.user.UserSchema;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.validation.ValidationException;

/**
 * Utility class for searching the db.
 */
@Component
public class SearchUtils {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            SearchUtils.class);
    @Autowired
    private SchemaDAO schemaDAO;

    private Criterion getCriterion(final AttributeCond.Type type,
            final AbstractAttributeValue example) {

        Criterion result = null;
        switch (type) {
            case EQ:
                result = Restrictions.disjunction().
                        add(Restrictions.eq("av.stringValue",
                        example.getStringValue())).
                        add(Restrictions.eq("av.booleanValue",
                        example.getBooleanValue())).
                        add(Restrictions.eq("av.longValue",
                        example.getLongValue())).
                        add(Restrictions.eq("av.doubleValue",
                        example.getDoubleValue())).
                        add(Restrictions.eq("av.dateValue",
                        example.getDateValue()));
                break;

            case GE:
                result = Restrictions.disjunction().
                        add(Restrictions.ge("av.stringValue",
                        example.getStringValue())).
                        add(Restrictions.ge("av.booleanValue",
                        example.getBooleanValue())).
                        add(Restrictions.ge("av.longValue",
                        example.getLongValue())).
                        add(Restrictions.ge("av.doubleValue",
                        example.getDoubleValue())).
                        add(Restrictions.ge("av.dateValue",
                        example.getDateValue()));
                break;

            case GT:
                result = Restrictions.disjunction().
                        add(Restrictions.gt("av.stringValue",
                        example.getStringValue())).
                        add(Restrictions.gt("av.booleanValue",
                        example.getBooleanValue())).
                        add(Restrictions.gt("av.longValue",
                        example.getLongValue())).
                        add(Restrictions.gt("av.doubleValue",
                        example.getDoubleValue())).
                        add(Restrictions.gt("av.dateValue",
                        example.getDateValue()));
                break;

            case ISNOTNULL:
                result = Restrictions.disjunction().
                        add(Restrictions.isNotNull("av.stringValue")).
                        add(Restrictions.isNotNull("av.booleanValue")).
                        add(Restrictions.isNotNull("av.longValue")).
                        add(Restrictions.isNotNull("av.doubleValue")).
                        add(Restrictions.isNotNull("av.dateValue"));
                break;

            case LE:
                result = Restrictions.disjunction().
                        add(Restrictions.le("av.stringValue",
                        example.getStringValue())).
                        add(Restrictions.le("av.booleanValue",
                        example.getBooleanValue())).
                        add(Restrictions.le("av.longValue",
                        example.getLongValue())).
                        add(Restrictions.le("av.doubleValue",
                        example.getDoubleValue())).
                        add(Restrictions.le("av.dateValue",
                        example.getDateValue()));
                break;

            case LIKE:
                result = Restrictions.disjunction().
                        add(Restrictions.like("av.stringValue",
                        example.getStringValue())).
                        add(Restrictions.like("av.booleanValue",
                        example.getBooleanValue())).
                        add(Restrictions.like("av.longValue",
                        example.getLongValue())).
                        add(Restrictions.like("av.doubleValue",
                        example.getDoubleValue())).
                        add(Restrictions.like("av.dateValue",
                        example.getDateValue()));
                break;

            case LT:
                result = Restrictions.disjunction().
                        add(Restrictions.lt("av.stringValue",
                        example.getStringValue())).
                        add(Restrictions.lt("av.booleanValue",
                        example.getBooleanValue())).
                        add(Restrictions.lt("av.longValue",
                        example.getLongValue())).
                        add(Restrictions.lt("av.doubleValue",
                        example.getDoubleValue())).
                        add(Restrictions.lt("av.dateValue",
                        example.getDateValue()));
                break;

            default:
        }
        return result;
    }

    private Criterion getCriterion(final NodeCond nodeCond) {

        Criterion result = null;

        switch (nodeCond.getType()) {
            case LEAF:
                if (nodeCond.getMembershipCond() != null) {
                    if (nodeCond.getMembershipCond().getRoleId() != null) {
                        result = Restrictions.eq("r.id",
                                nodeCond.getMembershipCond().getRoleId());
                    }
                    if (nodeCond.getMembershipCond().getRoleName() != null) {
                        result = Restrictions.eq("r.name",
                                nodeCond.getMembershipCond().getRoleName());
                    }
                } else if (nodeCond.getAttributeCond() != null) {
                    UserSchema userSchema = schemaDAO.find(
                            nodeCond.getAttributeCond().getSchema(),
                            UserSchema.class);
                    if (userSchema == null) {
                        LOG.warn("Ignoring invalid schema '"
                                + nodeCond.getAttributeCond().getSchema()
                                + "'");
                    } else {
                        if (nodeCond.getAttributeCond().getType()
                                == AttributeCond.Type.ISNULL) {

                            result = Restrictions.and(
                                    Restrictions.eq("a.schema.name",
                                    nodeCond.getAttributeCond().getSchema()),
                                    Restrictions.isEmpty("a.values"));
                        } else {
                            try {
                                UserAttributeValue example =
                                        userSchema.getValidator().
                                        getValue(nodeCond.getAttributeCond().
                                        getExpression(),
                                        new UserAttributeValue());
                                result = Restrictions.and(
                                        Restrictions.eq("a.schema.name",
                                        nodeCond.getAttributeCond().
                                        getSchema()),
                                        getCriterion(
                                        nodeCond.getAttributeCond().getType(),
                                        example));
                            } catch (ValidationException e) {
                                LOG.error("Could not validate expression '"
                                        + nodeCond.getAttributeCond().
                                        getExpression() + "'", e);
                            }
                        }
                    }

                }
                break;

            case AND:
                result = Restrictions.and(
                        getCriterion(nodeCond.getLeftNodeCond()),
                        getCriterion(nodeCond.getRightNodeCond()));
                break;

            case OR:
                result = Restrictions.or(
                        getCriterion(nodeCond.getLeftNodeCond()),
                        getCriterion(nodeCond.getRightNodeCond()));
                break;

            case NOT:
                result = Restrictions.not(
                        getCriterion(nodeCond.getLeftNodeCond()));
                break;

            default:
        }

        return result;
    }

    public Criteria buildUserCriteria(final Session hibernateSess,
            final NodeCond searchCondition) {

        Criteria userCriteria = hibernateSess.createCriteria(SyncopeUser.class).
                createAlias("memberships", "m").
                createAlias("m.syncopeRole", "r").
                createAlias("attributes", "a").
                createAlias("a.values", "av");

        userCriteria.add(getCriterion(searchCondition));
        userCriteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

        return userCriteria;
    }
}
