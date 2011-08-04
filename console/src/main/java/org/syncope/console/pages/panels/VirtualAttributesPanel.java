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

import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxPreprocessingCallDecorator;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
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
import org.syncope.client.to.AbstractAttributableTO;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.to.RoleTO;
import org.syncope.client.to.UserTO;
import org.syncope.console.rest.SchemaRestClient;
import org.syncope.console.wicket.markup.html.form.AjaxDecoratedCheckbox;

public class VirtualAttributesPanel extends Panel {

    /**
     * Logger.
     */
    protected static final Logger LOG =
            LoggerFactory.getLogger(VirtualAttributesPanel.class);

    @SpringBean
    private SchemaRestClient schemaRestClient;

    public <T extends AbstractAttributableTO> VirtualAttributesPanel(
            final String id, final T entityTO) {

        super(id);

        setOutputMarkupId(true);

        final IModel<List<String>> virtualSchemaNames =
                new LoadableDetachableModel<List<String>>() {

                    @Override
                    protected List<String> load() {
                        if (entityTO instanceof RoleTO) {
                            return schemaRestClient.getVirtualSchemaNames(
                                    "role");
                        } else if (entityTO instanceof UserTO) {
                            return schemaRestClient.getVirtualSchemaNames(
                                    "user");
                        } else {
                            return schemaRestClient.getVirtualSchemaNames(
                                    "membership");
                        }
                    }
                };

        final WebMarkupContainer attributesContainer =
                new WebMarkupContainer("virAttrContainer");

        attributesContainer.setOutputMarkupId(true);
        add(attributesContainer);

        AjaxButton addAttributeBtn = new IndicatingAjaxButton(
                "addAttributeBtn", new Model(getString("addAttributeBtn"))) {

            @Override
            protected void onSubmit(final AjaxRequestTarget target,
                    final Form form) {

                entityTO.addVirtualAttribute(new AttributeTO());
                target.addComponent(attributesContainer);
            }
        };

        add(addAttributeBtn.setDefaultFormProcessing(Boolean.FALSE));

        ListView<AttributeTO> attributes = new ListView<AttributeTO>(
                "attributes",
                new PropertyModel<List<? extends AttributeTO>>(
                entityTO, "virtualAttributes")) {

            @Override
            protected void populateItem(final ListItem<AttributeTO> item) {
                final AttributeTO attributeTO = item.getModelObject();

                item.add(new AjaxDecoratedCheckbox(
                        "toRemove", new Model(Boolean.FALSE)) {

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        entityTO.removeVirtualAttribute(attributeTO);
                        target.addComponent(attributesContainer);
                    }

                    @Override
                    protected IAjaxCallDecorator getAjaxCallDecorator() {
                        return new AjaxPreprocessingCallDecorator(
                                super.getAjaxCallDecorator()) {

                            @Override
                            public CharSequence preDecorateScript(
                                    final CharSequence script) {

                                return "if (confirm('"
                                        + getString("confirmDelete") + "'))"
                                        + "{" + script + "} "
                                        + "else {this.checked = false;}";
                            }
                        };
                    }
                });

                final DropDownChoice<String> schemaChoice =
                        new DropDownChoice<String>(
                        "schema",
                        new PropertyModel<String>(attributeTO, "schema"),
                        virtualSchemaNames);

                schemaChoice.add(new AjaxFormComponentUpdatingBehavior("onblur") {

                    @Override
                    protected void onUpdate(AjaxRequestTarget art) {
                        attributeTO.setSchema(schemaChoice.getModelObject());
                    }
                });

                schemaChoice.setOutputMarkupId(true);
                schemaChoice.setRequired(true);
                item.add(schemaChoice);

                if (attributeTO.getValues().isEmpty()) {
                    attributeTO.addValue("");
                }

                item.add(new ListView<String>(
                        "values", new PropertyModel<List<String>>(
                        attributeTO, "values")) {

                    @Override
                    protected void populateItem(final ListItem<String> item) {

                        final TextField field = new TextField(
                                "value",
                                item.getModel(),
                                String.class);

                        field.add(new AjaxFormComponentUpdatingBehavior("onblur") {

                            @Override
                            protected void onUpdate(AjaxRequestTarget art) {
                                attributeTO.getValues().set(
                                        item.getIndex(), item.getModelObject());
                            }
                        });

                        item.add(field);

                        AjaxButton dropButton = new IndicatingAjaxButton("drop",
                                new Model(getString("drop"))) {

                            @Override
                            protected void onSubmit(
                                    AjaxRequestTarget target, Form form) {

                                //Drop current component
                                attributeTO.getValues().remove(
                                        item.getModelObject());

                                target.addComponent(attributesContainer);
                            }
                        };

                        item.add(dropButton.setDefaultFormProcessing(
                                Boolean.FALSE));

                        AjaxButton addButton = new IndicatingAjaxButton("add",
                                new Model(getString("add"))) {

                            @Override
                            protected void onSubmit(
                                    AjaxRequestTarget target, Form form) {

                                attributeTO.addValue("");
                                target.addComponent(attributesContainer);
                            }
                        };

                        item.add(addButton.setDefaultFormProcessing(
                                Boolean.FALSE));

                        if (item.getIndex()
                                == attributeTO.getValues().size() - 1) {
                            addButton.setVisible(Boolean.TRUE);
                            addButton.setEnabled(Boolean.TRUE);
                        } else {
                            addButton.setVisible(Boolean.FALSE);
                            addButton.setEnabled(Boolean.FALSE);
                        }

                        if (attributeTO.getValues().size() <= 1) {
                            dropButton.setVisible(Boolean.FALSE);
                            dropButton.setEnabled(Boolean.FALSE);
                        } else {
                            dropButton.setVisible(Boolean.TRUE);
                            dropButton.setEnabled(Boolean.TRUE);
                        }
                    }
                });
            }
        };

        attributesContainer.add(attributes);
    }
}
