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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.syncope.client.console.rest.BaseRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.ajax.form.IndicatorAjaxFormChoiceComponentUpdatingBehavior;
import org.apache.syncope.client.console.bulk.BulkActionModal;
import org.apache.syncope.client.console.bulk.BulkContent;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.DirectoryPanel.EventDataWrapper;
import org.apache.syncope.client.console.rest.RestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.CheckGroupColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.AjaxFallbackDataTable;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.ResourceModel;

public final class AjaxDataTablePanel<T extends Serializable, S> extends DataTablePanel<T, S> {

    private static final long serialVersionUID = -7264400471578272966L;

    public static class Builder<T extends Serializable, S> implements Serializable {

        private static final long serialVersionUID = 8876232177473972722L;

        private boolean checkBoxEnabled = true;

        private final List<IColumn<T, S>> columns = new ArrayList<>();

        private final ISortableDataProvider<T, S> dataProvider;

        private int rowsPerPage = 10;

        private final Collection<ActionLink.ActionType> bulkActions = new ArrayList<>();

        private RestClient bulkActionExecutor;

        private String itemKeyField;

        private final PageReference pageRef;

        private WebMarkupContainer container;

        private MultilevelPanel multiLevelPanel;

        private BaseModal<?> baseModal;

        public Builder(final ISortableDataProvider<T, S> provider, final PageReference pageRef) {
            this.dataProvider = provider;
            this.pageRef = pageRef;
        }

        public AjaxDataTablePanel<T, S> build(final String id) {
            return new AjaxDataTablePanel<>(id, this);
        }

        public Builder<T, S> setContainer(final WebMarkupContainer container) {
            this.container = container;
            return this;
        }

        public Builder<T, S> addBulkAction(final ActionLink.ActionType actionType) {
            bulkActions.add(actionType);
            return this;
        }

        public Builder<T, S> setBulkActionExecutor(final BaseRestClient bulkActionExecutor) {
            this.bulkActionExecutor = bulkActionExecutor;
            return this;
        }

        public Builder<T, S> setItemKeyField(final String itemKeyField) {
            this.itemKeyField = itemKeyField;
            return this;
        }

        public Builder<T, S> setBulkActions(
                final Collection<ActionLink.ActionType> bulkActions,
                final RestClient bulkActionExecutor,
                final String itemKeyField) {
            this.bulkActions.clear();
            if (bulkActions != null) {
                this.bulkActions.addAll(bulkActions);
            }
            this.bulkActionExecutor = bulkActionExecutor;
            this.itemKeyField = itemKeyField;
            return this;
        }

        public Builder<T, S> addColumn(final IColumn<T, S> column) {
            columns.add(column);
            return this;
        }

        public Builder<T, S> setColumns(final List<IColumn<T, S>> columns) {
            this.columns.clear();
            if (columns != null) {
                this.columns.addAll(columns);
            }
            return this;
        }

        public Builder<T, S> setRowsPerPage(final int rowsPerPage) {
            this.rowsPerPage = rowsPerPage;
            return this;
        }

        public Builder<T, S> disableCheckBoxes() {
            this.checkBoxEnabled = false;
            return this;
        }

        private boolean isBulkEnabled() {
            return checkBoxEnabled && bulkActionExecutor != null && !bulkActions.isEmpty();
        }

        public void setMultiLevelPanel(final BaseModal<?> baseModal, final MultilevelPanel multiLevelPanel) {
            this.multiLevelPanel = multiLevelPanel;
            this.baseModal = baseModal;
        }
    }

    private AjaxDataTablePanel(final String id, final Builder<T, S> builder) {
        super(id);

        final BaseModal<T> bulkModal = new BaseModal<>("bulkModal");
        add(bulkModal);

        bulkModal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487149L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                bulkModal.show(false);

                EventDataWrapper data = new EventDataWrapper();
                data.setTarget(target);
                data.setRows(builder.rowsPerPage);

                send(builder.pageRef.getPage(), Broadcast.BREADTH, data);
                BasePage page = (BasePage) findPage();
                if (page != null) {
                    page.getNotificationPanel().refresh(target);
                }
            }
        });

        Fragment fragment = new Fragment("tablePanel", "bulkAvailable", this);
        add(fragment);

        Form<T> bulkActionForm = new Form<>("groupForm");
        fragment.add(bulkActionForm);

        group = new CheckGroup<>("checkgroup", model);
        group.add(new IndicatorAjaxFormChoiceComponentUpdatingBehavior() {

            private static final long serialVersionUID = -151291731388673682L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
            }
        });
        bulkActionForm.add(group);

        if (builder.checkBoxEnabled) {
            builder.columns.add(0, new CheckGroupColumn<T, S>(group));
        }

        dataTable = new AjaxFallbackDataTable<>(
                "dataTable", builder.columns, builder.dataProvider, builder.rowsPerPage, builder.container);
        dataTable.add(new AttributeModifier("class", "table table-bordered table-hover dataTable"));

        group.add(dataTable);

        fragment.add(new IndicatingAjaxButton("bulkActionLink", bulkActionForm) {

            private static final long serialVersionUID = 382302811235019988L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                if (builder.multiLevelPanel == null) {
                    bulkModal.header(new ResourceModel("bulk.action", "Bulk action"));
                    bulkModal.changeCloseButtonLabel(getString("cancel", null, "Cancel"), target);

                    target.add(bulkModal.setContent(new BulkActionModal<>(
                            bulkModal,
                            builder.pageRef,
                            new ArrayList<>(group.getModelObject()),
                            // serialization problem with sublist only
                            new ArrayList<>(builder.columns.subList(1, builder.columns.size() - 1)),
                            builder.bulkActions,
                            builder.bulkActionExecutor,
                            builder.itemKeyField)));

                    bulkModal.show(true);
                } else {
                    builder.multiLevelPanel.next(
                            getString("bulk.action"),
                            new BulkContent<>(
                                    builder.baseModal,
                                    new ArrayList<>(group.getModelObject()),
                                    // serialization problem with sublist only
                                    new ArrayList<>(builder.columns.subList(1, builder.columns.size() - 1)),
                                    builder.bulkActions,
                                    builder.bulkActionExecutor,
                                    builder.itemKeyField),
                            target);
                }
                group.setModelObject(Collections.<T>emptyList());
                target.add(group);
            }
        }.setEnabled(builder.isBulkEnabled()).setVisible(builder.isBulkEnabled()));
    }
}
