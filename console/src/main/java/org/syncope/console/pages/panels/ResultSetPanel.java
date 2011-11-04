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
package org.syncope.console.pages.panels;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.event.IEventSource;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.syncope.client.search.NodeCond;
import org.syncope.client.to.AbstractAttributableTO;
import org.syncope.client.to.UserTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.console.commons.Constants;
import org.syncope.console.commons.PreferenceManager;
import org.syncope.console.commons.UserDataProvider;
import org.syncope.console.commons.XMLRolesReader;
import org.syncope.console.pages.DisplayAttributesModalPage;
import org.syncope.console.pages.UserModalPage;
import org.syncope.console.pages.Users;
import org.syncope.console.rest.SchemaRestClient;
import org.syncope.console.rest.UserRestClient;
import org.syncope.console.wicket.ajax.markup.html.IndicatingDeleteOnConfirmAjaxLink;
import org.syncope.console.wicket.markup.html.form.DeleteLinkPanel;
import org.syncope.console.wicket.markup.html.form.EditLinkPanel;

public class ResultSetPanel extends Panel implements IEventSource {

    /**
     * Logger.
     */
    protected static final Logger LOG =
            LoggerFactory.getLogger(ResultSetPanel.class);

    /**
     * Edit modal window height.
     */
    private final static int EDIT_MODAL_WIN_HEIGHT = 550;

    /**
     * Edit modal window width.
     */
    private final static int EDIT_MODAL_WIN_WIDTH = 800;

    /**
     * Schemas to be shown modal window height.
     */
    private final static int DISPLAYATTRS_MODAL_WIN_HEIGHT = 350;

    /**
     * Schemas to be shown modal window width.
     */
    private final static int DISPLAYATTRS_MODAL_WIN_WIDTH = 550;

    /**
     * Prefix used to dintinguish between derived and not derived schemas.
     */
    private final static String DERIVED_ATTRIBUTE_PREFIX = "[D] ";

    /**
     * Prefix used to dintinguish between virtual and not virtual schemas.
     */
    private final static String VIRTUAL_ATTRIBUTE_PREFIX = "[V] ";

    /**
     * User rest client.
     */
    @SpringBean
    private UserRestClient userRestClient;

    /**
     * Schema rest client.
     */
    @SpringBean
    private SchemaRestClient schemaRestClient;

    /**
     * Application preferences.
     */
    @SpringBean
    private PreferenceManager preferences;

    /**
     * Role reader for authorizations management.
     */
    @SpringBean
    protected XMLRolesReader xmlRolesReader;

    /**
     * Number of rows per page.
     */
    private int rows;

    /**
     * Container used to refresh table.
     */
    final protected WebMarkupContainer container;

    /**
     * Feedback panel specified by the caller.
     */
    final private FeedbackPanel feedbackPanel;

    /**
     * Specify if results are about a filtered search or not.
     * Using this attribute it is possible to use this panel to show results
     * about user list and user search.
     */
    private boolean filtered;

    /**
     * Filter used in case of filtered search.
     */
    private NodeCond filter;

    /**
     * Result table.
     */
    private AjaxFallbackDefaultDataTable<UserTO> resultTable;

    /**
     * Data provider used to search for users.
     */
    private UserDataProvider dataProvider;

    /**
     * Modal window used for user profile editing.
     * Global visibility is required ...
     */
    private final ModalWindow editmodal = new ModalWindow("editModal");

    /**
     * Owner page.
     */
    private final Users page;

