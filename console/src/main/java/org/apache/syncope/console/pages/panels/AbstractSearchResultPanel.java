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

import java.util.List;
import org.apache.syncope.common.search.NodeCond;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.console.commons.AttributableDataProvider;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.commons.PreferenceManager;
import org.apache.syncope.console.commons.XMLRolesReader;
import org.apache.syncope.console.pages.AbstractBasePage;
import org.apache.syncope.console.pages.DisplayAttributesModalPage;
import org.apache.syncope.console.rest.AbstractAttributableRestClient;
import org.apache.syncope.console.rest.UserRestClient;
import org.apache.syncope.console.wicket.ajax.markup.html.ClearIndicatingAjaxLink;
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
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSearchResultPanel extends Panel implements IEventSource {

    private static final long serialVersionUID = -9170191461250434024L;

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractSearchResultPanel.class);

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
    private final static int DISPLAYATTRS_MODAL_WIN_HEIGHT = 550;

    /**
     * Schemas to be shown modal window width.
     */
    private final static int DISPLAYATTRS_MODAL_WIN_WIDTH = 550;

    /**
     * Schemas to be shown modal window height.
     */
    private final static int STATUS_MODAL_WIN_HEIGHT = 500;

    /**
     * Schemas to be shown modal window width.
     */
    private final static int STATUS_MODAL_WIN_WIDTH = 500;

    /**
     * Application preferences.
     */
    @SpringBean
    protected PreferenceManager prefMan;

    /**
     * Role reader for authorizations management.
     */
    @SpringBean
    protected XMLRolesReader xmlRolesReader;

    protected final AbstractAttributableRestClient restClient;

    /**
     * Number of rows per page.
     */
    private final int rows;

    /**
     * Container used to refresh table.
     */
    protected final WebMarkupContainer container;

    /**
     * Feedback panel specified by the caller.
     */
    protected final FeedbackPanel feedbackPanel;

    /**
     * Specify if results are about a filtered search or not. Using this attribute it is possible to use this panel to
     * show results about user list and user search.
     */
    private final boolean filtered;

    /**
     * Filter used in case of filtered search.
     */
    private NodeCond filter;

    /**
     * Result table.
     */
    private AjaxFallbackDefaultDataTable<AbstractAttributableTO, String> resultTable;

    /**
     * Data provider used to search for users.
     */
    private AttributableDataProvider dataProvider;

    /**
     * Modal window to be used for user profile editing. Global visibility is required ...
     */
    protected final ModalWindow editmodal = new ModalWindow("editModal");

    /**
     * Modal window to be used for attributes choosing to display in tables.
     */
    protected final ModalWindow displaymodal = new ModalWindow("displayModal");

    /**
     * Modal window to be used for user status management.
     */
    protected final ModalWindow statusmodal = new ModalWindow("statusModal");

    /**
     * Owner page.
     */
    protected final AbstractBasePage page;

    protected <T extends AbstractAttributableTO> AbstractSearchResultPanel(final String id, final boolean filtered,
            final NodeCond searchCond, final PageReference pageRef, final AbstractAttributableRestClient restClient) {

        super(id);

        setOutputMarkupId(true);

        this.page = (AbstractBasePage) pageRef.getPage();

        this.filtered = filtered;
        this.filter = searchCond;
        this.feedbackPanel = page.getFeedbackPanel();

        this.restClient = restClient;

        editmodal.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editmodal.setInitialHeight(EDIT_MODAL_WIN_HEIGHT);
        editmodal.setInitialWidth(EDIT_MODAL_WIN_WIDTH);
        editmodal.setCookieName("edit-modal");
        add(editmodal);

        displaymodal.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        displaymodal.setInitialHeight(DISPLAYATTRS_MODAL_WIN_HEIGHT);
        displaymodal.setInitialWidth(DISPLAYATTRS_MODAL_WIN_WIDTH);
        displaymodal.setCookieName("display-modal");
        add(displaymodal);

        statusmodal.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        statusmodal.setInitialHeight(STATUS_MODAL_WIN_HEIGHT);
        statusmodal.setInitialWidth(STATUS_MODAL_WIN_WIDTH);
        statusmodal.setCookieName("status-modal");
        add(statusmodal);

        // Container for user search result
        container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        // ---------------------------
        // Result table initialization
        // ---------------------------
        // preferences and container must be not null to use it ...
        rows = prefMan.getPaginatorRows(getRequest(), Constants.PREF_USERS_PAGINATOR_ROWS);
        updateResultTable(false);
        // ---------------------------

        final AjaxLink displayAttrsLink;
        if (restClient instanceof UserRestClient) {
            // ---------------------------
            // Link to select schemas/columns to be shown (User)
            // ---------------------------
            displayAttrsLink = new ClearIndicatingAjaxLink("displayAttrsLink", pageRef) {

                private static final long serialVersionUID = -7978723352517770644L;

                @Override
                protected void onClickInternal(final AjaxRequestTarget target) {
                    displaymodal.setPageCreator(new ModalWindow.PageCreator() {

                        private static final long serialVersionUID = -7834632442532690940L;

                        @Override
                        public Page createPage() {
                            return new DisplayAttributesModalPage(page.getPageReference(), displaymodal);
                        }
                    });

                    displaymodal.show(target);
                }
            };

            // Add class to specify relative position of the link.
            // Position depends on result pages number.
            displayAttrsLink.add(new Behavior() {

                private static final long serialVersionUID = 1469628524240283489L;

                @Override
                public void onComponentTag(final Component component, final ComponentTag tag) {
                    if (resultTable.getRowCount() > rows) {
                        tag.remove("class");
                        tag.put("class", "settingsPosMultiPage");
                    } else {
                        tag.remove("class");
                        tag.put("class", "settingsPos");
                    }
                }
            });

            MetaDataRoleAuthorizationStrategy.authorize(
                    displayAttrsLink, ENABLE, xmlRolesReader.getAllAllowedRoles("Users", "changeView"));
        } else {
            displayAttrsLink = new AjaxLink("displayAttrsLink") {

                private static final long serialVersionUID = -7978723352517770644L;

                @Override
                public void onClick(final AjaxRequestTarget target) {
                }
            };
            displayAttrsLink.setVisible(false);
        }
        container.add(displayAttrsLink);

        final AjaxLink reload = new ClearIndicatingAjaxLink("reload", pageRef) {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            protected void onClickInternal(final AjaxRequestTarget target) {
                if (target != null) {
                    target.add(resultTable);
                }
            }
        };

        reload.add(new Behavior() {

            private static final long serialVersionUID = 1469628524240283489L;

            @Override
            public void onComponentTag(final Component component, final ComponentTag tag) {

                if (resultTable.getRowCount() > rows) {
                    tag.remove("class");
                    tag.put("class", "settingsPosMultiPage");
                } else {
                    tag.remove("class");
                    tag.put("class", "settingsPos");
                }
            }
        });

        container.add(reload);

        // ---------------------------

        // ---------------------------
        // Rows-per-page selector
        // ---------------------------
        final Form paginatorForm = new Form("paginator");
        container.add(paginatorForm);

        final DropDownChoice<Integer> rowsChooser = new DropDownChoice<Integer>(
                "rowsChooser", new PropertyModel<Integer>(this, "rows"), prefMan.getPaginatorChoices());

        rowsChooser.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                prefMan.set(getRequest(), getResponse(), Constants.PREF_USERS_PAGINATOR_ROWS, String.valueOf(rows));

                final EventDataWrapper data = new EventDataWrapper();
                data.setTarget(target);
                data.setRows(rows);

                send(getParent(), Broadcast.BREADTH, data);
            }
        });
        paginatorForm.add(rowsChooser);
        // ---------------------------

        setWindowClosedReloadCallback(statusmodal);
        setWindowClosedReloadCallback(editmodal);
        setWindowClosedReloadCallback(displaymodal);
    }

    public void search(final NodeCond searchCond, final AjaxRequestTarget target) {
        this.filter = searchCond;
        dataProvider.setSearchCond(filter);
        target.add(container);
    }

    private void updateResultTable(final boolean create) {
        updateResultTable(create, rows);
    }

    private void updateResultTable(final boolean create, final int rows) {
        dataProvider = new AttributableDataProvider(restClient, rows, filtered);
        dataProvider.setSearchCond(filter);

        final int currentPage = resultTable != null
                ? (create
                ? (int) resultTable.getPageCount() - 1
                : (int) resultTable.getCurrentPage())
                : 0;

        resultTable = new AjaxFallbackDefaultDataTable<AbstractAttributableTO, String>(
                "resultTable", getColumns(), dataProvider, rows);

        resultTable.setCurrentPage(currentPage);

        resultTable.setOutputMarkupId(true);

        container.addOrReplace(resultTable);
    }

    protected abstract List<IColumn<AbstractAttributableTO, String>> getColumns();

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof EventDataWrapper) {
            final EventDataWrapper data = (EventDataWrapper) event.getPayload();

            if (data.getRows() < 1) {
                updateResultTable(data.isCreate());
            } else {
                updateResultTable(data.isCreate(), data.getRows());
            }

            data.getTarget().add(container);
        }
    }

    private void setWindowClosedReloadCallback(final ModalWindow window) {
        window.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                final EventDataWrapper data = new EventDataWrapper();
                data.setTarget(target);
                data.setRows(rows);

                send(getParent(), Broadcast.BREADTH, data);

                if (page.isModalResult()) {
                    // reset modal result
                    page.setModalResult(false);
                    // set operation succeeded
                    getSession().info(getString("operation_succeeded"));
                    // refresh feedback panel
                    target.add(feedbackPanel);
                }
            }
        });
    }

    public static class EventDataWrapper {

        private AjaxRequestTarget target;

        private boolean create;

        private int rows;

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

        public int getRows() {
            return rows;
        }

        public void setRows(int rows) {
            this.rows = rows;
        }
    }
}
