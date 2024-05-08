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
package org.apache.syncope.client.console.tasks;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.panels.AbstractModalPanel;
import org.apache.syncope.client.console.rest.TaskRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxGridFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.common.lib.form.FormPropertyType;
import org.apache.syncope.common.lib.to.FormPropertyDefTO;
import org.apache.syncope.common.lib.to.MacroTaskTO;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class FormPropertyDefsPanel extends AbstractModalPanel<MacroTaskTO> {

    private static final long serialVersionUID = 6991001927367507753L;

    @SpringBean
    protected TaskRestClient taskRestClient;

    protected final MacroTaskTO task;

    protected final IModel<List<FormPropertyDefTO>> model;

    public FormPropertyDefsPanel(
            final MacroTaskTO task,
            final BaseModal<MacroTaskTO> modal,
            final PageReference pageRef) {

        super(modal, pageRef);
        this.task = task;

        WebMarkupContainer propertyDefContainer = new WebMarkupContainer("propertyDefContainer");
        propertyDefContainer.setOutputMarkupId(true);
        add(propertyDefContainer);

        model = new ListModel<>(new ArrayList<>());
        model.getObject().addAll(task.getFormPropertyDefs());

        ListView<FormPropertyDefTO> propertyDefs = new ListView<>("propertyDefs", model) {

            private static final long serialVersionUID = 1814616131938968887L;

            @Override
            protected void populateItem(final ListItem<FormPropertyDefTO> item) {
                FormPropertyDefTO fpd = item.getModelObject();

                AjaxTextFieldPanel key = new AjaxTextFieldPanel(
                        "key",
                        "key",
                        new PropertyModel<>(fpd, "key"),
                        true);
                item.add(key.setRequired(true).hideLabel());

                AjaxTextFieldPanel name = new AjaxTextFieldPanel(
                        "name",
                        "name",
                        new PropertyModel<>(fpd, "name"),
                        true);
                item.add(name.setRequired(true).hideLabel());

                AjaxCheckBoxPanel readable = new AjaxCheckBoxPanel(
                        "readable",
                        "readable",
                        new PropertyModel<>(fpd, "readable"),
                        true);
                item.add(readable.hideLabel());

                AjaxCheckBoxPanel writable = new AjaxCheckBoxPanel(
                        "writable",
                        "writable",
                        new PropertyModel<>(fpd, "writable"),
                        true);
                item.add(writable.hideLabel());

                AjaxCheckBoxPanel required = new AjaxCheckBoxPanel(
                        "required",
                        "required",
                        new PropertyModel<>(fpd, "required"),
                        true);
                item.add(required.hideLabel());

                AjaxDropDownChoicePanel<FormPropertyType> type = new AjaxDropDownChoicePanel<>(
                        "type",
                        "type",
                        new PropertyModel<>(fpd, "type"),
                        true);
                type.setChoices(List.of(FormPropertyType.values())).setNullValid(false);
                item.add(type.setRequired(true).hideLabel());

                AjaxTextFieldPanel datePattern = new AjaxTextFieldPanel(
                        "datePattern",
                        "datePattern",
                        new PropertyModel<>(fpd, "datePattern"),
                        true);
                datePattern.setEnabled(fpd.getType() == FormPropertyType.Date);
                item.add(datePattern.hideLabel().setOutputMarkupId(true));

                AjaxGridFieldPanel<String, String> enumValues = new AjaxGridFieldPanel<>(
                        "enumValues",
                        "enumValues",
                        new PropertyModel<>(fpd, "enumValues"));
                enumValues.setEnabled(fpd.getType() == FormPropertyType.Enum);
                item.add(enumValues.hideLabel().setOutputMarkupId(true));

                type.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                    private static final long serialVersionUID = -1107858522700306810L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        switch (type.getModelObject()) {
                            case Date -> {
                                datePattern.setEnabled(true);
                                enumValues.setEnabled(false);
                                fpd.getEnumValues().clear();
                            }

                            case Enum -> {
                                datePattern.setEnabled(false);
                                enumValues.setEnabled(true);
                            }

                            default -> {
                                datePattern.setEnabled(false);
                                enumValues.setEnabled(false);
                                fpd.getEnumValues().clear();
                            }
                        }

                        target.add(datePattern);
                        target.add(enumValues);
                    }
                });

                ActionsPanel<Serializable> actions = new ActionsPanel<>("toRemove", null);
                actions.add(new ActionLink<>() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                        model.getObject().remove(item.getIndex());
                        item.getParent().removeAll();
                        target.add(propertyDefContainer);
                    }
                }, ActionLink.ActionType.DELETE, StringUtils.EMPTY, true).hideLabel();
                item.add(actions);
            }
        };
        propertyDefs.setReuseItems(true);
        propertyDefContainer.add(propertyDefs);

        IndicatingAjaxButton addPropertyDef = new IndicatingAjaxButton("addPropertyDef") {

            private static final long serialVersionUID = -4804368561204623354L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                model.getObject().add(new FormPropertyDefTO());
                target.add(propertyDefContainer);
            }
        };
        addPropertyDef.setDefaultFormProcessing(false);
        propertyDefContainer.add(addPropertyDef);
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target) {
        task.getFormPropertyDefs().clear();
        task.getFormPropertyDefs().addAll(model.getObject());
        try {
            taskRestClient.update(TaskType.MACRO, task);

            SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
            modal.close(target);
        } catch (Exception e) {
            LOG.error("While updating Macro Task {}", task.getKey(), e);
            SyncopeConsoleSession.get().onException(e);
        }
        ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
    }
}
