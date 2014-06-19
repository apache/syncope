/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.console.pages.panels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.cxf.jaxrs.ext.search.ConditionType;
import org.apache.cxf.jaxrs.ext.search.SearchBean;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.client.CompleteCondition;
import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;
import org.apache.syncope.common.search.RoleFiqlSearchConditionBuilder;
import org.apache.syncope.common.search.SearchableFields;
import org.apache.syncope.common.search.SpecialAttr;
import org.apache.syncope.common.search.SyncopeFiqlSearchConditionBuilder;
import org.apache.syncope.common.search.SyncopeProperty;
import org.apache.syncope.common.search.UserFiqlSearchConditionBuilder;
import org.apache.syncope.common.to.ResourceTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.console.rest.AuthRestClient;
import org.apache.syncope.console.rest.ResourceRestClient;
import org.apache.syncope.console.rest.SchemaRestClient;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSearchPanel extends Panel {

    private static final long serialVersionUID = 5922413053568696414L;

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractSearchPanel.class);

    @SpringBean
    protected SchemaRestClient schemaRestClient;

    @SpringBean
    protected ResourceRestClient resourceRestClient;

    @SpringBean
    protected AuthRestClient authRestClient;

    protected IModel<List<String>> dnames;

    protected IModel<List<String>> anames;

    protected IModel<List<String>> resourceNames;

    protected IModel<List<String>> entitlements;

    protected IModel<List<SearchClause.Type>> types;

    protected IModel<List<String>> roleNames;

    protected NotificationPanel searchFeedback;

    protected List<SearchClause> searchClauses;

    protected WebMarkupContainer searchFormContainer;

    protected AttributableType attributableType;

    protected boolean required;

    protected AbstractSearchPanel(final String id, final AttributableType attributableType) {
        this(id, attributableType, null, true);
    }

    protected AbstractSearchPanel(final String id, final AttributableType attributableType,
            final String fiql, final boolean required) {

        super(id);
        populate();

        this.attributableType = attributableType;
        this.required = required;

        setOutputMarkupId(true);

        searchFormContainer = new WebMarkupContainer("searchFormContainer");
        searchFormContainer.setOutputMarkupId(true);

        searchFeedback = new NotificationPanel("searchFeedback", "notificationpanel_top_right",
                new IFeedbackMessageFilter() {

                    private static final long serialVersionUID = 6895024863321391672L;

                    @Override
                    public boolean accept(final FeedbackMessage message) {
                        boolean result;

                        // messages reported on the session have a null reporter
                        if (message.getReporter() == null) {
                            result = false;
                        } else {
                            // only accept messages coming from the children of the search form container
                            result = searchFormContainer.contains(message.getReporter(), true);
                        }

                        return result;
                    }
                });
        searchFeedback.setOutputMarkupId(true);
        add(searchFeedback);

        this.searchClauses = new ArrayList<SearchClause>();
        this.searchClauses.add(new SearchClause());
        if (StringUtils.isNotBlank(fiql)) {
            try {
                FiqlParser<SearchBean> fiqlParser = new FiqlParser<SearchBean>(
                        SearchBean.class, SyncopeFiqlSearchConditionBuilder.CONTEXTUAL_PROPERTIES);
                List<SearchClause> parsed = getSearchClauses(fiqlParser.parse(fiql));

                this.searchClauses.clear();
                this.searchClauses.addAll(parsed);
            } catch (Exception e) {
                LOG.error("Unparseable FIQL expression '{}'", fiql, e);
            }
        }

        searchFormContainer.add(new SearchView("searchView", searchClauses, searchFormContainer, required,
                types, anames, dnames, roleNames, resourceNames, entitlements));
        add(searchFormContainer);
    }

    protected void populate() {
        dnames = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return SearchableFields.get(attributableType);
            }
        };

        anames = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return schemaRestClient.getSchemaNames(attributableType);
            }
        };

        resourceNames = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                List<ResourceTO> resourceTOs = resourceRestClient.getAllResources();

                List<String> result = new ArrayList<String>(resourceTOs.size());

                for (ResourceTO resource : resourceTOs) {
                    result.add(resource.getName());
                }

                return result;
            }
        };

        entitlements = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                List<String> result = authRestClient.getOwnedEntitlements();
                Collections.sort(result);
                return result;
            }
        };
    }

    public NotificationPanel getSearchFeedback() {
        return searchFeedback;
    }

    private SearchClause getPrimitiveSearchClause(final SearchCondition<SearchBean> sc) {
        SearchClause searchClause = new SearchClause();

        String property = sc.getCondition().getKeySet().iterator().next();
        searchClause.setProperty(property);
        String value = sc.getCondition().get(property);
        searchClause.setValue(value);

        if (SpecialAttr.ROLES.toString().equals(property)) {
            searchClause.setType(SearchClause.Type.MEMBERSHIP);
            for (String label : roleNames.getObject()) {
                if (value.equals(label.substring(0, label.indexOf(' ')))) {
                    searchClause.setProperty(label);
                }
            }
        } else if (SpecialAttr.RESOURCES.toString().equals(property)) {
            searchClause.setType(SearchClause.Type.RESOURCE);
        } else if (SpecialAttr.ENTITLEMENTS.toString().equals(property)) {
            searchClause.setType(SearchClause.Type.ENTITLEMENT);
        } else {
            searchClause.setType(SearchClause.Type.ATTRIBUTE);
        }

        switch (sc.getConditionType()) {
            case EQUALS:
                searchClause.setComparator(SpecialAttr.NULL.toString().equals(value)
                        ? SearchClause.Comparator.IS_NULL : SearchClause.Comparator.EQUALS);
                break;

            case NOT_EQUALS:
                searchClause.setComparator(SpecialAttr.NULL.toString().equals(value)
                        ? SearchClause.Comparator.IS_NOT_NULL : SearchClause.Comparator.NOT_EQUALS);
                break;

            case GREATER_OR_EQUALS:
                searchClause.setComparator(SearchClause.Comparator.GREATER_OR_EQUALS);
                break;

            case GREATER_THAN:
                searchClause.setComparator(SearchClause.Comparator.GREATER_THAN);
                break;

            case LESS_OR_EQUALS:
                searchClause.setComparator(SearchClause.Comparator.LESS_OR_EQUALS);
                break;

            case LESS_THAN:
                searchClause.setComparator(SearchClause.Comparator.LESS_THAN);
                break;

            default:
                break;
        }

        return searchClause;
    }

    private List<SearchClause> getCompoundSearchClause(final SearchCondition<SearchBean> sc) {
        List<SearchClause> clauses = new ArrayList<SearchClause>();

        for (SearchCondition<SearchBean> searchCondition : sc.getSearchConditions()) {
            if (searchCondition.getStatement() == null) {
                clauses.addAll(getCompoundSearchClause(searchCondition));
            } else {
                SearchClause clause = getPrimitiveSearchClause(searchCondition);
                if (sc.getConditionType() == ConditionType.AND) {
                    clause.setOperator(SearchClause.Operator.AND);
                }
                if (sc.getConditionType() == ConditionType.OR) {
                    clause.setOperator(SearchClause.Operator.OR);
                }
                clauses.add(clause);
            }
        }

        return clauses;
    }

    private List<SearchClause> getSearchClauses(final SearchCondition<SearchBean> sc) {
        List<SearchClause> clauses = new ArrayList<SearchClause>();

        if (sc.getStatement() == null) {
            clauses.addAll(getCompoundSearchClause(sc));
        } else {
            clauses.add(getPrimitiveSearchClause(sc));
        }

        return clauses;
    }

    protected abstract SyncopeFiqlSearchConditionBuilder getSearchConditionBuilder();

    public String buildFIQL() {
        LOG.debug("Generating FIQL from List<SearchClause>: {}", searchClauses);

        if (searchClauses.isEmpty() || searchClauses.get(0).getType() == null) {
            return StringUtils.EMPTY;
        }

        SyncopeFiqlSearchConditionBuilder builder = getSearchConditionBuilder();

        CompleteCondition prevCondition;
        CompleteCondition condition = null;
        for (int i = 0; i < searchClauses.size(); i++) {
            prevCondition = condition;

            switch (searchClauses.get(i).getType()) {
                case ENTITLEMENT:
                    condition = searchClauses.get(i).getComparator() == SearchClause.Comparator.EQUALS
                            ? ((RoleFiqlSearchConditionBuilder) builder).
                            hasEntitlements(searchClauses.get(i).getProperty())
                            : ((RoleFiqlSearchConditionBuilder) builder).
                            hasNotEntitlements(searchClauses.get(i).getProperty());
                    break;

                case MEMBERSHIP:
                    Long roleId = NumberUtils.toLong(searchClauses.get(i).getProperty().split(" ")[0]);
                    condition = searchClauses.get(i).getComparator() == SearchClause.Comparator.EQUALS
                            ? ((UserFiqlSearchConditionBuilder) builder).hasRoles(roleId)
                            : ((UserFiqlSearchConditionBuilder) builder).hasNotRoles(roleId);
                    break;

                case RESOURCE:
                    condition = searchClauses.get(i).getComparator() == SearchClause.Comparator.EQUALS
                            ? builder.hasResources(searchClauses.get(i).getProperty())
                            : builder.hasNotResources(searchClauses.get(i).getProperty());
                    break;

                case ATTRIBUTE:
                    SyncopeProperty property = builder.is(searchClauses.get(i).getProperty());
                    switch (searchClauses.get(i).getComparator()) {
                        case IS_NULL:
                            condition = builder.isNull(searchClauses.get(i).getProperty());
                            break;

                        case IS_NOT_NULL:
                            condition = builder.isNotNull(searchClauses.get(i).getProperty());
                            break;

                        case LESS_THAN:
                            condition = StringUtils.isNumeric(searchClauses.get(i).getProperty())
                                    ? property.lessThan(NumberUtils.toDouble(searchClauses.get(i).getValue()))
                                    : property.lexicalBefore(searchClauses.get(i).getValue());
                            break;

                        case LESS_OR_EQUALS:
                            condition = StringUtils.isNumeric(searchClauses.get(i).getProperty())
                                    ? property.lessOrEqualTo(NumberUtils.toDouble(searchClauses.get(i).getValue()))
                                    : property.lexicalNotAfter(searchClauses.get(i).getValue());
                            break;

                        case GREATER_THAN:
                            condition = StringUtils.isNumeric(searchClauses.get(i).getProperty())
                                    ? property.greaterThan(NumberUtils.toDouble(searchClauses.get(i).getValue()))
                                    : property.lexicalAfter(searchClauses.get(i).getValue());
                            break;

                        case GREATER_OR_EQUALS:
                            condition = StringUtils.isNumeric(searchClauses.get(i).getProperty())
                                    ? property.greaterOrEqualTo(NumberUtils.toDouble(searchClauses.get(i).getValue()))
                                    : property.lexicalNotBefore(searchClauses.get(i).getValue());
                            break;

                        case NOT_EQUALS:
                            condition = property.notEqualTo(searchClauses.get(i).getValue());
                            break;

                        case EQUALS:
                        default:
                            condition = property.equalTo(searchClauses.get(i).getValue());
                            break;
                    }
                default:
                    break;
            }

            if (i > 0) {
                if (searchClauses.get(i).getOperator() == SearchClause.Operator.AND) {
                    condition = builder.and(prevCondition, condition);
                }
                if (searchClauses.get(i).getOperator() == SearchClause.Operator.OR) {
                    condition = builder.or(prevCondition, condition);
                }
            }
        }

        String fiql = condition == null ? StringUtils.EMPTY : condition.query();
        LOG.debug("Generated FIQL: {}", fiql);
        return fiql;
    }
}
