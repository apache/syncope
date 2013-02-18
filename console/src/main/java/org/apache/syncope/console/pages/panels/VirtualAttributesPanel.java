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
package org.apache.syncope.console.pages.panels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.to.AttributeTO;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.to.VirtualSchemaTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.console.rest.SchemaRestClient;
import org.apache.syncope.console.wicket.ajax.markup.html.ClearIndicatingAjaxButton;
import org.apache.syncope.console.wicket.markup.html.form.AjaxDecoratedCheckbox;
import org.apache.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.console.wicket.markup.html.form.MultiValueSelectorPanel;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
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

public class VirtualAttributesPanel extends Panel {

    private static final long serialVersionUID = -7982691107029848579L;

    @SpringBean
    private SchemaRestClient schemaRestClient;

    public <T extends AbstractAttributableTO> VirtualAttributesPanel(final String id, final T entityTO,
            final boolean templateMode, final PageReference pageRef) {

        super(id);

        setOutputMarkupId(true);

        final IModel<Map<String, VirtualSchemaTO>> schemas =
                new LoadableDetachableModel<Map<String, VirtualSchemaTO>>() {

                    private static final long serialVersionUID = -5489981430516587774L;

                    @Override
                    protected Map<String, VirtualSchemaTO> load() {
                        final List<VirtualSchemaTO> schemaTOs;
                        if (entityTO instanceof RoleTO) {
                            schemaTOs = schemaRestClient.getVirtualSchemas(AttributableType.ROLE);
                        } else if (entityTO instanceof UserTO) {
                            schemaTOs = schemaRestClient.getVirtualSchemas(AttributableType.USER);
                        } else {
                            schemaTOs = schemaRestClient.getVirtualSchemas(AttributableType.MEMBERSHIP);
                        }

                        final Map<String, VirtualSchemaTO> schemas = new HashMap<String, VirtualSchemaTO>();

                        for (VirtualSchemaTO schemaTO : schemaTOs) {
                            schemas.put(schemaTO.getName(), schemaTO);
                        }

                        return schemas;
                    }
                };

        final List<String> virtualSchemaNames = new ArrayList<String>(schemas.getObject().keySet());

        final WebMarkupContainer attributesContainer = new WebMarkupContainer("virAttrContainer");

        attributesContainer.setOutputMarkupId(true);
        add(attributesContainer);

        AjaxButton addAttributeBtn = new ClearIndicatingAjaxButton("addAttributeBtn",
                new ResourceModel("addAttributeBtn"), pageRef) {

            private static final long serialVersionUID = -4804368561204623354L;

            @Override
            protected void onSubmitInternal(final AjaxRequestTarget target, final Form<?> form) {
                entityTO.addVirtualAttribute(new AttributeTO());
                target.add(attributesContainer);
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                target.add(attributesContainer);
            }
        };

        add(addAttributeBtn.setDefaultFormProcessing(Boolean.FALSE));

        ListView<AttributeTO> attributes = new ListView<AttributeTO>("attributes",
                new PropertyModel<List<? extends AttributeTO>>(entityTO, "virtualAttributes")) {

            private static final long serialVersionUID = 9101744072914090143L;

            @Override
            protected void populateItem(final ListItem<AttributeTO> item) {
                final AttributeTO attributeTO = item.getModelObject();

                item.add(new AjaxDecoratedCheckbox("toRemove", new Model<Boolean>(Boolean.FALSE)) {

                    private static final long serialVersionUID = 7170946748485726506L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        entityTO.removeVirtualAttribute(attributeTO);
                        target.add(attributesContainer);
                    }

                    @Override
                    protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                        super.updateAjaxAttributes(attributes);

                        final AjaxCallListener ajaxCallListener = new AjaxCallListener() {

                            private static final long serialVersionUID = 7160235486520935153L;

                            @Override
                            public CharSequence getPrecondition(final Component component) {
                                return "if (!confirm('" + getString("confirmDelete") + "')) return false;";
                            }
                        };
                        attributes.getAjaxCallListeners().add(ajaxCallListener);
                    }
                });

                if (attributeTO.getValues().isEmpty()) {
                    attributeTO.addValue("");
                }

                if (attributeTO.getSchema() != null) {
                    VirtualSchemaTO attributeSchema = schemas.getObject().get(attributeTO.getSchema());
                    if (attributeSchema != null) {
                        attributeTO.setReadonly(attributeSchema.isReadonly());
                    }
                }

                final AjaxTextFieldPanel panel;
                final MultiValueSelectorPanel multiPanel;
                if (templateMode) {
                    panel = new AjaxTextFieldPanel("values", "values", new Model<String>());
                    panel.setReadOnly(attributeTO.isReadonly());
                    multiPanel = null;
                } else {
                    panel = new AjaxTextFieldPanel("panel", "values", new Model<String>(null));
                    panel.setReadOnly(attributeTO.isReadonly());
                    multiPanel = new MultiValueSelectorPanel("values", new PropertyModel<List<String>>(attributeTO,
                            "values"), panel);
                }

                final DropDownChoice<String> schemaChoice = new DropDownChoice<String>("schema",
                        new PropertyModel<String>(attributeTO, "schema"), virtualSchemaNames,
                        new ChoiceRenderer<String>() {

                            private static final long serialVersionUID = 3109256773218160485L;

                            @Override
                            public Object getDisplayValue(final String object) {
                                final StringBuilder text = new StringBuilder(object);
                                if (templateMode) {
                                    text.append(" (JEXL)");
                                }
                                return text.toString();
                            }
                        });

                schemaChoice.add(new AjaxFormComponentUpdatingBehavior("onblur") {

                    private static final long serialVersionUID = -1107858522700306810L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {

                        attributeTO.setSchema(schemaChoice.getModelObject());

                        VirtualSchemaTO attributeSchema =
                                schemas.getObject().get(attributeTO.getSchema());
                        if (attributeSchema != null) {
                            attributeTO.setReadonly(attributeSchema.isReadonly());
                            panel.setReadOnly(attributeTO.isReadonly());
                        }

                        if (multiPanel != null) {
                            multiPanel.getView().setEnabled(false);
                        }
                    }
                });

                schemaChoice.add(new AjaxFormComponentUpdatingBehavior("onchange") {

                    private static final long serialVersionUID = -1107858522700306810L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        target.add(attributesContainer);
                    }
                });

                schemaChoice.setOutputMarkupId(true);
                schemaChoice.setRequired(true);
                item.add(schemaChoice);

                if (templateMode) {
                    item.add(panel);
                } else {
                    item.add(multiPanel);
                }
            }
        };

        attributesContainer.add(attributes);
    }
}
