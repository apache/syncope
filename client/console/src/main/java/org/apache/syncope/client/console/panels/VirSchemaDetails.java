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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.wicket.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.to.AbstractSchemaTO;
import org.apache.syncope.common.lib.to.ConnIdObjectClassTO;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.ProvisionTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.PropertyModel;

public class VirSchemaDetails extends AbstractSchemaDetailsPanel {

    private static final long serialVersionUID = 5979623248182851337L;

    private final ResourceRestClient resourceRestClient = new ResourceRestClient();

    private final ConnectorRestClient connRestClient = new ConnectorRestClient();

    private final Map<String, String> anyTypes = new HashMap<>();

    private final AjaxDropDownChoicePanel<String> anyType;

    private ResourceTO selectedResource;

    public VirSchemaDetails(final String id,
            final PageReference pageReference,
            final AbstractSchemaTO schemaTO) {

        super(id, pageReference, schemaTO);

        AjaxCheckBoxPanel readonly = new AjaxCheckBoxPanel("readonly", getString("readonly"),
                new PropertyModel<Boolean>(schemaTO, "readonly"));
        schemaForm.add(readonly);

        final AjaxDropDownChoicePanel<String> resource = new AjaxDropDownChoicePanel<>(
                "resource", getString("resource"), new PropertyModel<String>(schemaTO, "resource"), false).
                setNullValid(false);
        resource.setChoices(CollectionUtils.collect(resourceRestClient.list(),
                EntityTOUtils.<ResourceTO>keyTransformer(), new ArrayList<String>()));
        resource.setOutputMarkupId(true);
        resource.addRequiredLabel();
        if (resource.getModelObject() != null) {
            populateAnyTypes(resource.getModelObject());
        }
        schemaForm.add(resource);

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
        schemaForm.add(anyType);

        final AjaxTextFieldPanel extAttrName = new AjaxTextFieldPanel(
                "extAttrName", getString("extAttrName"), new PropertyModel<String>(schemaTO, "extAttrName"));
        extAttrName.setOutputMarkupId(true);
        extAttrName.addRequiredLabel();
        if (selectedResource != null) {
            extAttrName.setChoices(getExtAttrNames());
        }
        schemaForm.add(extAttrName);

        add(schemaForm);

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
                if (selectedResource != null && SyncopeConsoleSession.get().owns(StandardEntitlement.CONNECTOR_READ)) {
                    extAttrName.setChoices(getExtAttrNames());
                    target.add(extAttrName);
                }
            }
        });
    }

    private void populateAnyTypes(final String resourceKey) {
        anyTypes.clear();
        if (resourceKey != null && SyncopeConsoleSession.get().owns(StandardEntitlement.RESOURCE_READ)) {
            selectedResource = resourceRestClient.read(resourceKey);
            for (ProvisionTO provisionTO : selectedResource.getProvisions()) {
                anyTypes.put(provisionTO.getAnyType(), provisionTO.getObjectClass());
            }
        }
    }

    private List<String> getExtAttrNames() {
        ConnInstanceTO connInstanceTO = new ConnInstanceTO();
        connInstanceTO.setKey(selectedResource.getConnector());
        connInstanceTO.getConf().addAll(selectedResource.getConfOverride());

        ConnIdObjectClassTO connIdObjectClass = IterableUtils.find(
                connRestClient.buildObjectClassInfo(connInstanceTO, false), new Predicate<ConnIdObjectClassTO>() {

            @Override
            public boolean evaluate(final ConnIdObjectClassTO object) {
                return object.getType().equals(anyTypes.get(anyType.getModelObject()));
            }
        });

        return connIdObjectClass == null
                ? Collections.<String>emptyList()
                : connIdObjectClass.getAttributes();
    }
}
