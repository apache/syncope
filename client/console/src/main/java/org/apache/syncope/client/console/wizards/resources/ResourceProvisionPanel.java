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
import java.util.Collection;
import java.util.List;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.AbstractModalPanel;
import org.apache.syncope.client.console.panels.ListViewPanel;
import org.apache.syncope.client.console.panels.ListViewPanel.ListViewReload;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.wicket.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.common.lib.to.MappingItemTO;
import org.apache.syncope.common.lib.to.ProvisionTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.OrgUnitTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

public class ResourceProvisionPanel extends AbstractModalPanel<Serializable> {

    private static final long serialVersionUID = -7982691107029848579L;

    private final ConnectorRestClient connectorRestClient = new ConnectorRestClient();

    private final ResourceRestClient resourceRestClient = new ResourceRestClient();

    private final ResourceTO resourceTO;

    private Model<OrgUnitTO> baseModel;

    private final WebMarkupContainer aboutRealmProvison;

    public ResourceProvisionPanel(
            final BaseModal<Serializable> modal,
            final ResourceTO resourceTO,
            final PageReference pageRef) {

        super(modal, pageRef);
        this.resourceTO = resourceTO;

        baseModel = Model.of(resourceTO.getOrgUnit() == null ? new OrgUnitTO() : resourceTO.getOrgUnit());

        setOutputMarkupId(true);

        // ----------------------------------------------------------------------
        // Realms provisioning
        // ----------------------------------------------------------------------
        aboutRealmProvison = new WebMarkupContainer("aboutRealmProvison");
        aboutRealmProvison.setOutputMarkupPlaceholderTag(true);
        add(aboutRealmProvison);

        boolean realmProvisionEnabled = resourceTO.getOrgUnit() != null;

        final AjaxCheckBoxPanel enableRealmsProvision = new AjaxCheckBoxPanel(
                "enableRealmsProvision",
                "enableRealmsProvision",
                Model.of(realmProvisionEnabled),
                false);
        aboutRealmProvison.add(enableRealmsProvision);
        enableRealmsProvision.setIndex(1).setTitle(getString("enableRealmsProvision.title"));

        final WebMarkupContainer realmsProvisionContainer = new WebMarkupContainer("realmsProvisionContainer");
        realmsProvisionContainer.setOutputMarkupPlaceholderTag(true);
        realmsProvisionContainer.setEnabled(realmProvisionEnabled).setVisible(realmProvisionEnabled);
        aboutRealmProvison.add(realmsProvisionContainer);

        final AjaxTextFieldPanel objectClass = new AjaxTextFieldPanel(
                "objectClass",
                getString("objectClass"),
                new PropertyModel<String>(baseModel.getObject(), "objectClass"),
                false);
        realmsProvisionContainer.add(objectClass.addRequiredLabel());

        final AjaxTextFieldPanel extAttrName = new AjaxTextFieldPanel(
                "extAttrName",
                getString("extAttrName"),
                new PropertyModel<String>(baseModel.getObject(), "extAttrName"),
                false);
        if (resourceTO.getOrgUnit() != null) {
            extAttrName.setChoices(connectorRestClient.getExtAttrNames(
                    resourceTO.getOrgUnit().getObjectClass(),
                    resourceTO.getConnector(),
                    resourceTO.getConfOverride()));
        }
        realmsProvisionContainer.add(extAttrName.addRequiredLabel());

        objectClass.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_BLUR) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                extAttrName.setChoices(connectorRestClient.getExtAttrNames(
                        objectClass.getModelObject(),
                        resourceTO.getConnector(),
                        resourceTO.getConfOverride()));
                target.focusComponent(extAttrName);
            }
        });

        final AjaxTextFieldPanel connObjectLink = new AjaxTextFieldPanel(
                "connObjectLink",
                new ResourceModel("connObjectLink", "connObjectLink").getObject(),
                new PropertyModel<String>(baseModel.getObject(), "connObjectLink"),
                false);
        realmsProvisionContainer.add(connObjectLink.addRequiredLabel());

        enableRealmsProvision.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                boolean realmProvisionEnabled = enableRealmsProvision.getModelObject();
                realmsProvisionContainer.setEnabled(realmProvisionEnabled).setVisible(realmProvisionEnabled);
                target.add(realmsProvisionContainer);

                if (realmProvisionEnabled) {
                    resourceTO.setOrgUnit(baseModel.getObject());
                } else {
                    resourceTO.setOrgUnit(null);
                }

            }
        });
        // ----------------------------------------------------------------------

        final ProvisionWizardBuilder wizard = new ProvisionWizardBuilder(resourceTO, pageRef);

        final ListViewPanel.Builder<ProvisionTO> builder = new ListViewPanel.Builder<ProvisionTO>(
                ProvisionTO.class, pageRef) {

            private static final long serialVersionUID = 4907732721283972943L;

            @Override
            protected ProvisionTO getActualItem(final ProvisionTO item, final List<ProvisionTO> list) {
                return item == null
                        ? null
                        : IteratorUtils.find(list.iterator(), new Predicate<ProvisionTO>() {

                            @Override
                            public boolean evaluate(final ProvisionTO in) {
                                return ((item.getKey() == null && in.getKey() == null)
                                        || (in.getKey() != null && in.getKey().equals(item.getKey())))
                                        && ((item.getAnyType() == null && in.getAnyType() == null)
                                        || (in.getAnyType() != null && in.getAnyType().equals(item.getAnyType())));
                            }
                        });
            }

            @Override
            protected void customActionCallback(final AjaxRequestTarget target) {
                // change modal foter visibility
                send(ResourceProvisionPanel.this, Broadcast.BUBBLE, new BaseModal.ChangeFooterVisibilityEvent(target));
            }

            @Override
            protected void customActionOnCancelCallback(final AjaxRequestTarget target) {
                ResourceProvisionPanel.this.aboutRealmProvison.setVisible(true);
                target.add(ResourceProvisionPanel.this.aboutRealmProvison);

                // change modal foter visibility
                send(ResourceProvisionPanel.this, Broadcast.BUBBLE, new BaseModal.ChangeFooterVisibilityEvent(target));
            }

            @Override
            protected void customActionOnFinishCallback(final AjaxRequestTarget target) {
                ResourceProvisionPanel.this.aboutRealmProvison.setVisible(true);
                target.add(ResourceProvisionPanel.this.aboutRealmProvison);

                // change modal foter visibility
                send(ResourceProvisionPanel.this, Broadcast.BUBBLE, new BaseModal.ChangeFooterVisibilityEvent(target));
            }
        };

        builder.setItems(resourceTO.getProvisions());
        builder.includes("anyType", "objectClass", "auxClasses");
        builder.setReuseItem(false);

        builder.
                addAction(new ActionLink<ProvisionTO>() {

                    private static final long serialVersionUID = -3722207913631435504L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final ProvisionTO provisionTO) {
                        send(ResourceProvisionPanel.this, Broadcast.DEPTH,
                                new AjaxWizard.NewItemActionEvent<>(provisionTO, 2, target).setResourceModel(
                                        new StringResourceModel("inner.provision.mapping",
                                                ResourceProvisionPanel.this,
                                                Model.of(provisionTO))));
                    }
                }, ActionLink.ActionType.MAPPING, StandardEntitlement.RESOURCE_UPDATE).
                addAction(new ActionLink<ProvisionTO>() {

                    private static final long serialVersionUID = -7780999687733432439L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final ProvisionTO provisionTO) {
                        try {
                            resourceRestClient.setLatestSyncToken(resourceTO.getKey(), provisionTO.getAnyType());
                            SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                        } catch (Exception e) {
                            LOG.error("While setting latest sync token for {}/{}",
                                    resourceTO.getKey(), provisionTO.getAnyType(), e);
                            SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage()) ? e.getClass().
                                    getName() : e.getMessage());
                        }
                        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                    }
                }, ActionLink.ActionType.SET_LATEST_SYNC_TOKEN, StandardEntitlement.RESOURCE_UPDATE).
                addAction(new ActionLink<ProvisionTO>() {

                    private static final long serialVersionUID = -7780999687733432439L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final ProvisionTO provisionTO) {
                        try {
                            resourceRestClient.removeSyncToken(resourceTO.getKey(), provisionTO.getAnyType());
                            SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                        } catch (Exception e) {
                            LOG.error("While removing sync token for {}/{}",
                                    resourceTO.getKey(), provisionTO.getAnyType(), e);
                            SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage()) ? e.getClass().
                                    getName() : e.getMessage());
                        }
                        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                    }
                }, ActionLink.ActionType.REMOVE_SYNC_TOKEN, StandardEntitlement.RESOURCE_UPDATE).
                addAction(new ActionLink<ProvisionTO>() {

                    private static final long serialVersionUID = -3722207913631435534L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final ProvisionTO provisionTO) {
                        final ProvisionTO clone = SerializationUtils.clone(provisionTO);
                        clone.setKey(null);
                        clone.setAnyType(null);
                        clone.setObjectClass(null);
                        send(ResourceProvisionPanel.this, Broadcast.DEPTH,
                                new AjaxWizard.NewItemActionEvent<>(clone, target).setResourceModel(
                                        new StringResourceModel("inner.provision.clone",
                                                ResourceProvisionPanel.this,
                                                Model.of(provisionTO))));
                    }
                }, ActionLink.ActionType.CLONE, StandardEntitlement.RESOURCE_CREATE).
                addAction(new ActionLink<ProvisionTO>() {

                    private static final long serialVersionUID = -3722207913631435544L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final ProvisionTO provisionTO) {
                        resourceTO.getProvisions().remove(provisionTO);
                        send(ResourceProvisionPanel.this, Broadcast.DEPTH, new ListViewReload<>(target));
                    }
                }, ActionLink.ActionType.DELETE, StandardEntitlement.RESOURCE_DELETE);

        builder.addNewItemPanelBuilder(wizard);

        final WizardMgtPanel<ProvisionTO> list = builder.build("provision");
        add(list);
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
        try {
            final Collection<ProvisionTO> provisions = new ArrayList<>(resourceTO.getProvisions());

            for (ProvisionTO provision : provisions) {
                if (provision != null) {
                    if (provision.getMapping() == null || provision.getMapping().getItems().isEmpty()) {
                        resourceTO.getProvisions().remove(provision);
                    } else {
                        long connObjectKeyCount = IterableUtils.countMatches(
                                provision.getMapping().getItems(), new Predicate<MappingItemTO>() {

                            @Override
                            public boolean evaluate(final MappingItemTO item) {
                                return item.isConnObjectKey();
                            }
                        });

                        if (connObjectKeyCount != 1) {
                            throw new IllegalArgumentException(provision.getAnyType() + ": "
                                    + new StringResourceModel("connObjectKeyValidation", ResourceProvisionPanel.this).
                                            getString());
                        }
                    }
                }
            }

            new ResourceRestClient().update(resourceTO);
            SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
            modal.close(target);
        } catch (Exception e) {
            LOG.error("While creating or updating {}", resourceTO, e);
            SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage()) ? e.getClass().getName() : e.
                    getMessage());
        }
        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof AjaxWizard.NewItemActionEvent) {
            aboutRealmProvison.setVisible(false);
            final AjaxRequestTarget target = ((AjaxWizard.NewItemEvent) event.getPayload()).getTarget();
            target.add(aboutRealmProvison);
        }

        super.onEvent(event);
    }
}
