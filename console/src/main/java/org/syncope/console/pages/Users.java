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
import org.apache.wicket.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
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
import org.springframework.web.client.HttpServerErrorException;
import org.syncope.client.search.AttributeCond;
import org.syncope.client.search.MembershipCond;
import org.syncope.client.search.NodeCond;
import org.syncope.client.search.PaginatedResult;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.to.ConfigurationTO;
import org.syncope.client.to.RoleTO;
import org.syncope.client.to.UserTO;
import org.syncope.console.commons.Constants;
import org.syncope.console.commons.SearchConditionWrapper;
import org.syncope.console.commons.SearchConditionWrapper.OperationType;
import org.syncope.console.commons.Utility;
import org.syncope.console.rest.SchemaRestClient;
import org.syncope.console.rest.ConfigurationRestClient;
import org.syncope.console.rest.RoleRestClient;
import org.syncope.console.rest.UserRestClient;
import org.syncope.console.wicket.markup.html.form.UpdatingCheckBox;
import org.syncope.console.wicket.markup.html.form.UpdatingDropDownChoice;
import org.syncope.console.wicket.markup.html.form.UpdatingTextField;

/**
 * Users WebPage.
 */
public class Users extends BasePage {

    @SpringBean
    private UserRestClient userRestClient;

    @SpringBean
    private SchemaRestClient schemaRestClient;

    @SpringBean
    private RoleRestClient roleRestClient;

    @SpringBean
    private ConfigurationRestClient confRestClient;

    @SpringBean
    private Utility utility;

    private final ModalWindow createUserWin;

    private final ModalWindow editUserWin;

    private final ModalWindow changeAttribsViewWin;

    private final static int WIN_ATTRIBUTES_HEIGHT = 515;

    private final static int WIN_ATTRIBUTES_WIDTH = 775;

    private final static int WIN_USER_HEIGHT = 680;

    private final static int WIN_USER_WIDTH = 1133;

    private WebMarkupContainer usersTableSearchContainer;

    private WebMarkupContainer searchResultsContainer;

    private WebMarkupContainer usersTableContainer;

    /*
    Response flag set by the Modal Window after the operation is completed
     */
    private boolean operationResult = false;

    private List<SearchConditionWrapper> searchConditionsList;

    private List<UserTO> searchMatchedUsers;

    private int paginatorRows;

    private int paginatorSearchRows;

    private int currentViewPage = 1;

    private int currentSearchPage = 1;

    /** Ajax Links for User's view paginator**/
    private AjaxLink incrementUserViewLink;

    private AjaxLink decrementUserViewLink;

    private AjaxLink firstPageLink;

    private AjaxLink lastPageLink;

    /** Labels for Users' view paginator*/
    private Label pageRecordFrom;

    private Label pageRecordTo;

    private Label totalRecords;

    /** Ajax Links for Users' search paginator */
    private AjaxLink incrementUserLinkSearch;

    private AjaxLink decrementUserLinkSearch;

    private AjaxLink firstPageLinkSearch;

    private AjaxLink lastPageLinkSearch;

    /** 
     * Labels for Users' search paginator
     */
    private Label pageRecordFromSearch;

    private Label pageRecordToSearch;

    private Label totalRecordsSearch;

    private List<String> columnsList;

    private NodeCond nodeCond;

    private PaginatedResult paginatedUsers;

    private PaginatedResult paginatedSearchUsers;

    private ListView pageLinksView;

    private ListView pageLinksSearchView;

