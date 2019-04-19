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

import javax.ws.rs.core.MediaType;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.wicket.PageReference;

public class ParametersModalPanel extends AbstractModalPanel<ConfParam> {

    private static final long serialVersionUID = 4024126489500665435L;

    private final ParametersWizardPanel.ParametersForm form;

    public ParametersModalPanel(
            final BaseModal<ConfParam> modal,
            final ConfParam param,
            final ConfParamOps confParamOps,
            final AjaxWizard.Mode mode,
            final PageReference pageRef) {

        super(modal, pageRef);

        PlainSchemaTO schema = new PlainSchemaTO();
        if (param.getSchema() != null) {
            if (param.isInstance(Boolean.class)) {
                schema.setType(AttrSchemaType.Boolean);
            } else if (param.isInstance(Integer.class) || param.isInstance(Long.class)) {
                schema.setType(AttrSchemaType.Long);
            } else if (param.isInstance(Float.class) || param.isInstance(Double.class)) {
                schema.setType(AttrSchemaType.Double);
            } else {
                schema.setType(AttrSchemaType.String);
            }
            schema.setMultivalue(param.isMultivalue());
            schema.setMimeType(MediaType.APPLICATION_OCTET_STREAM);
        }
        form = new ParametersWizardPanel.ParametersForm(schema, param);

        add(new ParametersWizardPanel(form, confParamOps, pageRef).build("parametersCreateWizardPanel", mode));
    }

    @Override
    public final ConfParam getItem() {
        return this.form.getParam();
    }
}
