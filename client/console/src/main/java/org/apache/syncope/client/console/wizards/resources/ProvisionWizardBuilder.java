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
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.commons.ConnIdSpecialName;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.panels.ProvisionAuxClassesPanel;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.client.console.wicket.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wizards.AjaxWizardBuilder;
import org.apache.syncope.common.lib.to.MappingTO;
import org.apache.syncope.common.lib.to.ProvisionTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

public class ProvisionWizardBuilder extends AjaxWizardBuilder<ResourceProvision> {

    private static final long serialVersionUID = 3739399543837732640L;

    private final ConnectorRestClient connectorRestClient = new ConnectorRestClient();

    private final ResourceTO resourceTO;

    protected AjaxTextFieldPanel clazz;

    /**
     * The object type specification step.
     */
    private final class ObjectType extends WizardStep {

        private static final long serialVersionUID = -1657800545799468278L;

        ObjectType(final ResourceProvision item) {
            super(new ResourceModel("clazz.title", StringUtils.EMPTY),
                    new ResourceModel("clazz.summary", StringUtils.EMPTY), new Model<>(item));

            final WebMarkupContainer container = new WebMarkupContainer("container");
            container.setOutputMarkupId(true);
            add(container);

            clazz = new AjaxTextFieldPanel(
                    "clazz", "clazz", new PropertyModel<>(item, "objectClass"));
            clazz.setRequired(true);
            clazz.setChoices(connectorRestClient.getObjectClasses(resourceTO.getConnector()));
            container.add(clazz);
        }
    }

    protected void setObjectClassModelObject(final String type) {
        if (clazz != null) {
            if (AnyTypeKind.USER.name().equals(type)) {
                clazz.setModelObject(ConnIdSpecialName.ACCOUNT);
            } else if (AnyTypeKind.GROUP.name().equals(type)) {
                clazz.setModelObject(ConnIdSpecialName.GROUP);
            } else {
                clazz.setModelObject("");
            }
        }
    }

    /**
     * AuxClasses definition step.
     */
    private static final class AuxClasses extends WizardStep implements WizardModel.ICondition {

        private static final long serialVersionUID = 5315236191866427500L;

        private final ResourceProvision provision;

        AuxClasses(final ResourceProvision item) {
            this.provision = item;

            setTitleModel(new ResourceModel("auxClasses.title"));
            setSummaryModel(new StringResourceModel("auxClasses.summary", this, new Model<>(item)));
            add(new ProvisionAuxClassesPanel("auxClasses", item.getProvisionTO()));
        }

        @Override
        public boolean evaluate() {
            return provision.getProvisionTO() != null;
        }
    }

    /**
     * Mapping definition step.
     */
    private static final class Mapping extends WizardStep {

        private static final long serialVersionUID = 3454904947720856253L;

        Mapping(final ResourceProvision item) {
            setTitleModel(Model.of("Mapping"));
            setSummaryModel(Model.of(StringUtils.EMPTY));
        }
    }

    /**
     * AccountLink specification step.
     */
    private final class ConnObjectLink extends WizardStep {

        private static final long serialVersionUID = 2359955465172450478L;

        ConnObjectLink(final ResourceProvision item) {
            super(new ResourceModel("link.title", StringUtils.EMPTY),
                    new ResourceModel("link.summary", StringUtils.EMPTY));

            final WebMarkupContainer connObjectLinkContainer = new WebMarkupContainer("connObjectLinkContainer");
            connObjectLinkContainer.setOutputMarkupId(true);
            add(connObjectLinkContainer);

            boolean connObjectLinkEnabled = false;
            if (StringUtils.isNotBlank(item.getConnObjectLink())) {
                connObjectLinkEnabled = true;
            }

            final AjaxCheckBoxPanel connObjectLinkCheckbox = new AjaxCheckBoxPanel(
                    "connObjectLinkCheckbox",
                    new ResourceModel("connObjectLinkCheckbox", "connObjectLinkCheckbox").getObject(),
                    new Model<>(connObjectLinkEnabled),
                    false);
            connObjectLinkCheckbox.setEnabled(true);

            connObjectLinkContainer.add(connObjectLinkCheckbox);

            final AjaxTextFieldPanel connObjectLink = new AjaxTextFieldPanel(
                    "connObjectLink",
                    new ResourceModel("connObjectLink", "connObjectLink").getObject(),
                    new PropertyModel<>(item, "connObjectLink"),
                    false);
            connObjectLink.enableJexlHelp();
            connObjectLink.setEnabled(connObjectLinkEnabled);
            connObjectLinkContainer.add(connObjectLink);

            connObjectLinkCheckbox.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    connObjectLink.setEnabled(connObjectLinkCheckbox.getModelObject());
                    connObjectLink.setModelObject("");

                    target.add(connObjectLink);
                }
            });
        }
    }

    /**
     * Construct.
     *
     * @param resourceTO external resource to be updated.
     * @param pageRef Caller page reference.
     */
    public ProvisionWizardBuilder(final ResourceTO resourceTO, final PageReference pageRef) {
        super(new ResourceProvision(), pageRef);
        this.resourceTO = resourceTO;
    }

    @Override
    protected WizardModel buildModelSteps(final ResourceProvision modelObject, final WizardModel wizardModel) {
        wizardModel.add(new ObjectType(modelObject));
        wizardModel.add(new AuxClasses(modelObject));

        Mapping mapping = new Mapping(modelObject);
        mapping.setOutputMarkupId(true);

        ItemTransformersTogglePanel itemTransformers = new ItemTransformersTogglePanel(mapping, pageRef);
        addOuterObject(itemTransformers);
        JEXLTransformersTogglePanel jexlTransformers = new JEXLTransformersTogglePanel(mapping, pageRef);
        addOuterObject(jexlTransformers);
        if (modelObject.getProvisionTO() != null && modelObject.getProvisionTO().getMapping() == null) {
            modelObject.getProvisionTO().setMapping(new MappingTO());
        }
        mapping.add(new ResourceMappingPanel(
                "mapping", resourceTO, modelObject, itemTransformers, jexlTransformers));

        wizardModel.add(mapping);

        wizardModel.add(new ConnObjectLink(modelObject));
        return wizardModel;
    }

    @Override
    protected Serializable onApplyInternal(final ResourceProvision modelObject) {
        if (modelObject.getOrgUnitTO() != null) {
            this.resourceTO.setOrgUnit(modelObject.getOrgUnitTO());

            this.resourceTO.getOrgUnit().getItems().clear();
            this.resourceTO.getOrgUnit().getItems().addAll(modelObject.getItems());
        } else if (modelObject.getProvisionTO() != null) {
            final List<ProvisionTO> provisions;
            if (modelObject.getKey() == null) {
                provisions = this.resourceTO.getProvisions().stream().
                        filter(object -> !modelObject.getAnyType().equals(object.getAnyType())).
                        collect(Collectors.toList());
            } else {
                provisions = this.resourceTO.getProvisions().stream().
                        filter(object -> !modelObject.getKey().equals(object.getKey())).
                        collect(Collectors.toList());
            }

            ProvisionTO provisionTO = modelObject.getProvisionTO();
            provisionTO.getMapping().getItems().clear();
            provisionTO.getMapping().getItems().addAll(modelObject.getItems());
            provisions.add(provisionTO);

            this.resourceTO.getProvisions().clear();
            this.resourceTO.getProvisions().addAll(provisions);
        }

        return modelObject;
    }
}
