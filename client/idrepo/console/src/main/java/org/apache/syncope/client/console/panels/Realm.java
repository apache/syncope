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
import de.agilecoders.wicket.core.markup.html.bootstrap.tabs.AjaxBootstrapTabbedPanel;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.commons.ITabComponent;
import org.apache.syncope.client.console.layout.AnyLayout;
import org.apache.syncope.client.console.layout.AnyLayoutUtils;
import org.apache.syncope.client.console.rest.RealmRestClient;
import org.apache.syncope.client.console.rest.RoleRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.console.wizards.any.ConnObjectPanel;
import org.apache.syncope.client.ui.commons.ConnIdSpecialName;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.status.StatusUtils;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.ConnObject;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Realm extends WizardMgtPanel<RealmTO> {

    private static final long serialVersionUID = -1100228004207271270L;

    protected static final Logger LOG = LoggerFactory.getLogger(Realm.class);

    @SpringBean
    protected RoleRestClient roleRestClient;

    @SpringBean
    protected RealmRestClient realmRestClient;

    protected final RealmTO realmTO;

    protected final List<AnyTypeTO> anyTypes;

    protected final int selectedIndex;

    protected final RealmWizardBuilder wizardBuilder;

    public Realm(
            final String id,
            final RealmTO realmTO,
            final List<AnyTypeTO> anyTypes,
            final int selectedIndex,
            final PageReference pageRef) {

        super(id, true);
        this.realmTO = realmTO;
        this.anyTypes = anyTypes;
        this.selectedIndex = selectedIndex;

        setPageRef(pageRef);

        addInnerObject(new AjaxBootstrapTabbedPanel<>("tabbedPanel", buildTabList(pageRef)).
                setSelectedTab(selectedIndex));

        this.wizardBuilder = buildNewItemPanelBuilder(pageRef);
        addNewItemPanelBuilder(this.wizardBuilder, false);

        setShowResultPanel(true);

        modal.size(Modal.Size.Large);
        setWindowClosedReloadCallback(modal);
    }

    protected RealmWizardBuilder buildNewItemPanelBuilder(final PageReference pageRef) {
        return new RealmWizardBuilder(realmRestClient, pageRef);
    }

    public RealmTO getRealmTO() {
        return realmTO;
    }

    protected List<ITab> buildTabList(final PageReference pageRef) {
        List<ITab> tabs = new ArrayList<>();

        tabs.add(new RealmDetailsTabPanel());

        AnyLayout anyLayout = AnyLayoutUtils.fetch(
                roleRestClient,
                anyTypes.stream().map(AnyTypeTO::getKey).collect(Collectors.toList()));
        for (AnyTypeTO anyType : anyTypes) {
            tabs.add(new ITabComponent(
                    new ResourceModel("anyType." + anyType.getKey(), anyType.getKey()),
                    String.format("%s_SEARCH", anyType.getKey())) {

                private static final long serialVersionUID = 1169585538404171118L;

                @Override
                public WebMarkupContainer getPanel(final String panelId) {
                    return new AnyPanel.Builder<>(
                            anyLayout.getAnyPanelClass(), panelId, anyType, realmTO, anyLayout, true, pageRef).build();
                }

                @Override
                public boolean isVisible() {
                    return SyncopeWebApplication.get().getSecuritySettings().getAuthorizationStrategy().
                            isActionAuthorized(this, RENDER);
                }
            });
        }

        return tabs;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Panel customResultBody(final String panelId, final RealmTO item, final Serializable result) {
        if (!(result instanceof final ProvisioningResult provisioningResult)) {
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
                if ("afterObj".equalsIgnoreCase(key)) {
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

        builder.includes("resource", "afterObj", "status");
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

    protected abstract void onClickTemplate(AjaxRequestTarget target);

    protected abstract void onClickCreate(AjaxRequestTarget target);

    protected abstract void onClickEdit(AjaxRequestTarget target, RealmTO realmTO);

    protected abstract void onClickDelete(AjaxRequestTarget target, RealmTO realmTO);

    protected static class RemoteRealmPanel extends RemoteObjectPanel {

        private static final long serialVersionUID = 4303365227411467563L;

        protected final PropagationStatus bean;

        protected RemoteRealmPanel(final PropagationStatus bean) {
            this.bean = bean;
            add(new ConnObjectPanel(
                    REMOTE_OBJECT_PANEL_ID,
                    Pair.of(new ResourceModel("before"), new ResourceModel("after")),
                    getConnObjectTOs(),
                    false));
        }

        @Override
        protected final Pair<ConnObject, ConnObject> getConnObjectTOs() {
            return Pair.of(bean.getBeforeObj(), bean.getAfterObj());
        }
    }

    protected class RealmDetailsTabPanel extends ITabComponent {

        private static final long serialVersionUID = -5861786415855103549L;

        protected RealmDetailsTabPanel() {
            super(new ResourceModel("realm.details", "DETAILS"),
                    IdRepoEntitlement.REALM_CREATE, IdRepoEntitlement.REALM_UPDATE, IdRepoEntitlement.REALM_DELETE);
        }

        @Override
        public Panel getPanel(final String panelId) {
            ActionsPanel<RealmTO> actionPanel = new ActionsPanel<>("actions", null);
            if (securityCheck(Set.of(
                    IdRepoEntitlement.REALM_CREATE,
                    IdRepoEntitlement.REALM_UPDATE,
                    IdRepoEntitlement.REALM_DELETE))) {

                if (securityCheck(Set.of(IdRepoEntitlement.REALM_CREATE))) {
                    actionPanel.add(new ActionLink<>(realmTO) {

                        private static final long serialVersionUID = 2802988981431379827L;

                        @Override
                        public void onClick(final AjaxRequestTarget target, final RealmTO ignore) {
                            onClickCreate(target);
                        }
                    }, ActionLink.ActionType.CREATE, IdRepoEntitlement.REALM_CREATE).hideLabel();
                }

                if (securityCheck(Set.of(IdRepoEntitlement.REALM_UPDATE))) {
                    actionPanel.add(new ActionLink<>(realmTO) {

                        private static final long serialVersionUID = 2802988981431379828L;

                        @Override
                        public void onClick(final AjaxRequestTarget target, final RealmTO ignore) {
                            onClickEdit(target, realmTO);
                        }
                    }, ActionLink.ActionType.EDIT, IdRepoEntitlement.REALM_UPDATE).hideLabel();
                }

                if (securityCheck(Set.of(IdRepoEntitlement.REALM_UPDATE))) {
                    actionPanel.add(new ActionLink<>(realmTO) {

                        private static final long serialVersionUID = 2802988981431379827L;

                        @Override
                        public void onClick(final AjaxRequestTarget target, final RealmTO ignore) {
                            onClickTemplate(target);
                        }
                    }, ActionLink.ActionType.TEMPLATE, IdRepoEntitlement.REALM_UPDATE).hideLabel();
                }

                if (securityCheck(Set.of(IdRepoEntitlement.REALM_DELETE))) {
                    actionPanel.add(new ActionLink<>(realmTO) {

                        private static final long serialVersionUID = 2802988981431379829L;

                        @Override
                        public void onClick(final AjaxRequestTarget target, final RealmTO ignore) {
                            onClickDelete(target, realmTO);
                        }
                    }, ActionLink.ActionType.DELETE, IdRepoEntitlement.REALM_DELETE, true).hideLabel();
                }
            }

            RealmDetails panel = new RealmDetails(panelId, realmTO, actionPanel, false);
            panel.setContentEnabled(false);
            actionPanel.setEnabled(true);
            return panel;
        }

        @Override
        public boolean isVisible() {
            return SyncopeWebApplication.get().getSecuritySettings().
                    getAuthorizationStrategy().isActionAuthorized(this, RENDER);
        }

        private boolean securityCheck(final Set<String> entitlements) {
            return entitlements.stream().anyMatch(e -> SyncopeConsoleSession.get().owns(e, realmTO.getFullPath()));
        }
    }
}
