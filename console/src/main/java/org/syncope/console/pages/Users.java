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
import org.syncope.client.to.RoleTO;
import org.syncope.client.to.UserTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.console.commons.Constants;
import org.syncope.console.commons.PreferenceManager;
import org.syncope.console.commons.SearchConditionWrapper;
import org.syncope.console.commons.SearchConditionWrapper.FilterType;
import org.syncope.console.commons.SearchConditionWrapper.OperationType;
import org.syncope.console.commons.SortableDataProviderComparator;
import org.syncope.console.rest.RoleRestClient;
import org.syncope.console.rest.SchemaRestClient;
import org.syncope.console.rest.UserRestClient;
import org.syncope.console.wicket.ajax.markup.html.IndicatingDeleteOnConfirmAjaxLink;
import org.syncope.console.wicket.markup.html.form.DeleteLinkPanel;
import org.syncope.console.wicket.markup.html.form.EditLinkPanel;

public class Users extends BasePage {

    private final static int EDIT_MODAL_WIN_HEIGHT = 680;

    private final static int EDIT_MODAL_WIN_WIDTH = 1133;

    private final static int DISPLAYATTRS_MODAL_WIN_HEIGHT = 500;

    private final static int DISPLAYATTRS_MODAL_WIN_WIDTH = 600;

    @SpringBean
    private UserRestClient userRestClient;

    @SpringBean
    private SchemaRestClient schemaRestClient;

    @SpringBean
    private RoleRestClient roleRestClient;

    @SpringBean
    private PreferenceManager prefMan;

    final private ModalWindow editModalWin;

    final private ModalWindow displayAttrsModalWin;

    final private int paginatorRows = prefMan.getPaginatorRows(
            getWebRequestCycle().getWebRequest(),
            Constants.PREF_USERS_PAGINATOR_ROWS);

    final private int searchPaginatorRows = prefMan.getPaginatorRows(
            getWebRequestCycle().getWebRequest(),
            Constants.PREF_USERS_SEARCH_PAGINATOR_ROWS);

    private boolean modalResult = false;

