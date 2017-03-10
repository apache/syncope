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
import org.apache.syncope.client.console.rest.ConfRestClient;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.client.console.wizards.AjaxWizardBuilder;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel;

public class ParametersCreateWizardPanel extends AjaxWizardBuilder<ParametersCreateWizardPanel.ParametersForm> {

    private static final long serialVersionUID = -2868592590785581481L;

    private final ConfRestClient confRestClient = new ConfRestClient();

    private final SchemaRestClient schemaRestClient = new SchemaRestClient();

    public ParametersCreateWizardPanel(final ParametersForm defaultItem, final PageReference pageRef) {
        super(defaultItem, pageRef);

    }

    @Override
    protected WizardModel buildModelSteps(final ParametersForm modelObject, final WizardModel wizardModel) {
        wizardModel.add(new ParametersCreateWizardSchemaStep(modelObject));
        wizardModel.add(new ParametersCreateWizardAttrStep(modelObject));
        return wizardModel;
    }

    @Override
    protected void onCancelInternal(final ParametersForm modelObject) {
        //do nothing
    }

    @Override
    protected Serializable onApplyInternal(final ParametersForm modelObject) {
        final PlainSchemaTO plainSchemaTO = modelObject.getPlainSchemaTO();
        plainSchemaTO.setKey(modelObject.getAttrTO().getSchema());

        schemaRestClient.create(SchemaType.PLAIN, plainSchemaTO);
        try {
            confRestClient.set(modelObject.getAttrTO());
        } catch (Exception e) {
            LOG.error("While setting {}, removing {}", modelObject.getAttrTO(), plainSchemaTO, e);
            schemaRestClient.deletePlainSchema(plainSchemaTO.getKey());

            throw e;
        }
        return modelObject.getAttrTO();
    }

    public static class ParametersForm implements Serializable {

        private static final long serialVersionUID = 412294016850871853L;

        private final PlainSchemaTO plainSchemaTO;

        private final AttrTO attrTO;

        public ParametersForm() {
            plainSchemaTO = new PlainSchemaTO();
            attrTO = new AttrTO();
        }

        public PlainSchemaTO getPlainSchemaTO() {
            return plainSchemaTO;
        }

        public AttrTO getAttrTO() {
            return attrTO;
        }

    }
}
