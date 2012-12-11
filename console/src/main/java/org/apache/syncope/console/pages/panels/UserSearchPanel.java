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
import org.apache.syncope.client.search.AttributableCond;
import org.apache.syncope.client.search.AttributeCond;
import org.apache.syncope.client.search.MembershipCond;
import org.apache.syncope.client.search.NodeCond;
import org.apache.syncope.client.search.ResourceCond;
import org.apache.syncope.client.to.ResourceTO;
import org.apache.syncope.client.to.RoleTO;
import org.apache.syncope.client.to.UserTO;
import org.apache.syncope.console.commons.SearchCondWrapper;
import org.apache.syncope.console.commons.SearchCondWrapper.FilterType;
import org.apache.syncope.console.commons.SearchCondWrapper.OperationType;
import org.apache.syncope.console.rest.ResourceRestClient;
import org.apache.syncope.console.rest.RoleRestClient;
import org.apache.syncope.console.rest.SchemaRestClient;
import org.apache.syncope.types.AttributableType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserSearchPanel extends Panel {

    private static final long serialVersionUID = -1769527800450203738L;

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(UserSearchPanel.class);

    private List<String> ATTRIBUTES_NOTINCLUDED = Arrays.asList(new String[]{
                "attributes", "derivedAttributes", "virtualAttributes", "serialVersionUID", "memberships", "resources",
                "password", "propagationStatusMap"});

    @SpringBean
    private SchemaRestClient schemaRestClient;

    @SpringBean
    private RoleRestClient roleRestClient;

    @SpringBean
    private ResourceRestClient resourceRestClient;

    private final boolean required;

    final private IModel<List<String>> dnames = new LoadableDetachableModel<List<String>>() {

        private static final long serialVersionUID = 5275935387613157437L;

        @Override
        protected List<String> load() {
            final List<String> details = new ArrayList<String>();

            Class<?> clazz = UserTO.class;

            // loop on class and all superclasses searching for field
            while (clazz != null && clazz != Object.class) {
                for (Field field : clazz.getDeclaredFields()) {
                    if (!ATTRIBUTES_NOTINCLUDED.contains(field.getName())) {
                        details.add(field.getName());
                    }
                }
                clazz = clazz.getSuperclass();
            }

            Collections.reverse(details);
            return details;
        }
    };

    final private IModel<List<String>> unames = new LoadableDetachableModel<List<String>>() {

        private static final long serialVersionUID = 5275935387613157437L;

        @Override
        protected List<String> load() {
            return schemaRestClient.getSchemaNames(AttributableType.USER);
        }
    };

    final private IModel<List<String>> roleNames = new LoadableDetachableModel<List<String>>() {

        private static final long serialVersionUID = 5275935387613157437L;

        @Override
        protected List<String> load() {
            List<RoleTO> roleTOs = roleRestClient.getAllRoles();

            List<String> result = new ArrayList<String>(roleTOs.size());
            for (RoleTO role : roleTOs) {
                result.add(role.getDisplayName());
            }

            return result;
        }
    };

    final private IModel<List<String>> resourceNames = new LoadableDetachableModel<List<String>>() {

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

    final private IModel<List<AttributeCond.Type>> attributeTypes =
            new LoadableDetachableModel<List<AttributeCond.Type>>() {

                private static final long serialVersionUID = 5275935387613157437L;

                @Override
                protected List<AttributeCond.Type> load() {
                    return Arrays.asList(AttributeCond.Type.values());
                }
            };

    final private IModel<List<FilterType>> filterTypes = new LoadableDetachableModel<List<FilterType>>() {

        private static final long serialVersionUID = 5275935387613157437L;

        @Override
        protected List<FilterType> load() {
            return Arrays.asList(FilterType.values());
        }
    };

    final FeedbackPanel searchFeedback;

    final List<SearchCondWrapper> searchConditionList;

    public UserSearchPanel(final String id) {
        this(id, null, true);
    }

    public UserSearchPanel(final String id, final NodeCond initCond) {
        this(id, initCond, true);
    }

    public UserSearchPanel(final String id, final NodeCond initNodeCond, final boolean required) {
        super(id);
        this.required = required;

        setOutputMarkupId(true);

        final WebMarkupContainer searchFormContainer = new WebMarkupContainer("searchFormContainer");
        searchFormContainer.setOutputMarkupId(true);

        searchFeedback = new FeedbackPanel("searchFeedback", new IFeedbackMessageFilter() {

            private static final long serialVersionUID = 6895024863321391672L;

            @Override
            public boolean accept(final FeedbackMessage message) {
                boolean result;

                // messages reported on the session have a null reporter
                if (message.getReporter() != null) {
                    // only accept messages coming from the children
                    // of the search form container
                    result = searchFormContainer.contains(message.getReporter(), true);
                } else {
                    result = false;
                }

                return result;
            }
        });
        searchFeedback.setOutputMarkupId(true);
        add(searchFeedback);

        if (initNodeCond == null) {
            searchConditionList = new ArrayList<SearchCondWrapper>();
            searchConditionList.add(new SearchCondWrapper());
        } else {
            searchConditionList = getSearchCondWrappers(initNodeCond, null);
        }
        searchFormContainer.add(new SearchView("searchView", searchConditionList, searchFormContainer));

        AjaxButton addAndButton = new IndicatingAjaxButton("addAndButton", new ResourceModel("addAndButton")) {

            private static final long serialVersionUID = -4804368561204623354L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {

                SearchCondWrapper conditionWrapper = new SearchCondWrapper();
                conditionWrapper.setOperationType(OperationType.AND);
                searchConditionList.add(conditionWrapper);
                target.add(searchFormContainer);
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                target.add(searchFormContainer);
            }
        };
        addAndButton.setDefaultFormProcessing(false);
        searchFormContainer.add(addAndButton);

        AjaxButton addOrButton = new IndicatingAjaxButton("addOrButton", new ResourceModel("addOrButton")) {

            private static final long serialVersionUID = -4804368561204623354L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                SearchCondWrapper conditionWrapper = new SearchCondWrapper();
                conditionWrapper.setOperationType(OperationType.OR);
                searchConditionList.add(conditionWrapper);
                target.add(searchFormContainer);
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                target.add(searchFormContainer);
            }
        };
        addOrButton.setDefaultFormProcessing(false);
        searchFormContainer.add(addOrButton);

        add(searchFormContainer);
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
            wrapper.setFilterType(FilterType.ATTRIBUTE);
            wrapper.setFilterName(searchCond.getAttributableCond().getSchema());
            wrapper.setType(searchCond.getAttributableCond().getType());
            wrapper.setFilterValue(searchCond.getAttributableCond().getExpression());
        }
        if (searchCond.getAttributeCond() != null) {
            wrapper.setFilterType(FilterType.ATTRIBUTE);
            wrapper.setFilterName(searchCond.getAttributeCond().getSchema());
            wrapper.setType(searchCond.getAttributeCond().getType());
            wrapper.setFilterValue(searchCond.getAttributeCond().getExpression());
        }
        if (searchCond.getMembershipCond() != null) {
            wrapper.setFilterType(FilterType.MEMBERSHIP);
            RoleTO role = new RoleTO();
            role.setId(searchCond.getMembershipCond().getRoleId());
            role.setName(searchCond.getMembershipCond().getRoleName());
            wrapper.setFilterName(role.getDisplayName());
        }
        if (searchCond.getResourceCond() != null) {
            wrapper.setFilterType(FilterType.RESOURCE);
            wrapper.setFilterName(searchCond.getResourceCond().getResourceName());
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

    private class SearchView extends ListView<SearchCondWrapper> {

        private static final long serialVersionUID = -527351923968737757L;

        final private WebMarkupContainer searchFormContainer;

        public SearchView(final String id, final List<? extends SearchCondWrapper> list,
                final WebMarkupContainer searchFormContainer) {

            super(id, list);
            this.searchFormContainer = searchFormContainer;
        }

        @Override
        protected void populateItem(final ListItem<SearchCondWrapper> item) {
            final SearchCondWrapper searchCondition = item.getModelObject();

            if (item.getIndex() == 0) {
                item.add(new Label("operationType", ""));
            } else {
                item.add(new Label("operationType", searchCondition.getOperationType().toString()));
            }

            final CheckBox notOperator = new CheckBox("notOperator", new PropertyModel(searchCondition, "notOperator"));
            notOperator.add(new AjaxFormComponentUpdatingBehavior("onchange") {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                }
            });
            item.add(notOperator);

            final DropDownChoice<AttributeCond.Type> type = new DropDownChoice<AttributeCond.Type>("type",
                    new PropertyModel<AttributeCond.Type>(searchCondition, "type"), attributeTypes);
            type.add(new AjaxFormComponentUpdatingBehavior("onchange") {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                }
            });
            item.add(type);

            final DropDownChoice<String> filterNameChooser = new DropDownChoice<String>("filterName",
                    new PropertyModel<String>(searchCondition, "filterName"), (IModel) null);
            filterNameChooser.setOutputMarkupId(true);
            filterNameChooser.setRequired(required);
            filterNameChooser.add(new AjaxFormComponentUpdatingBehavior("onchange") {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                }
            });
            item.add(filterNameChooser);

            final TextField<String> filterValue = new TextField<String>("filterValue", new PropertyModel<String>(
                    searchCondition, "filterValue"));
            filterValue.add(new AjaxFormComponentUpdatingBehavior("onchange") {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                }
            });
            item.add(filterValue);

            final DropDownChoice<FilterType> filterTypeChooser = new DropDownChoice<FilterType>("filterType",
                    new PropertyModel<FilterType>(searchCondition, "filterType"), filterTypes);
            filterTypeChooser.setOutputMarkupId(true);
            filterTypeChooser.add(new AjaxFormComponentUpdatingBehavior("onchange") {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    target.add(searchFormContainer);
                }
            });
            filterTypeChooser.setRequired(required);
            item.add(filterTypeChooser);

            AjaxButton dropButton = new IndicatingAjaxButton("dropButton", new ResourceModel("dropButton")) {

                private static final long serialVersionUID = -4804368561204623354L;

                @Override
                protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                    getList().remove(Integer.valueOf(getParent().getId()).intValue());
                    target.add(searchFormContainer);
                }

                @Override
                protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                    target.add(searchFormContainer);
                }
            };
            dropButton.setDefaultFormProcessing(false);
            if (item.getIndex() == 0) {
                dropButton.setVisible(false);
            }
            item.add(dropButton);

            if (searchCondition == null || searchCondition.getFilterType() == null) {
                filterNameChooser.setChoices(Collections.EMPTY_LIST);
            } else {
                switch (searchCondition.getFilterType()) {
                    case ATTRIBUTE:
                        final List<String> names = new ArrayList<String>(dnames.getObject());

                        if (unames.getObject() != null && !unames.getObject().isEmpty()) {
                            names.addAll(unames.getObject());
                        }
                        Collections.sort(names);

                        filterNameChooser.setChoices(names);
                        if (!type.isEnabled()) {
                            type.setEnabled(true);
                            type.setRequired(true);
                        }
                        if (!filterValue.isEnabled()) {
                            filterValue.setEnabled(true);
                        }

                        break;

                    case MEMBERSHIP:
                        filterNameChooser.setChoices(roleNames);
                        filterNameChooser.setChoiceRenderer(new IChoiceRenderer<String>() {

                            private static final long serialVersionUID = -4288397951948436434L;

                            @Override
                            public Object getDisplayValue(String object) {
                                return object;
                            }

                            @Override
                            public String getIdValue(String object, int index) {
                                return object;
                            }
                        });
                        type.setEnabled(false);
                        type.setRequired(false);
                        type.setModelObject(null);

                        filterValue.setEnabled(false);
                        filterValue.setModelObject("");

                        break;

                    case RESOURCE:
                        filterNameChooser.setChoices(resourceNames);
                        type.setEnabled(false);
                        type.setRequired(false);
                        type.setModelObject(null);

                        filterValue.setEnabled(false);
                        filterValue.setModelObject("");

                        break;

                    default:
                        filterNameChooser.setChoices(Collections.EMPTY_LIST);
                }
            }
        }
    }
}
