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
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.common.lib.to.MappingItemTO;
import org.apache.syncope.common.lib.to.ProvisionTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.common.rest.api.service.ResourceService;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

public class ResourceProvisionPanel extends AbstractModalPanel<Serializable> {

    private static final long serialVersionUID = -7982691107029848579L;

    private final ResourceTO resourceTO;

    public ResourceProvisionPanel(
            final BaseModal<Serializable> modal,
            final ResourceTO resourceTO,
            final PageReference pageRef) {
        super(modal, pageRef);
        this.resourceTO = resourceTO;

        setOutputMarkupId(true);

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
                            SyncopeConsoleSession.get().getService(ResourceService.class).
                                    setLatestSyncToken(resourceTO.getKey(), provisionTO.getAnyType());
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
                            SyncopeConsoleSession.get().getService(ResourceService.class).
                                    removeSyncToken(resourceTO.getKey(), provisionTO.getAnyType());
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
                        send(ResourceProvisionPanel.this, Broadcast.DEPTH, new ListViewReload<Serializable>(target));
                    }
                }, ActionLink.ActionType.DELETE, StandardEntitlement.RESOURCE_DELETE);

        builder.addNewItemPanelBuilder(wizard);

        final WizardMgtPanel<ProvisionTO> list = builder.build("provision");
        add(list);
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
        try {
            boolean connObjectKeyError = false;

            final Collection<ProvisionTO> provisions = new ArrayList<>(resourceTO.getProvisions());

            for (ProvisionTO provision : provisions) {
                if (provision != null) {
                    if (provision.getMapping() == null || provision.getMapping().getItems().isEmpty()) {
                        resourceTO.getProvisions().remove(provision);
                    } else {
                        long uConnObjectKeyCount = IterableUtils.countMatches(
                                provision.getMapping().getItems(), new Predicate<MappingItemTO>() {

                            @Override
                            public boolean evaluate(final MappingItemTO item) {
                                return item.isConnObjectKey();
                            }
                        });

                        connObjectKeyError = uConnObjectKeyCount != 1;
                    }
                }
            }

            final ResourceTO res;
            if (connObjectKeyError) {
                throw new RuntimeException(new StringResourceModel("connObjectKeyValidation").getString());
            } else {
                new ResourceRestClient().update(resourceTO);
                res = resourceTO;
            }
            SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
            modal.close(target);
        } catch (Exception e) {
            LOG.error("While creating or updating {}", resourceTO, e);
            SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage()) ? e.getClass().getName() : e.
                    getMessage());
        }
        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
    }
}
