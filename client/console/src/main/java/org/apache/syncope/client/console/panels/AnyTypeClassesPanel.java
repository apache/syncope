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
import java.util.List;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.AbstractBasePage;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.common.rest.api.service.AnyTypeClassService;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnyTypeClassesPanel extends Panel {

    private static final long serialVersionUID = -2356760296223908382L;

    private static final Logger LOG = LoggerFactory.getLogger(AnyTypeClassesPanel.class);

    private final ListChoice<AnyTypeClassTO> anyTypeClasses;

    private AnyTypeClassDetails anyTypeClassesDetails;

    public AnyTypeClassesPanel(final String id, final PageReference pageRef, final BaseModal<AnyTypeClassTO> modal) {
        super(id);
        this.setOutputMarkupId(true);

        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        anyTypeClasses = new ListChoice<AnyTypeClassTO>(
                "anyTypeClasses", new Model<AnyTypeClassTO>(),
                SyncopeConsoleSession.get().getService(AnyTypeClassService.class).list()) {

            private static final long serialVersionUID = 4022366881854379834L;

            @Override
            protected CharSequence getDefaultChoice(final String selectedValue) {
                return null;
            }
        };

        anyTypeClasses.setChoiceRenderer(new IChoiceRenderer<AnyTypeClassTO>() {

            private static final long serialVersionUID = 1048000918946220007L;

            @Override
            public Object getDisplayValue(final AnyTypeClassTO object) {
                return object.getKey();
            }

            @Override
            public String getIdValue(final AnyTypeClassTO object, final int index) {
                return object.getKey();
            }

            @Override
            public AnyTypeClassTO getObject(final String id,
                    final IModel<? extends List<? extends AnyTypeClassTO>> choices) {
                for (AnyTypeClassTO item : choices.getObject()) {
                    if (item.getKey().equals(id)) {
                        return item;
                    }
                }
                return null;
            }
        });

        anyTypeClasses.setNullValid(true);
        container.add(anyTypeClasses);

        updateAnyTypeClassDetails(new AnyTypeClassTO(), false);
        container.add(anyTypeClassesDetails);

        anyTypeClasses.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                updateAnyTypeClassDetails(anyTypeClasses.getModelObject(), true);
                container.addOrReplace(anyTypeClassesDetails);
                target.add(container);
            }
        });

        final ActionLinksPanel.Builder<Serializable> actionLinks = ActionLinksPanel.builder(pageRef);
        actionLinks.setDisableIndicator(true);
        actionLinks.addWithRoles(new ActionLink<Serializable>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                modal.header(new ResourceModel("createAnyTypeClass"));
                modal.setFormModel(new AnyTypeClassTO());
                modal.size(Modal.Size.Large);
                target.add(modal.setContent(new AnyTypeClassModalPanel(modal, pageRef, true)));
                modal.addSumbitButton();
                modal.show(true);
            }
        }, ActionLink.ActionType.CREATE, StandardEntitlement.ANYTYPECLASS_CREATE).addWithRoles(
                new ActionLink<Serializable>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                if (anyTypeClasses != null && anyTypeClasses.getModelObject() != null) {
                    modal.header(Model.of(anyTypeClasses.getModelObject().getKey()));
                    modal.setFormModel(anyTypeClasses.getModelObject());
                    modal.addSumbitButton();
                    modal.show(true);
                    target.add(modal.setContent(new AnyTypeClassModalPanel(modal, pageRef, false)));
                }
            }
        }, ActionLink.ActionType.EDIT, StandardEntitlement.ANYTYPECLASS_UPDATE).addWithRoles(
                        new ActionLink<Serializable>() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                        try {
                            if (anyTypeClasses != null && anyTypeClasses.getModelObject() != null) {
                                SyncopeConsoleSession.get()
                                        .getService(AnyTypeClassService.class).delete(anyTypeClasses.getModelObject().
                                        getKey());
                                anyTypeClasses.setModelObject(null);
                                anyTypeClasses.setChoices(SyncopeConsoleSession.get().getService(
                                        AnyTypeClassService.class).
                                        list());
                                target.add(anyTypeClasses);
                                target.add(updateAnyTypeClassDetails(new AnyTypeClassTO(), true));
                                info(getString(Constants.OPERATION_SUCCEEDED));
                            }
                        } catch (Exception e) {
                            LOG.error("While deleting AnyTypeClass", e);
                            error(getString(Constants.ERROR) + ": " + e.getMessage());
                        }
                        ((AbstractBasePage) getPage()).getNotificationPanel().refresh(target);
                    }
                }, ActionLink.ActionType.DELETE, StandardEntitlement.ANYTYPECLASS_DELETE);

        container.add(actionLinks.build("editRemove"));
    }

    private Panel updateAnyTypeClassDetails(final AnyTypeClassTO anyTypeClassTO, final boolean visible) {
        anyTypeClassesDetails = new AnyTypeClassDetails("anyTypeClassesDetails", anyTypeClassTO, false);
        anyTypeClassesDetails.setOutputMarkupId(true);
        anyTypeClassesDetails.setOutputMarkupPlaceholderTag(true);
        anyTypeClassesDetails.setVisible(visible);
        anyTypeClassesDetails.setEnabled(false);
        return anyTypeClassesDetails;
    }
}
