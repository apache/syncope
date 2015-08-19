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

import static org.apache.wicket.Component.ENABLE;

import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.AbstractBasePage;
import org.apache.syncope.client.console.topology.TopologyNode;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.client.console.wizards.provision.ProvisionWizardBuilder;
import org.apache.syncope.common.lib.to.MappingItemTO;
import org.apache.syncope.common.lib.to.ProvisionTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.Entitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.ResourceModel;

/**
 * Modal window with Resource form.
 */
public class ResourceModal extends AbstractResourceModal {

    private static final long serialVersionUID = 1734415311027284221L;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ResourceModal(
            final ModalWindow window,
            final PageReference pageRef,
            final ResourceTO resourceTO,
            final boolean createFlag) {

        super(window, pageRef);

        final Form<ResourceTO> form = new Form<>(FORM);
        form.setModel(new CompoundPropertyModel<>(resourceTO));

        //--------------------------------
        // Resource details panel
        //--------------------------------
        form.add(new ResourceDetailsPanel("details", resourceTO,
                resourceRestClient.getPropagationActionsClasses(), createFlag));

        form.add(new AnnotatedBeanPanel("systeminformation", resourceTO));
        //--------------------------------

        //--------------------------------
        // Resource provision panels
        //--------------------------------
        final WebMarkupContainer provisions = new WebMarkupContainer("pcontainer");
        form.add(provisions.setOutputMarkupId(true));

        final ListViewPanel.Builder<ProvisionTO> builder = ListViewPanel.builder(ProvisionTO.class, pageRef);
        builder.setItems(resourceTO.getProvisions());
        builder.includes("anyType", "objectClass");

        builder.addAction(new ActionLink<ProvisionTO>() {

            private static final long serialVersionUID = -3722207913631435504L;

            @Override
            public void onClick(final AjaxRequestTarget target, final ProvisionTO provisionTO) {
                send(pageRef.getPage(), Broadcast.DEPTH,
                        new AjaxWizard.NewItemActionEvent<ProvisionTO>(provisionTO, 2, target));
            }
        }, ActionLink.ActionType.MAPPING, Entitlement.RESOURCE_UPDATE).addAction(new ActionLink<ProvisionTO>() {

            private static final long serialVersionUID = -3722207913631435514L;

            @Override
            public void onClick(final AjaxRequestTarget target, final ProvisionTO provisionTO) {
                send(pageRef.getPage(), Broadcast.DEPTH,
                        new AjaxWizard.NewItemActionEvent<ProvisionTO>(provisionTO, 3, target));
            }
        }, ActionLink.ActionType.ACCOUNT_LINK, Entitlement.RESOURCE_UPDATE).addAction(new ActionLink<ProvisionTO>() {

            private static final long serialVersionUID = -3722207913631435524L;

            @Override
            public void onClick(final AjaxRequestTarget target, final ProvisionTO provisionTO) {
                provisionTO.setSyncToken(null);
                send(pageRef.getPage(), Broadcast.DEPTH,
                        new AjaxWizard.NewItemFinishEvent<ProvisionTO>(provisionTO, target));
            }
        }, ActionLink.ActionType.RESET_TIME, Entitlement.RESOURCE_UPDATE).addAction(new ActionLink<ProvisionTO>() {

            private static final long serialVersionUID = -3722207913631435534L;

            @Override
            public void onClick(final AjaxRequestTarget target, final ProvisionTO provisionTO) {
                send(pageRef.getPage(), Broadcast.DEPTH,
                        new AjaxWizard.NewItemActionEvent<ProvisionTO>(SerializationUtils.clone(provisionTO), target));
            }
        }, ActionLink.ActionType.CLONE, Entitlement.RESOURCE_CREATE).addAction(new ActionLink<ProvisionTO>() {

            private static final long serialVersionUID = -3722207913631435544L;

            @Override
            public void onClick(final AjaxRequestTarget target, final ProvisionTO provisionTO) {
                resourceTO.getProvisions().remove(provisionTO);
                send(pageRef.getPage(), Broadcast.DEPTH,
                        new AjaxWizard.NewItemFinishEvent<ProvisionTO>(null, target));
            }
        }, ActionLink.ActionType.DELETE, Entitlement.RESOURCE_DELETE);

        builder.addNewItemPanelBuilder(new ProvisionWizardBuilder("wizard", resourceTO, pageRef));
        builder.addNotificationPanel(feedbackPanel);

        provisions.add(builder.build("provisions"));
        //--------------------------------

        //--------------------------------
        // Resource connector configuration panel
        //--------------------------------
        ResourceConnConfPanel resourceConnConfPanel = new ResourceConnConfPanel("connconf", resourceTO, createFlag);
        MetaDataRoleAuthorizationStrategy.authorize(resourceConnConfPanel, ENABLE, Entitlement.CONNECTOR_READ);
        form.add(resourceConnConfPanel);
        //--------------------------------

        //--------------------------------
        // Resource security panel
        //--------------------------------
        form.add(new ResourceSecurityPanel("security", resourceTO));
        //--------------------------------

        AjaxButton submit = new IndicatingAjaxButton(APPLY, new ResourceModel(SUBMIT, SUBMIT)) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                final ResourceTO resourceTO = (ResourceTO) form.getDefaultModelObject();

                boolean connObjectKeyError = false;

                final Collection<ProvisionTO> provisions = new ArrayList<>(resourceTO.getProvisions());

                for (ProvisionTO provision : provisions) {
                    if (provision != null) {
                        if (provision.getMapping() == null || provision.getMapping().getItems().isEmpty()) {
                            resourceTO.getProvisions().remove(provision);
                        } else {
                            int uConnObjectKeyCount = CollectionUtils.countMatches(
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

                if (connObjectKeyError) {
                    error(getString("connObjectKeyValidation"));
                    feedbackPanel.refresh(target);
                } else {
                    try {
                        if (createFlag) {
                            resourceRestClient.create(resourceTO);
                            send(pageRef.getPage(), Broadcast.BREADTH, new CreateEvent(
                                    resourceTO.getKey(),
                                    resourceTO.getKey(),
                                    TopologyNode.Kind.RESOURCE,
                                    resourceTO.getConnector(),
                                    target));
                        } else {
                            resourceRestClient.update(resourceTO);
                        }

                        if (pageRef.getPage() instanceof AbstractBasePage) {
                            ((AbstractBasePage) pageRef.getPage()).setModalResult(true);
                        }
                        window.close(target);
                    } catch (Exception e) {
                        LOG.error("Failure managing resource {}", resourceTO, e);
                        error(getString(Constants.ERROR) + ": " + e.getMessage());
                        feedbackPanel.refresh(target);
                    }
                }
            }

            @Override

            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                feedbackPanel.refresh(target);
            }
        };

        form.add(submit);
        form.setDefaultButton(submit);

        final AjaxButton cancel = new IndicatingAjaxButton(CANCEL, new ResourceModel(CANCEL)) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                window.close(target);
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
            }
        };

        cancel.setDefaultFormProcessing(false);
        form.add(cancel);

        add(form);

        MetaDataRoleAuthorizationStrategy.authorize(
                submit, ENABLE, createFlag ? Entitlement.RESOURCE_CREATE : Entitlement.RESOURCE_UPDATE);
    }
}
