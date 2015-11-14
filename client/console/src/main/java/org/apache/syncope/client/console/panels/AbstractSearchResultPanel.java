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
package org.apache.syncope.client.console.panels;

import java.util.Collection;
import java.util.List;
import org.apache.syncope.client.console.PreferenceManager;
import org.apache.syncope.client.console.commons.AnyDataProvider;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.AbstractBasePage;
import org.apache.syncope.client.console.rest.AbstractAnyRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSearchResultPanel<T extends AnyTO> extends WizardMgtPanel<T> {

    private static final long serialVersionUID = -9170191461250434024L;

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractSearchResultPanel.class);

    /**
     * Application preferences.
     */
    protected PreferenceManager prefMan = new PreferenceManager();

    protected final AbstractAnyRestClient<T> restClient;

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
    protected final NotificationPanel feedbackPanel;

    /**
     * Specify if results are about a filtered search or not. Using this attribute it is possible to use this panel to
     * show results about user list and user search.
     */
    private final boolean filtered;

    /**
     * Filter used in case of filtered search.
     */
    private String fiql;

    /**
     * Result table.
     */
    private AjaxDataTablePanel<T, String> resultTable;

    /**
     * Data provider used to search for users.
     */
    private AnyDataProvider<T> dataProvider;

    /**
     * Owner page.
     */
    protected final AbstractBasePage page;

    /**
     * Realm related to current panel.
     */
    private final String realm;

    /**
     * Any type related to current panel.
     */
    private final String type;

    protected AbstractSearchResultPanel(
            final String id,
            final boolean filtered,
            final String fiql,
            final PageReference pageRef,
            final AbstractAnyRestClient<T> restClient,
            final String realm,
            final String type) {

        super(id, pageRef, true);

        setOutputMarkupId(true);

        this.page = (AbstractBasePage) pageRef.getPage();

        this.filtered = filtered;
        this.fiql = fiql;
        this.feedbackPanel = page.getFeedbackPanel();

        this.restClient = restClient;

        // Container for user search result
        container = new WebMarkupContainer("searchContainer");
        container.setOutputMarkupId(true);
        add(container);

        rows = prefMan.getPaginatorRows(getRequest(), Constants.PREF_USERS_PAGINATOR_ROWS);

        this.realm = realm;
        this.type = type;

        setWindowClosedReloadCallback(modal);
    }

    protected void initResultTable() {
        // ---------------------------
        // Result table initialization
        // ---------------------------
        updateResultTable(false);
        // ---------------------------

        // ---------------------------
        // Rows-per-page selector
        // ---------------------------
        final Form<?> paginatorForm = new Form<>("paginator");
        container.add(paginatorForm);

        final DropDownChoice<Integer> rowsChooser = new DropDownChoice<>(
                "rowsChooser", new PropertyModel<Integer>(this, "rows"), prefMan.getPaginatorChoices());

        rowsChooser.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

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
    }

    public void search(final String fiql, final AjaxRequestTarget target) {
        this.fiql = fiql;
        dataProvider.setFIQL(fiql);
        target.add(container);
    }

    private void updateResultTable(final boolean create) {
        updateResultTable(create, rows);
    }

    private void updateResultTable(final boolean create, final int rows) {
        dataProvider = new AnyDataProvider<>(restClient, rows, filtered, realm, type);
        dataProvider.setFIQL(fiql);

        final int currentPage = resultTable != null
                ? (create
                        ? (int) resultTable.getPageCount() - 1
                        : (int) resultTable.getCurrentPage())
                : 0;

        resultTable = new AjaxDataTablePanel<>(
                "resultTable",
                getColumns(),
                dataProvider,
                rows,
                getBulkActions(),
                restClient,
                "key",
                getPageId(),
                page.getPageReference(),
                container);

        resultTable.setCurrentPage(currentPage);

        resultTable.setOutputMarkupId(true);

        container.addOrReplace(resultTable);
    }

    protected abstract List<IColumn<T, String>> getColumns();

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
        super.onEvent(event);
    }

    private void setWindowClosedReloadCallback(final BaseModal<?> modal) {
        modal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                modal.show(false);

                final EventDataWrapper data = new EventDataWrapper();
                data.setTarget(target);
                data.setRows(rows);

                send(getParent(), Broadcast.BREADTH, data);

                if (page.isModalResult()) {
                    // reset modal result
                    page.setModalResult(false);
                    // set operation succeeded
                    getSession().info(getString(Constants.OPERATION_SUCCEEDED));
                    // refresh feedback panel
                    feedbackPanel.refresh(target);
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

        public void setCreate(final boolean create) {
            this.create = create;
        }

        public int getRows() {
            return rows;
        }

        public void setRows(final int rows) {
            this.rows = rows;
        }
    }

    protected abstract <T extends AnyTO> Collection<ActionLink.ActionType> getBulkActions();

    protected abstract String getPageId();

    public abstract static class Builder<T extends AnyTO> extends WizardMgtPanel.Builder<T> {

        private static final long serialVersionUID = 5088962796986706805L;

        /**
         * Specify if results are about a filtered search or not. Using this attribute it is possible to use this panel
         * to
         * show results about user list and user search.
         */
        protected final boolean filtered;

        /**
         * Filter used in case of filtered search.
         */
        protected final String fiql;

        protected final AbstractAnyRestClient<T> restClient;

        /**
         * Realm related to current panel.
         */
        protected final String realm;

        /**
         * Any type related to current panel.
         */
        protected final String type;

        protected Builder(
                final Class<T> reference,
                final boolean filtered,
                final String fiql,
                final PageReference pageRef,
                final AbstractAnyRestClient<T> restClient,
                final String realm,
                final String type) {

            super(reference, pageRef);
            this.filtered = filtered;
            this.fiql = fiql;
            this.restClient = restClient;
            this.realm = realm;
            this.type = type;
        }

    }
}
