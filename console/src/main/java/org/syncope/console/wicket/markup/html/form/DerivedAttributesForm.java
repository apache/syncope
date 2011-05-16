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
package org.syncope.console.wicket.markup.html.form;

import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxPreprocessingCallDecorator;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.syncope.client.to.AbstractAttributableTO;
import org.syncope.client.to.AttributeTO;

public class DerivedAttributesForm extends Form {

    /**
     * Logger.
     */
    protected static final Logger LOG =
            LoggerFactory.getLogger(DerivedAttributesForm.class);

    public DerivedAttributesForm(String id, IModel<?> model) {
        super(id, model);
    }

    public DerivedAttributesForm(String id) {
        super(id);
    }

    public <T extends AbstractAttributableTO> Form build(
            final WebPage page,
            final T entityTO,
            final IModel<List<String>> schemaNames) {

        setOutputMarkupId(true);

        final WebMarkupContainer attributesContainer =
                new WebMarkupContainer("derAttrContainer");

        attributesContainer.setOutputMarkupId(true);
        add(attributesContainer);

        AjaxButton addAttributeBtn = new IndicatingAjaxButton(
                "addAttributeBtn",
                new Model(page.getString("addAttributeBtn"))) {

            @Override
            protected void onSubmit(final AjaxRequestTarget target,
                    final Form form) {

                entityTO.getDerivedAttributes().add(new AttributeTO());
                target.addComponent(attributesContainer);
            }
        };

        add(addAttributeBtn.setDefaultFormProcessing(Boolean.FALSE));

        ListView<AttributeTO> attributes = new ListView<AttributeTO>(
                "attributes",
                new PropertyModel<List<? extends AttributeTO>>(
                entityTO, "derivedAttributes")) {

            @Override
            protected void populateItem(final ListItem<AttributeTO> item) {
                final AttributeTO attributeTO = item.getModelObject();

                item.add(new AjaxDecoratedCheckbox(
                        "toRemove", new Model(Boolean.FALSE)) {

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        entityTO.getDerivedAttributes().remove(attributeTO);
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
                        schemaNames);

                schemaChoice.add(new AjaxFormComponentUpdatingBehavior("onblur") {

                    @Override
                    protected void onUpdate(AjaxRequestTarget art) {
                        attributeTO.setSchema(schemaChoice.getModelObject());
                    }
                });

                item.add(schemaChoice.setRequired(true));

                schemaChoice.setOutputMarkupId(true);
                schemaChoice.setRequired(true);
                item.add(schemaChoice);

                final List<String> values = attributeTO.getValues();

                if (values == null || values.isEmpty()) {
                    item.add(new TextField(
                            "value",
                            new Model(null)).setVisible(Boolean.FALSE));
                } else {
                    item.add(new TextField(
                            "value",
                            new Model(values.get(0))).setEnabled(
                            Boolean.FALSE));
                }
            }
        };

        attributesContainer.add(attributes);
        return this;
    }
}
