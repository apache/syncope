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
package org.apache.syncope.client.console.wizards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wizards.resources.JEXLTransformersTogglePanel;
import org.apache.syncope.client.console.wizards.resources.MappingItemTransformersTogglePanel;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.MappingItemTO;
import org.apache.syncope.common.lib.to.SAML2IdPTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;

public class SAML2IdPMappingPanel extends AbstractMappingPanel {

    private static final long serialVersionUID = 2248901624411541853L;

    public SAML2IdPMappingPanel(
            final String id,
            final SAML2IdPTO idpTO,
            final MappingItemTransformersTogglePanel mapItemTransformers,
            final JEXLTransformersTogglePanel jexlTransformers) {

        super(id,
                mapItemTransformers,
                jexlTransformers,
                new ListModel<MappingItemTO>(idpTO.getMappingItems()),
                true,
                true,
                MappingPurpose.NONE);

        setOutputMarkupId(true);
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        purposeLabel.setVisible(false);
    }

    @Override
    protected IModel<List<String>> getExtAttrNames() {
        return Model.ofList(Collections.<String>singletonList("NameID"));
    }

    @Override
    protected void setAttrNames(final AjaxTextFieldPanel toBeUpdated) {
        toBeUpdated.setRequired(true);
        toBeUpdated.setEnabled(true);

        List<String> choices = new ArrayList<>(USER_FIELD_NAMES);

        for (AnyTypeClassTO anyTypeClassTO : anyTypeClassRestClient.list(
                anyTypeRestClient.read(AnyTypeKind.USER.name()).getClasses())) {

            choices.addAll(anyTypeClassTO.getPlainSchemas());
            choices.addAll(anyTypeClassTO.getDerSchemas());
            choices.addAll(anyTypeClassTO.getVirSchemas());
        }

        Collections.sort(choices);
        toBeUpdated.setChoices(choices);
    }

}
