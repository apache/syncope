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
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;
import org.syncope.client.to.ConnInstanceTO;
import org.syncope.client.to.ResourceTO;
import org.syncope.client.util.ConnConfPropUtils;
import org.syncope.console.pages.BaseModalPage;
import org.syncope.console.pages.ResourceModalPage.ResourceEvent;
import org.syncope.console.pages.panels.ResourceDetailsPanel.DetailsModEvent;
import org.syncope.console.rest.ConnectorRestClient;
import org.syncope.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.syncope.console.wicket.markup.html.form.AjaxNumberFieldPanel;
import org.syncope.console.wicket.markup.html.form.AjaxPasswordFieldPanel;
import org.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.syncope.console.wicket.markup.html.form.FieldPanel;
import org.syncope.console.wicket.markup.html.form.MultiValueSelectorPanel;
import org.syncope.console.wicket.markup.html.form.MultiValueSelectorPanel.MultiValueSelectorEvent;
import org.syncope.types.ConnConfProperty;

public class ResourceConnConfPanel extends Panel {

    private static final long serialVersionUID = -7982691107029848579L;

    /**
     * Logger.
     */
    protected static final Logger LOG =
            LoggerFactory.getLogger(ResourceConnConfPanel.class);

    /**
     * GuardedString is not in classpath.
     */
    private static final String GUARDED_STRING =
            "org.identityconnectors.common.security.GuardedString";

    /**
     * GuardedByteArray is not in classpath.
     */
    private static final String GUARDED_BYTE_ARRAY =
            "org.identityconnectors.common.security.GuardedByteArray";

    /**
     * Number java types.
     */
    private static final List<Class> NUMBER = Arrays.asList(new Class[]{
                Integer.class,
                Double.class,
                Long.class,
                Float.class,
                Number.class,
                Integer.TYPE,
                Long.TYPE,
                Double.TYPE,
                Float.TYPE});

    @SpringBean
    private ConnectorRestClient connRestClient;

    private List<ConnConfProperty> connConfProperties;

    private WebMarkupContainer connConfPropContainer;

    private AjaxLink check;

    private boolean createFlag;

    private ResourceTO resourceTO;

