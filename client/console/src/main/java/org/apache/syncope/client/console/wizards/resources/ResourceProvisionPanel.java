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
package org.apache.syncope.client.console.wizards.resources;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.AbstractModalPanel;
import org.apache.syncope.client.console.panels.ListViewPanel;
import org.apache.syncope.client.console.panels.ListViewPanel.ListViewReload;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksTogglePanel;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.ItemTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

public class ResourceProvisionPanel extends AbstractModalPanel<Serializable> {

    private static final long serialVersionUID = -7982691107029848579L;

    private final ResourceRestClient resourceRestClient = new ResourceRestClient();

    private final ResourceTO resourceTO;

    private final List<ResourceProvision> provisions;

    private final ObjectTypeTogglePanel objectTypeTogglePanel;

    private final WizardMgtPanel<ResourceProvision> list;

    private final ProvisionWizardBuilder wizard;

    private final AjaxLink<ResourceProvision> addAjaxLink;

    protected ActionLinksTogglePanel<ResourceProvision> actionTogglePanel;

    public ResourceProvisionPanel(
            final BaseModal<Serializable> modal,
            final ResourceTO resourceTO,
            final String adminRealm,
            final PageReference pageRef) {

        super(modal, pageRef);
        this.resourceTO = resourceTO;

        setOutputMarkupId(true);

        actionTogglePanel = new ActionLinksTogglePanel<>("toggle", pageRef);
        add(actionTogglePanel);

        wizard = new ProvisionWizardBuilder(resourceTO, pageRef);

        final ListViewPanel.Builder<ResourceProvision> builder = new ListViewPanel.Builder<ResourceProvision>(
                ResourceProvision.class, pageRef) {

            private static final long serialVersionUID = 4907732721283972943L;

            @Override
            protected ResourceProvision getActualItem(
                    final ResourceProvision item, final List<ResourceProvision> list) {

                return item == null
                        ? null
                        : list.stream().filter(in -> ((item.getKey() == null && in.getKey() == null)
                        || (in.getKey() != null && in.getKey().equals(item.getKey())))
                        && ((item.getAnyType() == null && in.getAnyType() == null)
                        || (in.getAnyType() != null && in.getAnyType().equals(item.getAnyType())))).
                                findAny().orElse(null);
            }

            @Override
            protected void customActionCallback(final AjaxRequestTarget target) {
                // change modal footer visibility
                send(ResourceProvisionPanel.this, Broadcast.BUBBLE, new BaseModal.ChangeFooterVisibilityEvent(target));
            }

            @Override
            protected void customActionOnCancelCallback(final AjaxRequestTarget target) {
                // change modal footer visibility
                send(ResourceProvisionPanel.this, Broadcast.BUBBLE, new BaseModal.ChangeFooterVisibilityEvent(target));
            }

            @Override
            protected void customActionOnFinishCallback(final AjaxRequestTarget target) {
                checkAddButton(adminRealm);

                // keep list ordered - SYNCOPE-1154
                sortProvisions();

                // change modal footer visibility
                send(ResourceProvisionPanel.this, Broadcast.BUBBLE, new BaseModal.ChangeFooterVisibilityEvent(target));
            }

            @Override
            protected ActionLinksTogglePanel<ResourceProvision> getTogglePanel() {
                return actionTogglePanel;
            }
        };

        provisions = new ArrayList<>();
        if (resourceTO.getOrgUnit() != null) {
            provisions.add(new ResourceProvision(resourceTO.getOrgUnit()));
        }
        resourceTO.getProvisions().forEach(provision -> {
            provisions.add(new ResourceProvision(provision));
        });
        // keep list ordered - SYNCOPE-1154
        sortProvisions();

        builder.setItems(provisions);
        builder.includes("anyType", "objectClass", "auxClasses");
        builder.setReuseItem(false);

        builder.addAction(new ActionLink<ResourceProvision>() {

            private static final long serialVersionUID = -3722207913631435504L;

            @Override
            public void onClick(final AjaxRequestTarget target, final ResourceProvision provision) {
                try {
                    send(ResourceProvisionPanel.this, Broadcast.DEPTH,
                            new AjaxWizard.NewItemActionEvent<>(provision, 1, target).setResourceModel(
                                    new StringResourceModel("inner.provision.mapping",
                                            ResourceProvisionPanel.this,
                                            Model.of(provision))));
                } catch (SyncopeClientException e) {
                    LOG.error("While contacting resource", e);
                    SyncopeConsoleSession.get().error(
                            StringUtils.isBlank(e.getMessage()) ? e.getClass().getName() : e.getMessage());
                    ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                }
            }
        }, ActionLink.ActionType.MAPPING, StandardEntitlement.RESOURCE_READ).
                addAction(new ActionLink<ResourceProvision>() {

                    private static final long serialVersionUID = -7780999687733432439L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final ResourceProvision provision) {
                        try {
                            resourceRestClient.setLatestSyncToken(resourceTO.getKey(), provision.getAnyType());
                            SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                        } catch (Exception e) {
                            LOG.error("While setting latest sync token for {}/{}",
                                    resourceTO.getKey(), provision.getAnyType(), e);
                            SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage())
                                    ? e.getClass().getName() : e.getMessage());
                        }
                        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                    }
                }, ActionLink.ActionType.SET_LATEST_SYNC_TOKEN, StandardEntitlement.RESOURCE_UPDATE).
                addAction(new ActionLink<ResourceProvision>() {

                    private static final long serialVersionUID = -7780999687733432439L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final ResourceProvision provision) {
                        try {
                            resourceRestClient.removeSyncToken(resourceTO.getKey(), provision.getAnyType());
                            SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                        } catch (Exception e) {
                            LOG.error("While removing sync token for {}/{}",
                                    resourceTO.getKey(), provision.getAnyType(), e);
                            SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage())
                                    ? e.getClass().getName() : e.getMessage());
                        }
                        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                    }
                }, ActionLink.ActionType.REMOVE_SYNC_TOKEN, StandardEntitlement.RESOURCE_UPDATE).
                addAction(new ActionLink<ResourceProvision>() {

                    private static final long serialVersionUID = -3722207913631435544L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final ResourceProvision provision) {
                        if (provision.getOrgUnitTO() != null) {
                            resourceTO.setOrgUnit(null);
                        } else if (provision.getProvisionTO() != null) {
                            resourceTO.getProvisions().remove(provision.getProvisionTO());
                        }
                        provisions.remove(provision);
                        checkAddButton(adminRealm);
                        send(ResourceProvisionPanel.this, Broadcast.DEPTH, new ListViewReload<>(target));
                    }
                }, ActionLink.ActionType.DELETE, StandardEntitlement.RESOURCE_UPDATE, true);

        builder.addNewItemPanelBuilder(wizard);

        list = builder.build("provision");
        list.setReadOnly(!SyncopeConsoleSession.get().owns(StandardEntitlement.RESOURCE_UPDATE, adminRealm));

        addAjaxLink = new AjaxLink<ResourceProvision>("add") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                send(ResourceProvisionPanel.this, Broadcast.BREADTH,
                        new ActionLinksTogglePanel.ActionLinkToggleCloseEventPayload(target));
                objectTypeTogglePanel.setHeaderLabel(target);
                objectTypeTogglePanel.toggle(target, true);
            }
        };
        list.addOrReplaceInnerObject(addAjaxLink);
        add(list);

        // ----------------------------------------------------------------------
        // toggle panel, used to choose 'type' before starting wizard - SYNCOPE-1167
        final ResourceProvision provision = new ResourceProvision();
        provision.setAnyType("");
        objectTypeTogglePanel = new ObjectTypeTogglePanel("objectTypeToggle", provision, getAnyTypes(), pageRef) {

            private static final long serialVersionUID = 7878063325027015067L;

            @Override
            protected void onSubmit(final String type, final AjaxRequestTarget target) {
                provision.setAnyType(type);

                send(list, Broadcast.BREADTH,
                        new ActionLinksTogglePanel.ActionLinkToggleCloseEventPayload(target));
                send(list, Broadcast.DEPTH,
                        new AjaxWizard.NewItemActionEvent<>(provision, target));

                wizard.setObjectClassModelObject(type);
            }

        };
        checkAddButton(adminRealm);
        add(objectTypeTogglePanel);
    }

    private void checkConnObjectKeyCount(final String anyType, final List<ItemTO> items) {
        long connObjectKeyCount = items.stream().filter(ItemTO::isConnObjectKey).count();

        if (connObjectKeyCount != 1) {
            throw new IllegalArgumentException(anyType + ": "
                    + new StringResourceModel("connObjectKeyValidation", ResourceProvisionPanel.this).getString());
        }
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
        try {
            if (resourceTO.getOrgUnit() != null) {
                checkConnObjectKeyCount(SyncopeConstants.REALM_ANYTYPE, resourceTO.getOrgUnit().getItems());
            }

            new ArrayList<>(resourceTO.getProvisions()).stream().
                    filter(provision -> provision != null).
                    forEachOrdered(provision -> {
                        if (provision.getMapping() == null || provision.getMapping().getItems().isEmpty()) {
                            resourceTO.getProvisions().remove(provision);
                        } else {
                            checkConnObjectKeyCount(provision.getAnyType(), provision.getMapping().getItems());
                        }
                    });

            resourceRestClient.update(resourceTO);
            SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
            modal.close(target);
        } catch (Exception e) {
            LOG.error("While creating or updating {}", resourceTO, e);
            SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage())
                    ? e.getClass().getName() : e.getMessage());
        }
        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
    }

    private void sortProvisions() {
        Collections.sort(provisions, (o1, o2)
                -> new AnyTypeRestClient.AnyTypeKeyComparator().compare(o1.getAnyType(), o2.getAnyType()));
    }

    private LoadableDetachableModel<List<String>> getAnyTypes() {
        return new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                List<String> anyTypes = new AnyTypeRestClient().list().stream().
                        filter(anyType -> !resourceTO.getProvision(anyType).isPresent()).
                        collect(Collectors.toList());
                if (resourceTO.getOrgUnit() == null) {
                    anyTypes.add(SyncopeConstants.REALM_ANYTYPE);
                }

                Collections.sort(anyTypes, new AnyTypeRestClient.AnyTypeKeyComparator());
                return anyTypes;
            }
        };
    }

    private void checkAddButton(final String adminRealm) {
        boolean enabled = SyncopeConsoleSession.get().owns(StandardEntitlement.RESOURCE_UPDATE, adminRealm)
                && !getAnyTypes().getObject().isEmpty();
        addAjaxLink.setVisible(enabled);
        objectTypeTogglePanel.setEnabled(enabled);
    }
}
