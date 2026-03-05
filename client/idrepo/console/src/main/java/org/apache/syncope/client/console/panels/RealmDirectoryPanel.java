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
import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.PreferenceManager;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.audit.AuditHistoryModal;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.IdRepoConstants;
import org.apache.syncope.client.console.commons.RealmsUtils;
import org.apache.syncope.client.console.commons.StatusProvider;
import org.apache.syncope.client.console.pages.Anys;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.AuditRestClient;
import org.apache.syncope.client.console.rest.RealmRestClient;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.client.console.tasks.RealmPropagationTasks;
import org.apache.syncope.client.console.tasks.TemplatesTogglePanel;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.AttrColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.BooleanPropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.KeyPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wizards.any.ConnObjectPanel;
import org.apache.syncope.client.ui.commons.ConnIdSpecialName;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.status.StatusUtils;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.DerSchemaTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.rest.api.beans.RealmQuery;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.util.ReflectionUtils;

public class RealmDirectoryPanel
        extends DirectoryPanel<RealmTO, RealmTO, RealmDirectoryPanel.RealmDataProvider, RealmRestClient> {

    private static final long serialVersionUID = -619348700593732362L;

    @SpringBean
    protected SchemaRestClient schemaRestClient;

    @SpringBean
    protected AuditRestClient auditRestClient;

    protected final List<PlainSchemaTO> plainSchemas = new ArrayList<>();

    protected final List<DerSchemaTO> derSchemas = new ArrayList<>();

    protected final BaseModal<Serializable> utilityModal = new BaseModal<>(Constants.OUTER);

    protected final RealmWizardBuilder wizardBuilder;

    protected final Model<String> base = Model.of(StringUtils.EMPTY);

    protected final Model<String> fiql = Model.of(StringUtils.EMPTY);

    public RealmDirectoryPanel(final String id, final RealmRestClient restClient, final PageReference pageRef) {
        super(id, restClient, pageRef, true, true);

        plainSchemas.addAll(schemaRestClient.getSchemas(SchemaType.PLAIN, null, new String[0]));
        derSchemas.addAll(schemaRestClient.getSchemas(SchemaType.DERIVED, null, new String[0]));

        disableCheckBoxes();
        setShowResultPanel(true);

        utilityModal.size(Modal.Size.Large);
        addOuterObject(utilityModal);
        setWindowClosedReloadCallback(utilityModal);

        modal.size(Modal.Size.Large);
        wizardBuilder = new RealmWizardBuilder(restClient, pageRef);
        addNewItemPanelBuilder(wizardBuilder, true);
        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, IdRepoEntitlement.REALM_CREATE);

        initResultTable();
    }

    @Override
    protected RealmDataProvider dataProvider() {
        return new RealmDataProvider(rows);
    }

    public void search(final String base, final String fiql, final AjaxRequestTarget target) {
        this.base.setObject(StringUtils.trimToEmpty(base));
        this.fiql.setObject(StringUtils.trimToEmpty(fiql));
        if (dataProvider != null) {
            dataProvider.setBase(this.base.getObject());
            dataProvider.setFIQL(this.fiql.getObject());
        }
        super.search(target);
    }

    public void search(final String fiql, final AjaxRequestTarget target) {
        search(StringUtils.EMPTY, fiql, target);
    }

    @Override
    protected String paginatorRowsKey() {
        return IdRepoConstants.PREF_REALM_PAGINATOR_ROWS;
    }

    @Override
    protected List<IColumn<RealmTO, String>> getColumns() {
        List<IColumn<RealmTO, String>> columns = new ArrayList<>();
        columns.add(new KeyPropertyColumn<>(
                new ResourceModel(Constants.KEY_FIELD_NAME, Constants.KEY_FIELD_NAME), Constants.KEY_FIELD_NAME));

        List<IColumn<RealmTO, String>> prefcolumns = new ArrayList<>();
        PreferenceManager.getList(IdRepoConstants.PREF_REALM_DETAILS_VIEW).stream().
                filter(name -> !Constants.KEY_FIELD_NAME.equalsIgnoreCase(name)).
                forEach(name -> addPropertyColumn(
                name,
                ReflectionUtils.findField(RealmTO.class, name),
                prefcolumns));

        PreferenceManager.getList(IdRepoConstants.PREF_REALM_PLAIN_ATTRS_VIEW).stream().
                map(a -> plainSchemas.stream().filter(p -> p.getKey().equals(a)).findFirst()).
                flatMap(Optional::stream).
                forEach(s -> prefcolumns.add(new AttrColumn<>(
                s.getKey(), s.getLabel(SyncopeConsoleSession.get().getLocale()), SchemaType.PLAIN)));

        PreferenceManager.getList(IdRepoConstants.PREF_REALM_DER_ATTRS_VIEW).stream().
                map(a -> derSchemas.stream().filter(p -> p.getKey().equals(a)).findFirst()).
                flatMap(Optional::stream).
                forEach(s -> prefcolumns.add(new AttrColumn<>(
                s.getKey(), s.getLabel(SyncopeConsoleSession.get().getLocale()), SchemaType.DERIVED)));

        // Add defaults in case of no selection
        if (prefcolumns.isEmpty()) {
            for (String name : RealmDisplayAttributesModalPanel.DEFAULT_COLUMNS) {
                addPropertyColumn(
                        name,
                        ReflectionUtils.findField(RealmTO.class, name),
                        prefcolumns);
            }

            PreferenceManager.setList(
                    IdRepoConstants.PREF_REALM_DETAILS_VIEW,
                    RealmDisplayAttributesModalPanel.DEFAULT_COLUMNS);
        }

        columns.addAll(prefcolumns);
        return columns;
    }

    protected void addPropertyColumn(
            final String name,
            final Field field,
            final List<IColumn<RealmTO, String>> columns) {

        if (Constants.KEY_FIELD_NAME.equalsIgnoreCase(name)) {
            columns.add(new KeyPropertyColumn<>(new ResourceModel(name, name), name, name));
        } else if (field != null && !field.isSynthetic()
                && (field.getType().equals(Boolean.class) || field.getType().equals(boolean.class))) {

            columns.add(new BooleanPropertyColumn<>(new ResourceModel(name, name), name, name));
        } else if (field != null && !field.isSynthetic()
                && (field.getType().equals(Date.class) || field.getType().equals(OffsetDateTime.class))) {

            columns.add(new DatePropertyColumn<>(new ResourceModel(name, name), name, name));
        } else {
            columns.add(new PropertyColumn<>(new ResourceModel(name, name), name, name));
        }
    }

    @Override
    public ActionsPanel<Serializable> getHeader(final String componentId) {
        final ActionsPanel<Serializable> panel = super.getHeader(componentId);
        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -8740543624521405980L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                target.add(displayAttributeModal.setContent(new RealmDisplayAttributesModalPanel<>(
                        displayAttributeModal,
                        page.getPageReference(),
                        RealmDisplayAttributesModalPanel.AVAILABLE_COLUMNS,
                        plainSchemas.stream().map(PlainSchemaTO::getKey).sorted().toList(),
                        derSchemas.stream().map(DerSchemaTO::getKey).sorted().toList())));
                displayAttributeModal.header(new ResourceModel("any.attr.display"));
                displayAttributeModal.show(true);
            }
        }, ActionLink.ActionType.CHANGE_VIEW, IdRepoEntitlement.REALM_SEARCH).hideLabel();
        return panel;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of();
    }

    @Override
    public ActionsPanel<RealmTO> getActions(final IModel<RealmTO> model) {
        ActionsPanel<RealmTO> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = 6459651989453003611L;

            @Override
            public void onClick(final AjaxRequestTarget target, final RealmTO realmTO) {
                PageParameters parameters = new PageParameters();
                parameters.add(Anys.INITIAL_REALM, realmTO.getFullPath());
                setResponsePage(Anys.class, parameters);
            }
        }, ActionLink.ActionType.ZOOM_IN, IdRepoEntitlement.REALM_SEARCH, false).
                setRealm(model.getObject().getFullPath());

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = 7596172676990094774L;

            @Override
            public void onClick(final AjaxRequestTarget target, final RealmTO realmTO) {
                send(RealmDirectoryPanel.this, Broadcast.EXACT, new AjaxWizard.EditItemActionEvent<>(realmTO, target) {

                    @Override
                    public String getEventDescription() {
                        return "realm.edit";
                    }
                });
            }
        }, ActionLink.ActionType.EDIT, IdRepoEntitlement.REALM_UPDATE, false).setRealm(model.getObject().getFullPath());

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -4956983251621530095L;

            @Override
            public void onClick(final AjaxRequestTarget target, final RealmTO realmTO) {
                wizardBuilder.setParentPath(realmTO.getFullPath());

                send(RealmDirectoryPanel.this,
                        Broadcast.EXACT,
                        new AjaxWizard.NewItemActionEvent<>(new RealmTO(), target) {

                    @Override
                    public String getEventDescription() {
                        return "realm.new";
                    }
                });
            }

            @Override
            protected boolean statusCondition(final RealmTO realmTO) {
                return StringUtils.isNotBlank(realmTO.getKey());
            }
        }, ActionLink.ActionType.CREATE, IdRepoEntitlement.REALM_CREATE, false)
                .setRealm(model.getObject().getFullPath());

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -6115685745909501940L;

            @Override
            public void onClick(final AjaxRequestTarget target, final RealmTO realmTO) {
                target.add(utilityModal.setContent(new RealmPropagationTasks(
                        utilityModal, realmTO.getKey(), pageRef)));
                utilityModal.header(new StringResourceModel("realm.propagation.tasks", model));
                utilityModal.show(true);
            }

            @Override
            protected boolean statusCondition(final RealmTO realmTO) {
                return StringUtils.isNotBlank(realmTO.getKey());
            }
        }, ActionLink.ActionType.PROPAGATION_TASKS, IdRepoEntitlement.TASK_LIST)
                .setRealm(model.getObject().getFullPath());

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = 6267403882972164669L;

            @Override
            public void onClick(final AjaxRequestTarget target, final RealmTO ignore) {
                target.add(altDefaultModal.setContent(new AuditHistoryModal<>(
                        OpEvent.CategoryType.LOGIC,
                        "RealmLogic",
                        model.getObject(),
                        IdRepoEntitlement.REALM_UPDATE,
                        auditRestClient) {

                    private static final long serialVersionUID = -2209244156234833875L;

                    @Override
                    protected void restore(final String json, final AjaxRequestTarget target) {
                        try {
                            RealmTO updated = MAPPER.readValue(json, RealmTO.class);
                            ProvisioningResult<RealmTO> result = restClient.update(updated);
                            model.getObject().setFullPath(result.getEntity().getFullPath());
                            SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                            target.add(container);
                        } catch (Exception e) {
                            LOG.error("While restoring realm {}", model.getObject().getKey(), e);
                            SyncopeConsoleSession.get().onException(e);
                        }
                        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                    }
                }));

                altDefaultModal.header(new StringResourceModel("realm.auditHistory.title", model));
                altDefaultModal.show(true);
            }
        }, ActionLink.ActionType.VIEW_AUDIT_HISTORY,
                String.format("%s,%s", IdRepoEntitlement.REALM_UPDATE, IdRepoEntitlement.AUDIT_LIST)).
                setRealm(model.getObject().getFullPath());

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = 6267403882972164669L;

            @Override
            public void onClick(final AjaxRequestTarget target, final RealmTO realmTO) {
                actionTogglePanel.close(target);
                send(RealmDirectoryPanel.this, Broadcast.BUBBLE,
                        new TemplatesTogglePanel.ShowTemplatesTogglePanelEvent(realmTO, target));
            }
        }, ActionLink.ActionType.TEMPLATE, IdRepoEntitlement.REALM_UPDATE).setRealm(model.getObject().getFullPath());

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -8998343172643697605L;

            @Override
            public void onClick(final AjaxRequestTarget target, final RealmTO realmTO) {
                RealmTO clone = SerializationUtils.clone(realmTO);
                clone.setKey(null);
                clone.setFullPath(null);
                clone.setName(realmTO.getName() + "_clone");
                clone.getActions().clear();

                String parentPath = StringUtils.substringBeforeLast(
                        StringUtils.defaultString(realmTO.getFullPath()), "/");
                wizardBuilder.setParentPath(StringUtils.isBlank(parentPath) ? "/" : parentPath);
                send(RealmDirectoryPanel.this, Broadcast.EXACT, new AjaxWizard.NewItemActionEvent<>(clone, target) {

                    @Override
                    public String getEventDescription() {
                        return "realm.new";
                    }
                });
            }

            @Override
            protected boolean statusCondition(final RealmTO realmTO) {
                return StringUtils.isNotBlank(realmTO.getKey()) && StringUtils.isNotBlank(realmTO.getFullPath());
            }
        }, ActionLink.ActionType.CLONE, IdRepoEntitlement.REALM_CREATE).setRealm(model.getObject().getFullPath());

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = 5649072994971851604L;

            @Override
            public void onClick(final AjaxRequestTarget target, final RealmTO realmTO) {
                try {
                    restClient.delete(realmTO.getFullPath());
                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (Exception e) {
                    LOG.error("While deleting realm {}", realmTO.getFullPath(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }

            @Override
            protected boolean statusCondition(final RealmTO realmTO) {
                return StringUtils.isNotBlank(realmTO.getKey());
            }
        }, ActionLink.ActionType.DELETE, IdRepoEntitlement.REALM_DELETE, true)
                .setRealm(model.getObject().getFullPath());

        return panel;
    }

    @Override
    protected Panel customResultBody(final String panelId, final RealmTO item, final Serializable result) {
        if (!(result instanceof ProvisioningResult<?> provisioningResult)) {
            throw new IllegalStateException("Unsupported result type");
        }

        MultilevelPanel mlp = new MultilevelPanel(panelId);
        add(mlp);

        PropagationStatus syncope = new PropagationStatus();
        syncope.setStatus(ExecStatus.SUCCESS);
        syncope.setResource(Constants.SYNCOPE);

        List<PropagationStatus> propagations = new ArrayList<>();
        propagations.add(syncope);
        propagations.addAll(provisioningResult.getPropagationStatuses());

        ListViewPanel.Builder<PropagationStatus> builder =
                new ListViewPanel.Builder<>(PropagationStatus.class, pageRef) {

            private static final long serialVersionUID = -6809736686861678498L;

            @Override
            protected Component getValueComponent(final String key, final PropagationStatus bean) {
                if ("objectLink".equalsIgnoreCase(key)) {
                    String remoteId = Optional.ofNullable(bean.getAfterObj()).
                            flatMap(afterObj -> afterObj.getAttr(ConnIdSpecialName.NAME).
                            filter(s -> !s.getValues().isEmpty()).map(s -> s.getValues().getFirst())).
                            orElse(StringUtils.EMPTY);

                    return new Label("field", remoteId);
                }

                if ("status".equalsIgnoreCase(key)) {
                    return StatusUtils.getStatusImagePanel("field", bean.getStatus());
                }

                return super.getValueComponent(key, bean);
            }
        };

        builder.setItems(propagations);

        builder.includes("resource", "objectLink", "status");
        builder.withChecks(ListViewPanel.CheckAvailability.NONE);
        builder.setReuseItem(false);

        ActionLink<PropagationStatus> connObjectLink = new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            protected boolean statusCondition(final PropagationStatus bean) {
                return !Constants.SYNCOPE.equals(bean.getResource())
                        && (ExecStatus.CREATED == bean.getStatus()
                        || ExecStatus.SUCCESS == bean.getStatus());
            }

            @Override
            public void onClick(final AjaxRequestTarget target, final PropagationStatus status) {
                mlp.next(status.getResource(), new RemoteRealmPanel(status), target);
            }
        };
        SyncopeWebApplication.get().getStatusProvider().addConnObjectLink(builder, connObjectLink);

        builder.addAction(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            protected boolean statusCondition(final PropagationStatus status) {
                return StringUtils.isNotBlank(status.getFailureReason());
            }

            @Override
            public void onClick(final AjaxRequestTarget target, final PropagationStatus status) {
                mlp.next(status.getResource(), new PropagationErrorPanel(status.getFailureReason()), target);
            }
        }, ActionLink.ActionType.PROPAGATION_TASKS, StringUtils.EMPTY);

        mlp.setFirstLevel(builder.build(MultilevelPanel.FIRST_LEVEL_ID));
        return mlp;
    }

    protected static class RemoteRealmPanel extends RemoteObjectPanel {

        private static final long serialVersionUID = 4303365227411467563L;

        protected final PropagationStatus bean;

        protected RemoteRealmPanel(final PropagationStatus bean) {
            this.bean = bean;
            add(new ConnObjectPanel(
                    REMOTE_OBJECT_PANEL_ID,
                    Pair.of(new ResourceModel("before"), new ResourceModel("after")),
                    getStatusProviderInfo(),
                    false));
        }

        @Override
        protected StatusProvider.Info getStatusProviderInfo() {
            return new StatusProvider.Info(bean.getBeforeObj(), bean.getAfterObj());
        }
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof AjaxWizard.NewItemActionEvent<?> actionEvent
                && actionEvent.getItem() == null) {
            actionEvent.getTarget().ifPresent(target -> {
                wizardBuilder.setParent(null);
                wizardBuilder.setParentPath(StringUtils.isBlank(base.getObject())
                        ? SyncopeConstants.ROOT_REALM
                        : StringUtils.trim(base.getObject()));

                send(this, Broadcast.EXACT, new AjaxWizard.NewItemActionEvent<RealmTO>(new RealmTO(), target) {

                    @Override
                    public String getEventDescription() {
                        return "realm.new";
                    }
                });
            });
            return;
        }

        super.onEvent(event);
    }

    protected class RealmDataProvider extends DirectoryDataProvider<RealmTO> {

        private static final long serialVersionUID = 2400683093229736283L;

        protected String base;

        protected String fiql;

        protected int currentPage;

        protected boolean searchErrorNotified;

        RealmDataProvider(final int paginatorRows) {
            super(paginatorRows);
            base = RealmDirectoryPanel.this.base.getObject();
            fiql = RealmDirectoryPanel.this.fiql.getObject();
        }

        void setBase(final String base) {
            this.base = base;
            this.searchErrorNotified = false;
        }

        void setFIQL(final String fiql) {
            this.fiql = fiql;
            this.searchErrorNotified = false;
        }

        @Override
        public Iterator<RealmTO> iterator(final long first, final long count) {
            try {
                currentPage = ((int) first / paginatorRows);
                if (currentPage < 0) {
                    currentPage = 0;
                }

                RealmQuery query = buildQuery();
                query.setPage(currentPage + 1);
                query.setSize((int) count);
                if (getSort() != null && StringUtils.isNotBlank(getSort().getProperty())) {
                    query.setOrderBy(getSort().getProperty() + (getSort().isAscending() ? " ASC" : " DESC"));
                }

                Iterator<RealmTO> iterator = restClient.search(query).getResult().iterator();
                searchErrorNotified = false;
                return iterator;
            } catch (Exception e) {
                handleSearchException(e);
                return Collections.emptyIterator();
            }
        }

        @Override
        public long size() {
            try {
                RealmQuery query = buildQuery();
                query.setPage(1);
                query.setSize(1);
                long totalCount = restClient.search(query).getTotalCount();
                searchErrorNotified = false;
                return totalCount;
            } catch (Exception e) {
                handleSearchException(e);
                return 0;
            }
        }

        @Override
        public IModel<RealmTO> model(final RealmTO object) {
            return new CompoundPropertyModel<>(object);
        }

        protected RealmQuery buildQuery() {
            RealmQuery query;
            if (StringUtils.isBlank(base)) {
                query = RealmsUtils.buildBaseQuery();
            } else {
                query = new RealmQuery.Builder().base(StringUtils.trim(base)).build();
            }
            if (StringUtils.isNotBlank(fiql)) {
                query.setFiql(StringUtils.trim(fiql));
            }
            return query;
        }

        protected void handleSearchException(final Exception e) {
            LOG.error("While searching realms with base {} and FIQL {}", base, fiql, e);
            if (!searchErrorNotified) {
                searchErrorNotified = true;
                SyncopeConsoleSession.get().error(e.getMessage());
                RequestCycle.get().find(AjaxRequestTarget.class)
                        .ifPresent(target -> ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target));
            }
        }
    }
}