    final private IModel<List<String>> choosableSchemaNames =
            new LoadableDetachableModel<List<String>>() {

                private static final long serialVersionUID =
                        5275935387613157437L;

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
                            schemas.add(
                                    DERIVED_ATTRIBUTE_PREFIX + schema);
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

    public <T extends AbstractAttributableTO> ResultSetPanel(
            final String id,
            final boolean filtered,
            final NodeCond searchCond,
            final PageReference callerRef) {
        super(id);

        setOutputMarkupId(true);

        page = (Users) callerRef.getPage();

        this.filtered = filtered;
        this.filter = searchCond;
        this.feedbackPanel = page.getFeedbackPanel();

        editmodal.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editmodal.setInitialHeight(EDIT_MODAL_WIN_HEIGHT);
        editmodal.setInitialWidth(EDIT_MODAL_WIN_WIDTH);
        editmodal.setCookieName("edit-modal");
        add(editmodal);

        // Modal window for choosing which attributes to display in tables
        final ModalWindow displaymodal = new ModalWindow("displayModal");
        displaymodal.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        displaymodal.setInitialHeight(DISPLAYATTRS_MODAL_WIN_HEIGHT);
        displaymodal.setInitialWidth(DISPLAYATTRS_MODAL_WIN_WIDTH);
        displaymodal.setCookieName("display-modal");
        add(displaymodal);

        // Container for user search result
        container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        // ---------------------------
        // Result table initialization
        // ---------------------------
        // preferences and container must be not null to use it ...
        updateResultTable(false);
        // ---------------------------

        // ---------------------------
        // Link to select schemas/columns to be shown
        // ---------------------------
        AjaxLink displayAttrsLink = new IndicatingAjaxLink("displayAttrsLink") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {

                displaymodal.setPageCreator(
                        new ModalWindow.PageCreator() {

                            private static final long serialVersionUID =
                                    -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                return new DisplayAttributesModalPage(
                                        page.getPageReference(),
                                        choosableSchemaNames,
                                        displaymodal);
                            }
                        });

                displaymodal.show(target);
            }
        };

        // Add class to specify relative position of the link.
        // Position depends on result pages number.
        displayAttrsLink.add(new Behavior() {

            @Override
            public void onComponentTag(
                    final Component component, final ComponentTag tag) {

                if (resultTable.getRowCount() > rows) {
                    tag.remove("class");
                    tag.put("class", "settingsPosMultiPage");
                } else {
                    tag.remove("class");
                    tag.put("class", "settingsPos");
                }
            }
        });

        MetaDataRoleAuthorizationStrategy.authorize(displayAttrsLink, ENABLE,
                xmlRolesReader.getAllAllowedRoles("Users", "changeView"));

        container.add(displayAttrsLink);
        // ---------------------------

        // ---------------------------
        // Rows-per-page selector
        // ---------------------------
        final Form paginatorForm = new Form("paginator");
        container.add(paginatorForm);

        final DropDownChoice<Integer> rowsChooser =
                new DropDownChoice<Integer>("rowsChooser",
                new PropertyModel(this, "rows"),
                preferences.getPaginatorChoices());

        rowsChooser.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                preferences.set(
                        getRequest(),
                        getResponse(),
                        Constants.PREF_USERS_PAGINATOR_ROWS,
                        String.valueOf(rows));

                final EventDataWrapper data = new EventDataWrapper();
//                data.setTable(resultTable);
                data.setTarget(target);

