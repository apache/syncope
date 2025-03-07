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
package org.apache.syncope.client.console.wizards.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.syncope.client.console.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.console.rest.AnyTypeClassRestClient;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.to.SAML2SP4UIIdPTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class SAML2IdPMappingPanel extends AbstractMappingPanel {

    private static final long serialVersionUID = 2248901624411541853L;

    @SpringBean
    protected AnyTypeRestClient anyTypeRestClient;

    @SpringBean
    protected AnyTypeClassRestClient anyTypeClassRestClient;

    public SAML2IdPMappingPanel(
            final String id,
            final SAML2SP4UIIdPTO idpTO,
            final ItemTransformersTogglePanel mapItemTransformers,
            final JEXLTransformersTogglePanel jexlTransformers) {

        super(id,
                mapItemTransformers,
                jexlTransformers,
                new ListModel<>(idpTO.getItems()),
                true,
                MappingPurpose.NONE);

        setOutputMarkupId(true);
    }

    @Override
    protected boolean hidePurpose() {
        return true;
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        intAttrNameInfo.setVisible(false);
    }

    @Override
    protected IModel<List<String>> getExtAttrNames() {
        return Model.ofList(Collections.singletonList("NameID"));
    }

    @Override
    protected void setAttrNames(final AjaxTextFieldPanel toBeUpdated) {
        toBeUpdated.setRequired(true);
        toBeUpdated.setEnabled(true);

        List<String> choices = new ArrayList<>(ClassPathScanImplementationLookup.USER_FIELD_NAMES);

        anyTypeClassRestClient.list(anyTypeRestClient.read(AnyTypeKind.USER.name()).getClasses()).
                forEach(anyTypeClassTO -> {
                    choices.addAll(anyTypeClassTO.getPlainSchemas());
                    choices.addAll(anyTypeClassTO.getDerSchemas());
                    choices.addAll(anyTypeClassTO.getVirSchemas());
                });

        Collections.sort(choices);
        toBeUpdated.setChoices(choices);
    }
}
