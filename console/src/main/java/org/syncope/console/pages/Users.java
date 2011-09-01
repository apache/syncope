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
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.Page;
import org.apache.wicket.PageParameters;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.search.AttributeCond;
import org.syncope.client.search.MembershipCond;
import org.syncope.client.search.NodeCond;
import org.syncope.client.search.ResourceCond;
import org.syncope.client.to.ResourceTO;
import org.syncope.client.to.RoleTO;
import org.syncope.client.to.UserTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.console.commons.Constants;
import org.syncope.console.commons.PreferenceManager;
import org.syncope.console.commons.SearchConditionWrapper;
import org.syncope.console.commons.SearchConditionWrapper.FilterType;
import org.syncope.console.commons.SearchConditionWrapper.OperationType;
import org.syncope.console.commons.SelectChoiceRenderer;
import org.syncope.console.commons.SortableUserProviderComparator;
import org.syncope.console.rest.ResourceRestClient;
import org.syncope.console.rest.RoleRestClient;
import org.syncope.console.rest.SchemaRestClient;
import org.syncope.console.rest.UserRestClient;
import org.syncope.console.wicket.ajax.markup.html.IndicatingDeleteOnConfirmAjaxLink;
import org.syncope.console.wicket.markup.html.form.DeleteLinkPanel;
import org.syncope.console.wicket.markup.html.form.EditLinkPanel;

public class Users extends BasePage {

    private final static int EDIT_MODAL_WIN_HEIGHT = 680;

    private final static int EDIT_MODAL_WIN_WIDTH = 800;

    private final static int DISPLAYATTRS_MODAL_WIN_HEIGHT = 500;

    private final static int DISPLAYATTRS_MODAL_WIN_WIDTH = 600;

    private final static String DERIVED_ATTRIBUTE_PREFIX = "[D] ";

    private final static String VIRTUAL_ATTRIBUTE_PREFIX = "[V] ";

    @SpringBean
    private UserRestClient userRestClient;

    @SpringBean
    private SchemaRestClient schemaRestClient;

    @SpringBean
    private RoleRestClient roleRestClient;

    @SpringBean
    private ResourceRestClient resourceRestClient;

    @SpringBean
    private PreferenceManager prefMan;

    final private int paginatorRows = prefMan.getPaginatorRows(
            getWebRequestCycle().getWebRequest(),
            Constants.PREF_USERS_PAGINATOR_ROWS);

    final private int searchPaginatorRows = prefMan.getPaginatorRows(
            getWebRequestCycle().getWebRequest(),
            Constants.PREF_USERS_SEARCH_PAGINATOR_ROWS);

    protected boolean modalResult = false;

    final private IModel<List<String>> schemaNames =
            new LoadableDetachableModel<List<String>>() {

                @Override
                protected List<String> load() {
                    return schemaRestClient.getSchemaNames("user");
                }
            };

    final private IModel<List<String>> choosableSchemaNames =
            new LoadableDetachableModel<List<String>>() {

                @Override
                protected List<String> load() {

                    List<String> schemas =
                            schemaRestClient.getSchemaNames("user");

                    if (schemas == null) {
                        schemas = new ArrayList<String>();
                    }

                    List<String> derivedSchemas =
                            schemaRestClient.getDerivedSchemaNames("user");

                    if (derivedSchemas != null) {
                        for (String schema : derivedSchemas) {
                            schemas.add(DERIVED_ATTRIBUTE_PREFIX + schema);
                        }
                    }

                    List<String> virtualSchemas =
                            schemaRestClient.getVirtualSchemaNames("user");

                    if (virtualSchemas != null) {
                        for (String schema : virtualSchemas) {
                            schemas.add(VIRTUAL_ATTRIBUTE_PREFIX + schema);
                        }
                    }

                    return schemas;
                }
            };

    final private IModel<List<String>> roleNames =
            new LoadableDetachableModel<List<String>>() {

                @Override
                protected List<String> load() {
                    List<RoleTO> roleTOs = roleRestClient.getAllRoles();

                    List<String> result = new ArrayList<String>(
                            roleTOs.size());
                    for (RoleTO role : roleTOs) {
                        result.add(role.getDisplayName());
                    }

                    return result;
                }
            };

