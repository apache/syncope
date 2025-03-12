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

import java.io.Serializable;
import org.apache.syncope.client.console.rest.WAConfigRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class WAConfigModalPanel extends AbstractModalPanel<Attr> {

    private static final long serialVersionUID = 1690738212977L;

    protected static ConfParam toConfParam(final Attr attr) {
        ConfParam param = new ConfParam();
        param.setSchema(attr.getSchema());
        param.getValues().addAll(attr.getValues());
        return param;
    }

    protected static Attr toAttr(final ConfParam confParam) {
        Attr attr = new Attr.Builder(confParam.getSchema()).build();
        attr.getValues().addAll(
                confParam.getValues().stream().map(Serializable::toString).toList());
        return attr;
    }

    @SpringBean
    protected WAConfigRestClient waConfigRestClient;

    protected final ParametersWizardPanel.ParametersForm form;

    public WAConfigModalPanel(
            final BaseModal<Attr> modal,
            final Attr attr,
            final AjaxWizard.Mode mode,
            final PageReference pageRef) {

        super(modal, pageRef);

        PlainSchemaTO schema = new PlainSchemaTO();
        schema.setType(AttrSchemaType.String);
        schema.setMandatoryCondition("true");
        schema.setMultivalue(false);

        form = new ParametersWizardPanel.ParametersForm(schema, toConfParam(attr));

        add(new ParametersWizardPanel(form, null, pageRef) {

            private static final long serialVersionUID = -1469319240177117600L;

            @Override
            protected WizardModel buildModelSteps(final ParametersForm modelObject, final WizardModel wizardModel) {
                wizardModel.add(new ParametersWizardAttrStep(mode, modelObject));
                return wizardModel;
            }

            @Override
            protected Serializable onApplyInternal(final ParametersWizardPanel.ParametersForm modelObject) {
                modelObject.getParam().setMultivalue(modelObject.getSchema().isMultivalue());
                try {
                    waConfigRestClient.set(toAttr(modelObject.getParam()));
                } catch (Exception e) {
                    LOG.error("While setting {}", modelObject.getParam(), e);
                    throw e;
                }
                return modelObject.getParam();
            }
        }.build("waConfigWizardPanel", mode));
    }

    @Override
    public final Attr getItem() {
        return toAttr(form.getParam());
    }
}
