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
package org.syncope.console.pages;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import org.apache.wicket.Page;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.list.PageableListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.search.AttributeCond;
import org.syncope.client.search.MembershipCond;
import org.syncope.client.search.NodeCond;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.to.ConfigurationTO;
import org.syncope.client.to.RoleTO;
import org.syncope.client.to.RoleTOs;
import org.syncope.client.to.UserTO;
import org.syncope.console.commons.Constants;
import org.syncope.client.to.UserTOs;
import org.syncope.console.commons.SearchConditionWrapper;
import org.syncope.console.commons.SearchConditionWrapper.OperationType;
import org.syncope.console.rest.SchemaRestClient;
import org.syncope.console.rest.ConfigurationsRestClient;
import org.syncope.console.rest.RolesRestClient;
import org.syncope.console.rest.UsersRestClient;
import org.syncope.console.wicket.markup.html.form.UpdatingCheckBox;
import org.syncope.console.wicket.markup.html.form.UpdatingDropDownChoice;
import org.syncope.console.wicket.markup.html.form.UpdatingTextField;

/**
 * Users WebPage.
 */
public class Users extends BasePage {

    @SpringBean(name = "usersRestClient")
    private UsersRestClient usersRestClient;

    @SpringBean(name = "schemaRestClient")
    private SchemaRestClient schemaRestClient;

    @SpringBean(name = "rolesRestClient")
    private RolesRestClient rolesRestClient;

    @SpringBean(name = "configurationsRestClient")
    private ConfigurationsRestClient configurationsRestClient;

    final ModalWindow createUserWin;
    final ModalWindow editUserWin;
    final ModalWindow changeAttribsViewWin;

    final int WIN_ATTRIBUTES_HEIGHT = 515;
    final int WIN_ATTRIBUTES_WIDTH = 775;

    final int WIN_USER_HEIGHT = 680;
    final int WIN_USER_WIDTH = 1133;

    /** Navigator's rows to display for single view */
    final int ROWS_TO_DISPLAY = 10;

    WebMarkupContainer usersContainer;
    List<String> columnsList;

    /** Response flag set by the Modal Window after the operation is completed*/
    boolean operationResult = false;

    FeedbackPanel feedbackPanel;
    List<SearchConditionWrapper> searchConditionsList;

    UserTOs searchMatchedUsers;

