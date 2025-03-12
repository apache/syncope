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
package org.apache.syncope.client.console.wizards.any;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.rest.AnyTypeClassRestClient;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.wizards.BaseAjaxWizardBuilder;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.TypeExtensionTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

public class TypeExtensionWizardBuilder extends BaseAjaxWizardBuilder<TypeExtensionTO> {

    private static final long serialVersionUID = -7185214439144835423L;

    protected final GroupTO groupTO;

    protected final String anyTypeLabel;

    protected final String auxClassesLabel;

    protected final AnyTypeRestClient anyTypeRestClient;

    protected final AnyTypeClassRestClient anyTypeClassRestClient;

    public TypeExtensionWizardBuilder(
            final GroupTO groupTO,
            final TypeExtensionTO defaultItem,
            final String anyTypeLabel,
            final String auxClassesLabel,
            final AnyTypeRestClient anyTypeRestClient,
            final AnyTypeClassRestClient anyTypeClassRestClient,
            final PageReference pageRef) {

        super(defaultItem, pageRef);

        this.groupTO = groupTO;
        this.anyTypeLabel = anyTypeLabel;
        this.auxClassesLabel = auxClassesLabel;
        this.anyTypeRestClient = anyTypeRestClient;
        this.anyTypeClassRestClient = anyTypeClassRestClient;
    }

    @Override
    protected WizardModel buildModelSteps(final TypeExtensionTO modelObject, final WizardModel wizardModel) {
        wizardModel.add(new Details(modelObject));
        return wizardModel;
    }

    @Override
    protected Serializable onApplyInternal(final TypeExtensionTO modelObject) {
        List<TypeExtensionTO> typeExtensions = groupTO.getTypeExtensions().stream().
                filter(typeExt -> !typeExt.getAnyType().equals(modelObject.getAnyType())).collect(Collectors.toList());
        typeExtensions.add(modelObject);
        groupTO.getTypeExtensions().clear();
        groupTO.getTypeExtensions().addAll(typeExtensions);
        return groupTO;
    }

    public class Details extends WizardStep {

        private static final long serialVersionUID = 6472869166547883903L;

        public Details(final TypeExtensionTO typeExtensionTO) {
            super();
            setOutputMarkupId(true);

            add(new Label("anyType.label", anyTypeLabel));

            if (typeExtensionTO.getAnyType() == null) {
                List<String> anyTypes = anyTypeRestClient.list();
                anyTypes.remove(AnyTypeKind.GROUP.name());
                anyTypes.removeAll(anyTypes.stream().
                        filter(anyType -> groupTO.getTypeExtension(anyType).isPresent()).toList());

                AjaxDropDownChoicePanel<String> anyTypeComponent = new AjaxDropDownChoicePanel<>(
                        "anyType.component", "anyType", new PropertyModel<>(typeExtensionTO, "anyType"));
                anyTypeComponent.setChoices(anyTypes);
                anyTypeComponent.addRequiredLabel();
                add(anyTypeComponent.hideLabel().setOutputMarkupId(true));
            } else {
                AjaxTextFieldPanel anyTypeComponent = new AjaxTextFieldPanel(
                        "anyType.component", "anyType", new PropertyModel<>(typeExtensionTO, "anyType"));
                anyTypeComponent.setEnabled(false);
                add(anyTypeComponent.hideLabel());
            }

            add(new Label("auxClasses.label", auxClassesLabel));

            List<String> anyTypeClasses = anyTypeClassRestClient.list().stream().
                    map(AnyTypeClassTO::getKey).collect(Collectors.toList());
            AjaxPalettePanel<String> auxClassesPalette = new AjaxPalettePanel.Builder<String>().build(
                    "auxClasses.palette",
                    new PropertyModel<>(typeExtensionTO, "auxClasses"),
                    new ListModel<>(anyTypeClasses));
            add(auxClassesPalette.hideLabel().setOutputMarkupId(true));
        }
    }
}
