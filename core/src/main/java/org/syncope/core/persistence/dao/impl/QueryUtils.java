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

import org.syncope.client.to.LeafSearchCondition;
import org.syncope.client.to.NodeSearchCondition;

public class QueryUtils {

    private static StringBuilder getUserSearchQueryPart(
            LeafSearchCondition leafSearchCondition) {

        StringBuilder result = new StringBuilder();

        String expression = null;
        if (leafSearchCondition.getExpression() != null) {
            expression = "'"
                    + leafSearchCondition.getExpression().replaceAll("'", "\\'")
                    + "'";
        }

        switch (leafSearchCondition.getType()) {

            case EQ:
                result.append("schema.name = '").append(
                        leafSearchCondition.getSchema()).append("' AND (");
                result.append(" uav.stringValue = ").append(expression);
                result.append(" OR uav.booleanValue = ").append(expression);
                result.append(" OR uav.dateValue = ").append(expression);
                result.append(" OR uav.longValue = ").append(expression);
                result.append(" OR uav.doubleValue = ").append(expression);
                result.append(")");
                break;

            case LIKE:
                result.append("schema.name = '").append(
                        leafSearchCondition.getSchema()).append("' AND (");
                result.append(" uav.stringValue LIKE ").append(expression);
                result.append(" OR uav.booleanValue LIKE ").append(expression);
                result.append(" OR uav.dateValue LIKE ").append(expression);
                result.append(" OR uav.longValue LIKE ").append(expression);
                result.append(" OR uav.doubleValue LIKE ").append(expression);
                result.append(")");
                break;

            case GT:
                result.append("schema.name = '").append(
                        leafSearchCondition.getSchema()).append("' AND (");
                result.append(" uav.stringValue > ").append(expression);
                result.append(" OR uav.booleanValue > ").append(expression);
                result.append(" OR uav.dateValue > ").append(expression);
                result.append(" OR uav.longValue > ").append(expression);
                result.append(" OR uav.doubleValue > ").append(expression);
                result.append(")");
                break;

            case GE:
                result.append("schema.name = '").append(
                        leafSearchCondition.getSchema()).append("' AND (");
                result.append(" uav.stringValue >= ").append(expression);
                result.append(" OR uav.booleanValue >= ").append(expression);
                result.append(" OR uav.dateValue >= ").append(expression);
                result.append(" OR uav.longValue >= ").append(expression);
                result.append(" OR uav.doubleValue >= ").append(expression);
                result.append(")");
                break;

            case LT:
                result.append("schema.name = '").append(
                        leafSearchCondition.getSchema()).append("' AND (");
                result.append(" uav.stringValue < ").append(expression);
                result.append(" OR uav.booleanValue < ").append(expression);
                result.append(" OR uav.dateValue < ").append(expression);
                result.append(" OR uav.longValue < ").append(expression);
                result.append(" OR uav.doubleValue < ").append(expression);
                result.append(")");
                break;

            case LE:
                result.append("schema.name = '").append(
                        leafSearchCondition.getSchema()).append("' AND (");
                result.append(" uav.stringValue <= ").append(expression);
                result.append(" OR uav.booleanValue <= ").append(expression);
                result.append(" OR uav.dateValue <= ").append(expression);
                result.append(" OR uav.longValue <= ").append(expression);
                result.append(" OR uav.doubleValue <= ").append(expression);
                result.append(")");
                break;
        }

        return result;
    }

    private static StringBuilder getUserSearchQueryPart(
            NodeSearchCondition searchCondition) {

        StringBuilder result = new StringBuilder();

        switch (searchCondition.getType()) {

            case LEAF:
                result = getUserSearchQueryPart(
                        searchCondition.getLeafSearchCondition());
                break;

            case NOT:
                result.append("NOT (").append(getUserSearchQueryPart(
                        searchCondition.getLeftNodeSearchCondition())).
                        append(")");
                break;

            case AND:
                result.append("(").append(getUserSearchQueryPart(
                        searchCondition.getLeftNodeSearchCondition())).
                        append(") AND (").append(getUserSearchQueryPart(
                        searchCondition.getRightNodeSearchCondition())).
                        append(")");
                break;
            case OR:
                result.append("(").append(getUserSearchQueryPart(
                        searchCondition.getLeftNodeSearchCondition())).
                        append(") OR (").append(getUserSearchQueryPart(
                        searchCondition.getRightNodeSearchCondition())).
                        append(")");
                break;
        }

        return result;
    }

    public static String getUserSearchQuery(
            NodeSearchCondition searchCondition) {

        StringBuilder queryString = new StringBuilder();
        queryString.append("SELECT u ").
                append("FROM SyncopeUser u, ").
                append("UserSchema schema, ").
                append("UserAttribute ua, ").
                append("UserAttributeValue uav ").
                append("WHERE uav.attribute = ua ").
                append("AND ua.schema = schema ").
                append("AND ua.owner = u ").
                append("AND (");

        queryString.append(QueryUtils.getUserSearchQueryPart(searchCondition));

        queryString.append(")");

        return queryString.toString();
    }
}
