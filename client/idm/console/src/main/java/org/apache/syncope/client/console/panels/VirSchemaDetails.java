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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.VirSchemaTO;
import org.apache.syncope.common.lib.types.IdMEntitlement;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class VirSchemaDetails extends AbstractSchemaDetailsPanel {

    private static final long serialVersionUID = 5979623248182851337L;

    @SpringBean
    protected ResourceRestClient resourceRestClient;

    @SpringBean
    protected ConnectorRestClient connectorRestClient;

    protected final Map<String, String> anyTypes = new HashMap<>();

    protected final AjaxDropDownChoicePanel<String> anyType;

    protected ResourceTO selectedResource;

    public VirSchemaDetails(final String id, final VirSchemaTO schemaTO) {
        super(id, schemaTO);

        AjaxCheckBoxPanel readonly = new AjaxCheckBoxPanel("readonly", getString("readonly"),
                new PropertyModel<>(schemaTO, "readonly"));
        add(readonly);

        AjaxDropDownChoicePanel<String> resource = new AjaxDropDownChoicePanel<>(
                "resource", getString("resource"), new PropertyModel<String>(schemaTO, "resource"), false).
                setNullValid(false);
        resource.setChoices(resourceRestClient.list().stream().map(ResourceTO::getKey).collect(Collectors.toList()));
        resource.setOutputMarkupId(true);
        resource.addRequiredLabel();
        if (resource.getModelObject() != null) {
            populateAnyTypes(resource.getModelObject());
        }
        add(resource);

        anyType = new AjaxDropDownChoicePanel<>(
                "anyType", getString("anyType"), new PropertyModel<String>(schemaTO, "anyType"), false).
                setNullValid(false);
        anyType.setChoices(new ArrayList<>(anyTypes.keySet()));
        anyType.setOutputMarkupId(true);
        anyType.setOutputMarkupPlaceholderTag(true);
        anyType.addRequiredLabel();
        if (resource.getModelObject() == null) {
            anyType.setEnabled(false);
        }
        add(anyType);

        AjaxTextFieldPanel extAttrName = new AjaxTextFieldPanel(
                "extAttrName", getString("extAttrName"), new PropertyModel<>(schemaTO, "extAttrName"));
        extAttrName.setOutputMarkupId(true);
        extAttrName.addRequiredLabel();
        if (selectedResource != null) {
            extAttrName.setChoices(getExtAttrNames());
        }
        add(extAttrName);

        resource.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                anyTypes.clear();
                if (resource.getModelObject() != null) {
                    populateAnyTypes(resource.getModelObject());
                    anyType.setEnabled(true);
                }
                anyType.setChoices(new ArrayList<>(anyTypes.keySet()));
                anyType.setModelObject(null);
                target.add(anyType);
            }
        });

        anyType.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                if (selectedResource != null) {
                    String adminRealm = getAdminRealm(selectedResource.getConnector());

                    if (SyncopeConsoleSession.get().owns(IdMEntitlement.CONNECTOR_READ, adminRealm)) {
                        extAttrName.setChoices(getExtAttrNames());
                        target.add(extAttrName);
                    }
                }
            }
        });
    }

    protected String getAdminRealm(final String connectorKey) {
        String adminRealm = null;
        try {
            adminRealm = connectorRestClient.read(connectorKey).getAdminRealm();
        } catch (Exception e) {
            LOG.error("Could not read Admin Realm for External Resource {}", selectedResource.getKey());
        }

        return adminRealm;
    }

    protected void populateAnyTypes(final String resourceKey) {
        anyTypes.clear();
        if (resourceKey != null) {
            ResourceTO resource = resourceRestClient.read(resourceKey);
            String adminRealm = getAdminRealm(resource.getConnector());

            if (SyncopeConsoleSession.get().owns(IdMEntitlement.RESOURCE_READ, adminRealm)) {
                selectedResource = resource;
                selectedResource.getProvisions().forEach(
                        provisionTO -> anyTypes.put(provisionTO.getAnyType(), provisionTO.getObjectClass()));
            }
        }
    }

    protected List<String> getExtAttrNames() {
        return connectorRestClient.getExtAttrNames(
                SyncopeConstants.ROOT_REALM,
                anyTypes.get(anyType.getModelObject()),
                selectedResource.getConnector(),
                selectedResource.getConfOverride());
    }
}
