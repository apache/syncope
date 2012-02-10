/*
 *  Copyright 2011 fabio.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.console.pages.panels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxPreprocessingCallDecorator;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.syncope.client.to.ConnInstanceTO;
import org.syncope.client.to.ResourceTO;
import org.syncope.client.to.SchemaMappingTO;
import org.syncope.console.pages.panels.ResourceConnConfPanel.ConnConfModEvent;
import org.syncope.console.rest.ConnectorRestClient;
import org.syncope.console.rest.SchemaRestClient;
import org.syncope.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.syncope.console.wicket.markup.html.form.AjaxDecoratedCheckbox;
import org.syncope.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.syncope.console.wicket.markup.html.form.FieldPanel;
import org.syncope.types.ConnConfProperty;
import org.syncope.types.IntMappingType;

public class ResourceMappingPanel extends Panel {

    private static final long serialVersionUID = -7982691107029848579L;

    /**
     * Logger.
     */
    protected static final Logger LOG =
            LoggerFactory.getLogger(ResourceMappingPanel.class);

    @SpringBean
    private SchemaRestClient schemaRestClient;

    @SpringBean
    private ConnectorRestClient connRestClient;

    private List<String> uSchemaAttrNames;

    private List<String> uDerSchemaAttrNames;

    private List<String> uVirSchemaAttrNames;

    private List<String> resourceSchemaNames;

    private AjaxButton addSchemaMappingBtn;

    private ListView mappings;

    private ResourceTO resourceTO;

    private WebMarkupContainer mappingContainer;

    private boolean createFlag;

    public ResourceMappingPanel(
            final String id,
            final ResourceTO resourceTO,
            final boolean createFlag) {

        super(id);
        setOutputMarkupId(true);

        this.resourceTO = resourceTO;
        this.createFlag = createFlag;

        initResourceSchemaNames();

        uSchemaAttrNames =
                schemaRestClient.getSchemaNames("user");
        uDerSchemaAttrNames =
                schemaRestClient.getDerivedSchemaNames("user");
        uVirSchemaAttrNames =
                schemaRestClient.getVirtualSchemaNames("user");

        final IModel<List<IntMappingType>> intMappingTypes =
                new LoadableDetachableModel<List<IntMappingType>>() {

                    private static final long serialVersionUID =
                            5275935387613157437L;

                    @Override
                    protected List<IntMappingType> load() {
                        return Arrays.asList(IntMappingType.values());
                    }
                };

        final AjaxTextFieldPanel accountLink = new AjaxTextFieldPanel(
                "accountLink",
                new ResourceModel("accountLink", "accountLink").getObject(),
                new PropertyModel<String>(resourceTO, "accountLink"), false);
        add(accountLink);

        mappingContainer = new WebMarkupContainer("mappingContainer");
        mappingContainer.setOutputMarkupId(true);
        add(mappingContainer);

        mappings = new ListView<SchemaMappingTO>(
                "mappings", resourceTO.getMappings()) {

            private static final long serialVersionUID = 4949588177564901031L;

            @Override
            protected void populateItem(
                    final ListItem<SchemaMappingTO> item) {

                final SchemaMappingTO mappingTO = item.getModelObject();

                item.add(new AjaxDecoratedCheckbox("toRemove",
                        new Model(Boolean.FALSE)) {

                    private static final long serialVersionUID =
                            7170946748485726506L;

                    @Override
                    protected void onUpdate(
                            final AjaxRequestTarget target) {
                        int index = -1;
                        for (int i = 0; i < resourceTO.getMappings().
                                size()
                                && index == -1; i++) {

                            if (mappingTO.equals(
                                    resourceTO.getMappings().get(i))) {

                                index = i;
                            }
                        }

                        if (index != -1) {
                            resourceTO.getMappings().remove(index);
                            item.getParent().removeAll();
                            target.add(mappingContainer);
                        }
                    }

                    @Override
                    protected IAjaxCallDecorator getAjaxCallDecorator() {
                        return new AjaxPreprocessingCallDecorator(
                                super.getAjaxCallDecorator()) {

                            private static final long serialVersionUID =
                                    -7927968187160354605L;

                            @Override
                            public CharSequence preDecorateScript(
                                    final CharSequence script) {

                                return "if (confirm('"
                                        + getString("confirmDelete")
                                        + "'))"
                                        + "{" + script + "} "
                                        + "else {this.checked = false;}";
                            }
                        };
                    }
                });

                final AjaxDropDownChoicePanel intAttrNames =
                        new AjaxDropDownChoicePanel<String>(
                        "intAttrNames",
                        getString("intAttrNames"),
                        new PropertyModel(mappingTO, "intAttrName"),
                        true);
                intAttrNames.setChoices(resourceSchemaNames);
                intAttrNames.setRequired(true);
                intAttrNames.setStyleShet(
                        "ui-widget-content ui-corner-all short_fixedsize");

                if (mappingTO.getIntMappingType() == null) {
                    intAttrNames.setChoices(Collections.EMPTY_LIST);
                } else {
                    switch (mappingTO.getIntMappingType()) {
                        case UserSchema:
                            intAttrNames.setChoices(uSchemaAttrNames);
                            break;

                        case UserDerivedSchema:
                            intAttrNames.setChoices(uDerSchemaAttrNames);
                            break;

                        case UserVirtualSchema:
                            intAttrNames.setChoices(uVirSchemaAttrNames);
                            break;

                        case SyncopeUserId:
                            intAttrNames.setEnabled(false);
                            intAttrNames.setRequired(false);
                            intAttrNames.setChoices(Collections.EMPTY_LIST);
                            mappingTO.setIntAttrName("SyncopeUserId");
                            break;

                        case Password:
                            intAttrNames.setEnabled(false);
                            intAttrNames.setRequired(false);
                            intAttrNames.setChoices(Collections.EMPTY_LIST);
                            mappingTO.setIntAttrName("Password");
                            break;

                        case Username:
                            intAttrNames.setEnabled(false);
                            intAttrNames.setRequired(false);
                            intAttrNames.setChoices(Collections.EMPTY_LIST);
                            mappingTO.setIntAttrName("Username");
                            break;

                        default:
                            intAttrNames.setChoices(Collections.EMPTY_LIST);
                    }
                }

                item.add(intAttrNames);

                final IntMappingTypesDropDownChoice mappingTypesPanel =
                        new IntMappingTypesDropDownChoice(
                        "intMappingTypes",
                        new ResourceModel("intMappingTypes", "intMappingTypes").
                        getObject(),
                        new PropertyModel<IntMappingType>(
                        mappingTO, "intMappingType"),
                        intAttrNames);

                mappingTypesPanel.setRequired(true);
                mappingTypesPanel.setChoices(intMappingTypes.getObject());
                mappingTypesPanel.setStyleShet(
                        "ui-widget-content ui-corner-all short_fixedsize");
                item.add(mappingTypesPanel);

                final FieldPanel extAttrName;

                if (resourceSchemaNames.isEmpty()) {
                    extAttrName = new AjaxTextFieldPanel(
                            "extAttrName",
                            new ResourceModel("extAttrNames", "extAttrNames").
                            getObject(),
                            new PropertyModel<String>(mappingTO, "extAttrName"),
                            true);

                } else {
                    extAttrName =
                            new AjaxDropDownChoicePanel<String>(
                            "extAttrName",
                            new ResourceModel("extAttrNames", "extAttrNames").
                            getObject(),
                            new PropertyModel(mappingTO, "extAttrName"),
                            true);
                    ((AjaxDropDownChoicePanel) extAttrName).setChoices(
                            resourceSchemaNames);

                }

                boolean required = mappingTO != null
                        && !mappingTO.isAccountid() && !mappingTO.isPassword();

                extAttrName.setRequired(required);
                extAttrName.setEnabled(required);

                extAttrName.setStyleShet(
                        "ui-widget-content ui-corner-all short_fixedsize");
                item.add(extAttrName);

                final AjaxTextFieldPanel mandatoryCondition =
                        new AjaxTextFieldPanel(
                        "mandatoryCondition",
                        new ResourceModel(
                        "mandatoryCondition", "mandatoryCondition").getObject(),
                        new PropertyModel(mappingTO,
                        "mandatoryCondition"),
                        true);

                mandatoryCondition.setChoices(
                        Arrays.asList(new String[]{"true", "false"}));

                mandatoryCondition.setStyleShet(
                        "ui-widget-content ui-corner-all short_fixedsize");

                item.add(mandatoryCondition);

                final AjaxCheckBoxPanel accountId =
                        new AjaxCheckBoxPanel(
                        "accountId",
                        new ResourceModel("accountId", "accountId").getObject(),
                        new PropertyModel(mappingTO, "accountid"), false);

                accountId.getField().add(
                        new AjaxFormComponentUpdatingBehavior("onchange") {

                            private static final long serialVersionUID =
                                    -1107858522700306810L;

                            @Override
                            protected void onUpdate(
                                    final AjaxRequestTarget target) {
                                extAttrName.setEnabled(
                                        !accountId.getModelObject()
                                        && !mappingTO.isPassword());
                                extAttrName.setModelObject(null);
                                extAttrName.setRequired(
                                        !accountId.getModelObject());
                                target.add(extAttrName);
                            }
                        });

                item.add(accountId);

                final AjaxCheckBoxPanel password =
                        new AjaxCheckBoxPanel(
                        "password",
                        new ResourceModel("password", "password").getObject(),
                        new PropertyModel(mappingTO, "password"), true);

                password.getField().add(
                        new AjaxFormComponentUpdatingBehavior("onchange") {

                            private static final long serialVersionUID =
                                    -1107858522700306810L;

                            @Override
                            protected void onUpdate(
                                    AjaxRequestTarget target) {
                                extAttrName.setEnabled(
                                        !mappingTO.isAccountid()
                                        && !password.getModelObject());
                                extAttrName.setModelObject(null);
                                extAttrName.setRequired(
                                        !password.getModelObject());
                                target.add(extAttrName);
                            }
                        });

                item.add(password);
            }
        };

        mappings.setReuseItems(true);
        mappingContainer.add(mappings);

        addSchemaMappingBtn = new IndicatingAjaxButton(
                "addUserSchemaMappingBtn", new ResourceModel("add")) {

            private static final long serialVersionUID = -4804368561204623354L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target,
                    final Form form) {

                resourceTO.getMappings().add(new SchemaMappingTO());
                target.add(mappingContainer);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                // ignore errors
            }
        };

        addSchemaMappingBtn.setDefaultFormProcessing(false);
        addSchemaMappingBtn.setEnabled(!createFlag);
        mappingContainer.add(addSchemaMappingBtn);

    }

    /**
     * Extension class of DropDownChoice. It's purposed for storing values in
     * the corresponding property model after pressing 'Add' button.
     */
    private class IntMappingTypesDropDownChoice
            extends AjaxDropDownChoicePanel {

        private static final long serialVersionUID = -2855668124505116627L;

        public IntMappingTypesDropDownChoice(
                final String id,
                final String name,
                final PropertyModel<IntMappingType> model,
                final AjaxDropDownChoicePanel<String> chooserToPopulate) {

            super(id, name, model, false);

            field.add(new AjaxFormComponentUpdatingBehavior("onchange") {

                private static final long serialVersionUID =
                        -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {

                    chooserToPopulate.setRequired(true);
                    chooserToPopulate.setEnabled(true);

                    final List<String> result;

                    switch (model.getObject()) {
                        case UserSchema:
                            result = uSchemaAttrNames;
                            break;

                        case UserDerivedSchema:
                            result = uDerSchemaAttrNames;
                            break;

                        case UserVirtualSchema:
                            result = uVirSchemaAttrNames;
                            break;

                        case SyncopeUserId:
                        case Password:
                        case Username:
                        default:
                            chooserToPopulate.setRequired(false);
                            chooserToPopulate.setEnabled(false);
                            result = Collections.EMPTY_LIST;
                    }

                    chooserToPopulate.setChoices(result);
                    target.add(chooserToPopulate);
                }
            });
        }
    }

    /**
     * Initialize resource schema names.
     */
    private void initResourceSchemaNames() {
        if (resourceTO != null
                && resourceTO.getConnectorId() != null
                && resourceTO.getConnectorId() > 0) {

            final ConnInstanceTO connInstanceTO = new ConnInstanceTO();
            connInstanceTO.setId(resourceTO.getConnectorId());

            connInstanceTO.setConfiguration(
                    resourceTO.getConnConfProperties());

            resourceSchemaNames = getResourceSchemaNames(
                    resourceTO.getConnectorId(),
                    resourceTO.getConnConfProperties());

        } else {
            resourceSchemaNames = Collections.EMPTY_LIST;
        }
    }

    /**
     * Get resource schema names.
     *
     * @param connectorId connector id.
     * @param conf connector configuration properties.
     * @return resource schema names.
     */
    private List<String> getResourceSchemaNames(
            final Long connectorId, final Set<ConnConfProperty> conf) {
        final List<String> names = new ArrayList<String>();

        try {

            final ConnInstanceTO connInstanceTO = new ConnInstanceTO();
            connInstanceTO.setId(connectorId);
            connInstanceTO.setConfiguration(conf);

            names.addAll(connRestClient.getSchemaNames(connInstanceTO));

        } catch (Exception e) {
            LOG.warn("Error retrieving resource schema names", e);
        }

        return names;
    }

    @Override
    public void onEvent(final IEvent<?> event) {

        if (event.getPayload() instanceof ConnConfModEvent) {

            final AjaxRequestTarget target =
                    ((ConnConfModEvent) event.getPayload()).getTarget();

            final List<ConnConfProperty> conf =
                    ((ConnConfModEvent) event.getPayload()).getConfiguration();

            mappings.removeAll();

            addSchemaMappingBtn.setEnabled(
                    resourceTO.getConnectorId() != null
                    && resourceTO.getConnectorId() > 0);

            resourceSchemaNames = getResourceSchemaNames(
                    resourceTO.getConnectorId(),
                    new HashSet<ConnConfProperty>(conf));

            target.add(mappingContainer);
        }
    }
}
