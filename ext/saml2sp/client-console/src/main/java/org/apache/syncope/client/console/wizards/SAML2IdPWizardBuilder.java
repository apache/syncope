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
package org.apache.syncope.client.console.wizards;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.panels.SAML2IdPsDirectoryPanel;
import org.apache.syncope.client.console.rest.SAML2IdPsRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.FieldPanel;
import org.apache.syncope.client.console.wizards.resources.JEXLTransformersTogglePanel;
import org.apache.syncope.client.console.wizards.resources.MappingItemTransformersTogglePanel;
import org.apache.syncope.common.lib.to.MappingItemTO;
import org.apache.syncope.common.lib.to.SAML2IdPTO;
import org.apache.syncope.common.lib.types.SAML2BindingType;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

public class SAML2IdPWizardBuilder extends AjaxWizardBuilder<SAML2IdPTO> {

    private static final long serialVersionUID = 5952696913893950460L;

    private final SAML2IdPsRestClient restClient = new SAML2IdPsRestClient();

    private final SAML2IdPsDirectoryPanel directoryPanel;

    public SAML2IdPWizardBuilder(
            final SAML2IdPsDirectoryPanel directoryPanel, final SAML2IdPTO idpTO, final PageReference pageRef) {

        super(idpTO, pageRef);
        this.directoryPanel = directoryPanel;
    }

    @Override
    protected WizardModel buildModelSteps(final SAML2IdPTO modelObject, final WizardModel wizardModel) {
        wizardModel.add(new IdP(modelObject));

        Mapping mapping = new Mapping(modelObject);
        mapping.setOutputMarkupId(true);

        MappingItemTransformersTogglePanel mapItemTransformers =
                new MappingItemTransformersTogglePanel(mapping, pageRef);
        addOuterObject(mapItemTransformers);
        JEXLTransformersTogglePanel jexlTransformers = new JEXLTransformersTogglePanel(mapping, pageRef);
        addOuterObject(jexlTransformers);
        mapping.add(new SAML2IdPMappingPanel("mapping", modelObject, mapItemTransformers, jexlTransformers));

        wizardModel.add(mapping);

        return wizardModel;
    }

    private static final class IdP extends WizardStep {

        private static final long serialVersionUID = 854012593185195024L;

        IdP(final SAML2IdPTO idpTO) {
            super(StringUtils.EMPTY, StringUtils.EMPTY);

            List<Component> fields = new ArrayList<>();

            FieldPanel<String> name = new AjaxTextFieldPanel(
                    "field", "name", new PropertyModel<String>(idpTO, "name"), false);
            name.setRequired(true);
            fields.add(name);

            AjaxCheckBoxPanel useDeflateEncoding = new AjaxCheckBoxPanel(
                    "field", "useDeflateEncoding", new PropertyModel<Boolean>(idpTO, "useDeflateEncoding"), false);
            fields.add(useDeflateEncoding);

            AjaxDropDownChoicePanel<SAML2BindingType> bindingType =
                    new AjaxDropDownChoicePanel<>("field", "bindingType",
                            new PropertyModel<SAML2BindingType>(idpTO, "bindingType"), false);
            bindingType.setChoices(Arrays.asList(SAML2BindingType.values()));
            fields.add(bindingType);

            add(new ListView<Component>("fields", fields) {

                private static final long serialVersionUID = -9180479401817023838L;

                @Override
                protected void populateItem(final ListItem<Component> item) {
                    item.add(item.getModelObject());
                }

            });
        }
    }

    /**
     * Mapping definition step.
     */
    private static final class Mapping extends WizardStep {

        private static final long serialVersionUID = 3454904947720856253L;

        Mapping(final SAML2IdPTO item) {
            setTitleModel(Model.of("Mapping"));
            setSummaryModel(Model.of(StringUtils.EMPTY));
        }
    }

    @Override
    protected Serializable onApplyInternal(final SAML2IdPTO modelObject) {
        long connObjectKeyCount = IterableUtils.countMatches(
                modelObject.getMappingItems(), new Predicate<MappingItemTO>() {

            @Override
            public boolean evaluate(final MappingItemTO item) {
                return item.isConnObjectKey();
            }
        });
        if (connObjectKeyCount != 1) {
            throw new IllegalArgumentException(
                    new StringResourceModel("connObjectKeyValidation", directoryPanel).getString());
        }

        restClient.update(modelObject);
        return modelObject;
    }

}