    final private IModel<List<String>> schemaNames =
            new LoadableDetachableModel<List<String>>() {

                @Override
                protected List<String> load() {
                    return schemaRestClient.getAllUserSchemasNames();
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

    private NodeCond searchCond;

    public Users(final PageParameters parameters) {
        super(parameters);

        // Modal window for editing user attributes
        editModalWin = new ModalWindow("editModalWin");
        editModalWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editModalWin.setInitialHeight(EDIT_MODAL_WIN_HEIGHT);
        editModalWin.setInitialWidth(EDIT_MODAL_WIN_WIDTH);
        editModalWin.setPageMapName("user-edit-modal");
        editModalWin.setCookieName("user-edit-modal");
        add(editModalWin);

        // Modal window for choosing which attributes to display in tables
        displayAttrsModalWin = new ModalWindow("displayAttrsModalWin");
        displayAttrsModalWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        displayAttrsModalWin.setInitialHeight(DISPLAYATTRS_MODAL_WIN_HEIGHT);
        displayAttrsModalWin.setInitialWidth(DISPLAYATTRS_MODAL_WIN_WIDTH);
        displayAttrsModalWin.setPageMapName("user-displayAttrs-modal");
        displayAttrsModalWin.setCookieName("user-displayAttrs-modal");
        add(displayAttrsModalWin);

        // Container for user list
        final WebMarkupContainer listContainer =
                new WebMarkupContainer("listContainer");
        listContainer.setOutputMarkupId(true);
        setWindowClosedCallback(editModalWin, listContainer);
        setWindowClosedCallback(displayAttrsModalWin, listContainer);
        add(listContainer);

        // Container for user search result
        final WebMarkupContainer searchResultContainer =
                new WebMarkupContainer("searchResultContainer");
        searchResultContainer.setOutputMarkupId(true);
        setWindowClosedCallback(editModalWin, searchResultContainer);
        add(searchResultContainer);

        // columns to be displayed in tables
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
                                        return new UserModalPage(Users.this,
                                                editModalWin,
                                                model.getObject(), false);
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

        final AjaxFallbackDefaultDataTable<UserTO> listTable =
                new AjaxFallbackDefaultDataTable<UserTO>("listTable",
                columns, new UserDataProvider(), paginatorRows);
        listContainer.add(listTable);

        // create new user
        AjaxLink createLink = new IndicatingAjaxLink("createLink") {

            @Override
            public void onClick(final AjaxRequestTarget target) {

                editModalWin.setPageCreator(new ModalWindow.PageCreator() {

                    @Override
                    public Page createPage() {
                        return new UserModalPage(Users.this, editModalWin,
                                new UserTO(), true);
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
                                        Users.this, displayAttrsModalWin);
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
                prefMan.getPaginatorChoices());
        rowsChooser.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                prefMan.set(getWebRequestCycle().getWebRequest(),
                        getWebRequestCycle().getWebResponse(),
                        Constants.PREF_USERS_PAGINATOR_ROWS,
                        String.valueOf(paginatorRows));

                listTable.setRowsPerPage(paginatorRows);

                target.addComponent(listContainer);
            }
        });
        paginatorForm.add(rowsChooser);

        // search form
        Form searchForm = new Form("searchForm");
        add(searchForm);

        searchForm.add(new FeedbackPanel("searchFeedback").setOutputMarkupId(
                true));

        final WebMarkupContainer searchFormContainer =
                new WebMarkupContainer("searchFormContainer");
        searchFormContainer.setOutputMarkupId(true);
        setWindowClosedCallback(editModalWin, searchFormContainer);
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
        final AjaxFallbackDefaultDataTable<UserTO> searchResultTable =
                new AjaxFallbackDefaultDataTable<UserTO>("searchResultTable",
                columns, new UserSearchDataProvider(), searchPaginatorRows);
        searchResultTable.setOutputMarkupId(true);
        searchResultContainer.add(searchResultTable);
        searchForm.add(new IndicatingAjaxButton("search", new Model(
                getString("search"))) {

            @Override
            protected void onSubmit(final AjaxRequestTarget target,
                    final Form<?> form) {

                searchCond = buildSearchCond(searchConditionList);
                LOG.debug("Node condition " + searchCond);

                if (searchCond == null || !searchCond.checkValidity()) {
                    error(getString("search_error"));
                    return;
                }

                target.addComponent(searchResultTable);
            }

            @Override
            protected void onError(final AjaxRequestTarget target,
                    final Form form) {

                target.addComponent(form.get("searchFeedback"));
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

    private void setWindowClosedCallback(final ModalWindow window,
            final WebMarkupContainer container) {

        window.setWindowClosedCallback(
                new ModalWindow.WindowClosedCallback() {

                    @Override
                    public void onClose(final AjaxRequestTarget target) {
                        target.addComponent(container);
                        target.addComponent(feedbackPanel);

                        if (modalResult) {
                            info(getString("operation_succeded"));
                            modalResult = false;
                        }
                    }
                });
    }

    public void setModalResult(final boolean modalResult) {
        this.modalResult = modalResult;
    }

    private NodeCond buildSearchCond(
            final List<SearchConditionWrapper> conditions) {

        SearchConditionWrapper searchConditionWrapper =
                conditions.iterator().next();

        AttributeCond attributeCond = null;
        MembershipCond membershipCond = null;
        if (searchConditionWrapper.getFilterType() == FilterType.ATTRIBUTE) {
            attributeCond = new AttributeCond();
            attributeCond.setSchema(searchConditionWrapper.getFilterName());
            attributeCond.setType(searchConditionWrapper.getType());
            attributeCond.setExpression(
                    searchConditionWrapper.getFilterValue());
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
            List<SearchConditionWrapper> subList =
                    conditions.subList(1, conditions.size());

            searchConditionWrapper = subList.iterator().next();

            if (searchConditionWrapper.getOperationType()
                    == SearchConditionWrapper.OperationType.AND) {

                if (searchConditionWrapper.getFilterType()
                        == SearchConditionWrapper.FilterType.ATTRIBUTE) {

                    if (attributeCond != null) {
                        return NodeCond.getAndCond(
                                NodeCond.getLeafCond(attributeCond),
                                buildSearchCond(subList));
                    } else {
                        return NodeCond.getAndCond(
                                NodeCond.getLeafCond(membershipCond),
                                buildSearchCond(subList));
                    }
                } else {
                    if (attributeCond != null) {
                        return NodeCond.getAndCond(
                                NodeCond.getLeafCond(attributeCond),
                                buildSearchCond(subList));
                    } else {
                        return NodeCond.getAndCond(
                                NodeCond.getLeafCond(membershipCond),
                                buildSearchCond(subList));
                    }
                }
            } else {
                if (searchConditionWrapper.getFilterType()
                        == SearchConditionWrapper.FilterType.ATTRIBUTE) {
                    if (attributeCond != null) {
                        return NodeCond.getOrCond(
                                NodeCond.getLeafCond(attributeCond),
                                buildSearchCond(subList));
                    } else {
                        return NodeCond.getOrCond(
                                NodeCond.getLeafCond(membershipCond),
                                buildSearchCond(subList));
                    }
                } else {
                    if (attributeCond != null) {
                        return NodeCond.getOrCond(
                                NodeCond.getLeafCond(attributeCond),
                                buildSearchCond(subList));
                    } else {
                        return NodeCond.getOrCond(
                                NodeCond.getLeafCond(membershipCond),
                                buildSearchCond(subList));
                    }
                }
            }
        }
    }

    private class UserDataProvider extends SortableDataProvider<UserTO> {

        private SortableDataProviderComparator<UserTO> comparator;

        public UserDataProvider() {
            super();
            //Default sorting
            setSort("id", true);
            comparator = new SortableDataProviderComparator<UserTO>(getSort());
        }

        @Override
        public Iterator<UserTO> iterator(final int first, final int count) {
            List<UserTO> users = userRestClient.list(
                    (first / paginatorRows) + 1, count);
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

        private SortableDataProviderComparator<UserTO> comparator;

        public UserSearchDataProvider() {
            super();
            //Default sorting
            setSort("id", true);
            comparator = new SortableDataProviderComparator<UserTO>(getSort());
        }

        @Override
        public Iterator<UserTO> iterator(final int first, final int count) {
            List<UserTO> users;
            if (searchCond == null) {
                users = Collections.EMPTY_LIST;
            } else {
                users = userRestClient.search(searchCond,
                        (first / paginatorRows) + 1, count);
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

            super(displayModel);
            this.schemaName = schemaName;
        }

        @Override
        public void populateItem(
                final Item<ICellPopulator<UserTO>> cellItem,
                final String componentId,
                final IModel<UserTO> rowModel) {

            Label label;

            List<String> values =
                    rowModel.getObject().getAttributeMap().get(schemaName);
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

            if (searchCondition.getFilterType() == null) {
                filterNameChooser.setChoices(
                        Collections.EMPTY_LIST);
            } else if (searchCondition.getFilterType()
                    == FilterType.ATTRIBUTE) {

                filterNameChooser.setChoices(schemaNames);
            } else {
                filterNameChooser.setChoices(roleNames);
            }

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

            if (searchCondition.getFilterType()
                    == FilterType.MEMBERSHIP) {

                type.setEnabled(false);
                type.setRequired(false);
                type.setModelObject(null);

                filterValue.setEnabled(false);
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

                            filterNameChooser.setChoices(searchCondition.
                                    getFilterType() == FilterType.ATTRIBUTE
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
