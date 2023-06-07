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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.syncope.client.console.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.console.rest.AnyTypeClassRestClient;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.client.console.wizards.mapping.AbstractMappingPanel;
import org.apache.syncope.client.console.wizards.mapping.ItemTransformersTogglePanel;
import org.apache.syncope.client.console.wizards.mapping.JEXLTransformersTogglePanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resource mapping panel.
 */
public class ResourceMappingPanel extends AbstractMappingPanel {

    private static final long serialVersionUID = -7982691107029848579L;

    protected static final Logger LOG = LoggerFactory.getLogger(ResourceMappingPanel.class);

    @SpringBean
    protected ConnectorRestClient connectorRestClient;

    @SpringBean
    protected AnyTypeRestClient anyTypeRestClient;

    @SpringBean
    protected AnyTypeClassRestClient anyTypeClassRestClient;

    /**
     * External resource provisioning configuration instance to be updated.
     */
    protected final ResourceProvision provision;

    protected final LoadableDetachableModel<List<String>> extAttrNames;

    /**
     * Attribute Mapping Panel.
     *
     * @param id panel id
     * @param resourceTO external resource to be updated
     * @param adminRealm admin realm
     * @param provision external resource provisioning configuration instance
     * @param itemTransformers mapping item transformers toggle panel
     * @param jexlTransformers JEXL transformers toggle panel
     */
    public ResourceMappingPanel(
            final String id,
            final ResourceTO resourceTO,
            final String adminRealm,
            final ResourceProvision provision,
            final ItemTransformersTogglePanel itemTransformers,
            final JEXLTransformersTogglePanel jexlTransformers) {

        super(id,
                itemTransformers,
                jexlTransformers,
                new ListModel<>(provision.getItems()),
                resourceTO.getConnector() != null,
                MappingPurpose.BOTH);

        setOutputMarkupId(true);

        this.provision = provision;

        extAttrNames = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return connectorRestClient.getExtAttrNames(
                        adminRealm,
                        provision.getObjectClass(),
                        resourceTO.getConnector(),
                        resourceTO.getConfOverride());
            }
        };
    }

    @Override
    protected boolean hidePassword() {
        return !AnyTypeKind.USER.name().equals(provision.getAnyType());
    }

    @Override
    protected IModel<List<String>> getExtAttrNames() {
        return extAttrNames;
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        passwordLabel.setVisible(AnyTypeKind.USER.name().equals(this.provision.getAnyType()));
    }

    @Override
    protected void setAttrNames(final AjaxTextFieldPanel toBeUpdated) {
        toBeUpdated.setRequired(true);
        toBeUpdated.setEnabled(true);

        Set<String> choices = new HashSet<>();
        if (SyncopeConstants.REALM_ANYTYPE.equals(provision.getAnyType())) {
            choices.add(Constants.KEY_FIELD_NAME);
            choices.add("name");
            choices.add("fullpath");
        } else {
            AnyTypeTO anyType = null;
            try {
                anyType = anyTypeRestClient.read(provision.getAnyType());
            } catch (Exception e) {
                LOG.error("Could not read AnyType {}", provision.getAnyType(), e);
            }

            List<AnyTypeClassTO> anyTypeClassTOs = new ArrayList<>();
            if (anyType != null) {
                try {
                    anyTypeClassTOs.addAll(anyTypeClassRestClient.list(anyType.getClasses()));
                } catch (Exception e) {
                    LOG.error("Could not read AnyType classes for {}", anyType.getClasses(), e);
                }
            }
            provision.getAuxClasses().forEach(auxClass -> {
                try {
                    anyTypeClassTOs.add(anyTypeClassRestClient.read(auxClass));
                } catch (Exception e) {
                    LOG.error("Could not read AnyTypeClass for {}", auxClass, e);
                }
            });

            switch (provision.getAnyType()) {
                case "USER":
                    choices.addAll(ClassPathScanImplementationLookup.USER_FIELD_NAMES);
                    break;

                case "GROUP":
                    choices.addAll(ClassPathScanImplementationLookup.GROUP_FIELD_NAMES);
                    break;

                default:
                    choices.addAll(ClassPathScanImplementationLookup.ANY_OBJECT_FIELD_NAMES);
            }

            anyTypeClassTOs.forEach(anyTypeClassTO -> {
                choices.addAll(anyTypeClassTO.getPlainSchemas());
                choices.addAll(anyTypeClassTO.getDerSchemas());
                choices.addAll(anyTypeClassTO.getVirSchemas());
            });
        }

        List<String> names = new ArrayList<>(choices);
        Collections.sort(names);
        toBeUpdated.setChoices(names);
    }
}
