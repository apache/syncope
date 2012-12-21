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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang.ArrayUtils;
import org.apache.syncope.client.search.AttributableCond;
import org.apache.syncope.client.search.AttributeCond;
import org.apache.syncope.client.search.EntitlementCond;
import org.apache.syncope.client.search.MembershipCond;
import org.apache.syncope.client.search.NodeCond;
import org.apache.syncope.client.search.ResourceCond;
import org.apache.syncope.client.to.ResourceTO;
import org.apache.syncope.client.to.RoleTO;
import org.apache.syncope.client.to.UserTO;
import org.apache.syncope.console.commons.SearchCondWrapper;
import org.apache.syncope.console.commons.SearchCondWrapper.OperationType;
import org.apache.syncope.console.rest.AuthRestClient;
import org.apache.syncope.console.rest.ResourceRestClient;
import org.apache.syncope.console.rest.SchemaRestClient;
import org.apache.syncope.types.AttributableType;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
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

    protected static final String[] ATTRIBUTES_NOTINCLUDED = {"attributes", "derivedAttributes", "virtualAttributes",
        "serialVersionUID", "memberships", "entitlements", "resources", "password",
        "propagationTOs", "propagationStatusMap"};

    protected IModel<List<String>> dnames;

    protected IModel<List<String>> anames;

    protected IModel<List<String>> resourceNames;

    protected IModel<List<String>> entitlements;

    protected IModel<List<AttributeCond.Type>> attributeTypes;

    protected IModel<List<SearchCondWrapper.FilterType>> filterTypes;

    protected IModel<List<String>> roleNames;

    @SpringBean
    protected SchemaRestClient schemaRestClient;

    @SpringBean
    protected ResourceRestClient resourceRestClient;

    @SpringBean
    protected AuthRestClient authRestClient;

    protected FeedbackPanel searchFeedback;

    protected List<SearchCondWrapper> searchConditionList;

    protected WebMarkupContainer searchFormContainer;

    protected AttributableType attributableType;

    protected boolean required;

    protected AbstractSearchPanel(final String id, final AttributableType attributableType) {
        this(id, attributableType, null, true);
    }

    protected AbstractSearchPanel(final String id, final AttributableType attributableType, final NodeCond initCond,
            final boolean required) {

        super(id);
        populate();

        this.attributableType = attributableType;
        this.required = required;

        setOutputMarkupId(true);

        searchFormContainer = new WebMarkupContainer("searchFormContainer");
        searchFormContainer.setOutputMarkupId(true);

        searchFeedback = new FeedbackPanel("searchFeedback", new IFeedbackMessageFilter() {

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

        if (initCond == null) {
            searchConditionList = new ArrayList<SearchCondWrapper>();
            searchConditionList.add(new SearchCondWrapper());
        } else {
            searchConditionList = getSearchCondWrappers(initCond, null);
        }
        searchFormContainer.add(new SearchView("searchView", searchConditionList, searchFormContainer, required,
                attributeTypes, filterTypes, anames, dnames, roleNames, resourceNames, entitlements));

        add(searchFormContainer);
    }

    protected void populate() {
        dnames = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                final List<String> details = new ArrayList<String>();

                Class<?> clazz = attributableType == AttributableType.USER
                        ? UserTO.class
                        : RoleTO.class;

                // loop on class and all superclasses searching for field
                while (clazz != null && clazz != Object.class) {
                    for (Field field : clazz.getDeclaredFields()) {
                        if (!ArrayUtils.contains(ATTRIBUTES_NOTINCLUDED, field.getName())) {
                            details.add(field.getName());
                        }
                    }
                    clazz = clazz.getSuperclass();
                }

                Collections.reverse(details);
                return details;
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

        attributeTypes = new LoadableDetachableModel<List<AttributeCond.Type>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<AttributeCond.Type> load() {
                return Arrays.asList(AttributeCond.Type.values());
            }
        };
    }

    public FeedbackPanel getSearchFeedback() {
        return searchFeedback;
    }

    private List<SearchCondWrapper> getSearchCondWrappers(final NodeCond searchCond, final NodeCond.Type type) {
        LOG.debug("Search condition: {}", searchCond);

        List<SearchCondWrapper> wrappers = new ArrayList<SearchCondWrapper>();

        switch (searchCond.getType()) {
            case LEAF:
            case NOT_LEAF:
                final SearchCondWrapper wrapper = getSearchCondWrapper(searchCond);

                if (type != null) {
                    switch (type) {
                        case AND:
                            wrapper.setOperationType(OperationType.AND);
                            break;
                        case OR:
                            wrapper.setOperationType(OperationType.OR);
                            break;
                        default:
                        // nothing to specify
                    }
                }

                wrappers.add(wrapper);
                break;

            case AND:
            case OR:
                wrappers.addAll(getSearchCondWrappers(searchCond.getLeftNodeCond(), type));
                wrappers.addAll(getSearchCondWrappers(searchCond.getRightNodeCond(), searchCond.getType()));
                break;

            default:
        }

        LOG.debug("Search condition wrappers: {}", wrappers);

        return wrappers;
    }

    private SearchCondWrapper getSearchCondWrapper(final NodeCond searchCond) {
        SearchCondWrapper wrapper = new SearchCondWrapper();

        if (searchCond.getAttributableCond() != null) {
            wrapper.setFilterType(SearchCondWrapper.FilterType.ATTRIBUTE);
            wrapper.setFilterName(searchCond.getAttributableCond().getSchema());
            wrapper.setType(searchCond.getAttributableCond().getType());
            wrapper.setFilterValue(searchCond.getAttributableCond().getExpression());
        }
        if (searchCond.getAttributeCond() != null) {
            wrapper.setFilterType(SearchCondWrapper.FilterType.ATTRIBUTE);
            wrapper.setFilterName(searchCond.getAttributeCond().getSchema());
            wrapper.setType(searchCond.getAttributeCond().getType());
            wrapper.setFilterValue(searchCond.getAttributeCond().getExpression());
        }
        if (searchCond.getMembershipCond() != null) {
            wrapper.setFilterType(SearchCondWrapper.FilterType.MEMBERSHIP);
            RoleTO role = new RoleTO();
            role.setId(searchCond.getMembershipCond().getRoleId());
            role.setName(searchCond.getMembershipCond().getRoleName());
            wrapper.setFilterName(role.getDisplayName());
        }
        if (searchCond.getResourceCond() != null) {
            wrapper.setFilterType(SearchCondWrapper.FilterType.RESOURCE);
            wrapper.setFilterName(searchCond.getResourceCond().getResourceName());
        }
        if (searchCond.getEntitlementCond() != null) {
            wrapper.setFilterType(SearchCondWrapper.FilterType.ENTITLEMENT);
            wrapper.setFilterName(searchCond.getEntitlementCond().getExpression());
        }

        wrapper.setNotOperator(searchCond.getType() == NodeCond.Type.NOT_LEAF);

        return wrapper;
    }

    public NodeCond buildSearchCond() {
        return buildSearchCond(searchConditionList);
    }

    private NodeCond buildSearchCond(final List<SearchCondWrapper> conditions) {
        SearchCondWrapper searchConditionWrapper = conditions.get(0);
        if (searchConditionWrapper == null || searchConditionWrapper.getFilterType() == null) {
            return null;
        }

        LOG.debug("Search conditions: fname {}; ftype {}; fvalue {}; OP {}; type {}; isnot {}", new Object[]{
                    searchConditionWrapper.getFilterName(), searchConditionWrapper.getFilterType(),
                    searchConditionWrapper.getFilterValue(), searchConditionWrapper.getOperationType(),
                    searchConditionWrapper.getType(), searchConditionWrapper.isNotOperator()});

        NodeCond nodeCond = null;

        switch (searchConditionWrapper.getFilterType()) {
            case ATTRIBUTE:
                // AttributeCond or SyncopeUserCond
                final String schema = searchConditionWrapper.getFilterName();

                final AttributeCond attributeCond;
                if (dnames.getObject().contains(schema)) {
                    attributeCond = new AttributableCond();
                    nodeCond = searchConditionWrapper.isNotOperator()
                            ? NodeCond.getNotLeafCond((AttributableCond) attributeCond)
                            : NodeCond.getLeafCond((AttributableCond) attributeCond);
                } else {
                    attributeCond = new AttributeCond();
                    nodeCond = searchConditionWrapper.isNotOperator()
                            ? NodeCond.getNotLeafCond(attributeCond)
                            : NodeCond.getLeafCond(attributeCond);
                }

                attributeCond.setSchema(schema);
                attributeCond.setType(searchConditionWrapper.getType());
                attributeCond.setExpression(searchConditionWrapper.getFilterValue());

                break;

            case MEMBERSHIP:
                final MembershipCond membershipCond = new MembershipCond();
                membershipCond.setRoleId(RoleTO.fromDisplayName(searchConditionWrapper.getFilterName()));
                membershipCond.setRoleName(searchConditionWrapper.getFilterName().split(" ")[1]);

                if (searchConditionWrapper.isNotOperator()) {
                    nodeCond = NodeCond.getNotLeafCond(membershipCond);
                } else {
                    nodeCond = NodeCond.getLeafCond(membershipCond);
                }

                break;

            case RESOURCE:
                final ResourceCond resourceCond = new ResourceCond();
                resourceCond.setResourceName(searchConditionWrapper.getFilterName());

                if (searchConditionWrapper.isNotOperator()) {
                    nodeCond = NodeCond.getNotLeafCond(resourceCond);
                } else {
                    nodeCond = NodeCond.getLeafCond(resourceCond);
                }

                break;

            case ENTITLEMENT:
                final EntitlementCond entitlementCond = new EntitlementCond();
                entitlementCond.setExpression(searchConditionWrapper.getFilterName());

                if (searchConditionWrapper.isNotOperator()) {
                    nodeCond = NodeCond.getNotLeafCond(entitlementCond);
                } else {
                    nodeCond = NodeCond.getLeafCond(entitlementCond);
                }

                break;

            default:
            // nothing to do
        }

        LOG.debug("Processed condition {}", nodeCond);

        if (conditions.size() > 1) {
            List<SearchCondWrapper> subList = conditions.subList(1, conditions.size());

            if (OperationType.OR.equals(subList.get(0).getOperationType())) {
                nodeCond = NodeCond.getOrCond(nodeCond, buildSearchCond(subList));
            } else {
                nodeCond = NodeCond.getAndCond(nodeCond, buildSearchCond(subList));
            }
        }

        return nodeCond;
    }
}
