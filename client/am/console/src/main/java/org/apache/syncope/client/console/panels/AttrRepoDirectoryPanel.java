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
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.audit.AuditHistoryModal;
import org.apache.syncope.client.console.commons.AMConstants;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.AttrRepoDirectoryPanel.AttrRepoProvider;
import org.apache.syncope.client.console.rest.AttrRepoRestClient;
import org.apache.syncope.client.console.rest.AuditRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wizards.AttrRepoWizardBuilder;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.to.AttrRepoTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class AttrRepoDirectoryPanel
        extends DirectoryPanel<AttrRepoTO, AttrRepoTO, AttrRepoProvider, AttrRepoRestClient> {

    private static final long serialVersionUID = 1005345990563741296L;

    @SpringBean
    protected AuditRestClient auditRestClient;

    protected final BaseModal<Serializable> historyModal;

    public AttrRepoDirectoryPanel(final String id, final AttrRepoRestClient restClient, final PageReference pageRef) {
        super(id, restClient, pageRef);

        disableCheckBoxes();

        this.addNewItemPanelBuilder(new AttrRepoWizardBuilder(new AttrRepoTO(), restClient, pageRef), true);

        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, AMEntitlement.ATTR_REPO_CREATE);

        modal.size(Modal.Size.Extra_large);
        initResultTable();

        historyModal = new BaseModal<>(Constants.OUTER);
        historyModal.size(Modal.Size.Large);
        addOuterObject(historyModal);
    }

    @Override
    protected AttrRepoProvider dataProvider() {
        return new AttrRepoProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return AMConstants.PREF_AUTHMODULE_PAGINATOR_ROWS;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of();
    }

    @Override
    protected List<IColumn<AttrRepoTO, String>> getColumns() {
        List<IColumn<AttrRepoTO, String>> columns = new ArrayList<>();
        columns.add(new PropertyColumn<>(
                new StringResourceModel(Constants.KEY_FIELD_NAME, this),
                Constants.KEY_FIELD_NAME, Constants.KEY_FIELD_NAME));
        columns.add(new PropertyColumn<>(new ResourceModel(Constants.DESCRIPTION_FIELD_NAME),
                Constants.DESCRIPTION_FIELD_NAME, Constants.DESCRIPTION_FIELD_NAME));
        columns.add(new PropertyColumn<>(new ResourceModel("type"), "conf") {

            private static final long serialVersionUID = -1822504503325964706L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<AttrRepoTO>> item,
                    final String componentId,
                    final IModel<AttrRepoTO> rowModel) {

                item.add(new Label(componentId, rowModel.getObject().getConf() == null
                        ? StringUtils.EMPTY
                        : StringUtils.substringBefore(
                                rowModel.getObject().getConf().getClass().getSimpleName(), "AttrRepoConf")));
            }
        });
        columns.add(new PropertyColumn<>(new ResourceModel("state"), "state", "state"));
        columns.add(new PropertyColumn<>(new ResourceModel("order"), "order", "order"));
        return columns;
    }

    @Override
    public ActionsPanel<AttrRepoTO> getActions(final IModel<AttrRepoTO> model) {
        ActionsPanel<AttrRepoTO> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final AttrRepoTO ignore) {
                send(AttrRepoDirectoryPanel.this, Broadcast.EXACT, new AjaxWizard.EditItemActionEvent<>(
                        restClient.read(model.getObject().getKey()), target));
            }
        }, ActionLink.ActionType.EDIT, AMEntitlement.ATTR_REPO_UPDATE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -5432034353017728756L;

            @Override
            public void onClick(final AjaxRequestTarget target, final AttrRepoTO ignore) {
                model.setObject(restClient.read(model.getObject().getKey()));

                target.add(historyModal.setContent(new AuditHistoryModal<>(
                        OpEvent.CategoryType.LOGIC,
                        "AttrRepoLogic",
                        model.getObject(),
                        AMEntitlement.ATTR_REPO_UPDATE,
                        auditRestClient) {

                    private static final long serialVersionUID = -3712506022627033822L;

                    @Override
                    protected void restore(final String json, final AjaxRequestTarget target) {
                        try {
                            AttrRepoTO updated = MAPPER.readValue(json, AttrRepoTO.class);
                            restClient.update(updated);

                            SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                        } catch (Exception e) {
                            LOG.error("While restoring AttrRepo {}", model.getObject().getKey(), e);
                            SyncopeConsoleSession.get().onException(e);
                        }
                        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                    }
                }));

                historyModal.header(new Model<>(getString("auditHistory.title", new Model<>(model.getObject()))));

                historyModal.show(true);
            }
        }, ActionLink.ActionType.VIEW_AUDIT_HISTORY, String.format("%s,%s", AMEntitlement.ATTR_REPO_READ,
                IdRepoEntitlement.AUDIT_LIST));

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final AttrRepoTO ignore) {
                try {
                    restClient.delete(model.getObject().getKey());

                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (Exception e) {
                    LOG.error("While deleting {}", model.getObject().getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, AMEntitlement.ATTR_REPO_DELETE, true);

        return panel;
    }

    protected final class AttrRepoProvider extends DirectoryDataProvider<AttrRepoTO> {

        private static final long serialVersionUID = -185944053385660794L;

        private final SortableDataProviderComparator<AttrRepoTO> comparator;

        private AttrRepoProvider(final int paginatorRows) {
            super(paginatorRows);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<AttrRepoTO> iterator(final long first, final long count) {
            List<AttrRepoTO> attrRepos = restClient.list();
            attrRepos.sort(comparator);
            return attrRepos.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return restClient.list().size();
        }

        @Override
        public IModel<AttrRepoTO> model(final AttrRepoTO object) {
            return new CompoundPropertyModel<>(object);
        }
    }
}
