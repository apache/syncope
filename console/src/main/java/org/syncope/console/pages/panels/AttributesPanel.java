/*
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

import java.io.Serializable;
import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.syncope.client.to.AbstractAttributableTO;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.to.RoleTO;
import org.syncope.client.to.SchemaTO;
import org.syncope.client.to.UserTO;
import org.syncope.console.pages.Schema;
import org.syncope.console.rest.SchemaRestClient;
import org.syncope.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.syncope.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.syncope.console.wicket.markup.html.form.DateFieldPanel;
import org.syncope.types.SchemaType;

public class AttributesPanel extends Panel {

    /**
     * Logger.
     */
    protected static final Logger LOG =
            LoggerFactory.getLogger(AttributesPanel.class);

    @SpringBean
    private SchemaRestClient schemaRestClient;

    final WebMarkupContainer attributesContainer;

    public <T extends AbstractAttributableTO> AttributesPanel(
            final String id, final T entityTO, final Form form) {
        super(id);

        final IModel<Map<String, SchemaTO>> schemas =
                new LoadableDetachableModel<Map<String, SchemaTO>>() {

                    @Override
                    protected Map<String, SchemaTO> load() {
                        final List<SchemaTO> schemaTOs;
                        if (entityTO instanceof RoleTO) {
                            schemaTOs = schemaRestClient.getSchemas(
                                    "role");
                        } else if (entityTO instanceof UserTO) {
                            schemaTOs = schemaRestClient.getSchemas(
                                    "user");
                        } else {
                            schemaTOs = schemaRestClient.getSchemas(
                                    "membership");
                        }

                        final Map<String, SchemaTO> schemas =
                                new HashMap<String, SchemaTO>();

                        for (SchemaTO schemaTO : schemaTOs) {
                            schemas.put(schemaTO.getName(), schemaTO);
                        }

                        return schemas;
                    }
                };

        attributesContainer = new WebMarkupContainer("container");
        attributesContainer.setOutputMarkupId(true);
        add(attributesContainer);

        initEntityData(entityTO, schemas.getObject().values());

        final ListView<AttributeTO> attributeView = new ListView<AttributeTO>(
                "schemas", new PropertyModel<List<? extends AttributeTO>>(
                entityTO, "attributes")) {

            @Override
            protected void populateItem(ListItem item) {
                final AttributeTO attributeTO =
                        (AttributeTO) item.getDefaultModelObject();

                item.add(new Label("name", attributeTO.getSchema()));

                item.add(new ListView(
                        "fields", attributeTO.getValues()) {

                    @Override
                    protected void populateItem(final ListItem item) {
                        item.add(getFieldPanel(schemas.getObject().get(
                                attributeTO.getSchema()), form, item));
                    }
                });

                final AjaxButton addButton =
                        new IndicatingAjaxButton("add",
                        new Model(getString("add"))) {

                            @Override
                            protected void onSubmit(
                                    final AjaxRequestTarget target,
                                    final Form form) {
                                attributeTO.getValues().add("");
                                target.addComponent(attributesContainer);
                            }
                        };

                final AjaxButton dropButton =
                        new IndicatingAjaxButton("drop",
                        new Model(getString("drop"))) {

                            @Override
                            protected void onSubmit(
                                    final AjaxRequestTarget target,
                                    final Form form) {
                                //Drop the last component added
                                attributeTO.getValues().remove(
                                        attributeTO.getValues().size() - 1);

                                target.addComponent(attributesContainer);
                            }
                        };

                if (schemas.getObject().get(attributeTO.getSchema()).
                        getType() == SchemaType.Boolean) {
                    addButton.setVisible(false);
                    dropButton.setVisible(false);
                }

                addButton.setVisible(
                        schemas.getObject().get(
                        attributeTO.getSchema()).isMultivalue());
                dropButton.setVisible(
                        schemas.getObject().get(
                        attributeTO.getSchema()).isMultivalue());

                dropButton.setVisible(
                        attributeTO.getValues().size() > 1);

                addButton.setEnabled(!attributeTO.isReadonly());
                dropButton.setEnabled(!attributeTO.isReadonly());

                addButton.setDefaultFormProcessing(false);
                dropButton.setDefaultFormProcessing(false);

                item.add(addButton);
                item.add(dropButton);
            }
        };

        attributesContainer.add(attributeView);
    }

    private List<AttributeTO> initEntityData(
            final AbstractAttributableTO entityTO,
            final Collection<SchemaTO> schemas) {

        final List<AttributeTO> entityData = new ArrayList<AttributeTO>();

        final Map<String, List<String>> attributeMap =
                entityTO.getAttributeMap();

        AttributeTO attributeTO;
        List<String> values;

        for (SchemaTO schema : schemas) {
            attributeTO = new AttributeTO();
            attributeTO.setSchema(schema.getName());

            if (attributeMap.get(schema.getName()) == null
                    || attributeMap.get(schema.getName()).isEmpty()) {

                values = new ArrayList<String>();
                values.add("");
                attributeTO.setValues(values);

                // is important to set readonly only after valus setting
                attributeTO.setReadonly(schema.isReadonly());

            } else {
                attributeTO.setValues(attributeMap.get(schema.getName()));
            }
            entityData.add(attributeTO);
        }

        entityTO.setAttributes(entityData);

        return entityData;
    }

    private Panel getFieldPanel(
            final SchemaTO schemaTO,
            final Form form,
            final ListItem item) {
        Panel panel;

        boolean required =
                schemaTO.getMandatoryCondition().equalsIgnoreCase("true");

        switch (schemaTO.getType()) {
            case Boolean:
                panel = new AjaxCheckBoxPanel(
                        "panel", schemaTO.getName(), new Model() {

                    @Override
                    public Serializable getObject() {
                        return (String) item.getModelObject();
                    }

                    @Override
                    public void setObject(Serializable object) {
                        item.setModelObject(((Boolean) object).toString());
                    }
                }, required, schemaTO.isReadonly());

                break;
            case Date:
                panel = new DateFieldPanel(
                        "panel", schemaTO.getName(), new Model() {

                    @Override
                    public Serializable getObject() {
                        final DateFormat formatter = new SimpleDateFormat(
                                schemaTO.getConversionPattern());
                        Date date = null;
                        try {
                            String dateValue = (String) item.getModelObject();
                            //Default value:yyyy-MM-dd
                            if (StringUtils.hasText(dateValue)) {
                                date = formatter.parse(dateValue);
                            }
                        } catch (ParseException e) {
                            LOG.error("While parsing date", e);
                        }
                        return date;
                    }

                    @Override
                    public void setObject(Serializable object) {
                        if (object != null) {
                            final Format formatter = new SimpleDateFormat(
                                    schemaTO.getConversionPattern());
                            item.setModelObject(formatter.format((Date) object));
                        } else {
                            item.setModelObject(object);
                        }
                    }
                }, schemaTO.getConversionPattern(),
                        required,
                        schemaTO.isReadonly(),
                        form);
                break;

            case Enum:
                panel = new AjaxDropDownChoicePanel(
                        "panel", schemaTO.getName(), new Model() {

                    @Override
                    public Serializable getObject() {
                        return (String) item.getModelObject();
                    }

                    @Override
                    public void setObject(Serializable object) {
                        item.setModelObject((String) object);
                    }
                }, Arrays.asList(schemaTO.getEnumerationValues().
                        split(Schema.enumValuesSeparator)),
                        new ChoiceRenderer(),
                        required);
                break;

            default:
                panel = new AjaxTextFieldPanel(
                        "panel", schemaTO.getName(), new Model() {

                    @Override
                    public Serializable getObject() {
                        return (String) item.getModelObject();
                    }

                    @Override
                    public void setObject(Serializable object) {
                        item.setModelObject((String) object);
                    }
                }, required, schemaTO.isReadonly());
        }

        return panel;
    }
}
