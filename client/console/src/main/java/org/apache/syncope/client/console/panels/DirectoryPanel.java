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

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import org.apache.syncope.client.console.PreferenceManager;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.RestClient;
import org.apache.syncope.client.console.wicket.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
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

public abstract class DirectoryPanel<
        T extends Serializable, W extends Serializable, DP extends DirectoryDataProvider<T>, E extends RestClient>
        extends WizardMgtPanel<W> {

    private static final long serialVersionUID = -9170191461250434024L;

    protected static final Logger LOG = LoggerFactory.getLogger(DirectoryPanel.class);

    /**
     * Application preferences.
     */
    protected PreferenceManager prefMan = new PreferenceManager();

    protected E restClient;

    /**
     * Number of rows per page.
     */
    protected int rows;

    /**
     * Container used to refresh table.
     */
    protected final WebMarkupContainer container;

    /**
     * Specify if results are about a filtered search or not. Using this attribute it is possible to use this panel to
     * show results about user list and user search.
     */
    protected final boolean filtered;

    private boolean checkBoxEnabled;

    private boolean showPaginator;

    /**
     * Result table.
     */
    private AjaxDataTablePanel<T, String> resultTable;

    /**
     * Data provider used to search for users.
     */
    protected DP dataProvider;

    /**
     * Owner page.
     */
    protected final BasePage page;

    protected String itemKeyFieldName = "key";

    protected final BaseModal<W> altDefaultModal = new BaseModal<>("outer");

    protected final BaseModal<W> displayAttributeModal = new BaseModal<>("outer");

    /**
     * Create simple unfiltered search result panel.
     * Use the available builder for powerfull configuration options.
     *
     * @param id panel id.
     * @param pageRef page reference.
     */
    public DirectoryPanel(final String id, final PageReference pageRef) {
        this(id, pageRef, true);
    }

    public DirectoryPanel(final String id, final PageReference pageRef, final boolean wizardInModal) {
        this(id, new Builder<T, W, E>(null, pageRef) {

            private static final long serialVersionUID = -8424727765826509309L;

            @Override
            protected WizardMgtPanel<W> newInstance(final String id, final boolean wizardInModal) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        }.setFiltered(false), wizardInModal);
        setPageRef(pageRef);
    }

    protected DirectoryPanel(final String id, final Builder<T, W, E> builder) {
        this(id, builder, true);
    }

    protected DirectoryPanel(final String id, final Builder<T, W, E> builder, final boolean wizardInModal) {
        super(id, wizardInModal);
        setOutputMarkupId(true);

        addOuterObject(altDefaultModal);
        addOuterObject(displayAttributeModal);

        setPageRef(builder.getPageRef());
        this.page = (BasePage) builder.getPageRef().getPage();

        this.filtered = builder.filtered;
        this.checkBoxEnabled = builder.checkBoxEnabled;
        this.showPaginator = builder.showPaginator;

        this.restClient = builder.restClient;

        // Container for user search result
        container = new WebMarkupContainer("searchContainer");
        container.setOutputMarkupId(true);
        addInnerObject(container);

        rows = prefMan.getPaginatorRows(getRequest(), paginatorRowsKey());

        setWindowClosedReloadCallback(modal);
        setWindowClosedReloadCallback(altDefaultModal);
        setWindowClosedReloadCallback(displayAttributeModal);

        displayAttributeModal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                final EventDataWrapper data = new EventDataWrapper();
                data.setTarget(target);
                data.setRows(rows);

                send(DirectoryPanel.this, Broadcast.EXACT, data);

                modal.show(false);
            }
        });

        altDefaultModal.size(Modal.Size.Medium);
        displayAttributeModal.size(Modal.Size.Medium);
        displayAttributeModal.addSubmitButton();
    }

    protected abstract DP dataProvider();

    protected abstract String paginatorRowsKey();

    protected abstract List<IColumn<T, String>> getColumns();

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
        paginatorForm.setOutputMarkupPlaceholderTag(true);
        paginatorForm.setVisible(showPaginator);
        container.add(paginatorForm);

        final DropDownChoice<Integer> rowsChooser = new DropDownChoice<>(
                "rowsChooser", new PropertyModel<Integer>(this, "rows"), prefMan.getPaginatorChoices());

        rowsChooser.add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                prefMan.set(getRequest(), getResponse(), paginatorRowsKey(), String.valueOf(rows));

                final EventDataWrapper data = new EventDataWrapper();
                data.setTarget(target);
                data.setRows(rows);

                send(getParent(), Broadcast.BREADTH, data);
            }
        });
        paginatorForm.add(rowsChooser);
        // ---------------------------
    }

    public void search(final AjaxRequestTarget target) {
        target.add(container);
    }

    public void updateResultTable(final AjaxRequestTarget target) {
        updateResultTable(false);
        if (DirectoryPanel.this.container.isVisibleInHierarchy()) {
            target.add(DirectoryPanel.this.container);
        }
    }

    private void updateResultTable(final boolean create) {
        updateResultTable(create, rows);
    }

    private void updateResultTable(final boolean create, final int rows) {
        dataProvider = dataProvider();

        final int currentPage = resultTable != null
                ? (create ? (int) resultTable.getPageCount() - 1 : (int) resultTable.getCurrentPage()) : 0;

        // take care of restClient handle: maybe not useful to keep into
        AjaxDataTablePanel.Builder<T, String> resultTableBuilder = new AjaxDataTablePanel.Builder<>(
                dataProvider, page.getPageReference()).
                setColumns(getColumns()).
                setRowsPerPage(rows).
                setBulkActions(getBulkActions(), restClient, itemKeyFieldName).
                setContainer(container);

        if (!checkBoxEnabled) {
            resultTableBuilder.disableCheckBoxes();
        }

        resultTableCustomChanges(resultTableBuilder);
        resultTable = resultTableBuilder.build("resultTable");

        resultTable.setCurrentPage(currentPage);
        resultTable.setOutputMarkupId(true);
        container.addOrReplace(resultTable);
    }

    /**
     * Called before build. Override it to customize result table.
     *
     * @param resultTableBuilder result table builder.
     */
    protected void resultTableCustomChanges(final AjaxDataTablePanel.Builder<T, String> resultTableBuilder) {
    }

    public DirectoryPanel<T, W, DP, E> disableCheckBoxes() {
        this.checkBoxEnabled = false;
        return this;
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof EventDataWrapper) {
            final EventDataWrapper data = (EventDataWrapper) event.getPayload();

            if (data.getRows() < 1) {
                updateResultTable(data.isCreate());
            } else {
                updateResultTable(data.isCreate(), data.getRows());
            }

            if (DirectoryPanel.this.container.isVisibleInHierarchy()) {
                data.getTarget().add(DirectoryPanel.this.container);
            }
        }
        super.onEvent(event);
    }

    @Override
    protected void customActionOnFinishCallback(final AjaxRequestTarget target) {
        final EventDataWrapper data = new EventDataWrapper();
        data.setTarget(target);
        data.setRows(rows);
        send(getParent(), Broadcast.BREADTH, data);
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

    protected abstract Collection<ActionLink.ActionType> getBulkActions();

    public abstract static class Builder<T extends Serializable, W extends Serializable, E extends RestClient>
            extends WizardMgtPanel.Builder<W> {

        private static final long serialVersionUID = 5088962796986706805L;

        /**
         * Specify if results are about a filtered search or not.
         * By using this attribute it is possible to force this panel to show results about user list and user search.
         */
        protected boolean filtered = false;

        protected boolean checkBoxEnabled = true;

        protected boolean showPaginator = true;

        /**
         * Filter used in case of filtered search.
         */
        protected String fiql;

        protected final E restClient;

        protected Builder(final E restClient, final PageReference pageRef) {
            super(pageRef);
            this.restClient = restClient;
        }

        public Builder<T, W, E> setFiltered(final boolean filtered) {
            this.filtered = filtered;
            return this;
        }

        public Builder<T, W, E> disableCheckBoxes() {
            this.checkBoxEnabled = false;
            return this;
        }

        public Builder<T, W, E> hidePaginator() {
            this.showPaginator = false;
            return this;
        }

        public Builder<T, W, E> setFiql(final String fiql) {
            this.fiql = fiql;
            return this;
        }

        private PageReference getPageRef() {
            return this.pageRef;
        }
    }
}
