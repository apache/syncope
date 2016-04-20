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

import java.util.List;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.client.console.panels.ListViewPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.common.lib.to.ProvisionTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.wizard.WizardStep;

public class ResourceProvisionPanel extends WizardStep {

    private static final long serialVersionUID = -7982691107029848579L;

    public ResourceProvisionPanel(final ResourceTO resourceTO, final PageReference pageRef) {
        super();
        setOutputMarkupId(true);

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
                        send(pageRef.getPage(), Broadcast.DEPTH,
                                new AjaxWizard.NewItemActionEvent<>(provisionTO, 2, target));
                    }
                }, ActionLink.ActionType.MAPPING, StandardEntitlement.RESOURCE_UPDATE).
                addAction(new ActionLink<ProvisionTO>() {

                    private static final long serialVersionUID = -3722207913631435524L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final ProvisionTO provisionTO) {
                        provisionTO.setSyncToken(null);
                        send(pageRef.getPage(), Broadcast.DEPTH, new ListViewPanel.ListViewReload(target));
                    }
                }, ActionLink.ActionType.RESET_TIME, StandardEntitlement.RESOURCE_UPDATE).
                addAction(new ActionLink<ProvisionTO>() {

                    private static final long serialVersionUID = -3722207913631435534L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final ProvisionTO provisionTO) {
                        final ProvisionTO clone = SerializationUtils.clone(provisionTO);
                        clone.setKey(null);
                        clone.setAnyType(null);
                        clone.setObjectClass(null);
                        send(pageRef.getPage(), Broadcast.DEPTH, new AjaxWizard.NewItemActionEvent<>(clone, target));
                    }
                }, ActionLink.ActionType.CLONE, StandardEntitlement.RESOURCE_CREATE).
                addAction(new ActionLink<ProvisionTO>() {

                    private static final long serialVersionUID = -3722207913631435544L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final ProvisionTO provisionTO) {
                        resourceTO.getProvisions().remove(provisionTO);
                        send(pageRef.getPage(), Broadcast.DEPTH, new ListViewPanel.ListViewReload(target));
                    }
                }, ActionLink.ActionType.DELETE, StandardEntitlement.RESOURCE_DELETE);

        builder.addNewItemPanelBuilder(new ProvisionWizardBuilder(resourceTO, pageRef));
        add(builder.build("provision"));
    }
}