    final private IModel<List<String>> resourceNames =
            new LoadableDetachableModel<List<String>>() {

                @Override
                protected List<String> load() {
                    List<ResourceTO> resourceTOs = resourceRestClient.getAllResources();

                    List<String> result =
                            new ArrayList<String>(resourceTOs.size());

                    for (ResourceTO resource : resourceTOs) {
                        result.add(resource.getName());
                    }

                    return result;
                }
            };

    final private IModel<List<AttributeCond.Type>> attributeTypes =
            new LoadableDetachableModel<List<AttributeCond.Type>>() {

                @Override
                protected List<AttributeCond.Type> load() {
                    return Arrays.asList(AttributeCond.Type.values());
                }
            };

    final private IModel<List<FilterType>> filterTypes =
            new LoadableDetachableModel<List<FilterType>>() {

                @Override
                protected List<FilterType> load() {
                    return Arrays.asList(FilterType.values());
                }
            };

    final protected WebMarkupContainer listContainer;

    final protected WebMarkupContainer searchResultContainer;

    public Users(final PageParameters parameters) {
        super(parameters);

        // Modal window for editing user attributes
        final ModalWindow editModalWin =
                new ModalWindow("editModalWin");
        editModalWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editModalWin.setInitialHeight(EDIT_MODAL_WIN_HEIGHT);
        editModalWin.setInitialWidth(EDIT_MODAL_WIN_WIDTH);
        editModalWin.setPageMapName("user-edit-modal");
        editModalWin.setCookieName("user-edit-modal");
        add(editModalWin);

        // Modal window for choosing which attributes to display in tables
        final ModalWindow displayAttrsModalWin =
                new ModalWindow("displayAttrsModalWin");
        displayAttrsModalWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        displayAttrsModalWin.setInitialHeight(DISPLAYATTRS_MODAL_WIN_HEIGHT);
        displayAttrsModalWin.setInitialWidth(DISPLAYATTRS_MODAL_WIN_WIDTH);
        displayAttrsModalWin.setPageMapName("user-displayAttrs-modal");
        displayAttrsModalWin.setCookieName("user-displayAttrs-modal");
        add(displayAttrsModalWin);

        // Modal window for editing user attributes (in search tab)
        final ModalWindow searchEditModalWin =
                new ModalWindow("searchEditModalWin");
        searchEditModalWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        searchEditModalWin.setInitialHeight(EDIT_MODAL_WIN_HEIGHT);
        searchEditModalWin.setInitialWidth(EDIT_MODAL_WIN_WIDTH);
        searchEditModalWin.setPageMapName("user-search-edit-modal");
        searchEditModalWin.setCookieName("user-search-edit-modal");
        add(searchEditModalWin);

        // Container for user list
        listContainer = new WebMarkupContainer("listContainer");
        listContainer.setOutputMarkupId(true);
        add(listContainer);

        // Container for user search result
        searchResultContainer = new WebMarkupContainer("searchResultContainer");
        searchResultContainer.setOutputMarkupId(true);
        add(searchResultContainer);

        final AjaxFallbackDefaultDataTable<UserTO> listTable =
                new AjaxFallbackDefaultDataTable<UserTO>("listTable",
                getColumns(editModalWin), new UserDataProvider(),
                paginatorRows);
        if (parameters.getAsBoolean(Constants.PAGEPARAM_CREATE, false)) {
            listTable.setCurrentPage(listTable.getPageCount() - 1);
            parameters.remove(Constants.PAGEPARAM_CREATE);
        } else {
            listTable.setCurrentPage(parameters.getAsInteger(
                    listTable.getId() + Constants.PAGEPARAM_CURRENT_PAGE, 0));
        }
        listContainer.add(listTable);
        setWindowClosedReloadCallback(editModalWin, listTable);
        setWindowClosedReloadCallback(displayAttrsModalWin, listTable);

        // create new user
        AjaxLink createLink = new IndicatingAjaxLink("createLink") {

            @Override
            public void onClick(final AjaxRequestTarget target) {
                editModalWin.setPageCreator(new ModalWindow.PageCreator() {

                    @Override
                    public Page createPage() {
                        return new UserModalPage(
                                Users.this, editModalWin, new UserTO());
                    }
                });

                editModalWin.show(target);
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(createLink, ENABLE,
                xmlRolesReader.getAllAllowedRoles("Users", "create"));
        add(createLink);

        // select attributes to be displayed
        AjaxLink displayAttrsLink = new IndicatingAjaxLink("displayAttrsLink") {

            @Override
            public void onClick(final AjaxRequestTarget target) {

                displayAttrsModalWin.setPageCreator(
                        new ModalWindow.PageCreator() {

                            @Override
                            public Page createPage() {
                                return new DisplayAttributesModalPage(
                                        Users.this,
                                        choosableSchemaNames,
                                        displayAttrsModalWin);
                            }
                        });

                displayAttrsModalWin.show(target);
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(displayAttrsLink, ENABLE,
                xmlRolesReader.getAllAllowedRoles("Users", "changeView"));
        add(displayAttrsLink);

        // rows-per-page management
        Form paginatorForm = new Form("paginator");
        add(paginatorForm);
        final DropDownChoice<Integer> rowsChooser =
                new DropDownChoice<Integer>("rowsChooser",
                new PropertyModel(this, "paginatorRows"),
                prefMan.getPaginatorChoices(),
                new SelectChoiceRenderer());
        rowsChooser.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                prefMan.set(getWebRequestCycle().getWebRequest(),
                        getWebRequestCycle().getWebResponse(),
                        Constants.PREF_USERS_PAGINATOR_ROWS,
                        String.valueOf(rowsChooser.getInput()));

                listTable.setRowsPerPage(
                        Integer.parseInt(rowsChooser.getInput()));

                target.addComponent(listContainer);
            }
        });
        paginatorForm.add(rowsChooser);

        // search form
        final Form searchForm = new Form("searchForm");
        add(searchForm);

        final FeedbackPanel searchFeedback = new FeedbackPanel(
                "searchFeedback", new IFeedbackMessageFilter() {

            @Override
            public boolean accept(final FeedbackMessage message) {
                boolean result;

                // messages reported on the session have a null reporter
                if (message.getReporter() != null) {
                    // only accept messages coming from the children
                    // of the form
                    result = searchForm.contains(message.getReporter(), true);
                } else {
                    result = false;
                }

                return result;
            }
        });
        searchFeedback.setOutputMarkupId(true);
        searchForm.add(searchFeedback);

        final WebMarkupContainer searchFormContainer =
                new WebMarkupContainer("searchFormContainer");
        searchFormContainer.setOutputMarkupId(true);
        searchForm.add(searchFormContainer);

        final List<SearchConditionWrapper> searchConditionList =
                new ArrayList<SearchConditionWrapper>();
        searchConditionList.add(new SearchConditionWrapper());

        searchFormContainer.add(new SearchView("searchView",
                searchConditionList, searchFormContainer));

        AjaxButton addAndButton = new IndicatingAjaxButton("addAndButton",
                new Model(getString("addAndButton"))) {

            @Override
            protected void onSubmit(final AjaxRequestTarget target,
                    final Form form) {

                SearchConditionWrapper conditionWrapper =
                        new SearchConditionWrapper();
                conditionWrapper.setOperationType(OperationType.AND);
                searchConditionList.add(conditionWrapper);
                target.addComponent(searchFormContainer);
            }
        };
        addAndButton.setDefaultFormProcessing(false);
        searchFormContainer.add(addAndButton);

        AjaxButton addOrButton = new IndicatingAjaxButton("addOrButton",
                new Model(getString("addOrButton"))) {

            @Override
            protected void onSubmit(final AjaxRequestTarget target,
                    final Form form) {

                SearchConditionWrapper conditionWrapper =
                        new SearchConditionWrapper();
                conditionWrapper.setOperationType(OperationType.OR);
                searchConditionList.add(conditionWrapper);
                target.addComponent(searchFormContainer);
            }
        };
        addOrButton.setDefaultFormProcessing(false);
        searchFormContainer.add(addOrButton);

        // search result
        final UserSearchDataProvider searchDataProvider =
                new UserSearchDataProvider();
        final AjaxFallbackDefaultDataTable<UserTO> searchResultTable =
                new AjaxFallbackDefaultDataTable<UserTO>("searchResultTable",
                getColumns(searchEditModalWin), searchDataProvider,
                searchPaginatorRows);
        searchResultTable.setOutputMarkupId(true);
        searchResultTable.setCurrentPage(parameters.getAsInteger(
                searchResultTable.getId()
                + Constants.PAGEPARAM_CURRENT_PAGE, 0));
        searchResultContainer.add(searchResultTable);

        searchEditModalWin.setWindowClosedCallback(
                new ModalWindow.WindowClosedCallback() {

                    @Override
                    public void onClose(final AjaxRequestTarget target) {
                        doSearch(target, searchConditionList,
                                searchDataProvider, searchResultTable);

                        if (modalResult) {
                            info(getString("operation_succeded"));

                            getPage().getPageParameters().put(
                                    searchResultTable.getId()
                                    + Constants.PAGEPARAM_CURRENT_PAGE,
                                    searchResultTable.getCurrentPage());
                            setResponsePage(Users.class,
                                    getPage().getPageParameters());

                            target.addComponent(feedbackPanel);

                            modalResult = false;
                        }
                    }
                });

        searchForm.add(new IndicatingAjaxButton("search", new Model(
                getString("search"))) {

            @Override
            protected void onSubmit(final AjaxRequestTarget target,
                    final Form<?> form) {

                doSearch(target, searchConditionList, searchDataProvider,
                        searchResultTable);

                Session.get().getFeedbackMessages().clear();
                target.addComponent(searchFeedback);
            }

            @Override
            protected void onError(final AjaxRequestTarget target,
                    final Form form) {

                target.addComponent(searchFeedback);
            }
        });

        // search rows-per-page management
        Form searchPaginatorForm = new Form("searchPaginator");
        add(searchPaginatorForm);
        final DropDownChoice<Integer> searchRowsChooser =
                new DropDownChoice<Integer>("searchRowsChooser",
                new PropertyModel(this, "searchPaginatorRows"),
                prefMan.getPaginatorChoices());
        searchRowsChooser.add(
                new AjaxFormComponentUpdatingBehavior("onchange") {

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        prefMan.set(getWebRequestCycle().getWebRequest(),
                                getWebRequestCycle().getWebResponse(),
                                Constants.PREF_USERS_SEARCH_PAGINATOR_ROWS,
                                String.valueOf(searchPaginatorRows));

                        searchResultTable.setRowsPerPage(searchPaginatorRows);

                        target.addComponent(searchResultContainer);
                    }
                });
        searchPaginatorForm.add(searchRowsChooser);
    }