    public ResourceConnConfPanel(
            final String id,
            final ResourceTO resourceTO,
            final boolean createFlag) {

        super(id);
        setOutputMarkupId(true);

        this.createFlag = createFlag;
        this.resourceTO = resourceTO;

        connConfProperties = getConnConfProperties();

        connConfPropContainer =
                new WebMarkupContainer("connectorPropertiesContainer");
        connConfPropContainer.setOutputMarkupId(true);
        add(connConfPropContainer);

        check = new IndicatingAjaxLink("check", new ResourceModel("check")) {

            private static final long serialVersionUID =
                    -4199438518229098169L;

            @Override
            public void onClick(final AjaxRequestTarget target) {

                ConnInstanceTO connectorTO =
                        connRestClient.read(
                        resourceTO.getConnectorId());

                connectorTO.setConfiguration(
                        ConnConfPropUtils.joinConnInstanceProperties(
                        connectorTO.getConfigurationMap(),
                        ConnConfPropUtils.getConnConfPropertyMap(
                        resourceTO.getConnConfProperties())));

                if (connRestClient.check(
                        connectorTO).booleanValue()) {
                    info(getString("success_connection"));
                } else {
                    error(getString("error_connection"));
                }

                target.add(((BaseModalPage) getPage()).getFeedbackPanel());
            }
        };

        check.setEnabled(!connConfProperties.isEmpty());
        connConfPropContainer.add(check);

        /*
         * the list of overridable connector properties
         */
        connConfPropContainer.add(new ListView<ConnConfProperty>(
                "connectorProperties",
                new PropertyModel(this, "connConfProperties")) {

            private static final long serialVersionUID = 9101744072914090143L;

            @Override
            protected void populateItem(final ListItem<ConnConfProperty> item) {
                final ConnConfProperty property = item.getModelObject();

                final Label label = new Label("connPropAttrSchema",
                        property.getSchema().getDisplayName() == null
                        || property.getSchema().getDisplayName().isEmpty()
                        ? property.getSchema().getName()
                        : property.getSchema().getDisplayName());

                item.add(label);

                final FieldPanel field;

                boolean required = false;

                boolean isArray = false;

                if (GUARDED_STRING.equalsIgnoreCase(
                        property.getSchema().getType())
                        || GUARDED_BYTE_ARRAY.equalsIgnoreCase(
                        property.getSchema().getType())) {

                    field = new AjaxPasswordFieldPanel(
                            "panel",
                            label.getDefaultModelObjectAsString(),
                            new Model(),
                            true);

                    ((PasswordTextField) field.getField()).setResetPassword(
                            false);

                    required = property.getSchema().isRequired();

                } else {
                    Class propertySchemaClass;

                    try {
                        propertySchemaClass = ClassUtils.forName(
                                property.getSchema().getType(),
                                ClassUtils.getDefaultClassLoader());
                    } catch (Exception e) {
                        LOG.error("Error parsing attribute type", e);
                        propertySchemaClass = String.class;
                    }

                    if (NUMBER.contains(propertySchemaClass)) {
                        field = new AjaxNumberFieldPanel(
                                "panel",
                                label.getDefaultModelObjectAsString(),
                                new Model(),
                                ClassUtils.resolvePrimitiveIfNecessary(
                                propertySchemaClass),
                                true);

                        required = property.getSchema().isRequired();
                    } else if (Boolean.class.equals(propertySchemaClass)
                            || boolean.class.equals(propertySchemaClass)) {
                        field = new AjaxCheckBoxPanel(
                                "panel",
                                label.getDefaultModelObjectAsString(),
                                new Model(),
                                true);
                    } else {

                        field = new AjaxTextFieldPanel(
                                "panel",
                                label.getDefaultModelObjectAsString(),
                                new Model(),
                                true);

                        required = property.getSchema().isRequired();
                    }

                    if (String[].class.equals(propertySchemaClass)) {
                        isArray = true;
                    }
                }

                field.setTitle(property.getSchema().getHelpMessage());

                if (isArray) {
                    field.removeRequiredLabel();

                    if (property.getValues().isEmpty()) {
                        property.getValues().add(null);
                    }

                    final MultiValueSelectorPanel multiFields =
                            new MultiValueSelectorPanel<String>(
                            "panel",
                            new PropertyModel<List<String>>(property, "values"),
                            String.class,
                            field,
                            true);

                    item.add(multiFields);
                } else {
                    if (required) {
                        field.addRequiredLabel();
                    }

                    field.getField().add(
                            new AjaxFormComponentUpdatingBehavior("onchange") {

                                private static final long serialVersionUID =
                                        -1107858522700306810L;

                                @Override
                                protected void onUpdate(
                                        final AjaxRequestTarget target) {
                                    send(getPage(), Broadcast.BREADTH,
                                            new ConnConfModEvent(
                                            target, connConfProperties));
                                }
                            });

                    field.setNewModel(property.getValues());
                    item.add(field);
                }

                resourceTO.getConnConfProperties().add(property);
            }
        });
    }

    /**
     * Get overridable properties.
     *
     * @return overridable properties.
     */
    private List<ConnConfProperty> getConnConfProperties() {

        final List<ConnConfProperty> props = new ArrayList<ConnConfProperty>();

        if (!createFlag && !resourceTO.getConnConfProperties().isEmpty()) {
            props.addAll(resourceTO.getConnConfProperties());
        } else {
            resourceTO.getConnConfProperties().clear();

            final Long connectorId = resourceTO.getConnectorId();

            if (connectorId != null && connectorId > 0) {
                for (ConnConfProperty property :
                        connRestClient.getConnectorProperties(connectorId)) {

                    if (property.isOverridable()) {
                        props.add(property);
                    }
                }
            }
        }

        return props;
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof DetailsModEvent) {
            // connectro change: update properties and forward event
            final AjaxRequestTarget target =
                    ((ResourceEvent) event.getPayload()).getTarget();

            connConfProperties = getConnConfProperties();
            check.setEnabled(!connConfProperties.isEmpty());

            target.add(connConfPropContainer);

            // get configuration properties and send a new event
            send(getPage(), Broadcast.BREADTH,
                    new ConnConfModEvent(target, connConfProperties));
            
        } else if (event.getPayload() instanceof MultiValueSelectorEvent) {
            // multi value connector property change: forward event
            final AjaxRequestTarget target =
                    ((MultiValueSelectorEvent) event.getPayload()).getTarget();

            send(getPage(), Broadcast.BREADTH,
                    new ConnConfModEvent(target, connConfProperties));
        }
    }

    /**
     * Connector configuration properties modification event.
     */
    public static class ConnConfModEvent extends ResourceEvent {

        private List<ConnConfProperty> configuration;

        /**
         * Constructor.
         *
         * @param target request target.
         * @param target connector configuration properties.
         */
        public ConnConfModEvent(
                final AjaxRequestTarget target,
                final List<ConnConfProperty> conf) {
            super(target);
            this.configuration = conf;
        }

        public List<ConnConfProperty> getConfiguration() {
            return configuration;
        }
    }
}
