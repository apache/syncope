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

import java.io.Serializable;
import java.util.ArrayList;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.wicket.Page;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxPreprocessingCallDecorator;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table
        .AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.web.client.HttpServerErrorException;
import org.syncope.client.search.AttributeCond;
import org.syncope.client.search.MembershipCond;
import org.syncope.client.search.NodeCond;
import org.syncope.client.to.ConfigurationTO;
import org.syncope.client.to.RoleTO;
import org.syncope.client.to.UserTO;
import org.syncope.console.commons.Constants;
import org.syncope.console.commons.SearchConditionWrapper;
import org.syncope.console.commons.SearchConditionWrapper.OperationType;
import org.syncope.console.commons.Utility;
import org.syncope.console.rest.SchemaRestClient;
import org.syncope.console.rest.ConfigurationsRestClient;
import org.syncope.console.rest.RolesRestClient;
import org.syncope.console.rest.UsersRestClient;
import org.syncope.console.wicket.markup.html.form.DeleteLinkPanel;
import org.syncope.console.wicket.markup.html.form.EditLinkPanel;
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

    @SpringBean(name = "utility")
    Utility utility;

    final ModalWindow createUserWin;
    final ModalWindow editUserWin;
    final ModalWindow changeAttribsViewWin;

    final int WIN_ATTRIBUTES_HEIGHT = 515;
    final int WIN_ATTRIBUTES_WIDTH = 775;

    final int WIN_USER_HEIGHT = 680;
    final int WIN_USER_WIDTH = 1133;

    WebMarkupContainer container;

    /*
     Response flag set by the Modal Window after the operation is completed
     */
    boolean operationResult = false;

    FeedbackPanel feedbackPanel;
    List<SearchConditionWrapper> searchConditionsList;

    List<UserTO> searchMatchedUsers;

    private int paginatorRows;

    public Users(PageParameters parameters) {
        super(parameters);

        setupSearchConditionsList();

        searchMatchedUsers = new ArrayList<UserTO>();

        add(createUserWin = new ModalWindow("createUserWin"));
        add(editUserWin = new ModalWindow("editUserWin"));
        add(changeAttribsViewWin = new ModalWindow("changeAttributesViewWin"));

        feedbackPanel = new FeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId(true);

        add(feedbackPanel);

        /*try {
            paginatorRows = utility.getPaginatorRowsToDisplay(Constants
                    .CONF_USERS_PAGINATOR_ROWS);
        }
        catch(RestClientException e){
           PageParameters errorParameters = new PageParameters();

           errorParameters.add("errorTitle", getString("alert"));
           errorParameters.add("errorMessage", getString("connectionError"));

           setResponsePage(new ErrorPage(errorParameters));

           return;
        }*/
        
        paginatorRows = utility.getPaginatorRowsToDisplay(Constants
                    .CONF_USERS_PAGINATOR_ROWS);

        List<IColumn> columns = new ArrayList<IColumn>();

        columns.add(new PropertyColumn(new Model(getString("id")),
                "id", "id"));

        columns.add(new PropertyColumn(new Model(getString("status")),
                "status", "status"));

        columns.add(new PropertyColumn(new Model(getString("token")),
                "token", "token"));

        columns = addCustomizedUserProperties(columns);

        columns.add(new AbstractColumn<UserTO>(new Model<String>(
                getString("edit")))
        {
            public void populateItem(Item<ICellPopulator<UserTO>>
                    cellItem, String componentId, IModel<UserTO> model)
            {
                    final UserTO userTO = model.getObject();
                    AjaxLink editLink = new AjaxLink("editLink") {

                        @Override
                    public void onClick(AjaxRequestTarget target) {

                        editUserWin.setPageCreator(
                                new ModalWindow.PageCreator() {

                            public Page createPage() {
                                UserModalPage window = new UserModalPage(
                                        Users.this, editUserWin, userTO, false);
                                return window;
                            }
                        });

                        editUserWin.show(target);
                        }};

                EditLinkPanel panel = new EditLinkPanel(componentId, model);
                panel.add(editLink);

                cellItem.add(panel);
            }
        });

        columns.add(new AbstractColumn<UserTO>(new Model<String>
                (getString("delete")))
        {
            public void populateItem(Item<ICellPopulator<UserTO>>
                    cellItem, String componentId, IModel<UserTO> model)
            {
                    final UserTO userTO = model.getObject();

                    AjaxLink deleteLink = new AjaxLink("deleteLink") {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        usersRestClient.deleteUser(String.valueOf(userTO
                                .getId()));

                        info(getString("operation_succeded"));
                        target.addComponent(feedbackPanel);

                        target.addComponent(container);
                    }

                    @Override
                    protected IAjaxCallDecorator getAjaxCallDecorator() {
                        return new AjaxPreprocessingCallDecorator(super.getAjaxCallDecorator()) {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public CharSequence preDecorateScript(CharSequence script) {
                                return "if (confirm('"+getString("confirmDelete")+"'))"
                                        +"{"+script+"}";
                            }
                        };
                    }
                };

                DeleteLinkPanel panel = new DeleteLinkPanel(componentId, model);
                panel.add(deleteLink);

                cellItem.add(panel);
            }
        });


        final AjaxFallbackDefaultDataTable table =
                new AjaxFallbackDefaultDataTable("datatable", columns,
                new UsersProvider(), paginatorRows);

        container = new WebMarkupContainer("container");
        container.add(table);
        container.setOutputMarkupId(true);

        add(container);

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

        setWindowClosedCallback(createUserWin, container);
        setWindowClosedCallback(editUserWin, container);

        changeAttribsViewWin.setWindowClosedCallback(
                new ModalWindow.WindowClosedCallback() {

                    public void onClose(AjaxRequestTarget target) {

                        if (operationResult) {
                            getSession().info(getString("operation_succeded"));
                            setResponsePage(Users.class);
                        } //When the window is closed without calling backend
                        else
                            target.addComponent(feedbackPanel);
                    }
                });

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

        Form paginatorForm = new Form("PaginatorForm");

        final DropDownChoice rowsChooser = new DropDownChoice("rowsChooser",
        new PropertyModel(this,"paginatorRows"),utility.paginatorRowsChooser());

        rowsChooser.add(new AjaxFormComponentUpdatingBehavior( "onchange" ){
          protected void onUpdate( AjaxRequestTarget target )
            {
              utility.updatePaginatorRows(Constants.CONF_USERS_PAGINATOR_ROWS,
                      paginatorRows);

              table.setRowsPerPage(paginatorRows);

              target.addComponent(container);
            }

          });

        paginatorForm.add(rowsChooser);
        add(paginatorForm);

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
                List<RoleTO> roleTOs = rolesRestClient.getAllRoles();

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
                return searchMatchedUsers;
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

        //Display warning message if no serach matches have been found
        final Label noResults = new Label("noResults",new Model<String>(""));
        noResults.setOutputMarkupId(true);
        searchResultsContainer.add(noResults);

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
                    } catch (HttpServerErrorException e) {
                        e.printStackTrace();
                        error(e.getMessage());
                        return;
                    }
                } else {
                    error(getString("search_error"));
                }

                if(searchMatchedUsers.isEmpty())
                    noResults.setDefaultModel(new Model<String>(
                            getString("search_noResults")));
                else
                     noResults.setDefaultModel(new Model<String>(""));

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
     * Set a WindowClosedCallback for a ModalWindow instance.
     * @param window
     * @param container
     */
    public void setWindowClosedCallback(final ModalWindow window,
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

    private List<IColumn> addCustomizedUserProperties(List<IColumn> columns) {
        ConfigurationTO configuration =
                        configurationsRestClient.readConfiguration(
                        Constants.CONF_USERS_ATTRIBUTES_VIEW);

                if (configuration != null
                        && configuration.getConfValue() != null) {

                    String conf = configuration.getConfValue();
                    StringTokenizer st = new StringTokenizer(conf, ";");

                    String columnName = null;
                    
                    while (st.hasMoreTokens()) {
                        columnName = st.nextToken();

                        columns.add(new PropertyColumn(new Model(columnName),
                        "attributeMap["+columnName+"]"));
                    }
                }

                return columns;
    }

    /**
     * Wrapper class for displaying attribute
     */
    public class AttributeWrapper implements Serializable {

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

    class UsersProvider extends SortableDataProvider<UserTO> {

        private SortableUsersProviderComparator comparator =
                new SortableUsersProviderComparator();

        public UsersProvider() {
            //Default sorting
            setSort("id",true);
        }

        @Override
        public Iterator<UserTO> iterator(int first, int count) {
            List<UserTO> list = getUsersListDB();

            Collections.sort(list, comparator);

            return list.subList(first, first+count).iterator();
        }

        @Override
        public int size() {
            return getUsersListDB().size();
        }

        @Override
        public IModel<UserTO> model(final UserTO
                user) {
            return new AbstractReadOnlyModel<UserTO>() {

                @Override
                public UserTO getObject() {
                    return user;
                }
            };
        }

        public List<UserTO> getUsersListDB(){
        List<UserTO> list = usersRestClient.getAllUsers();

        for(UserTO user : list) {

            if (user.getToken() != null
                        && !user.getToken().equals("")) {
                    user.setToken(getString("tokenValued"));
                } else {
                    user.setToken(getString("tokenNotValued"));
                }
        }

        return list;
        }

        class SortableUsersProviderComparator implements
                Comparator<UserTO>, Serializable {
            public int compare(final UserTO o1,
                    final UserTO o2) {

                String expression = null;

//                if(getSort().getProperty().contains("attributeMap["))
//                    expression = getSort().getProperty().substring(13,
//                            getSort().getProperty().length()-1);
//
//                else
                    expression = getSort().getProperty();

                    PropertyModel<Comparable> model1 =
                            new PropertyModel<Comparable>(o1, expression);
                    PropertyModel<Comparable> model2 =
                            new PropertyModel<Comparable>(o2, expression);

                    int result = 1;

                    if(model1.getObject() == null && model2.getObject() == null)
                        result = 0;
                    else if(model1.getObject() == null)
                        result = 1;
                    else if(model2.getObject() == null)
                        result = -1;
                    else
                        result = ((Comparable)model1.getObject()).compareTo(
                                model2.getObject());

                    result = getSort().isAscending() ? result : -result;

                    return result;
            }
	}
    }
}
