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
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.syncope.client.to.AbstractAttributableTO;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.to.RoleTO;
import org.syncope.client.to.UserTO;
import org.syncope.console.rest.SchemaRestClient;
import org.syncope.console.wicket.markup.html.form.AjaxDecoratedCheckbox;

public class DerivedAttributesPanel extends Panel {

    /**
     * Logger.
     */
    protected static final Logger LOG =
            LoggerFactory.getLogger(DerivedAttributesPanel.class);

    @SpringBean
    private SchemaRestClient schemaRestClient;

    public <T extends AbstractAttributableTO> DerivedAttributesPanel(
            final String id, final T entityTO) {

        super(id);
        setOutputMarkupId(true);

        final IModel<List<String>> derivedSchemaNames =
                new LoadableDetachableModel<List<String>>() {

                    private static final long serialVersionUID =
                            5275935387613157437L;

                    @Override
                    protected List<String> load() {
                        if (entityTO instanceof RoleTO) {
                            return schemaRestClient.getDerivedSchemaNames(
                                    "role");
                        } else if (entityTO instanceof UserTO) {
                            return schemaRestClient.getDerivedSchemaNames(
                                    "user");
                        } else {
                            return schemaRestClient.getDerivedSchemaNames(
                                    "membership");
                        }
                    }
                };

        final WebMarkupContainer attributesContainer =
                new WebMarkupContainer("derAttrContainer");

        attributesContainer.setOutputMarkupId(true);
        add(attributesContainer);

        AjaxButton addAttributeBtn = new IndicatingAjaxButton(
                "addAttributeBtn", new ResourceModel("addAttributeBtn")) {

            private static final long serialVersionUID = -4804368561204623354L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target,
                    final Form form) {

                entityTO.getDerivedAttributes().add(new AttributeTO());
                target.addComponent(attributesContainer);
            }
        };

        add(addAttributeBtn.setDefaultFormProcessing(Boolean.FALSE));

        final ListView<AttributeTO> attributes = new ListView<AttributeTO>(
                "attributes",
                new PropertyModel<List<? extends AttributeTO>>(
                entityTO, "derivedAttributes")) {

            private static final long serialVersionUID =
                    9101744072914090143L;

            @Override
            protected void populateItem(final ListItem<AttributeTO> item) {
                final AttributeTO attributeTO = item.getModelObject();

                item.add(new AjaxDecoratedCheckbox(
                        "toRemove", new Model(Boolean.FALSE)) {

                    private static final long serialVersionUID =
                            7170946748485726506L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        entityTO.getDerivedAttributes().remove(attributeTO);
                        target.addComponent(attributesContainer);
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
                        derivedSchemaNames);

                schemaChoice.add(new AjaxFormComponentUpdatingBehavior("onblur") {

                    private static final long serialVersionUID =
                            -1107858522700306810L;

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
    }
}
