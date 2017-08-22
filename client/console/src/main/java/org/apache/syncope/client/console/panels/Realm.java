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
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.client.console.SyncopeConsoleApplication;
import org.apache.syncope.client.console.commons.ConnIdSpecialName;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.ITabComponent;
import org.apache.syncope.client.console.commons.status.StatusUtils;
import org.apache.syncope.client.console.layout.AnyObjectFormLayoutInfo;
import org.apache.syncope.client.console.layout.FormLayoutInfoUtils;
import org.apache.syncope.client.console.layout.GroupFormLayoutInfo;
import org.apache.syncope.client.console.layout.UserFormLayoutInfo;
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
import org.apache.syncope.common.lib.types.PropagationTaskExecStatus;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
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
        this.anyTypes = new AnyTypeRestClient().listAnyTypes();

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
                StandardEntitlement.REALM_CREATE, StandardEntitlement.REALM_UPDATE, StandardEntitlement.REALM_DELETE) {

            private static final long serialVersionUID = -5861786415855103549L;

            @Override
            public Panel getPanel(final String panelId) {
                final ActionsPanel<RealmTO> actionPanel = new ActionsPanel<>("actions", null);

                if (realmTO.getFullPath().startsWith(SyncopeConstants.ROOT_REALM)) {

                    actionPanel.add(new ActionLink<RealmTO>(realmTO) {

                        private static final long serialVersionUID = 2802988981431379827L;

                        @Override
                        public void onClick(final AjaxRequestTarget target, final RealmTO ignore) {
                            onClickCreate(target);
                        }
                    }, ActionLink.ActionType.CREATE, StandardEntitlement.REALM_CREATE).hideLabel();

                    actionPanel.add(new ActionLink<RealmTO>(realmTO) {

                        private static final long serialVersionUID = 2802988981431379828L;

                        @Override
                        public void onClick(final AjaxRequestTarget target, final RealmTO ignore) {
                            onClickEdit(target, realmTO);
                        }
                    }, ActionLink.ActionType.EDIT, StandardEntitlement.REALM_UPDATE).hideLabel();

                    actionPanel.add(new ActionLink<RealmTO>(realmTO) {

                        private static final long serialVersionUID = 2802988981431379827L;

                        @Override
                        public void onClick(final AjaxRequestTarget target, final RealmTO ignore) {
                            onClickTemplate(target);
                        }
                    }, ActionLink.ActionType.TEMPLATE, StandardEntitlement.REALM_UPDATE).hideLabel();

                    actionPanel.add(new ActionLink<RealmTO>(realmTO) {

                        private static final long serialVersionUID = 2802988981431379829L;

                        @Override
                        public void onClick(final AjaxRequestTarget target, final RealmTO ignore) {
                            onClickDelete(target, realmTO);
                        }
                    }, ActionLink.ActionType.DELETE, StandardEntitlement.REALM_DELETE, true).hideLabel();
                }

                RealmDetails panel = new RealmDetails(panelId, realmTO, actionPanel, false);
                panel.setContentEnabled(false);
                actionPanel.setEnabled(true);
                return panel;
            }

            @Override
            public boolean isVisible() {
                return SyncopeConsoleApplication.get().getSecuritySettings().getAuthorizationStrategy().
                        isActionAuthorized(this, RENDER);
            }
        });

        final Triple<UserFormLayoutInfo, GroupFormLayoutInfo, Map<String, AnyObjectFormLayoutInfo>> formLayoutInfo =
                FormLayoutInfoUtils.fetch(anyTypes.stream().map(EntityTO::getKey).collect(Collectors.toList()));

        for (final AnyTypeTO anyType : anyTypes) {
            tabs.add(new ITabComponent(
                    new Model<>(anyType.getKey()),
                    StandardEntitlement.ANYTYPE_READ, String.format("%s_SEARCH", anyType)) {

                private static final long serialVersionUID = 1169585538404171118L;

                @Override
                public WebMarkupContainer getPanel(final String panelId) {
                    return new AnyPanel(panelId, anyType, realmTO, formLayoutInfo, true, pageRef);
                }

                @Override
                public boolean isVisible() {
                    return SyncopeConsoleApplication.get().getSecuritySettings().getAuthorizationStrategy().
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
        syncope.setStatus(PropagationTaskExecStatus.SUCCESS);
        syncope.setResource(Constants.SYNCOPE);

        ArrayList<PropagationStatus> propagations = new ArrayList<>();
        propagations.add(syncope);
        propagations.addAll(((ProvisioningResult) result).getPropagationStatuses());

        ListViewPanel.Builder<PropagationStatus> builder = new ListViewPanel.Builder<PropagationStatus>(
                PropagationStatus.class, pageRef) {

            private static final long serialVersionUID = -6809736686861678498L;

            @Override
            protected Component getValueComponent(final String key, final PropagationStatus bean) {
                if ("afterObj".equalsIgnoreCase(key)) {
                    ConnObjectTO afterObj = bean.getAfterObj();
                    String remoteId = afterObj == null
                            || afterObj.getAttrs().isEmpty()
                            || !afterObj.getAttr(ConnIdSpecialName.NAME).isPresent()
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

        builder.addAction(new ActionLink<PropagationStatus>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            protected boolean statusCondition(final PropagationStatus bean) {
                return !Constants.SYNCOPE.equals(bean.getResource())
                        && (PropagationTaskExecStatus.CREATED == bean.getStatus()
                        || PropagationTaskExecStatus.SUCCESS == bean.getStatus());
            }

            @Override
            public void onClick(final AjaxRequestTarget target, final PropagationStatus bean) {
                mlp.next(bean.getResource(), new RemoteRealmPanel(bean), target);
            }
        }, ActionLink.ActionType.VIEW, StandardEntitlement.RESOURCE_GET_CONNOBJECT);

        mlp.setFirstLevel(builder.build(MultilevelPanel.FIRST_LEVEL_ID));
        return mlp;
    }

    protected abstract void onClickTemplate(final AjaxRequestTarget target);

    protected abstract void onClickCreate(final AjaxRequestTarget target);

    protected abstract void onClickEdit(final AjaxRequestTarget target, final RealmTO realmTO);

    protected abstract void onClickDelete(final AjaxRequestTarget target, final RealmTO realmTO);

    public class RemoteRealmPanel extends RemoteObjectPanel {

        private static final long serialVersionUID = 4303365227411467563L;

        private final PropagationStatus bean;

        public RemoteRealmPanel(final PropagationStatus bean) {
            this.bean = bean;
            add(new ConnObjectPanel(REMOTE_OBJECT_PANEL_ID, getConnObjectTO(), false));
        }

        @Override
        protected final Pair<ConnObjectTO, ConnObjectTO> getConnObjectTO() {
            return Pair.of(bean.getBeforeObj(), bean.getAfterObj());
        }
    }
}
