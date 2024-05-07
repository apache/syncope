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
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.wizards.BaseAjaxWizardBuilder;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel;

public class ParametersWizardPanel extends BaseAjaxWizardBuilder<ParametersWizardPanel.ParametersForm> {

    private static final long serialVersionUID = -2868592590785581481L;

    private final ConfParamOps confParamOps;

    public ParametersWizardPanel(
            final ParametersForm defaultItem, final ConfParamOps confParamOps, final PageReference pageRef) {

        super(defaultItem, pageRef);
        this.confParamOps = confParamOps;
    }

    @Override
    protected WizardModel buildModelSteps(final ParametersForm modelObject, final WizardModel wizardModel) {
        wizardModel.add(new ParametersWizardSchemaStep(mode, modelObject));
        wizardModel.add(new ParametersWizardAttrStep(mode, modelObject));
        return wizardModel;
    }

    @Override
    protected Serializable onApplyInternal(final ParametersForm modelObject) {
        modelObject.getParam().setMultivalue(modelObject.getSchema().isMultivalue());
        try {
            confParamOps.set(
                    SyncopeConsoleSession.get().getDomain(),
                    modelObject.getParam().getSchema(),
                    modelObject.getParam().valueAsObject());
        } catch (Exception e) {
            LOG.error("While setting {}", modelObject.getParam(), e);
            throw e;
        }
        return modelObject.getParam();
    }

    public static class ParametersForm implements Serializable {

        private static final long serialVersionUID = 412294016850871853L;

        private final PlainSchemaTO schema;

        private final ConfParam param;

        public ParametersForm(final PlainSchemaTO schema, final ConfParam param) {
            this.schema = schema;
            this.param = param;
        }

        public PlainSchemaTO getSchema() {
            return schema;
        }

        public ConfParam getParam() {
            return param;
        }
    }
}
