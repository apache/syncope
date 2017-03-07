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
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.client.console.rest.AnyTypeClassRestClient;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wizards.AjaxWizardBuilder;
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.TypeExtensionTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

public class TypeExtensionWizardBuilder extends AjaxWizardBuilder<TypeExtensionTO> {

    private static final long serialVersionUID = -7185214439144835423L;

    private final GroupTO groupTO;

    private final String anyTypeLabel;

    private final String auxClassesLabel;

    public TypeExtensionWizardBuilder(
            final GroupTO groupTO,
            final TypeExtensionTO defaultItem,
            final String anyTypeLabel,
            final String auxClassesLabel,
            final PageReference pageRef) {

        super(defaultItem, pageRef);

        this.groupTO = groupTO;
        this.anyTypeLabel = anyTypeLabel;
        this.auxClassesLabel = auxClassesLabel;
    }

    @Override
    protected WizardModel buildModelSteps(final TypeExtensionTO modelObject, final WizardModel wizardModel) {
        wizardModel.add(new Details(modelObject));
        return wizardModel;
    }
    
    @Override
    protected Serializable onApplyInternal(final TypeExtensionTO modelObject) {
        final List<TypeExtensionTO> typeExtensions
                = ListUtils.select(groupTO.getTypeExtensions(), new Predicate<TypeExtensionTO>() {

                    @Override
                    public boolean evaluate(final TypeExtensionTO object) {
                        return !object.getAnyType().equals(modelObject.getAnyType());
                    }
                });
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
                List<String> anyTypes = CollectionUtils.collect(new AnyTypeRestClient().list(),
                        EntityTOUtils.keyTransformer(), new ArrayList<String>());
                anyTypes.remove(AnyTypeKind.GROUP.name());
                CollectionUtils.filter(anyTypes, new Predicate<String>() {

                    @Override
                    public boolean evaluate(final String anyType) {
                        return groupTO.getTypeExtension(anyType) == null;
                    }
                });

                AjaxDropDownChoicePanel<String> anyTypeComponent = new AjaxDropDownChoicePanel<>(
                        "anyType.component", "anyType", new PropertyModel<String>(typeExtensionTO, "anyType"));
                anyTypeComponent.setChoices(anyTypes);
                anyTypeComponent.addRequiredLabel();
                add(anyTypeComponent.hideLabel().setOutputMarkupId(true));
            } else {
                AjaxTextFieldPanel anyTypeComponent = new AjaxTextFieldPanel(
                        "anyType.component", "anyType", new PropertyModel<String>(typeExtensionTO, "anyType"));
                anyTypeComponent.setEnabled(false);
                add(anyTypeComponent.hideLabel());
            }

            add(new Label("auxClasses.label", auxClassesLabel));

            List<String> anyTypeClasses = CollectionUtils.collect(new AnyTypeClassRestClient().list(),
                    EntityTOUtils.keyTransformer(), new ArrayList<String>());
            AjaxPalettePanel<String> auxClassesPalette = new AjaxPalettePanel.Builder<String>().build(
                    "auxClasses.palette",
                    new PropertyModel<List<String>>(typeExtensionTO, "auxClasses"),
                    new ListModel<>(anyTypeClasses));
            add(auxClassesPalette.hideLabel().setOutputMarkupId(true));
        }
    }
}
