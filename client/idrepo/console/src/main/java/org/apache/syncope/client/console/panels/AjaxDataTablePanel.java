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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import org.apache.syncope.client.console.batch.BatchContent;
import org.apache.syncope.client.console.batch.BatchModal;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.DirectoryPanel.EventDataWrapper;
import org.apache.syncope.client.console.rest.BaseRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.AjaxFallbackDataTable;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.CheckGroupColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksTogglePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormChoiceComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.rest.RestClient;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.form.CheckGroupSelector;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

public final class AjaxDataTablePanel<T extends Serializable, S> extends DataTablePanel<T, S> {

    private static final long serialVersionUID = -7264400471578272966L;

    public static class Builder<T extends Serializable, S> implements Serializable {

        private static final long serialVersionUID = 8876232177473972722L;

        private boolean checkBoxEnabled = true;

        private final List<IColumn<T, S>> columns = new ArrayList<>();

        private final ISortableDataProvider<T, S> dataProvider;

        private int rowsPerPage = 10;

        private final Collection<ActionLink.ActionType> batches = new ArrayList<>();

        private RestClient batchExecutor;

        private String itemKeyField;

        private final PageReference pageRef;

        private WebMarkupContainer container;

        private MultilevelPanel multiLevelPanel;

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

        public Builder<T, S> addBatch(final ActionLink.ActionType actionType) {
            batches.add(actionType);
            return this;
        }

        public Builder<T, S> setBatchExecutor(final BaseRestClient batchExecutor) {
            this.batchExecutor = batchExecutor;
            return this;
        }

        public Builder<T, S> setItemKeyField(final String itemKeyField) {
            this.itemKeyField = itemKeyField;
            return this;
        }

        public Builder<T, S> setBatches(
                final Collection<ActionLink.ActionType> batches,
                final RestClient batchExecutor,
                final String itemKeyField) {

            this.batches.clear();
            if (batches != null) {
                this.batches.addAll(batches);
            }
            this.batchExecutor = batchExecutor;
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

        private boolean isBatchEnabled() {
            return checkBoxEnabled && batchExecutor != null && !batches.isEmpty();
        }

        public void setMultiLevelPanel(final MultilevelPanel multiLevelPanel) {
            this.multiLevelPanel = multiLevelPanel;
        }

        protected ActionsPanel<T> getActions(final IModel<T> model) {
            return null;
        }

        protected ActionLinksTogglePanel<T> getTogglePanel() {
            return null;
        }

        protected BiConsumer<AjaxRequestTarget, IModel<T>> onDoubleClick() {
            return null;
        }
    }

    protected final BaseModal<T> batchModal;

    private AjaxDataTablePanel(final String id, final Builder<T, S> builder) {
        super(id);

        batchModal = new BaseModal<>("batchModal");
        batchModal.size(Modal.Size.Extra_large);
        add(batchModal);

        batchModal.setWindowClosedCallback(target -> {
            batchModal.show(false);

            EventDataWrapper data = new EventDataWrapper();
            data.setTarget(target);
            data.setRows(builder.rowsPerPage);

            send(builder.pageRef.getPage(), Broadcast.BREADTH, data);
            Optional.ofNullable((BasePage) findPage()).
                    ifPresent(page -> page.getNotificationPanel().refresh(target));
        });

        Fragment fragment = new Fragment("tablePanel", "batchAvailable", this);
        add(fragment);

        Form<T> groupForm = new Form<>("groupForm");
        fragment.add(groupForm);

        group = new CheckGroup<>("checkgroup", model);
        group.add(new IndicatorAjaxFormChoiceComponentUpdatingBehavior() {

            private static final long serialVersionUID = -151291731388673682L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                group.visitChildren(CheckGroupSelector.class, (selector, ivisit) -> {
                    target.focusComponent(selector);
                    ivisit.stop();
                });
            }
        });
        groupForm.add(group);

        if (builder.checkBoxEnabled) {
            builder.columns.addFirst(new CheckGroupColumn<>(group));
        }

        dataTable = new AjaxFallbackDataTable<>(
                "dataTable", builder.columns, builder.dataProvider, builder.rowsPerPage, builder.container) {

            private static final long serialVersionUID = -7370603907251344224L;

            @Override
            protected ActionsPanel<T> getActions(final IModel<T> model) {
                return builder.getActions(model);
            }

            @Override
            protected ActionLinksTogglePanel<T> getTogglePanel() {
                return builder.getTogglePanel();
            }

            @Override
            protected void onDoubleClick(final AjaxRequestTarget target, final IModel<T> model) {
                Optional.ofNullable(builder.onDoubleClick()).ifPresentOrElse(
                        odc -> odc.accept(target, model),
                        () -> super.onDoubleClick(target, model));
            }
        };

        dataTable.add(new AttributeModifier("class", "table table-bordered table-hover dataTable"));

        group.add(dataTable);

        fragment.add(new IndicatingAjaxButton("batchLink", groupForm) {

            private static final long serialVersionUID = 382302811235019988L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                // send event to close eventually opened actions toggle panel
                Optional.ofNullable(builder.getTogglePanel()).ifPresent(p -> p.close(target));

                if (builder.multiLevelPanel == null) {
                    batchModal.header(new ResourceModel("batch"));
                    batchModal.changeCloseButtonLabel(getString("cancel", null, "Cancel"), target);

                    target.add(batchModal.setContent(new BatchModal<>(
                            batchModal,
                            builder.pageRef,
                            new ArrayList<>(group.getModelObject()),
                            builder.columns.size() == 1
                            ? builder.columns
                            // serialization problem with sublist only
                            : new ArrayList<>(builder.columns.subList(1, builder.columns.size())),
                            builder.batches,
                            builder.batchExecutor,
                            builder.itemKeyField)));

                    batchModal.show(true);
                } else {
                    builder.multiLevelPanel.next(getString("batch"),
                            new BatchContent<>(
                                    new ArrayList<>(group.getModelObject()),
                                    builder.columns.size() == 1
                                    ? builder.columns
                                    // serialization problem with sublist only
                                    : new ArrayList<>(builder.columns.subList(1, builder.columns.size())),
                                    builder.batches,
                                    builder.batchExecutor,
                                    builder.itemKeyField),
                            target);
                }
                group.setModelObject(List.of());
                target.add(group);
            }
        }.setEnabled(builder.isBatchEnabled()).setVisible(builder.isBatchEnabled()));
    }
}
