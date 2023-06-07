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
import org.apache.syncope.common.lib.to.OIDCC4UIProviderTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class OIDCProviderMappingPanel extends AbstractMappingPanel {

    private static final long serialVersionUID = -4123879435574382968L;

    @SpringBean
    protected AnyTypeRestClient anyTypeRestClient;

    @SpringBean
    protected AnyTypeClassRestClient anyTypeClassRestClient;

    public OIDCProviderMappingPanel(
            final String id,
            final OIDCC4UIProviderTO opTO,
            final ItemTransformersTogglePanel mapItemTransformers,
            final JEXLTransformersTogglePanel jexlTransformers) {

        super(id,
                mapItemTransformers,
                jexlTransformers,
                new ListModel<>(opTO.getItems()),
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
        List<String> extAttrNames = new ArrayList<>();
        extAttrNames.add("email");
        extAttrNames.add("family_name");
        extAttrNames.add("name");
        extAttrNames.add("middle_name");
        extAttrNames.add("given_name");
        extAttrNames.add("preferred_username");
        extAttrNames.add("nickname");
        extAttrNames.add("profile");
        extAttrNames.add("gender");
        extAttrNames.add("locale");
        extAttrNames.add("zoneinfo");
        extAttrNames.add("birthdate");
        extAttrNames.add("phone_number");
        extAttrNames.add("address");
        extAttrNames.add("updated_at");

        return Model.ofList(extAttrNames);
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
