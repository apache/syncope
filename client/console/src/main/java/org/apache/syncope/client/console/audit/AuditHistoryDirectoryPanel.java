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

import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.AjaxDataTablePanel;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.panels.HistoryAuditDetails;
import org.apache.syncope.client.console.panels.ModalPanel;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.rest.AuditHistoryRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AuditEntryTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class AuditHistoryDirectoryPanel extends
    DirectoryPanel<AnyTOAuditEntryBean, AnyTOAuditEntryBean,
        DirectoryDataProvider<AnyTOAuditEntryBean>, AuditHistoryRestClient>
    implements ModalPanel {

    private static final long serialVersionUID = -8248734710505211261L;

    private final BaseModal<?> baseModal;

    private final MultilevelPanel multiLevelPanelRef;

    private final AnyTO anyTO;

    private final AnyTypeKind anyTypeKind;

    public AuditHistoryDirectoryPanel(
        final BaseModal<?> baseModal,
        final MultilevelPanel multiLevelPanelRef,
        final PageReference pageRef,
        final AnyTO anyTO) {

        super(MultilevelPanel.FIRST_LEVEL_ID, pageRef);
        disableCheckBoxes();

        this.baseModal = baseModal;
        this.multiLevelPanelRef = multiLevelPanelRef;
        this.anyTO = anyTO;

        anyTypeKind = AnyTypeKind.fromTOClass(anyTO.getClass());
        this.restClient = new AuditHistoryRestClient();
        initResultTable();
    }

    @Override
    protected DirectoryDataProvider<AnyTOAuditEntryBean> dataProvider() {
        return new AuditHistoryProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return Constants.PREF_AUDIT_HISTORY_PAGINATOR_ROWS;
    }

    @Override
    protected List<IColumn<AnyTOAuditEntryBean, String>> getColumns() {
        final List<IColumn<AnyTOAuditEntryBean, String>> columns = new ArrayList<>();
        columns.add(new PropertyColumn<>(
            new StringResourceModel("who", this), "who"));
        columns.add(new DatePropertyColumn<>(
            new StringResourceModel("date", this), null, "date"));
        return columns;
    }

    @Override
    protected void resultTableCustomChanges(
        final AjaxDataTablePanel.Builder<AnyTOAuditEntryBean, String> resultTableBuilder) {
        resultTableBuilder.setMultiLevelPanel(baseModal, multiLevelPanelRef);
    }

    @Override
    protected ActionsPanel<AnyTOAuditEntryBean> getActions(final IModel<AnyTOAuditEntryBean> model) {
        final ActionsPanel<AnyTOAuditEntryBean> panel = super.getActions(model);
        final AnyTOAuditEntryBean auditEntryTO = model.getObject();

        panel.add(new ActionLink<AnyTOAuditEntryBean>() {
            private static final long serialVersionUID = -6745431735457245600L;

            @Override
            public void onClick(final AjaxRequestTarget target, final AnyTOAuditEntryBean modelObject) {
                AuditHistoryDirectoryPanel.this.getTogglePanel().close(target);
                viewAuditHistory(modelObject, target);
                target.add(modal);
            }
        }, ActionLink.ActionType.VIEW, StandardEntitlement.AUDIT_READ);

        panel.add(new ActionLink<AnyTOAuditEntryBean>() {

            private static final long serialVersionUID = -6745431735457245600L;

            @Override
            public void onClick(final AjaxRequestTarget target, final AnyTOAuditEntryBean modelObject) {
                try {
                    restClient.restore(modelObject.getKey());
                    AuditHistoryDirectoryPanel.this.getTogglePanel().close(target);
                    target.add(container);
                } catch (SyncopeClientException e) {
                    LOG.error("While restoring {}", auditEntryTO.getKey(), e);
                    SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage())
                        ? e.getClass().getName() : e.getMessage());
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.RESTORE, StandardEntitlement.AUDIT_RESTORE);

        return panel;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return Collections.emptyList();
    }

    private void viewAuditHistory(final AnyTOAuditEntryBean auditEntryBean, final AjaxRequestTarget target) {
        List<AuditEntryTO> search = restClient.search(anyTO.getKey(),
            getSortParam(),
            getQueryableAuditEvents(),
            getQueryableAuditResults());

        multiLevelPanelRef.next(
            new StringResourceModel("audit.diff.view", this).getObject(),
            new HistoryAuditDetails(modal, auditEntryBean,
                getPage().getPageReference(), toAnyTOAuditEntryBeans(search), anyTO), target);
    }

    private SortParam<String> getSortParam() {
        return new SortParam<>("event_date", false);
    }

    private List<AuditElements.Result> getQueryableAuditResults() {
        return Collections.singletonList(AuditElements.Result.SUCCESS);
    }

    private List<String> getQueryableAuditEvents() {
        return Arrays.asList("create", "update");
    }

    private List<AnyTOAuditEntryBean> toAnyTOAuditEntryBeans(final List<AuditEntryTO> search) {
        return search
            .stream()
            .map(entry -> {
                AnyTOAuditEntryBean bean = new AnyTOAuditEntryBean(anyTO.getKey());
                bean.setBefore(entry.getBefore());
                bean.setDate(entry.getDate());
                bean.setEvent(entry.getEvent());
                bean.setInputs(entry.getInputs());
                bean.setLoggerName(entry.getLoggerName());
                bean.setOutput(entry.getOutput());
                bean.setResult(entry.getResult());
                bean.setSubCategory(entry.getSubCategory());
                bean.setThrowable(entry.getThrowable());
                bean.setWho(entry.getWho());
                return bean;
            })
            .collect(Collectors.toList());
    }

    private class AuditHistoryProvider extends DirectoryDataProvider<AnyTOAuditEntryBean> {
        private static final long serialVersionUID = 415113175628260864L;

        AuditHistoryProvider(final int paginatorRows) {
            super(paginatorRows);
        }

        @Override
        public Iterator<? extends AnyTOAuditEntryBean> iterator(final long first, final long count) {
            return getAuditEntryBeans(first, count).iterator();
        }

        @Override
        public long size() {
            return restClient.count(anyTO.getKey(), getQueryableAuditEvents(), getQueryableAuditResults());
        }

        @Override
        public IModel<AnyTOAuditEntryBean> model(final AnyTOAuditEntryBean auditEntryBean) {
            return new CompoundPropertyModel<>(auditEntryBean);
        }

        private List<AnyTOAuditEntryBean> getAuditEntryBeans(final long first, final long count) {
            int page = (int) first / paginatorRows;
            List<AuditEntryTO> search = restClient.search(anyTO.getKey(),
                Math.max(page, 0) + 1, paginatorRows,
                getSortParam(),
                getQueryableAuditEvents(),
                getQueryableAuditResults());
            return toAnyTOAuditEntryBeans(search);
        }
    }
}
