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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.audit.AuditHistoryDirectoryPanel.AuditHistoryProvider;
import org.apache.syncope.client.console.commons.IdRepoConstants;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.AjaxDataTablePanel;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.rest.AnyObjectRestClient;
import org.apache.syncope.client.console.rest.AuditHistoryRestClient;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.DirectoryDataProvider;
import org.apache.syncope.client.ui.commons.panels.ModalPanel;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.request.GroupUR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AuditEntryTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

public class AuditHistoryDirectoryPanel extends
        DirectoryPanel<AuditEntryTO, AuditEntryTO, AuditHistoryProvider, AuditHistoryRestClient>
        implements ModalPanel {

    private static final long serialVersionUID = -8248734710505211261L;

    private static final int TOTAL_AUDIT_HISTORY_COMPARISONS = 25;

    private static final ObjectMapper MAPPER = new ObjectMapper();

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
        initResultTable();
    }

    /**
     * Restore an object based on the audit record.
     *
     * Note that for user objects, the original audit record masks
     * the password and the security answer; so we cannot use the audit
     * record to resurrect the entry based on mask data. The method behavior
     * below will reset the audit record such that the current security answer
     * and the password for the object are always maintained, and such properties
     * for the user cannot be restored using audit records.
     *
     * @param entryBean the entry bean
     * @param anyTO the any to
     * @return the response
     */
    private static ProvisioningResult<? extends AnyTO> restore(final AuditEntryTO entryBean,
            final AnyTO anyTO) {
        try {
            String json = getJSONFromAuditEntry(entryBean);
            if (anyTO instanceof UserTO) {
                UserTO userTO = MAPPER.readValue(json, UserTO.class);
                UserUR req = AnyOperations.diff(userTO, anyTO, false);
                req.setPassword(null);
                req.setSecurityAnswer(null);
                return new UserRestClient().update(anyTO.getETagValue(), req);
            }
            if (anyTO instanceof GroupTO) {
                GroupTO groupTO = MAPPER.readValue(json, GroupTO.class);
                GroupUR req = AnyOperations.diff(groupTO, anyTO, false);
                return new GroupRestClient().update(anyTO.getETagValue(), req);
            }
            if (anyTO instanceof AnyObjectTO) {
                AnyObjectTO anyObjectTO = MAPPER.readValue(json, AnyObjectTO.class);
                AnyObjectUR req = AnyOperations.diff(anyObjectTO, anyTO, false);
                return new AnyObjectRestClient().update(anyTO.getETagValue(), req);
            }
        } catch (final Exception e) {
            LOG.error("Could not restore object for {}", anyTO, e);
        }
        throw SyncopeClientException.build(ClientExceptionType.InvalidAnyObject);
    }

    private static String getJSONFromAuditEntry(final AuditEntryTO entryBean) throws JsonProcessingException {
        final String json;
        if (entryBean.getBefore() == null) {
            json = MAPPER.readTree(entryBean.getOutput()).get("entity").toPrettyString();
        } else {
            json = entryBean.getBefore();
        }
        return json;
    }

    private static SortParam<String> getSortParam() {
        return new SortParam<>("event_date", false);
    }

    private static AuditElements.Result getQueryableAuditResult() {
        return AuditElements.Result.SUCCESS;
    }

    private static List<String> getQueryableAuditEvents() {
        return Arrays.asList("create", "update");
    }

    @Override
    protected AuditHistoryDirectoryPanel.AuditHistoryProvider dataProvider() {
        return new AuditHistoryProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return IdRepoConstants.PREF_AUDIT_HISTORY_PAGINATOR_ROWS;
    }

    @Override
    protected List<IColumn<AuditEntryTO, String>> getColumns() {
        final List<IColumn<AuditEntryTO, String>> columns = new ArrayList<>();
        columns.add(new PropertyColumn<>(
                new StringResourceModel("who", this), "who"));
        columns.add(new DatePropertyColumn<>(
                new StringResourceModel("date", this), null, "date"));
        return columns;
    }

    @Override
    protected void resultTableCustomChanges(
            final AjaxDataTablePanel.Builder<AuditEntryTO, String> resultTableBuilder) {
        resultTableBuilder.setMultiLevelPanel(baseModal, multiLevelPanelRef);
    }

    @Override
    protected ActionsPanel<AuditEntryTO> getActions(final IModel<AuditEntryTO> model) {
        final ActionsPanel<AuditEntryTO> panel = super.getActions(model);

        panel.add(new ActionLink<AuditEntryTO>() {

            private static final long serialVersionUID = -6745431735457245600L;

            @Override
            public void onClick(final AjaxRequestTarget target, final AuditEntryTO modelObject) {
                AuditHistoryDirectoryPanel.this.getTogglePanel().close(target);
                viewAuditHistory(modelObject, target);
                target.add(modal);
            }
        }, ActionLink.ActionType.VIEW, IdRepoEntitlement.AUDIT_READ);

        final String auditRestoreEntitlement;
        switch (this.anyTypeKind) {
            case USER:
                auditRestoreEntitlement = IdRepoEntitlement.USER_UPDATE;
                break;
            case GROUP:
                auditRestoreEntitlement = IdRepoEntitlement.GROUP_UPDATE;
                break;
            default:
                auditRestoreEntitlement = IdRepoEntitlement.ANYTYPE_UPDATE;
                break;
        }

        panel.add(new ActionLink<AuditEntryTO>() {

            private static final long serialVersionUID = -6745431735457245600L;

            @Override
            public void onClick(final AjaxRequestTarget target, final AuditEntryTO modelObject) {
                try {
                    AuditHistoryDirectoryPanel.this.getTogglePanel().close(target);
                    ProvisioningResult<? extends AnyTO> result = restore(modelObject, anyTO);
                    anyTO.setLastChangeDate(new Date(Long.parseLong(result.getEntity().getETagValue())));
                    target.add(container);
                } catch (SyncopeClientException e) {
                    LOG.error("While restoring {}", anyTypeKind, e);
                    SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage())
                            ? e.getClass().getName() : e.getMessage());
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.RESTORE, auditRestoreEntitlement);

        return panel;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return Collections.emptyList();
    }

    private void viewAuditHistory(final AuditEntryTO auditEntryBean, final AjaxRequestTarget target) {
        List<AuditEntryTO> search = AuditHistoryRestClient.search(anyTO.getKey(),
                0,
                TOTAL_AUDIT_HISTORY_COMPARISONS,
                getSortParam(),
                getQueryableAuditEvents(),
                getQueryableAuditResult());

        multiLevelPanelRef.next(
                new StringResourceModel("audit.diff.view", this).getObject(),
                new HistoryAuditDetails(modal, auditEntryBean,
                        getPage().getPageReference(), toAuditEntryTOs(search), anyTO, anyTypeKind), target);
    }

    private List<AuditEntryTO> toAuditEntryTOs(final List<AuditEntryTO> search) {
        return search
                .stream()
                .map(entry -> {
                    AuditEntryTO bean = new AuditEntryTO();
                    bean.setKey(anyTO.getKey());
                    bean.setBefore(entry.getBefore());
                    bean.setDate(entry.getDate());
                    bean.setEvent(entry.getEvent());
                    bean.getInputs().addAll(entry.getInputs());
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

    protected class AuditHistoryProvider extends DirectoryDataProvider<AuditEntryTO> {

        private static final long serialVersionUID = 415113175628260864L;

        AuditHistoryProvider(final int paginatorRows) {
            super(paginatorRows);
        }

        @Override
        public Iterator<? extends AuditEntryTO> iterator(final long first, final long count) {
            return getAuditEntryBeans(first, count).iterator();
        }

        @Override
        public long size() {
            return AuditHistoryRestClient.count(anyTO.getKey(), getQueryableAuditEvents(), getQueryableAuditResult());
        }

        @Override
        public IModel<AuditEntryTO> model(final AuditEntryTO auditEntryBean) {
            return new CompoundPropertyModel<>(auditEntryBean);
        }

        private List<AuditEntryTO> getAuditEntryBeans(final long first, final long count) {
            int page = (int) first / paginatorRows;
            return AuditHistoryRestClient.search(anyTO.getKey(),
                    Math.max(page, 0) + 1,
                    Long.valueOf(count).intValue(),
                    getSortParam(),
                    getQueryableAuditEvents(),
                    getQueryableAuditResult());
        }
    }
}
