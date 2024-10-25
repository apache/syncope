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
package org.apache.syncope.client.console.panels.mapping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.console.rest.AnyTypeClassRestClient;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.scim.SCIMItem;
import org.apache.syncope.common.lib.scim.SCIMReturned;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class SCIMExtensionMappingPanel extends Panel {

    private static final long serialVersionUID = -5268147603868322754L;

    @SpringBean
    protected AnyTypeRestClient anyTypeRestClient;

    @SpringBean
    protected AnyTypeClassRestClient anyTypeClassRestClient;

    protected final Label intAttrNameInfo;

    protected final WebMarkupContainer mandatoryHeader;

    protected final Label caseExactLabel;

    protected final Label mutabilityLabel;

    protected final Label returnedLabel;

    protected final Label uniquenessLabel;

    protected final Label multiValuedLabel;

    protected final AjaxButton addMappingBtn;

    protected final ListView<SCIMItem> mappings;

    protected final WebMarkupContainer mappingContainer;

    public SCIMExtensionMappingPanel(
            final String id,
            final IModel<List<SCIMItem>> model) {

        super(id);
        setOutputMarkupId(true);

        mappingContainer = new WebMarkupContainer("mappingContainer");
        mappingContainer.setOutputMarkupId(true);
        add(mappingContainer);

        intAttrNameInfo = new Label("intAttrNameInfo", Model.of());
        mappingContainer.add(intAttrNameInfo);

        mandatoryHeader = new WebMarkupContainer("mandatoryHeader");
        mandatoryHeader.setOutputMarkupId(true);
        mappingContainer.add(mandatoryHeader);

        caseExactLabel = new Label("caseExactLabel", Model.of());
        mappingContainer.add(caseExactLabel);
        mutabilityLabel = new Label("mutabilityLabel", Model.of());
        mappingContainer.add(mutabilityLabel);
        returnedLabel = new Label("returnedLabel", Model.of());
        mappingContainer.add(returnedLabel);
        uniquenessLabel = new Label("uniquenessLabel", Model.of());
        mappingContainer.add(uniquenessLabel);
        multiValuedLabel = new Label("multiValuedLabel", Model.of());
        mappingContainer.add(multiValuedLabel);

        mappings = new ListView<>("mappings", model) {

            private static final long serialVersionUID = -8749412138042656239L;

            @Override
            protected void populateItem(final ListItem<SCIMItem> item) {
                final SCIMItem itemTO = item.getModelObject();

                //--------------------------------
                // Internal attribute
                // -------------------------------
                AjaxTextFieldPanel intAttrName = new AjaxTextFieldPanel(
                        "intAttrName",
                        "intAttrName",
                        new PropertyModel<>(itemTO, "intAttrName"),
                        false);
                intAttrName.setChoices(List.of());
                intAttrName.setRequired(true).hideLabel();
                item.add(intAttrName);
                // -------------------------------

                //--------------------------------
                // External attribute
                // -------------------------------
                AjaxTextFieldPanel extAttrName = new AjaxTextFieldPanel(
                        "extAttrName",
                        "extAttrName",
                        new PropertyModel<>(itemTO, "extAttrName"));
                extAttrName.setChoices(getExtAttrNames().getObject());

                extAttrName.setRequired(true).hideLabel();
                extAttrName.setEnabled(true);
                item.add(extAttrName);
                // -------------------------------

                //--------------------------------
                // mandatoryCondition
                // -------------------------------
                AjaxCheckBoxPanel mandatoryCondition = new AjaxCheckBoxPanel(
                        "mandatoryCondition",
                        "mandatoryCondition",
                        new PropertyModel<>(itemTO, "mandatoryCondition"));
                mandatoryCondition.hideLabel();
                mandatoryCondition.setEnabled(true);
                item.add(mandatoryCondition);
                // -------------------------------

                //--------------------------------
                // CaseExact
                // -------------------------------
                AjaxCheckBoxPanel caseExact = new AjaxCheckBoxPanel(
                        "caseExact",
                        "caseExact",
                        new PropertyModel<>(itemTO, "caseExact"));
                caseExact.hideLabel();
                caseExact.setEnabled(true);
                item.add(caseExact);
                // -------------------------------

                //--------------------------------
                // Mutability
                // -------------------------------
                AjaxCheckBoxPanel mutability = new AjaxCheckBoxPanel(
                        "mutability",
                        "mutability",
                        new PropertyModel<>(itemTO, "mutability"));
                mutability.hideLabel();
                mutability.setEnabled(true);
                item.add(mutability);
                // -------------------------------

                //--------------------------------
                // Returned
                // -------------------------------
                AjaxDropDownChoicePanel<SCIMReturned> returned = new AjaxDropDownChoicePanel<>(
                        "returned",
                        "returned",
                        new PropertyModel<>(itemTO, "returned"));
                returned.hideLabel();
                returned.setChoices(List.of(SCIMReturned.values()));
                returned.setEnabled(true);
                item.add(returned);
                // -------------------------------

                //--------------------------------
                // Uniqueness
                // -------------------------------
                AjaxCheckBoxPanel uniqueness = new AjaxCheckBoxPanel(
                        "uniqueness",
                        "uniqueness",
                        new PropertyModel<>(itemTO, "uniqueness"));
                uniqueness.hideLabel();
                uniqueness.setEnabled(true);
                item.add(uniqueness);
                // -------------------------------

                //--------------------------------
                // MultiValued
                // -------------------------------
                AjaxCheckBoxPanel multiValued = new AjaxCheckBoxPanel(
                        "multiValued",
                        "multiValued",
                        new PropertyModel<>(itemTO, "multiValued"));
                multiValued.hideLabel();
                multiValued.setEnabled(true);
                item.add(multiValued);
                // -------------------------------

                //--------------------------------
                // Remove
                // -------------------------------
                ActionsPanel<Serializable> actions = new ActionsPanel<>("toRemove", null);
                actions.add(new ActionLink<>() {

                    private static final long serialVersionUID = -4097030429755746419L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                        model.getObject().remove(item.getIndex());
                        item.getParent().removeAll();
                        target.add(SCIMExtensionMappingPanel.this);
                    }
                }, ActionLink.ActionType.DELETE, StringUtils.EMPTY, true).hideLabel();
                item.add(actions);
                // -------------------------------

                intAttrName.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                    private static final long serialVersionUID = 6890150953186587184L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                    }
                });
            }
        };

        mappings.setReuseItems(true);
        mappingContainer.add(mappings);

        addMappingBtn = new IndicatingAjaxButton("addMappingBtn") {

            private static final long serialVersionUID = -971427869417596230L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                model.getObject().add(new SCIMItem());
                target.add(SCIMExtensionMappingPanel.this);
            }
        };
        addMappingBtn.setDefaultFormProcessing(false);
        addMappingBtn.setEnabled(true);
        mappingContainer.add(addMappingBtn);
    }

    protected IModel<List<String>> getExtAttrNames() {
        List<String> choices = new ArrayList<>(ClassPathScanImplementationLookup.USER_FIELD_NAMES);

        anyTypeClassRestClient.list(anyTypeRestClient.read(AnyTypeKind.USER.name()).getClasses()).
                forEach(anyTypeClassTO -> {
                    choices.addAll(anyTypeClassTO.getPlainSchemas());
                    choices.addAll(anyTypeClassTO.getDerSchemas());
                    choices.addAll(anyTypeClassTO.getVirSchemas());
                });

        Collections.sort(choices);
        return Model.ofList(choices);
    }
}