    public Users(PageParameters parameters) {
        super(parameters);

        setupSearchConditionsList();

        searchMatchedUsers = new UserTOs();

        add(createUserWin = new ModalWindow("createUserWin"));
        add(editUserWin = new ModalWindow("editUserWin"));
        add(changeAttribsViewWin = new ModalWindow("changeAttributesViewWin"));

        feedbackPanel = new FeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId(true);

        add(feedbackPanel);

        //table's columnsList = attributes to view
        final IModel columns = new LoadableDetachableModel() {

            @Override
            protected Object load() {
                ConfigurationTO configuration =
                        configurationsRestClient.readConfiguration(
                        Constants.CONF_USERS_ATTRIBUTES_VIEW);

                columnsList = new ArrayList<String>();

                if (configuration != null
                        && configuration.getConfValue() != null) {

                    String conf = configuration.getConfValue();
                    StringTokenizer st = new StringTokenizer(conf, ";");

                    while (st.hasMoreTokens()) {
                        columnsList.add(st.nextToken());
                    }
                }

                Collections.sort(columnsList);
                return columnsList;
            }
        };

        ListView columnsView = new ListView("usersSchema", columns) {

            @Override
            protected void populateItem(final ListItem item) {
                final String name =
                        (String) item.getDefaultModelObject();

                item.add(new Label("attribute", name));
            }
        };

        final IModel users = new LoadableDetachableModel() {

            protected Object load() {
                return usersRestClient.getAllUsers().getUsers();
            }
        };

        PageableListView usersView = new PageableListView("users", users,
                ROWS_TO_DISPLAY) {

            @Override
            protected void populateItem(final ListItem item) {
                final UserTO userTO =
                        (UserTO) item.getDefaultModelObject();

                item.add(new Label("id", userTO.getId() + ""));

                item.add(new Label("status", userTO.getStatus()));

                if (userTO.getToken() != null
                        && !userTO.getToken().equals("")) {
                    item.add(new Label("token", getString("tokenValued")));
                } else {
                    item.add(new Label("token", getString("tokenNotValued")));
                }

                item.add(new ListView("selectedAttributes",
                        attributesToDisplay(userTO)) {

                    @Override
                    protected void populateItem(ListItem item) {
                        AttributeWrapper attribute =
                                (AttributeWrapper) item.getDefaultModelObject();

/*
                        for (String name : columnsList) {

                            if (name.equalsIgnoreCase(attribute.getKey())) {
                                item.add(new Label("name", attribute.getValue()));
                            } else if (!name.equalsIgnoreCase(attribute.getKey())) {
                            }

*/
                        for (String name : columnsList) {
                            if (name.equalsIgnoreCase(attribute.getKey())) {
                                item.add(new Label("name",
                                        attribute.getValue()));
                            }
                        }

                    }
                });

                AjaxLink editLink = new AjaxLink("editLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        final UserTO userTO =
                                (UserTO) item.getDefaultModelObject();

                        editUserWin.setPageCreator(
                                new ModalWindow.PageCreator() {

                            public Page createPage() {
                                UserModalPage window = new UserModalPage(
                                        Users.this, editUserWin, userTO, false);
                                return window;
                            }
                        });

                        editUserWin.show(target);
                    }
                };

                item.add(editLink);

                AjaxLink deleteLink = new AjaxLink("deleteLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        usersRestClient.deleteUser(userTO.getId() + "");

                        info(getString("operation_succeded"));
                        target.addComponent(feedbackPanel);

                        target.addComponent(usersContainer);
                    }
                };

                item.add(deleteLink);
            }
        };

        add(new AjaxPagingNavigator("usersNavigator", usersView));

        usersContainer = new WebMarkupContainer("usersContainer");
        usersContainer.add(usersView);
        usersContainer.add(columnsView);
        usersContainer.setOutputMarkupId(true);

        add(usersContainer);

        createUserWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createUserWin.setInitialHeight(WIN_USER_HEIGHT);
        createUserWin.setInitialWidth(WIN_USER_WIDTH);
        createUserWin.setPageMapName("create-user-modal");
        createUserWin.setCookieName("create-user-modal");

        editUserWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editUserWin.setInitialHeight(WIN_USER_HEIGHT);
        editUserWin.setInitialWidth(WIN_USER_HEIGHT);
        editUserWin.setPageMapName("edit-user-modal");
        editUserWin.setCookieName("edit-user-modal");

        changeAttribsViewWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        changeAttribsViewWin.setInitialHeight(WIN_ATTRIBUTES_HEIGHT);
        changeAttribsViewWin.setInitialWidth(WIN_ATTRIBUTES_WIDTH);
        changeAttribsViewWin.setPageMapName("change-attribs-modal");
        changeAttribsViewWin.setCookieName("change-attribs-modal");

        setWindowClosedCallback(createUserWin, usersContainer);
        setWindowClosedCallback(editUserWin, usersContainer);

        setWindowClosedCallback(createUserWin, usersContainer);
        setWindowClosedCallback(editUserWin, usersContainer);

        setWindowClosedCallback(changeAttribsViewWin, usersContainer);

        add(new AjaxLink("createUserLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {

                createUserWin.setPageCreator(new ModalWindow.PageCreator() {

                    public Page createPage() {
                        UserModalPage window = new UserModalPage(Users.this,
                                createUserWin, new UserTO(), true);
                        return window;
                    }
                });

                createUserWin.show(target);
            }
        });

        add(new AjaxLink("changeAttributesViewLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {

                changeAttribsViewWin.setPageCreator(
                        new ModalWindow.PageCreator() {

                    public Page createPage() {
                        DisplayAttributesModalPage window =
                                new DisplayAttributesModalPage(Users.this,
                                changeAttribsViewWin, true);
                        return window;
                    }
                });

                changeAttribsViewWin.show(target);
            }
        });

        //TAB 2 - Search section start
        
        final IModel userAttributes = new LoadableDetachableModel() {

            protected Object load() {
                return schemaRestClient.getAllUserSchemasNames();
            }
        };

        final IModel roleNames = new LoadableDetachableModel() {

            protected Object load() {
                RoleTOs roleTOs = rolesRestClient.getAllRoles();

                List<String> roleNames = new ArrayList<String>();

                for (RoleTO role : roleTOs)
                    roleNames.add(role.getName());

                return roleNames;
            }
        };

        final IModel attributeTypes = new LoadableDetachableModel() {

            protected Object load() {
                return Arrays.asList(AttributeCond.Type.values());
            }
        };

        final IModel filterTypes = new LoadableDetachableModel() {

            protected Object load() {
                return Arrays.asList(SearchConditionWrapper.FilterType
                        .values());
            }
        };

        Form form = new Form("UserSearchForm");

        form.add(new FeedbackPanel("feedback").setOutputMarkupId(true));

        final WebMarkupContainer container;

        container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);

        ListView searchView = new ListView("searchView", searchConditionsList) {

            @Override
            protected void populateItem(final ListItem item) {
                final SearchConditionWrapper searchCondition =
                        (SearchConditionWrapper) item.getDefaultModelObject();

                if (item.getIndex() == 0) 
                    item.add(new Label("operationType", ""));
                else 
                    item.add(new Label("operationType", searchCondition
                            .getOperationType().toString()));

                item.add(new UpdatingCheckBox("notOperator",
                        new PropertyModel(searchCondition,
                        "notOperator")));

                final UpdatingDropDownChoice filterNameChooser =
                       new UpdatingDropDownChoice("filterName",
                       new PropertyModel(searchCondition, "filterName"),
                       null);
                
                if(searchCondition.getFilterType() == null)
                    filterNameChooser.setChoices(Collections.emptyList());
                else if(searchCondition.getFilterType() ==
                        SearchConditionWrapper.FilterType.ATTRIBUTE)
                            filterNameChooser.setChoices(userAttributes);
                else
                    filterNameChooser.setChoices(roleNames);

                filterNameChooser.setRequired(true);

                item.add(filterNameChooser);

                final UpdatingDropDownChoice type = new UpdatingDropDownChoice(
                        "type", new PropertyModel(searchCondition, "type"),
                        attributeTypes);

                item.add(type);
                
                final UpdatingTextField filterValue = new UpdatingTextField(
                        "filterValue", new PropertyModel(searchCondition,
                        "filterValue"));

                item.add(filterValue);

                if(searchCondition.getFilterType() ==
                        SearchConditionWrapper.FilterType.MEMBERSHIP) {

                    type.setEnabled(false);
                    type.setRequired(false);
                    type.setModelObject(null);

                    filterValue.setEnabled(false);
                    //filterValue.setRequired(false);
                    filterValue.setModelObject("");

                } else {

                    if (!type.isEnabled()) {
                        type.setEnabled(true);
                        type.setRequired(true);
                    }

                    if (!filterValue.isEnabled()) {
                        filterValue.setEnabled(true);
                    }

                }

                UpdatingDropDownChoice filterTypeChooser =
                        new UpdatingDropDownChoice("filterType",
                        new PropertyModel(searchCondition, "filterType"),
                        filterTypes);

                filterTypeChooser.add(new AjaxFormComponentUpdatingBehavior(
                        "onchange") {

                protected void onUpdate(AjaxRequestTarget target) {
                    filterNameChooser.setChoices(new LoadableDetachableModel() {

                        @Override
                        protected Object load() {
                            SearchConditionWrapper.FilterType schemaType =
                                    searchCondition.getFilterType();

                            if (schemaType ==  SearchConditionWrapper.
                                    FilterType.ATTRIBUTE) {

                                return userAttributes;
                            } else {

                                return roleNames;
                            }

                        }
                    });
                    target.addComponent(filterNameChooser);
                    target.addComponent(container);
                }});

                filterTypeChooser.setRequired(true);

                item.add(filterTypeChooser);

                AjaxButton dropButton = new AjaxButton("dropButton",
                        new Model(getString("dropButton"))) {

                    @Override
                    protected void onSubmit(AjaxRequestTarget target,
                            Form form) {
                        final int parentId = new Integer(getParent().getId());
                        searchConditionsList.remove(parentId);
                        target.addComponent(container);
                    }
                };

                dropButton.setDefaultFormProcessing(false);

                if (item.getIndex() == 0) {
                    dropButton.setVisible(false);
                }

                item.add(dropButton);
            }
        };

        container.add(searchView);

        AjaxButton addAndButton = new AjaxButton("addAndButton", new Model(
                getString("addAndButton"))) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                SearchConditionWrapper conditionWrapper =
                        new SearchConditionWrapper();
                conditionWrapper.setOperationType(OperationType.AND);
                searchConditionsList.add(conditionWrapper);
                target.addComponent(container);
            }
        };

        addAndButton.setDefaultFormProcessing(false);
        container.add(addAndButton);

        AjaxButton addOrButton = new AjaxButton("addOrButton", new Model(
                getString("addOrButton"))) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                SearchConditionWrapper conditionWrapper =
                        new SearchConditionWrapper();
                conditionWrapper.setOperationType(OperationType.OR);
                searchConditionsList.add(conditionWrapper);
                target.addComponent(container);
            }
        };

        addOrButton.setDefaultFormProcessing(false);
        container.add(addOrButton);

        form.add(container);

        IModel resultsModel = new LoadableDetachableModel() {

            @Override
            protected Object load() {
                return searchMatchedUsers.getUsers();
            }
        };

        final ListView resultsView = new ListView("results", resultsModel) {

            @Override
            protected void populateItem(final ListItem item) {

                UserTO userTO = (UserTO) item.getModelObject();

                item.add(new Label("id", String.valueOf(userTO.getId())));

                item.add(new Label("status", String.valueOf(
                        userTO.getStatus())));

                if (userTO.getToken() != null && !userTO.getToken().equals("")) {
                    item.add(new Label("token", getString("tokenValued")));
                } else {
                    item.add(new Label("token", getString("tokenNotValued")));
                }

                AjaxButton editButton = new AjaxButton("editLink",
                        new Model(getString("edit"))) {

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form form) {
                        final UserTO userTO =
                                (UserTO) item.getDefaultModelObject();

                        editUserWin.setPageCreator(new ModalWindow.PageCreator() {

                            public Page createPage() {
                                UserModalPage window = new UserModalPage(
                                        Users.this, editUserWin, userTO, false);
                                return window;
                            }
                        });

                        editUserWin.show(target);
                    }
                };

                item.add(editButton);

                item.add(new AjaxButton("deleteLink", new Model(
                        getString("delete"))) {

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form form) {
                    }
                });
            }
        };

        final WebMarkupContainer searchResultsContainer =
                new WebMarkupContainer("searchResultsContainer");
        searchResultsContainer.setOutputMarkupId(true);
        searchResultsContainer.add(resultsView);

        setWindowClosedCallback(editUserWin, searchResultsContainer);

        form.add(new AjaxButton("search", new Model(getString("search"))) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {

                NodeCond nodeCond =
                        buildSearchExpression(searchConditionsList);

                if (nodeCond != null) {

                    try {
                        searchMatchedUsers =
                                usersRestClient.searchUsers(nodeCond);

                        //Clean the feedback panel if the operation succedes
                        target.addComponent(form.get("feedback"));
                    } catch (Exception e) {
                        error(e.getMessage());
                        return;
                    }
                } else {
                    error(getString("search_error"));
                }

                target.addComponent(searchResultsContainer);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                target.addComponent(form.get("feedback"));
            }
        });
        
        form.add(searchResultsContainer);

        add(form);
    }

    /**
     * Return the user's attributes columnsList to display, ordered
     * @param userTO instance
     * @return attributes columnsList to view depending the selection
     */
    public List<AttributeWrapper> attributesToDisplay(UserTO user) {
        Set<AttributeTO> attributes = user.getAttributes();
        List<AttributeWrapper> attributesList =
                new ArrayList<AttributeWrapper>();

        ConfigurationTO configuration =
                configurationsRestClient.readConfiguration(
                Constants.CONF_USERS_ATTRIBUTES_VIEW);

        columnsList = new ArrayList<String>();

        if (configuration != null && configuration.getConfValue() != null &&
                !configuration.getConfValue().equals("")) {
            String conf = configuration.getConfValue();
            StringTokenizer st = new StringTokenizer(conf, ";");

            while (st.hasMoreTokens()) {
                columnsList.add(st.nextToken());
            }
        }

        Collections.sort(columnsList);

        AttributeWrapper attributeWrapper = null;

        boolean found = false;
        for (String name : columnsList) {
            for (AttributeTO attribute : attributes) {
                if (name.equals(attribute.getSchema()) && !found) {
                    attributeWrapper = new AttributeWrapper();
                    attributeWrapper.setKey(attribute.getSchema());
                    for (String value : attribute.getValues()) {
                        attributeWrapper.setValue(value);
                        found = true;
                    }
                    attributesList.add(attributeWrapper);
                }
            }
            //case the attribute's value is blank
            if (!found) {
                attributeWrapper = new AttributeWrapper();
                attributeWrapper.setKey(name);
                attributeWrapper.setValue("");

                attributesList.add(attributeWrapper);
            } else {
                found = false;
            }
        }

        return attributesList;
    }

    /**
     * Set a WindowClosedCallback for a ModalWindow instance.
     * @param window
     * @param container
     */
    public void setWindowClosedCallback(ModalWindow window,
            final WebMarkupContainer container) {

        window.setWindowClosedCallback(
                new ModalWindow.WindowClosedCallback() {

                    public void onClose(AjaxRequestTarget target) {
                        target.addComponent(container);
                        if (operationResult) {
                            info(getString("operation_succeded"));
                            target.addComponent(feedbackPanel);
                            operationResult = false;
                        } //When the window is closed without calling backend
                        else {
                            target.addComponent(feedbackPanel);
                        }
                    }
                });
    }

    public boolean isOperationResult() {
        return operationResult;
    }

    public void setOperationResult(boolean operationResult) {
        this.operationResult = operationResult;
    }

    /**
     * Init search conditions list.
     */
    private void setupSearchConditionsList() {
        searchConditionsList = new ArrayList<SearchConditionWrapper>();

        searchConditionsList.add(new SearchConditionWrapper());
    }

    /**
     * Build recursively search users expression from searchConditionsList.
     * @return NodeCond
     */
    public NodeCond buildSearchExpression(
            List<SearchConditionWrapper> conditions) {

        AttributeCond attributeCond = null;
        MembershipCond membershipCond = null;
        
        List<SearchConditionWrapper> subList = null;

        SearchConditionWrapper searchConditionWrapper = conditions.iterator()
                .next();

        if(searchConditionWrapper.getFilterType() ==
                SearchConditionWrapper.FilterType.ATTRIBUTE) {

            attributeCond = new AttributeCond();
            attributeCond.setSchema(searchConditionWrapper.getFilterName());
            attributeCond.setType(searchConditionWrapper.getType());
            attributeCond.setExpression(searchConditionWrapper.getFilterValue());

        }

        else {

        membershipCond = new MembershipCond();
        membershipCond.setRoleName(searchConditionWrapper.getFilterName());

        }

        if (conditions.size() == 1) {

             if(searchConditionWrapper.getFilterType() ==
                SearchConditionWrapper.FilterType.ATTRIBUTE) {
                      if (searchConditionWrapper.isNotOperator()) {
                    return NodeCond.getNotLeafCond(attributeCond);
                } else {
                    return NodeCond.getLeafCond(attributeCond);
                }
             }
             else {
                   if (searchConditionWrapper.isNotOperator()) {
                    return NodeCond.getNotLeafCond(membershipCond);
                } else {
                    return NodeCond.getLeafCond(membershipCond);
                }
             }

        } else {

            subList = conditions.subList(1, conditions.size());

            searchConditionWrapper = subList.iterator().next();

            if (searchConditionWrapper.getOperationType() ==
                    SearchConditionWrapper.OperationType.AND) {

                if(searchConditionWrapper.getFilterType() ==
                SearchConditionWrapper.FilterType.ATTRIBUTE) {

                    if(attributeCond != null)
                    return NodeCond.getAndCond(
                            NodeCond.getLeafCond(attributeCond),
                            buildSearchExpression(
                            new ArrayList<SearchConditionWrapper>(subList)));
                    else
                        return NodeCond.getAndCond(
                            NodeCond.getLeafCond(membershipCond),
                            buildSearchExpression(
                            new ArrayList<SearchConditionWrapper>(subList)));
                } else {
                   if(attributeCond != null)
                    return NodeCond.getAndCond(
                        NodeCond.getLeafCond(attributeCond),
                        buildSearchExpression(
                        new ArrayList<SearchConditionWrapper>(subList)));
                   else
                       return NodeCond.getAndCond(
                        NodeCond.getLeafCond(membershipCond),
                        buildSearchExpression(
                        new ArrayList<SearchConditionWrapper>(subList)));
                }

            } else {
                if(searchConditionWrapper.getFilterType() ==
                SearchConditionWrapper.FilterType.ATTRIBUTE) {
                if(attributeCond != null)
                return NodeCond.getOrCond(
                        NodeCond.getLeafCond(attributeCond),
                        buildSearchExpression(
                        new ArrayList<SearchConditionWrapper>(subList)));
                else
                return NodeCond.getOrCond(
                        NodeCond.getLeafCond(membershipCond),
                        buildSearchExpression(
                        new ArrayList<SearchConditionWrapper>(subList)));
                }
                else {
                   if(attributeCond != null)
                   return NodeCond.getOrCond(
                        NodeCond.getLeafCond(attributeCond),
                        buildSearchExpression(
                        new ArrayList<SearchConditionWrapper>(subList)));
                   else
                   return NodeCond.getOrCond(
                        NodeCond.getLeafCond(membershipCond),
                        buildSearchExpression(
                        new ArrayList<SearchConditionWrapper>(subList)));
                }
            }
        }
    }

    /**
     * Wrapper class for displaying attribute
     */
    public class AttributeWrapper {

        private String key;
        private String value;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