    public Users(PageParameters parameters) {
        super(parameters);

        setupSearchConditionsList();

        searchMatchedUsers = new ArrayList<UserTO>();

        add(createUserWin = new ModalWindow("createUserWin"));
        add(editUserWin = new ModalWindow("editUserWin"));
        add(changeAttribsViewWin = new ModalWindow("changeAttributesViewWin"));

        //table's columnsList = attributes to view
        final IModel columns = new LoadableDetachableModel() {

            protected Object load() {
                ConfigurationTO configuration =
                        confRestClient.readConfiguration(
                        "users.attributes.view");
                columnsList = new ArrayList<String>();

                if (configuration != null && configuration.getConfValue()
                        != null) {
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


        paginatorRows = utility.getPaginatorRowsToDisplay(
                Constants.CONF_USERS_PAGINATOR_ROWS);

        paginatorSearchRows = utility.getPaginatorRowsToDisplay(
                Constants.CONF_USERS_SEARCH_PAGINATOR_ROWS);

        setupPaginatedUsers();

        IModel usersModel = new LoadableDetachableModel() {

            @Override
            protected Object load() {
                paginatedUsers = userRestClient.getPaginatedUser(
                        currentViewPage, paginatorRows);

                //Refresh links just after the selecting page click
                if (incrementUserViewLink != null && decrementUserViewLink != null
                        && firstPageLink != null && lastPageLink != null) {
                    int totalPages = (int) Math.ceil(
                            paginatedUsers.getTotalRecords().doubleValue()
                            / new Double(paginatedUsers.getPageSize()));

                    if (currentViewPage == totalPages) {
                        incrementUserViewLink.setEnabled(false);
                        lastPageLink.setEnabled(false);
                    } else {
                        incrementUserViewLink.setEnabled(true);
                        lastPageLink.setEnabled(true);
                    }

                    if (currentViewPage == 1) {
                        decrementUserViewLink.setEnabled(false);
                    } else {
                        decrementUserViewLink.setEnabled(true);
                    }

                    if (totalPages == 1 || currentViewPage == 1) {
                        firstPageLink.setEnabled(false);
                    } else {
                        firstPageLink.setEnabled(true);
                    }
                }

                if (pageRecordFrom != null && pageRecordTo != null
                        && totalRecords != null) {

                    //Records indexes for paginator's labels
                    int firstPageRecord = 1;
                    int lastPageRecord = paginatedUsers.getRecordsInPage();

                    if (paginatedUsers.getPageNumber() > 1) {
                        firstPageRecord = (paginatedUsers.getPageSize()
                                * (paginatedUsers.getPageNumber() - 1)) + 1;

                        lastPageRecord = (paginatedUsers.getPageSize()
                                * (paginatedUsers.getPageNumber() - 1))
                                + paginatedUsers.getRecordsInPage();
                    }

                    pageRecordFrom.setDefaultModelObject(
                            String.valueOf(firstPageRecord));

                    pageRecordTo.setDefaultModelObject(
                            String.valueOf(lastPageRecord));

                    totalRecords.setDefaultModelObject(String.valueOf(
                            paginatedUsers.getTotalRecords()));

                }


                return paginatedUsers.getRecords();
            }
        };

        usersTableContainer = new WebMarkupContainer("usersTableContainer");

        final PageableListView usersView = new PageableListView("results",
                usersModel, paginatorRows) {

            @Override
            protected void populateItem(final ListItem item) {

                final UserTO userTO = (UserTO) item.getModelObject();

                item.add(new Label("id", String.valueOf(userTO.getId())));

                item.add(new Label("status", String.valueOf(
                        userTO.getStatus())));

                if (userTO.getToken() != null && !userTO.getToken().equals("")) {
                    item.add(new Label("token", getString("tokenValued")));
                } else {
                    item.add(new Label("token", getString("tokenNotValued")));
                }

                item.add(new ListView("selectedAttributes", attributesToDisplay(
                        userTO)) {

                    @Override
                    protected void populateItem(ListItem item) {
                        AttributeWrapper attribute =
                                (AttributeWrapper) item.getDefaultModelObject();

                        for (String name : columnsList) {

                            if (name.equalsIgnoreCase(attribute.getKey())) {
                                item.add(new Label("name", attribute.getValue()));
                            } else if (!name.equalsIgnoreCase(attribute.
                                    getKey())) {
                            }

                        }

                    }
                });

                AjaxLink editLink = new AjaxLink("editLink",
                        new Model(getString("edit"))) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        final UserTO userTO = (UserTO) item.
                                getDefaultModelObject();

                        editUserWin.setPageCreator(
                                new ModalWindow.PageCreator() {

                                    public Page createPage() {
                                        UserModalPage window =
                                                new UserModalPage(
                                                Users.this, editUserWin, userTO,
                                                false);
                                        return window;
                                    }
                                });

                        editUserWin.show(target);
                    }
                };

                item.add(editLink);

                item.add(new AjaxLink("deleteLink", new Model(
                        getString("delete"))) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        userRestClient.deleteUser(
                                String.valueOf(userTO.getId()));

                        info(getString("operation_succeded"));
                        target.addComponent(feedbackPanel);

                        target.addComponent(usersTableSearchContainer);
                    }

                    @Override
                    protected IAjaxCallDecorator getAjaxCallDecorator() {
                        return new AjaxPreprocessingCallDecorator(super.
                                getAjaxCallDecorator()) {

                            private static final long serialVersionUID = 1L;

                            @Override
                            public CharSequence preDecorateScript(
                                    CharSequence script) {

                                return "if (confirm('" + getString(
                                        "confirmDelete") + "'))"
                                        + "{" + script + "}";
                            }
                        };
                    }
                });
            }
        };

        usersTableContainer.add(usersView);
        usersTableContainer.setOutputMarkupId(true);