    private void setWindowClosedReloadCallback(final ModalWindow window,
            final AjaxFallbackDefaultDataTable<UserTO> table) {

        window.setWindowClosedCallback(
                new ModalWindow.WindowClosedCallback() {

                    @Override
                    public void onClose(final AjaxRequestTarget target) {
                        if (modalResult) {
                            getSession().info(getString("operation_succeded"));

                            getPage().getPageParameters().put(table.getId()
                                    + Constants.PAGEPARAM_CURRENT_PAGE,
                                    table.getCurrentPage());
                            setResponsePage(Users.class,
                                    getPage().getPageParameters());

                            target.addComponent(feedbackPanel);

                            modalResult = false;
                        }
                    }
                });
    }

    private List<IColumn<UserTO>> getColumns(final ModalWindow editModalWin) {
        List<IColumn<UserTO>> columns = new ArrayList<IColumn<UserTO>>();
        columns.add(new PropertyColumn(
                new Model(getString("id")), "id", "id"));
        columns.add(new PropertyColumn(
                new Model(getString("status")), "status", "status"));
        columns.add(new TokenColumn(new Model(getString("token")), "token"));

        for (String schemaName : prefMan.getList(getWebRequestCycle().
                getWebRequest(), Constants.PREF_USERS_ATTRIBUTES_VIEW)) {

            columns.add(new UserAttrColumn(
                    new Model<String>(schemaName), schemaName));
        }

        columns.add(new AbstractColumn<UserTO>(new Model<String>(getString(
                "edit"))) {

            @Override
            public void populateItem(
                    final Item<ICellPopulator<UserTO>> cellItem,
                    final String componentId,
                    final IModel<UserTO> model) {

                Panel panel = new EditLinkPanel(componentId, model);
                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                        xmlRolesReader.getAllAllowedRoles("Users", "read"));

                panel.add(new IndicatingAjaxLink("editLink") {

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        editModalWin.setPageCreator(
                                new ModalWindow.PageCreator() {

                                    @Override
                                    public Page createPage() {
                                        return new UserModalPage(
                                                Users.this, editModalWin,
                                                model.getObject());
                                    }
                                });

                        editModalWin.show(target);
                    }
                });
                cellItem.add(panel);
            }
        });
        columns.add(new AbstractColumn<UserTO>(new Model<String>(getString(
                "delete"))) {

            @Override
            public void populateItem(
                    final Item<ICellPopulator<UserTO>> cellItem,
                    final String componentId,
                    final IModel<UserTO> model) {

                Panel panel = new DeleteLinkPanel(componentId, model);
                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                        xmlRolesReader.getAllAllowedRoles("Users", "delete"));

                panel.add(new IndicatingDeleteOnConfirmAjaxLink("deleteLink") {

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {
                            userRestClient.delete(model.getObject().getId());
                            info(getString("operation_succeded"));
                        } catch (SyncopeClientCompositeErrorException scce) {
                            error(scce.getMessage());
                        }
                        target.addComponent(feedbackPanel);
                        target.addComponent(listContainer);
                        target.addComponent(searchResultContainer);
                    }
                });
                cellItem.add(panel);
            }
        });

        return columns;
    }

    private void doSearch(final AjaxRequestTarget target,
            final List<SearchConditionWrapper> searchConditionList,
            final UserSearchDataProvider searchDataProvider,
            final AjaxFallbackDefaultDataTable<UserTO> searchResultTable) {

        NodeCond searchCond = buildSearchCond(searchConditionList);
        LOG.debug("Node condition " + searchCond);

        if (searchCond == null || !searchCond.checkValidity()) {
            error(getString("search_error"));
            return;
        }
        searchDataProvider.setSearchCond(searchCond);

        target.addComponent(searchResultTable);
    }

    public void setModalResult(final boolean modalResult) {
        this.modalResult = modalResult;
    }

    private NodeCond buildSearchCond(
            final List<SearchConditionWrapper> conditions) {

        // inverse processing: from right to left
        // (OperationType is specified on the right)
        SearchConditionWrapper searchConditionWrapper =
                conditions.get(conditions.size() - 1);

        LOG.debug("Search conditions: "
                + "fname {}; ftype {}; fvalue {}; OP {}; type {}; isnot {}",
                new Object[]{
                    searchConditionWrapper.getFilterName(),
                    searchConditionWrapper.getFilterType(),
                    searchConditionWrapper.getFilterValue(),
                    searchConditionWrapper.getOperationType(),
                    searchConditionWrapper.getType(),
                    searchConditionWrapper.isNotOperator()});

        NodeCond nodeCond = null;

        switch (searchConditionWrapper.getFilterType()) {
            case ATTRIBUTE:
                final AttributeCond attributeCond = new AttributeCond();
                attributeCond.setSchema(searchConditionWrapper.getFilterName());
                attributeCond.setType(searchConditionWrapper.getType());
                attributeCond.setExpression(
                        searchConditionWrapper.getFilterValue());

                if (searchConditionWrapper.isNotOperator()) {
                    nodeCond = NodeCond.getNotLeafCond(attributeCond);
                } else {
                    nodeCond = NodeCond.getLeafCond(attributeCond);
                }

                break;
            case MEMBERSHIP:
                final MembershipCond membershipCond = new MembershipCond();
                membershipCond.setRoleId(RoleTO.fromDisplayName(
                        searchConditionWrapper.getFilterName()));

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
            List<SearchConditionWrapper> subList =
                    conditions.subList(0, conditions.size() - 1);

            if (OperationType.OR.equals(
                    searchConditionWrapper.getOperationType())) {
                nodeCond = NodeCond.getOrCond(
                        nodeCond,
                        buildSearchCond(subList));
            } else {
                nodeCond = NodeCond.getAndCond(
                        nodeCond,
                        buildSearchCond(subList));
            }
        }

        return nodeCond;
    }

    private class UserDataProvider extends SortableDataProvider<UserTO> {

        private SortableUserProviderComparator comparator;

        public UserDataProvider() {
            super();
            //Default sorting
            setSort("id", true);
            comparator = new SortableUserProviderComparator(this);
        }

        @Override
        public Iterator<UserTO> iterator(final int first, final int count) {
            List<UserTO> users = userRestClient.list(
                    (first / paginatorRows) + 1, paginatorRows);
            Collections.sort(users, comparator);
            return users.iterator();
        }

        @Override
        public int size() {
            return userRestClient.count();
        }

        @Override
        public IModel<UserTO> model(final UserTO object) {
            return new CompoundPropertyModel<UserTO>(object);
        }
    }

    private class UserSearchDataProvider extends SortableDataProvider<UserTO> {

        private SortableUserProviderComparator comparator;

        private NodeCond searchCond = null;

        public UserSearchDataProvider() {
            super();
            //Default sorting
            setSort("id", true);
            comparator = new SortableUserProviderComparator(this);
        }

        public void setSearchCond(NodeCond searchCond) {
            this.searchCond = searchCond;
        }

        @Override
        public Iterator<UserTO> iterator(final int first, final int count) {
            List<UserTO> users;
            if (searchCond == null) {
                users = Collections.EMPTY_LIST;
            } else {
                users = userRestClient.search(searchCond,
                        (first / searchPaginatorRows) + 1, searchPaginatorRows);
                Collections.sort(users, comparator);
            }

            return users.iterator();
        }

        @Override
        public int size() {
            return searchCond == null
                    ? 0 : userRestClient.searchCount(searchCond);
        }

        @Override
        public IModel<UserTO> model(final UserTO object) {
            return new CompoundPropertyModel<UserTO>(object);
        }
    }

    private class TokenColumn extends AbstractColumn<UserTO> {

        public TokenColumn(final IModel<String> displayModel,
                final String sortProperty) {

            super(displayModel, sortProperty);
        }

        @Override
        public void populateItem(final Item<ICellPopulator<UserTO>> cellItem,
                final String componentId,
                final IModel<UserTO> rowModel) {

            if (rowModel.getObject().getToken() != null
                    && !rowModel.getObject().getToken().isEmpty()) {
                cellItem.add(
                        new Label(componentId, getString("tokenValued")));
            } else {
                cellItem.add(
                        new Label(componentId, getString("tokenNotValued")));
            }
        }
    }

    private static class UserAttrColumn extends AbstractColumn<UserTO> {

        private final String schemaName;

        public UserAttrColumn(final IModel<String> displayModel,
                final String schemaName) {

            super(displayModel,
                    schemaName.startsWith(DERIVED_ATTRIBUTE_PREFIX)
                    ? schemaName.substring(
                    DERIVED_ATTRIBUTE_PREFIX.length(), schemaName.length())
                    : schemaName.startsWith(VIRTUAL_ATTRIBUTE_PREFIX)
                    ? schemaName.substring(
                    VIRTUAL_ATTRIBUTE_PREFIX.length(), schemaName.length())
                    : schemaName);

            this.schemaName = schemaName;
        }

        @Override
        public void populateItem(
                final Item<ICellPopulator<UserTO>> cellItem,
                final String componentId,
                final IModel<UserTO> rowModel) {

            Label label;

            List<String> values =
                    schemaName.startsWith(DERIVED_ATTRIBUTE_PREFIX)
                    ? rowModel.getObject().getDerivedAttributeMap().get(
                    schemaName.substring(
                    DERIVED_ATTRIBUTE_PREFIX.length(), schemaName.length()))
                    : schemaName.startsWith(VIRTUAL_ATTRIBUTE_PREFIX)
                    ? rowModel.getObject().getVirtualAttributeMap().get(
                    schemaName.substring(
                    VIRTUAL_ATTRIBUTE_PREFIX.length(), schemaName.length()))
                    : rowModel.getObject().getAttributeMap().get(schemaName);

            if (values == null || values.isEmpty()) {
                label = new Label(componentId, "");
            } else {
                if (values.size() == 1) {
                    label = new Label(componentId, values.iterator().next());
                } else {
                    label = new Label(componentId, values.toString());
                }
            }

            cellItem.add(label);
        }
    }

    private class SearchView extends ListView<SearchConditionWrapper> {

        final private WebMarkupContainer searchFormContainer;

        public SearchView(final String id,
                final List<? extends SearchConditionWrapper> list,
                final WebMarkupContainer searchFormContainer) {

            super(id, list);
            this.searchFormContainer = searchFormContainer;
        }

        @Override
        protected void populateItem(
                final ListItem<SearchConditionWrapper> item) {

            final SearchConditionWrapper searchCondition =
                    item.getModelObject();

            if (item.getIndex() == 0) {
                item.add(new Label("operationType", ""));
            } else {
                item.add(new Label("operationType",
                        searchCondition.getOperationType().
                        toString()));
            }

            item.add(new CheckBox("notOperator",
                    new PropertyModel(searchCondition,
                    "notOperator")));

            final DropDownChoice<String> filterNameChooser =
                    new DropDownChoice<String>("filterName",
                    new PropertyModel<String>(
                    searchCondition, "filterName"), (IModel) null);
            filterNameChooser.setOutputMarkupId(true);
            filterNameChooser.setRequired(true);
            item.add(filterNameChooser);

            final DropDownChoice<AttributeCond.Type> type =
                    new DropDownChoice<AttributeCond.Type>(
                    "type", new PropertyModel<AttributeCond.Type>(
                    searchCondition, "type"),
                    attributeTypes);
            item.add(type);

            final TextField<String> filterValue =
                    new TextField<String>("filterValue",
                    new PropertyModel<String>(searchCondition,
                    "filterValue"));
            item.add(filterValue);

            try {
                switch (searchCondition.getFilterType()) {
                    case ATTRIBUTE:
                        filterNameChooser.setChoices(schemaNames);
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
            } catch (NullPointerException npe) {
                // searchCondition null
                filterNameChooser.setChoices(Collections.EMPTY_LIST);
            }

            DropDownChoice<FilterType> filterTypeChooser =
                    new DropDownChoice<FilterType>("filterType",
                    new PropertyModel<FilterType>(searchCondition,
                    "filterType"), filterTypes);
            filterTypeChooser.setOutputMarkupId(true);

            filterTypeChooser.add(
                    new AjaxFormComponentUpdatingBehavior(
                    "onchange") {

                        @Override
                        protected void onUpdate(
                                final AjaxRequestTarget target) {

                            filterNameChooser.setChoices(
                                    searchCondition.getFilterType() ==
                                    FilterType.ATTRIBUTE
                                    ? schemaNames : roleNames);
                            target.addComponent(filterNameChooser);
                            target.addComponent(searchFormContainer);
                        }
                    });

            filterTypeChooser.setRequired(true);

            item.add(filterTypeChooser);

            AjaxButton dropButton = new IndicatingAjaxButton(
                    "dropButton", new Model(getString("dropButton"))) {

                @Override
                protected void onSubmit(
                        final AjaxRequestTarget target,
                        final Form form) {

                    getList().remove(
                            Integer.valueOf(getParent().getId()).intValue());
                    target.addComponent(searchFormContainer);
                }
            };

            dropButton.setDefaultFormProcessing(false);

            if (item.getIndex() == 0) {
                dropButton.setVisible(false);
            }

            item.add(dropButton);
        }
    }
}