                send(getParent(), Broadcast.BREADTH, data);
            }
        });
        paginatorForm.add(rowsChooser);
        // ---------------------------

        setWindowClosedReloadCallback(editmodal);
        setWindowClosedReloadCallback(displaymodal);
    }

    public void search(
            final NodeCond searchCond, final AjaxRequestTarget target) {
        this.filter = searchCond;
        dataProvider.setSearchCond(filter);
        target.add(container);
    }

    private void updateResultTable(final boolean create) {
        // Requires preferences/container attributes not null ...

        rows = preferences.getPaginatorRows(
                getRequest(), Constants.PREF_USERS_PAGINATOR_ROWS);

        dataProvider = new UserDataProvider(userRestClient, rows, filtered);
        dataProvider.setSearchCond(filter);

        final int page = resultTable != null
                ? (create
                ? resultTable.getPageCount() - 1
                : resultTable.getCurrentPage())
                : 0;

        resultTable = new AjaxFallbackDefaultDataTable<UserTO>(
                "resultTable", getColumns(), dataProvider, rows);

        resultTable.setCurrentPage(page);

        resultTable.setOutputMarkupId(true);

        container.addOrReplace(resultTable);
    }

    private List<IColumn<UserTO>> getColumns() {
        List<IColumn<UserTO>> columns = new ArrayList<IColumn<UserTO>>();
        columns.add(new PropertyColumn(
                new ResourceModel("id"), "id", "id"));
        columns.add(new PropertyColumn(
                new ResourceModel("username"), "username", "username"));
        columns.add(new PropertyColumn(
                new ResourceModel("status"), "status", "status"));
        columns.add(new TokenColumn(new ResourceModel("token"), "token"));

        for (String schemaName : preferences.getList(getRequest(),
                Constants.PREF_USERS_ATTRIBUTES_VIEW)) {

            columns.add(new UserAttrColumn(
                    new Model<String>(schemaName), schemaName));
        }

        columns.add(new AbstractColumn<UserTO>(new ResourceModel("edit")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<UserTO>> cellItem,
                    final String componentId,
                    final IModel<UserTO> model) {

                Panel panel = new EditLinkPanel(componentId, model);
                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                        xmlRolesReader.getAllAllowedRoles("Users", "read"));

                panel.add(new IndicatingAjaxLink("editLink") {

                    private static final long serialVersionUID =
                            -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        editmodal.setPageCreator(
                                new ModalWindow.PageCreator() {

                                    private static final long serialVersionUID =
                                            -7834632442532690940L;

                                    @Override
                                    public Page createPage() {
                                        return new UserModalPage(
                                                page.getPageReference(),
                                                editmodal,
                                                model.getObject());
                                    }
                                });

                        editmodal.show(target);
                    }
                });
                cellItem.add(panel);
            }
        });
        columns.add(new AbstractColumn<UserTO>(new ResourceModel("delete")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<UserTO>> cellItem,
                    final String componentId,
                    final IModel<UserTO> model) {

                Panel panel = new DeleteLinkPanel(componentId, model);
                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                        xmlRolesReader.getAllAllowedRoles("Users", "delete"));

                panel.add(new IndicatingDeleteOnConfirmAjaxLink("deleteLink") {

                    private static final long serialVersionUID =
                            -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {
                            userRestClient.delete(model.getObject().getId());
                            info(getString("operation_succeded"));
                        } catch (SyncopeClientCompositeErrorException scce) {
                            error(scce.getMessage());
                        }
                        target.add(feedbackPanel);
                        target.add(container);
                    }
                });
                cellItem.add(panel);
            }
        });

        return columns;
    }

    private class TokenColumn extends AbstractColumn<UserTO> {

        private static final long serialVersionUID = 8077865338230121496L;

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

        private static final long serialVersionUID = 2624734332447371372L;

        private final String schemaName;

        public UserAttrColumn(
                final IModel<String> displayModel, final String schemaName) {

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

    @Override
    public void onEvent(IEvent<?> event) {
        if (event.getPayload() instanceof EventDataWrapper) {

            final EventDataWrapper data = (EventDataWrapper) event.getPayload();
            final AjaxRequestTarget target = data.getTarget();

            updateResultTable(data.isCreate());

            target.add(container);
        }
    }

    private void setWindowClosedReloadCallback(final ModalWindow window) {

        window.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                final EventDataWrapper data = new EventDataWrapper();
                data.setTarget(target);

                send(getParent(), Broadcast.BREADTH, data);

                if (page.isModalResult()) {
                    // reset modal result
                    page.setModalResult(false);
                    // set operation succeded
                    getSession().info(getString("operation_succeded"));
                    // refresh feedback panel
                    target.add(feedbackPanel);
                }
            }
        });
    }

    public static class EventDataWrapper {

        private AjaxRequestTarget target;

        private boolean create;

        public AjaxRequestTarget getTarget() {
            return target;
        }

        public void setTarget(final AjaxRequestTarget target) {
            this.target = target;
        }

        public boolean isCreate() {
            return create;
        }

        public void setCreate(boolean create) {
            this.create = create;
        }
    }
}