        incrementUserViewLink = new AjaxLink("incrementLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                currentViewPage++;

                //Update pageLinks on paginator
                List<Integer> pageIdList = getPaginatorIndexes();
                pageLinksView.setList(pageIdList);
                target.addChildren(pageLinksView, AjaxLink.class);

                target.addComponent(usersTableContainer);
            }
        };

        int totalPages = (int) Math.ceil(paginatedUsers.getTotalRecords().
                doubleValue() / new Double(paginatedUsers.getPageSize()));

        firstPageLink = new AjaxLink("firstPageLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                currentViewPage = 1;

                //Update pageLinks on paginator
                List<Integer> pageIdList = getPaginatorIndexes();
                pageLinksView.setList(pageIdList);
                target.addChildren(pageLinksView, AjaxLink.class);

                target.addComponent(usersTableContainer);
            }
        };

        lastPageLink = new AjaxLink("lastPageLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                int totalPages = (int) Math.ceil(paginatedUsers.getTotalRecords().
                        doubleValue() / new Double(
                        paginatedUsers.getPageSize()));
                currentViewPage = totalPages;

                //Update pageLinks on paginator
                List<Integer> pageIdList = getPaginatorIndexes();
                pageLinksView.setList(pageIdList);
                target.addChildren(pageLinksView, AjaxLink.class);

                target.addComponent(usersTableContainer);
            }
        };

        decrementUserViewLink = new AjaxLink("decrementLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                currentViewPage--;

                //Update pageLinks on paginator
                List<Integer> pageIdList = getPaginatorIndexes();
                pageLinksView.setList(pageIdList);
                target.addChildren(pageLinksView, AjaxLink.class);

                target.addComponent(usersTableContainer);
            }
        };

        if (currentViewPage == totalPages) {
            incrementUserViewLink.setEnabled(false);
            lastPageLink.setEnabled(false);
        }

        if (currentViewPage == 1) {
            decrementUserViewLink.setEnabled(false);
        }

        if (totalPages == 1 || currentViewPage == 1) {
            firstPageLink.setEnabled(false);
        }

        //Add to usersTableSearchContainer users' list navigation controls
        usersTableContainer.add(incrementUserViewLink);
        //usersTableContainer.add(currentPageUserViewLabel);
        usersTableContainer.add(firstPageLink);
        usersTableContainer.add(lastPageLink);

        //Records indexes for paginator's labels
        int firstPageRecord = 1;
        int lastPageRecord = paginatedUsers.getRecordsInPage();

        if (paginatedUsers.getPageNumber() > 1) {
            firstPageRecord = (paginatedUsers.getPageSize()
                    * (paginatedUsers.getPageNumber() - 1)) + 1;

            lastPageRecord = (paginatedUsers.getPageSize()
                    * (paginatedUsers.getPageNumber() - 1))
                    + paginatedUsers.getRecordsInPage();
        }

        usersTableContainer.add(pageRecordFrom = new Label("pageRecordFrom",
                new Model<String>(String.valueOf(firstPageRecord))));

        usersTableContainer.add(pageRecordTo = new Label("pageRecordTo",
                new Model<String>(String.valueOf(lastPageRecord))));

        usersTableContainer.add(totalRecords = new Label("totalRecords",
                new Model<String>(String.valueOf(
                paginatedUsers.getTotalRecords()))));

        usersTableContainer.add(decrementUserViewLink);

        //Build pages link for paginator
        List<Integer> pageIdList = getPaginatorIndexes();

        pageLinksView = new ListView("pageLinksView", pageIdList) {

            @Override
            protected void populateItem(ListItem item) {
                final int pageId = (Integer) item.getDefaultModelObject();

                AjaxLink pageLink = new AjaxLink("pageLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        currentViewPage = pageId;

                        //Update pageLinks on paginator
                        List<Integer> pageIdList = getPaginatorSearchIndexes();
                        pageLinksSearchView.setList(pageIdList);
                        target.addChildren(pageLinksSearchView, AjaxLink.class);

                        target.addComponent(usersTableContainer);
                    }
                };

                if (currentViewPage == pageId) {
                    pageLink.setEnabled(false);
                } else {
                    pageLink.setEnabled(true);
                }

                pageLink.add(new Label("name", new Model<String>(
                        String.valueOf(pageId))));

                item.add(pageLink);
            }
        };

        usersTableContainer.add(pageLinksView);
        usersTableContainer.add(columnsView);

        add(usersTableContainer);

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

        changeAttribsViewWin.setWindowClosedCallback(
                new ModalWindow.WindowClosedCallback() {

                    public void onClose(AjaxRequestTarget target) {

                        if (operationResult) {
                            getSession().info(getString("operation_succeded"));
                            setResponsePage(Users.class);
                        } //When the window is closed without calling backend
                        else {
                            target.addComponent(feedbackPanel);
                        }
                    }
                });

        AjaxLink createUserLink = new AjaxLink("createUserLink") {

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
        };

        String allowedRoles = null;

        allowedRoles = xmlRolesReader.getAllAllowedRoles("Users", "create");

        MetaDataRoleAuthorizationStrategy.authorize(createUserLink, ENABLE,
                allowedRoles);

        add(createUserLink);

        Form paginatorForm = new Form("PaginatorForm");

        final DropDownChoice rowsChooser = new DropDownChoice("rowsChooser",
                new PropertyModel(this, "paginatorRows"), utility.
                paginatorRowsChooser());

        rowsChooser.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            protected void onUpdate(AjaxRequestTarget target) {
                utility.updatePaginatorRows(Constants.CONF_USERS_PAGINATOR_ROWS,
                        paginatorRows);

                usersView.setRowsPerPage(paginatorRows);

                //Reload page
                setResponsePage(Users.class);
            }
        });

        paginatorForm.add(rowsChooser);
        add(paginatorForm);

        AjaxLink changeAttributesViewLink = new AjaxLink(
                "changeAttributesViewLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {

                changeAttribsViewWin.setPageCreator(
                        new ModalWindow.PageCreator() {

                            public Page createPage() {
                                DisplayAttributesModalPage window =
                                        new DisplayAttributesModalPage(
                                        Users.this,
                                        changeAttribsViewWin, true);
                                return window;
                            }
                        });

                changeAttribsViewWin.show(target);
            }
        };

        String allowedViewRoles = null;

        allowedViewRoles = xmlRolesReader.getAllAllowedRoles("Users",
                "changeView");

        MetaDataRoleAuthorizationStrategy.authorize(changeAttributesViewLink,
                RENDER, allowedViewRoles);

        add(changeAttributesViewLink);

        //TAB 2 - Search section start
        final IModel userAttributes = new LoadableDetachableModel() {

            protected Object load() {
                return schemaRestClient.getAllUserSchemasNames();
            }
        };

        final IModel roleNames = new LoadableDetachableModel() {

            protected Object load() {
                List<RoleTO> roleTOs = roleRestClient.getAllRoles();

                List<String> roleNames = new ArrayList<String>();

                for (RoleTO role : roleTOs) {
                    roleNames.add(role.getName());
                }

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
                return Arrays.asList(SearchConditionWrapper.FilterType.values());
            }
        };

        Form form = new Form("UserSearchForm");

        form.add(new FeedbackPanel("searchFeedback").setOutputMarkupId(true));

        usersTableSearchContainer = new WebMarkupContainer("container");
        usersTableSearchContainer.setOutputMarkupId(true);

        setWindowClosedCallback(createUserWin, usersTableContainer);
        setWindowClosedCallback(editUserWin, usersTableSearchContainer);

        ListView searchView = new ListView("searchView", searchConditionsList) {

            @Override
            protected void populateItem(final ListItem item) {
                final SearchConditionWrapper searchCondition =
                        (SearchConditionWrapper) item.getDefaultModelObject();

                if (item.getIndex() == 0) {
                    item.add(new Label("operationType", ""));
                } else {
                    item.add(new Label("operationType", searchCondition.
                            getOperationType().toString()));
                }

                item.add(new UpdatingCheckBox("notOperator",
                        new PropertyModel(searchCondition,
                        "notOperator")));

                final UpdatingDropDownChoice filterNameChooser =
                        new UpdatingDropDownChoice("filterName",
                        new PropertyModel(searchCondition, "filterName"),
                        null);

                if (searchCondition.getFilterType() == null) {
                    filterNameChooser.setChoices(Collections.emptyList());
                } else if (searchCondition.getFilterType()
                        == SearchConditionWrapper.FilterType.ATTRIBUTE) {
                    filterNameChooser.setChoices(userAttributes);
                } else {
                    filterNameChooser.setChoices(roleNames);
                }

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

                if (searchCondition.getFilterType()
                        == SearchConditionWrapper.FilterType.MEMBERSHIP) {

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

                                if (schemaType == SearchConditionWrapper.FilterType.ATTRIBUTE) {

                                    return userAttributes;
                                } else {

                                    return roleNames;
                                }

                            }
                        });
                        target.addComponent(filterNameChooser);
                        target.addComponent(usersTableSearchContainer);
                    }
                });

                filterTypeChooser.setRequired(true);

                item.add(filterTypeChooser);

                AjaxButton dropButton = new AjaxButton("dropButton",
                        new Model(getString("dropButton"))) {

                    @Override
                    protected void onSubmit(AjaxRequestTarget target,
                            Form form) {
                        final int parentId = new Integer(getParent().getId());
                        searchConditionsList.remove(parentId);
                        target.addComponent(usersTableSearchContainer);
                    }
                };

                dropButton.setDefaultFormProcessing(false);

                if (item.getIndex() == 0) {
                    dropButton.setVisible(false);
                }

                item.add(dropButton);
            }
        };

        usersTableSearchContainer.add(searchView);

        AjaxButton addAndButton = new AjaxButton("addAndButton", new Model(
                getString("addAndButton"))) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                SearchConditionWrapper conditionWrapper =
                        new SearchConditionWrapper();
                conditionWrapper.setOperationType(OperationType.AND);
                searchConditionsList.add(conditionWrapper);
                target.addComponent(usersTableSearchContainer);
            }
        };

        addAndButton.setDefaultFormProcessing(false);
        usersTableSearchContainer.add(addAndButton);

        AjaxButton addOrButton = new AjaxButton("addOrButton", new Model(
                getString("addOrButton"))) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                SearchConditionWrapper conditionWrapper =
                        new SearchConditionWrapper();
                conditionWrapper.setOperationType(OperationType.OR);
                searchConditionsList.add(conditionWrapper);
                target.addComponent(usersTableSearchContainer);
            }
        };

        addOrButton.setDefaultFormProcessing(false);
        usersTableSearchContainer.add(addOrButton);

        form.add(usersTableSearchContainer);

        IModel resultsModel = new LoadableDetachableModel() {

            @Override
            protected Object load() {

                if (nodeCond != null) {

                    paginatedSearchUsers = userRestClient.paginatedSearchUser(
                            nodeCond, currentSearchPage, paginatorSearchRows);

                    //Refresh links just after the selecting page click
                    if (incrementUserLinkSearch != null && decrementUserLinkSearch != null
                            && firstPageLinkSearch != null && lastPageLinkSearch != null) {
                        int totalPages = (int) Math.ceil(
                                paginatedSearchUsers.getTotalRecords().
                                doubleValue()
                                / new Double(paginatedSearchUsers.getPageSize()));

                        if (currentSearchPage == totalPages) {
                            incrementUserLinkSearch.setEnabled(false);
                            lastPageLinkSearch.setEnabled(false);
                        } else {
                            incrementUserLinkSearch.setEnabled(true);
                            lastPageLinkSearch.setEnabled(true);
                        }

                        if (currentSearchPage > 1) {
                            decrementUserLinkSearch.setEnabled(true);
                        } else {
                            decrementUserLinkSearch.setEnabled(false);
                        }

                        if (totalPages == 1 || currentSearchPage == 1) {
                            firstPageLinkSearch.setEnabled(false);
                        } else {
                            firstPageLinkSearch.setEnabled(true);
                        }
                    }

                    if (pageRecordFromSearch != null && pageRecordToSearch != null
                            && totalRecordsSearch != null) {

                        //Records indexes for paginator's labels
                        int firstPageRecord = 1;
                        int lastPageRecord = paginatedSearchUsers.
                                getRecordsInPage();

                        if (paginatedSearchUsers.getPageNumber() > 1) {
                            firstPageRecord = (paginatedSearchUsers.getPageSize()
                                    * (paginatedSearchUsers.getPageNumber() - 1)) + 1;

                            lastPageRecord = (paginatedSearchUsers.getPageSize()
                                    * (paginatedSearchUsers.getPageNumber() - 1))
                                    + paginatedSearchUsers.getRecordsInPage();
                        }

                        if (paginatedSearchUsers.getRecordsInPage() == 0) {
                            lastPageRecord = 0;
                            firstPageRecord = 0;

                            firstPageLinkSearch.setEnabled(false);
                            incrementUserLinkSearch.setEnabled(false);
                            lastPageLinkSearch.setEnabled(false);
                            incrementUserLinkSearch.setEnabled(false);
                        }

                        pageRecordFromSearch.setDefaultModelObject(
                                String.valueOf(firstPageRecord));

                        pageRecordToSearch.setDefaultModelObject(
                                String.valueOf(lastPageRecord));

                        totalRecordsSearch.setDefaultModelObject(String.valueOf(
                                paginatedSearchUsers.getTotalRecords()));

                    }
                    return paginatedSearchUsers.getRecords();
                } else {
                    return new ArrayList();
                }

            }
        };

        final PageableListView resultsView = new PageableListView("results",
                resultsModel, paginatorSearchRows) {

            @Override
            protected void populateItem(final ListItem item) {

                final UserTO userTO = (UserTO) item.getModelObject();

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

                item.add(new AjaxLink("deleteLink", new Model(
                        getString("delete"))) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        userRestClient.deleteUser(
                                String.valueOf(userTO.getId()));

                        info(getString("operation_succeded"));
                        target.addComponent(feedbackPanel);

                        target.addComponent(usersTableContainer);
                    }
                });
            }
        };

        searchResultsContainer =
                new WebMarkupContainer("searchResultsContainer");
        searchResultsContainer.setOutputMarkupId(true);
        searchResultsContainer.add(resultsView);

        /** SEARCH PAGiNATOR START */
        incrementUserLinkSearch = new AjaxLink("incrementLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                currentSearchPage++;

                //Update pageLinks on paginator
                List<Integer> pageIdList = getPaginatorIndexes();
                pageLinksSearchView.setList(pageIdList);
                target.addChildren(pageLinksSearchView, AjaxLink.class);

                target.addComponent(searchResultsContainer);
            }
        };

        //Check if it is not null
        int totalSearchPages = 0;
        if (paginatedSearchUsers != null) {
            totalSearchPages = (int) Math.ceil(paginatedSearchUsers.
                    getTotalRecords().doubleValue() / new Double(paginatedSearchUsers.
                    getPageSize()));
        }

        firstPageLinkSearch = new AjaxLink("firstPageLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                currentSearchPage = 1;

                //Update pageLinks on paginator
                List<Integer> pageIdList = getPaginatorSearchIndexes();
                pageLinksSearchView.setList(pageIdList);
                target.addChildren(pageLinksSearchView, AjaxLink.class);

                target.addComponent(searchResultsContainer);
            }
        };

        lastPageLinkSearch = new AjaxLink("lastPageLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                int totalPages = (int) Math.ceil(paginatedSearchUsers.
                        getTotalRecords().doubleValue() / new Double(
                        paginatedSearchUsers.getPageSize()));
                currentSearchPage = totalPages;

                //Update pageLinks on paginator
                List<Integer> pageIdList = getPaginatorSearchIndexes();
                pageLinksSearchView.setList(pageIdList);
                target.addChildren(pageLinksSearchView, AjaxLink.class);

                target.addComponent(searchResultsContainer);
            }
        };

        decrementUserLinkSearch = new AjaxLink("decrementLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                currentSearchPage--;

                //Update pageLinks on paginator
                List<Integer> pageIdList = getPaginatorSearchIndexes();
                pageLinksSearchView.setList(pageIdList);
                target.addChildren(pageLinksSearchView, AjaxLink.class);

                target.addComponent(searchResultsContainer);
            }
        };

        if (currentSearchPage <= totalSearchPages) {
            incrementUserLinkSearch.setEnabled(false);
            lastPageLinkSearch.setEnabled(false);
        }

        if (currentSearchPage <= 1) {
            decrementUserLinkSearch.setEnabled(false);
        }

        if (totalPages == 1 || currentSearchPage == 1) {
            firstPageLinkSearch.setEnabled(false);
        }

        if (currentSearchPage <= 1 && paginatedSearchUsers == null) {
            incrementUserLinkSearch.setEnabled(false);
            lastPageLinkSearch.setEnabled(false);
            firstPageLinkSearch.setEnabled(false);
            decrementUserLinkSearch.setEnabled(false);
        } else if (currentSearchPage > 1 && currentSearchPage <= totalPages) {
            decrementUserLinkSearch.setEnabled(true);
        }

        searchResultsContainer.add(incrementUserLinkSearch);
        searchResultsContainer.add(firstPageLinkSearch);
        searchResultsContainer.add(lastPageLinkSearch);

        //Records indexes for paginator's labels
        int firstPageRecordSearch = 1;
        int lastPageRecordSearch = 1;

        if (paginatedSearchUsers != null) {
            lastPageRecordSearch = paginatedSearchUsers.getRecordsInPage();

            if (paginatedSearchUsers.getPageNumber() > 1) {
                firstPageRecordSearch = (paginatedSearchUsers.getPageSize()
                        * (paginatedSearchUsers.getPageNumber() - 1)) + 1;

                lastPageRecordSearch = (paginatedSearchUsers.getPageSize()
                        * (paginatedSearchUsers.getPageNumber() - 1))
                        + paginatedSearchUsers.getRecordsInPage();
            }
        } else {
            firstPageRecordSearch = 0;
            lastPageRecordSearch = 0;
        }

        searchResultsContainer.add(pageRecordFromSearch = new Label(
                "pageRecordFrom",
                new Model<String>(String.valueOf(firstPageRecordSearch))));

        searchResultsContainer.add(pageRecordToSearch = new Label("pageRecordTo",
                new Model<String>(String.valueOf(lastPageRecordSearch))));

        if (paginatedSearchUsers != null) {
            searchResultsContainer.add(totalRecordsSearch = new Label(
                    "totalRecords",
                    new Model<String>(String.valueOf(paginatedSearchUsers.
                    getTotalRecords()))));
        } else {
            searchResultsContainer.add(totalRecordsSearch = new Label(
                    "totalRecords",
                    new Model<String>("0")));
        }

        searchResultsContainer.add(decrementUserLinkSearch);

        //Build pages link for paginator
        List<Integer> pageIdListSearch = getPaginatorSearchIndexes();

        pageLinksSearchView = new ListView("pageLinksView", pageIdListSearch) {

            @Override
            protected void populateItem(ListItem item) {
                final int pageId = (Integer) item.getDefaultModelObject();

                AjaxLink pageLink = new AjaxLink("pageLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        currentSearchPage = pageId;
                        target.addComponent(searchResultsContainer);
                    }
                };

                if (currentSearchPage == pageId) {
                    pageLink.setEnabled(false);
                } else {
                    pageLink.setEnabled(true);
                }

                pageLink.add(new Label("name", new Model<String>(
                        String.valueOf(pageId))));

                item.add(pageLink);
            }
        };

        searchResultsContainer.add(pageLinksSearchView);

        /** SEARCH PAGiNATOR END */
        //Display warning message if no search matches have been found
        final Label noResults = new Label("noResults", new Model<String>(""));
        noResults.setOutputMarkupId(true);
        searchResultsContainer.add(noResults);

        setWindowClosedCallback(editUserWin, searchResultsContainer);

        form.add(new IndicatingAjaxButton("search", new Model(
                getString("search"))) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {

                nodeCond = buildSearchExpression(searchConditionsList);

                if (nodeCond != null) {

                    try {
                        paginatedSearchUsers = userRestClient.
                                paginatedSearchUser(
                                nodeCond, currentSearchPage = 1,
                                paginatorSearchRows);

                        //Clean the feedback panel if the operation succedes
                        target.addComponent(form.get("searchFeedback"));

                        //Update pageLinks on paginator
                        List<Integer> pageIdList = getPaginatorSearchIndexes();
                        pageLinksSearchView.setList(pageIdList);
                        target.addChildren(pageLinksSearchView, AjaxLink.class);
                    } catch (HttpServerErrorException e) {
                        LOG.error("While searching users", e);
                        error(e.getMessage());
                        return;
                    }
                } else {
                    error(getString("search_error"));
                }

                if (paginatedSearchUsers.getRecords().isEmpty()) {
                    noResults.setDefaultModel(new Model<String>(
                            getString("search_noResults")));
                } else {
                    noResults.setDefaultModel(new Model<String>(""));
                }

                target.addComponent(searchResultsContainer);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                target.addComponent(form.get("searchFeedback"));
            }
        });

        add(form);

        form.add(searchResultsContainer);

        Form paginatorSearchForm = new Form("PaginatorSearchForm");

        final DropDownChoice rowsSearchChooser = new DropDownChoice(
                "rowsSearchChooser",
                new PropertyModel(this, "paginatorSearchRows"), utility.
                paginatorRowsChooser());

        rowsSearchChooser.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            protected void onUpdate(AjaxRequestTarget target) {
                utility.updatePaginatorRows(
                        Constants.CONF_USERS_SEARCH_PAGINATOR_ROWS,
                        paginatorSearchRows);

                resultsView.setRowsPerPage(paginatorSearchRows);

                //Re-execute the query
                paginatedSearchUsers = userRestClient.paginatedSearchUser(
                        nodeCond, currentSearchPage = 1, paginatorSearchRows);

                //Update pageLinks on paginator
                List<Integer> pageIdList = getPaginatorSearchIndexes();
                pageLinksSearchView.setList(pageIdList);
                target.addChildren(pageLinksSearchView, AjaxLink.class);

                target.addComponent(searchResultsContainer);
            }
        });

        paginatorSearchForm.add(rowsSearchChooser);

        add(paginatorSearchForm);
    }

    public PaginatedResult getPaginatedUsers(int page, int size) {

        PaginatedResult paginatedResult =
                userRestClient.getPaginatedUser(page, size);

        return paginatedResult;
    }

    /**
     * Refresh paginator after page link click.
     */
    public List<Integer> getPaginatorIndexes() {
        int totalPages = (int) Math.ceil(paginatedUsers.getTotalRecords().
                doubleValue() / new Double(paginatedUsers.getPageSize()));

        //Build pages link for paginator
        List<Integer> pageIdList = new ArrayList<Integer>();

        int startIndex = 1;

        if (totalPages > 10) {
            if (currentViewPage < 10) {
                startIndex = 1;
            } else {
                startIndex = currentViewPage - 2;
            }
        }

        int endIndex = totalPages;

        if (totalPages > 10) {

            if (startIndex + 9 <= totalPages) {
                endIndex = startIndex + 9;
            } else {
                endIndex = startIndex + (totalPages - startIndex);
            }
        }

        for (int i = startIndex; i <= endIndex; i++) {
            pageIdList.add(i);
        }

        return pageIdList;
    }

    /**
     * Return the user's attributes columnsList to display, ordered
     * @param userTO instance
     * @return attributes columnsList to view depending the selection
     */
    public List<AttributeWrapper> attributesToDisplay(UserTO user) {
        List<AttributeTO> attributes = user.getAttributes();
        List<AttributeWrapper> attributesList = new ArrayList<AttributeWrapper>();

        ConfigurationTO configuration =
                confRestClient.readConfiguration(
                "users.attributes.view");
        columnsList = new ArrayList<String>();

        if (configuration != null && configuration.getConfValue() != null
                && !configuration.getConfValue().equals("")) {
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
     * @param usersTableSearchContainer
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

        SearchConditionWrapper searchConditionWrapper = conditions.iterator().
                next();

        if (searchConditionWrapper.getFilterType()
                == SearchConditionWrapper.FilterType.ATTRIBUTE) {

            attributeCond = new AttributeCond();
            attributeCond.setSchema(searchConditionWrapper.getFilterName());
            attributeCond.setType(searchConditionWrapper.getType());
            attributeCond.setExpression(searchConditionWrapper.getFilterValue());

        } else {

            membershipCond = new MembershipCond();
            membershipCond.setRoleName(searchConditionWrapper.getFilterName());

        }

        if (conditions.size() == 1) {

            if (searchConditionWrapper.getFilterType()
                    == SearchConditionWrapper.FilterType.ATTRIBUTE) {
                if (searchConditionWrapper.isNotOperator()) {
                    return NodeCond.getNotLeafCond(attributeCond);
                } else {
                    return NodeCond.getLeafCond(attributeCond);
                }
            } else {
                if (searchConditionWrapper.isNotOperator()) {
                    return NodeCond.getNotLeafCond(membershipCond);
                } else {
                    return NodeCond.getLeafCond(membershipCond);
                }
            }

        } else {

            subList = conditions.subList(1, conditions.size());

            searchConditionWrapper = subList.iterator().next();

            if (searchConditionWrapper.getOperationType()
                    == SearchConditionWrapper.OperationType.AND) {

                if (searchConditionWrapper.getFilterType()
                        == SearchConditionWrapper.FilterType.ATTRIBUTE) {

                    if (attributeCond != null) {
                        return NodeCond.getAndCond(
                                NodeCond.getLeafCond(attributeCond),
                                buildSearchExpression(
                                new ArrayList<SearchConditionWrapper>(subList)));
                    } else {
                        return NodeCond.getAndCond(
                                NodeCond.getLeafCond(membershipCond),
                                buildSearchExpression(
                                new ArrayList<SearchConditionWrapper>(subList)));
                    }
                } else {
                    if (attributeCond != null) {
                        return NodeCond.getAndCond(
                                NodeCond.getLeafCond(attributeCond),
                                buildSearchExpression(
                                new ArrayList<SearchConditionWrapper>(subList)));
                    } else {
                        return NodeCond.getAndCond(
                                NodeCond.getLeafCond(membershipCond),
                                buildSearchExpression(
                                new ArrayList<SearchConditionWrapper>(subList)));
                    }
                }

            } else {
                if (searchConditionWrapper.getFilterType()
                        == SearchConditionWrapper.FilterType.ATTRIBUTE) {
                    if (attributeCond != null) {
                        return NodeCond.getOrCond(
                                NodeCond.getLeafCond(attributeCond),
                                buildSearchExpression(
                                new ArrayList<SearchConditionWrapper>(subList)));
                    } else {
                        return NodeCond.getOrCond(
                                NodeCond.getLeafCond(membershipCond),
                                buildSearchExpression(
                                new ArrayList<SearchConditionWrapper>(subList)));
                    }
                } else {
                    if (attributeCond != null) {
                        return NodeCond.getOrCond(
                                NodeCond.getLeafCond(attributeCond),
                                buildSearchExpression(
                                new ArrayList<SearchConditionWrapper>(subList)));
                    } else {
                        return NodeCond.getOrCond(
                                NodeCond.getLeafCond(membershipCond),
                                buildSearchExpression(
                                new ArrayList<SearchConditionWrapper>(subList)));
                    }
                }
            }
        }
    }

    /**
     *  Init users to display.
     */
    public void setupPaginatedUsers() {
        paginatedUsers = userRestClient.getPaginatedUser(currentViewPage,
                paginatorRows);
    }

    /**
     * Refresh paginator search after page link click.
     */
    public List<Integer> getPaginatorSearchIndexes() {
        int totalPages;

        if (paginatedSearchUsers != null) {
            totalPages = (int) Math.ceil(paginatedSearchUsers.getTotalRecords().
                    doubleValue() / new Double(
                    paginatedSearchUsers.getPageSize()));
        } else {
            totalPages = 0;
        }

        //Build pages link for paginator
        List<Integer> pageIdList = new ArrayList<Integer>();

        int startIndex = 1;

        if (totalPages > 10) {
            if (currentSearchPage < 10) {
                startIndex = 1;
            } else {
                startIndex = currentSearchPage - 2;
            }
        }

        int endIndex = totalPages;

        if (totalPages > 10) {

            if (startIndex + 9 <= totalPages) {
                endIndex = startIndex + 9;
            } else {
                endIndex = startIndex + (totalPages - startIndex);
            }
        }

        for (int i = startIndex; i <= endIndex; i++) {
            pageIdList.add(i);
        }

        return pageIdList;
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
}
