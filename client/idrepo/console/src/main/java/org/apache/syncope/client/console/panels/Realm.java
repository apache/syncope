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
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.ui.commons.ConnIdSpecialName;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.console.commons.ITabComponent;
import org.apache.syncope.client.ui.commons.status.StatusUtils;
import org.apache.syncope.client.console.layout.AnyLayout;
import org.apache.syncope.client.console.layout.AnyLayoutUtils;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.console.wizards.any.ConnObjectPanel;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.EntityTO;
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
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Realm extends WizardMgtPanel<RealmTO> {

    private static final long serialVersionUID = -1100228004207271270L;

    protected static final Logger LOG = LoggerFactory.getLogger(Realm.class);

    private final RealmTO realmTO;

    private final List<AnyTypeTO> anyTypes;

    protected final RealmWizardBuilder wizardBuilder;

    public Realm(final String id, final RealmTO realmTO, final PageReference pageRef, final int selectedIndex) {
        super(id, true);
        this.realmTO = realmTO;
        this.anyTypes = AnyTypeRestClient.listAnyTypes();

        setPageRef(pageRef);

        AjaxBootstrapTabbedPanel<ITab> tabbedPanel =
                new AjaxBootstrapTabbedPanel<>("tabbedPanel", buildTabList(pageRef));
        tabbedPanel.setSelectedTab(selectedIndex);
        addInnerObject(tabbedPanel);
        this.wizardBuilder = new RealmWizardBuilder(pageRef);
        addNewItemPanelBuilder(this.wizardBuilder, false);

        setShowResultPage(true);

        modal.size(Modal.Size.Large);
        setWindowClosedReloadCallback(modal);
    }

    public RealmTO getRealmTO() {
        return realmTO;
    }

    private List<ITab> buildTabList(final PageReference pageRef) {
        List<ITab> tabs = new ArrayList<>();

        tabs.add(new ITabComponent(new Model<>("DETAILS"),
                IdRepoEntitlement.REALM_CREATE, IdRepoEntitlement.REALM_UPDATE, IdRepoEntitlement.REALM_DELETE) {

            private static final long serialVersionUID = -5861786415855103549L;

            @Override
            public Panel getPanel(final String panelId) {
                final ActionsPanel<RealmTO> actionPanel = new ActionsPanel<>("actions", null);

                if (StringUtils.startsWith(realmTO.getFullPath(), SyncopeConstants.ROOT_REALM)) {
                    actionPanel.add(new ActionLink<>(realmTO) {

                        private static final long serialVersionUID = 2802988981431379827L;

                        @Override
                        public void onClick(final AjaxRequestTarget target, final RealmTO ignore) {
                            onClickCreate(target);
                        }
                    }, ActionLink.ActionType.CREATE, IdRepoEntitlement.REALM_CREATE).hideLabel();

                    actionPanel.add(new ActionLink<>(realmTO) {

                        private static final long serialVersionUID = 2802988981431379828L;

                        @Override
                        public void onClick(final AjaxRequestTarget target, final RealmTO ignore) {
                            onClickEdit(target, realmTO);
                        }
                    }, ActionLink.ActionType.EDIT, IdRepoEntitlement.REALM_UPDATE).hideLabel();

                    actionPanel.add(new ActionLink<>(realmTO) {

                        private static final long serialVersionUID = 2802988981431379827L;

                        @Override
                        public void onClick(final AjaxRequestTarget target, final RealmTO ignore) {
                            onClickTemplate(target);
                        }
                    }, ActionLink.ActionType.TEMPLATE, IdRepoEntitlement.REALM_UPDATE).hideLabel();

                    actionPanel.add(new ActionLink<>(realmTO) {

                        private static final long serialVersionUID = 2802988981431379829L;

                        @Override
                        public void onClick(final AjaxRequestTarget target, final RealmTO ignore) {
                            onClickDelete(target, realmTO);
                        }
                    }, ActionLink.ActionType.DELETE, IdRepoEntitlement.REALM_DELETE, true).hideLabel();
                }

                RealmDetails panel = new RealmDetails(panelId, realmTO, actionPanel, false);
                panel.setContentEnabled(false);
                actionPanel.setEnabled(true);
                return panel;
            }

            @Override
            public boolean isVisible() {
                return SyncopeWebApplication.get().getSecuritySettings().getAuthorizationStrategy().
                        isActionAuthorized(this, RENDER);
            }
        });

        AnyLayout anyLayout = AnyLayoutUtils.fetch(
                anyTypes.stream().map(EntityTO::getKey).collect(Collectors.toList()));
        for (AnyTypeTO anyType : anyTypes) {
            tabs.add(new ITabComponent(new Model<>(anyType.getKey()), String.format("%s_SEARCH", anyType.getKey())) {

                private static final long serialVersionUID = 1169585538404171118L;

                @Override
                public WebMarkupContainer getPanel(final String panelId) {
                    return AnyLayoutUtils.newAnyPanel(
                            anyLayout.getAnyPanelClass(), panelId, anyType, realmTO, anyLayout, true, pageRef);
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
        if (!(result instanceof ProvisioningResult)) {
            throw new IllegalStateException("Unsupported result type");
        }

        final MultilevelPanel mlp = new MultilevelPanel(panelId);
        add(mlp);

        final PropagationStatus syncope = new PropagationStatus();
        syncope.setStatus(ExecStatus.SUCCESS);
        syncope.setResource(Constants.SYNCOPE);

        List<PropagationStatus> propagations = new ArrayList<>();
        propagations.add(syncope);
        propagations.addAll(((ProvisioningResult) result).getPropagationStatuses());

        ListViewPanel.Builder<PropagationStatus> builder = new ListViewPanel.Builder<>(
            PropagationStatus.class, pageRef) {

            private static final long serialVersionUID = -6809736686861678498L;

            @Override
            protected Component getValueComponent(final String key, final PropagationStatus bean) {
                if ("afterObj".equalsIgnoreCase(key)) {
                    ConnObjectTO afterObj = bean.getAfterObj();
                    String remoteId = afterObj == null
                        || afterObj.getAttrs().isEmpty()
                        || afterObj.getAttr(ConnIdSpecialName.NAME).isEmpty()
                        || afterObj.getAttr(ConnIdSpecialName.NAME).get().getValues() == null
                        || afterObj.getAttr(ConnIdSpecialName.NAME).get().getValues().isEmpty()
                        ? StringUtils.EMPTY
                        : afterObj.getAttr(ConnIdSpecialName.NAME).get().getValues().get(0);

                    return new Label("field", remoteId);
                } else if ("status".equalsIgnoreCase(key)) {
                    return StatusUtils.getStatusImagePanel("field", bean.getStatus());
                } else {
                    return super.getValueComponent(key, bean);
                }
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

    static class RemoteRealmPanel extends RemoteObjectPanel {

        private static final long serialVersionUID = 4303365227411467563L;

        private final PropagationStatus bean;

        RemoteRealmPanel(final PropagationStatus bean) {
            this.bean = bean;
            add(new ConnObjectPanel(
                    REMOTE_OBJECT_PANEL_ID,
                    Pair.<IModel<?>, IModel<?>>of(new ResourceModel("before"), new ResourceModel("after")),
                    getConnObjectTOs(),
                    false));
        }

        @Override
        protected final Pair<ConnObjectTO, ConnObjectTO> getConnObjectTOs() {
            return Pair.of(bean.getBeforeObj(), bean.getAfterObj());
        }
    }
}
