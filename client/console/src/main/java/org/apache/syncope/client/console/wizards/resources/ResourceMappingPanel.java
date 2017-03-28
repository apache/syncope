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
package org.apache.syncope.client.console.wizards.resources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wizards.AbstractMappingPanel;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.MappingItemTO;
import org.apache.syncope.common.lib.to.ProvisionTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.util.ListModel;

/**
 * Resource mapping panel.
 */
public class ResourceMappingPanel extends AbstractMappingPanel {

    private static final long serialVersionUID = -7982691107029848579L;

    /**
     * External resource provisioning configuration instance to be updated.
     */
    private final ProvisionTO provisionTO;

    private final LoadableDetachableModel<List<String>> extAttrNames;

    /**
     * Attribute Mapping Panel.
     *
     * @param id panel id
     * @param resourceTO external resource to be updated
     * @param provisionTO external resource provisioning configuration instance
     * @param mapItemTransformers mapping item transformers toggle panel
     * @param jexlTransformers JEXL transformers toggle panel
     */
    public ResourceMappingPanel(
            final String id,
            final ResourceTO resourceTO,
            final ProvisionTO provisionTO,
            final MappingItemTransformersTogglePanel mapItemTransformers,
            final JEXLTransformersTogglePanel jexlTransformers) {

        super(id,
                mapItemTransformers,
                jexlTransformers,
                new ListModel<MappingItemTO>(provisionTO.getMapping().getItems()),
                resourceTO.getConnector() != null,
                false,
                MappingPurpose.BOTH);

        setOutputMarkupId(true);

        this.provisionTO = provisionTO;

        extAttrNames = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return new ConnectorRestClient().getExtAttrNames(
                        provisionTO.getObjectClass(),
                        resourceTO.getConnector(),
                        resourceTO.getConfOverride());
            }
        };
    }

    @Override
    protected boolean hidePassword() {
        return !AnyTypeKind.USER.name().equals(provisionTO.getAnyType());
    }
    
    @Override
    protected IModel<List<String>> getExtAttrNames() {
        return extAttrNames;
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        passwordLabel.setVisible(AnyTypeKind.USER.name().equals(this.provisionTO.getAnyType()));
    }

    @Override
    protected void setAttrNames(final AjaxTextFieldPanel toBeUpdated) {
        toBeUpdated.setRequired(true);
        toBeUpdated.setEnabled(true);

        AnyTypeTO anyTypeTO = anyTypeRestClient.read(provisionTO.getAnyType());

        List<AnyTypeClassTO> anyTypeClassTOs = new ArrayList<>();
        anyTypeClassTOs.addAll(anyTypeClassRestClient.list(anyTypeTO.getClasses()));
        for (String auxClass : provisionTO.getAuxClasses()) {
            anyTypeClassTOs.add(anyTypeClassRestClient.read(auxClass));
        }

        List<String> choices = new ArrayList<>();

        switch (provisionTO.getAnyType()) {
            case "USER":
                choices.addAll(USER_FIELD_NAMES);
                break;

            case "GROUP":
                choices.addAll(GROUP_FIELD_NAMES);
                break;

            default:
                choices.addAll(ANY_OBJECT_FIELD_NAMES);
        }

        for (AnyTypeClassTO anyTypeClassTO : anyTypeClassTOs) {
            choices.addAll(anyTypeClassTO.getPlainSchemas());
            choices.addAll(anyTypeClassTO.getDerSchemas());
            choices.addAll(anyTypeClassTO.getVirSchemas());
        }

        Collections.sort(choices);
        toBeUpdated.setChoices(choices);
    }
}
