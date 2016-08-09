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
package org.apache.syncope.client.console.panels;

import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.commons.PropertyList;
import org.apache.syncope.client.console.wicket.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

public class ParametersCreateWizardSchemaStep extends WizardStep {

    private static final long serialVersionUID = -7843275202297616553L;

    public ParametersCreateWizardSchemaStep(final ParametersCreateWizardPanel.ParametersForm modelObject) {
        modelObject.getPlainSchemaTO().setMandatoryCondition("false");

        final WebMarkupContainer content = new WebMarkupContainer("content");
        this.setOutputMarkupId(true);
        content.setOutputMarkupId(true);
        add(content);
        final AjaxDropDownChoicePanel<AttrSchemaType> type = new AjaxDropDownChoicePanel<>(
                "type", getString("type"), new PropertyModel<AttrSchemaType>(modelObject.getPlainSchemaTO(), "type"));
        type.setChoices(Arrays.asList(AttrSchemaType.values()));
        content.add(type);

        final MultiFieldPanel<String> panel = new MultiFieldPanel.Builder<String>(
                new PropertyModel<List<String>>(modelObject.getPlainSchemaTO(), "enumerationValues") {

            private static final long serialVersionUID = 3985215199105092649L;

            @Override
            public PropertyList<PlainSchemaTO> getObject() {
                return new PropertyList<PlainSchemaTO>() {

                    @Override
                    public String getValues() {
                        return modelObject.getPlainSchemaTO().getEnumerationValues();
                    }

                    @Override
                    public void setValues(final List<String> list) {
                        modelObject.getPlainSchemaTO().setEnumerationValues(getEnumValuesAsString(list));
                    }
                };
            }

            @Override
            public void setObject(final List<String> object) {
                modelObject.getPlainSchemaTO().setEnumerationValues(PropertyList.getEnumValuesAsString(object));
            }
        }) {

            private static final long serialVersionUID = -8752965211744734798L;

            @Override
            protected String newModelObject() {
                return StringUtils.EMPTY;
            }

        }.build("values", getString("values"), new AjaxTextFieldPanel(
                "panel", getString("values"), new Model<String>(), false));

        panel.setVisible(false);
        content.add(panel);

        type.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior("onchange") {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                if ("enum".equalsIgnoreCase(type.getField().getModelObject().name())) {
                    panel.setVisible(true);
                    content.add(panel);
                    target.add(content);
                } else {
                    panel.setVisible(false);
                    content.add(panel);
                    target.add(content);
                }
            }
        });

        final AjaxCheckBoxPanel multiValue = new AjaxCheckBoxPanel("panel", getString("multivalue"),
                new PropertyModel<Boolean>(modelObject.getPlainSchemaTO(), "multivalue"), false);
        content.add(multiValue);
    }
}
