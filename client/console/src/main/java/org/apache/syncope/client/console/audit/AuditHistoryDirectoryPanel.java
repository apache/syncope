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
package org.apache.syncope.client.console.audit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.panels.AjaxDataTablePanel;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.panels.ModalPanel;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.rest.AuditHistoryRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.common.lib.to.AuditEntryTO;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

public abstract class AuditHistoryDirectoryPanel<T extends EntityTO> extends DirectoryPanel<
        AuditEntryTO, AuditEntryTO, AuditHistoryDirectoryPanel<T>.AuditHistoryProvider, AuditHistoryRestClient>
        implements ModalPanel {

    private static final long serialVersionUID = -8248734710505211261L;

    private static final List<String> EVENTS = Arrays.asList("create", "update");

    private static final SortParam<String> REST_SORT = new SortParam<>("event_date", false);

    private final BaseModal<?> baseModal;

    private final MultilevelPanel mlp;

    private final AuditElements.EventCategoryType type;

    private final String category;

    private final T entity;

    private final String auditRestoreEntitlement;

    public AuditHistoryDirectoryPanel(
            final BaseModal<?> baseModal,
            final MultilevelPanel mlp,
            final AuditElements.EventCategoryType type,
            final String category,
            final T entity,
            final String auditRestoreEntitlement,
            final PageReference pageRef) {

        super(MultilevelPanel.FIRST_LEVEL_ID, pageRef);
        disableCheckBoxes();

        this.baseModal = baseModal;
        this.mlp = mlp;
        this.type = type;
        this.category = category;
        this.entity = entity;
        this.auditRestoreEntitlement = auditRestoreEntitlement;
        this.pageRef = pageRef;

        this.restClient = new AuditHistoryRestClient();
        initResultTable();
    }

    @Override
    protected AuditHistoryDirectoryPanel<T>.AuditHistoryProvider dataProvider() {
        return new AuditHistoryProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return Constants.PREF_AUDIT_HISTORY_PAGINATOR_ROWS;
    }

    @Override
    protected List<IColumn<AuditEntryTO, String>> getColumns() {
        List<IColumn<AuditEntryTO, String>> columns = new ArrayList<>();
        columns.add(new PropertyColumn<>(new StringResourceModel("who", this), "who"));
        columns.add(new DatePropertyColumn<>(new StringResourceModel("date", this), null, "date"));
        return columns;
    }

    @Override
    protected void resultTableCustomChanges(final AjaxDataTablePanel.Builder<AuditEntryTO, String> resultTableBuilder) {
        resultTableBuilder.setMultiLevelPanel(baseModal, mlp);
    }

    protected abstract void restore(String json, AjaxRequestTarget target);

    @Override
    protected ActionsPanel<AuditEntryTO> getActions(final IModel<AuditEntryTO> model) {
        final ActionsPanel<AuditEntryTO> panel = super.getActions(model);

        panel.add(new ActionLink<AuditEntryTO>() {

            private static final long serialVersionUID = -6745431735457245600L;

            @Override
            public void onClick(final AjaxRequestTarget target, final AuditEntryTO modelObject) {
                AuditHistoryDirectoryPanel.this.getTogglePanel().close(target);

                mlp.next(
                        new StringResourceModel("audit.diff.view", AuditHistoryDirectoryPanel.this).getObject(),
                        new AuditHistoryDetails<T>(
                                mlp,
                                modelObject,
                                entity,
                                auditRestoreEntitlement,
                                pageRef) {

                    private static final long serialVersionUID = -5311898419151367494L;

                    @Override
                    protected void restore(final String json, final AjaxRequestTarget target) {
                        AuditHistoryDirectoryPanel.this.restore(json, target);
                    }
                }, target);

                target.add(modal);
            }
        }, ActionLink.ActionType.VIEW, StandardEntitlement.AUDIT_READ);

        return panel;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return Collections.emptyList();
    }

    protected class AuditHistoryProvider extends DirectoryDataProvider<AuditEntryTO> {

        private static final long serialVersionUID = 415113175628260864L;

        AuditHistoryProvider(final int paginatorRows) {
            super(paginatorRows);
            setSort("date", SortOrder.DESCENDING);
        }

        @Override
        public long size() {
            return restClient.count(entity.getKey(), type, category, EVENTS, AuditElements.Result.SUCCESS);
        }

        @Override
        public Iterator<AuditEntryTO> iterator(final long first, final long count) {
            int page = ((int) first / paginatorRows);
            return restClient.search(
                    entity.getKey(),
                    (page < 0 ? 0 : page) + 1,
                    paginatorRows,
                    type,
                    category,
                    EVENTS,
                    AuditElements.Result.SUCCESS,
                    REST_SORT).
                    iterator();
        }

        @Override
        public IModel<AuditEntryTO> model(final AuditEntryTO auditEntryBean) {
            return new CompoundPropertyModel<>(auditEntryBean);
        }
    }
}
