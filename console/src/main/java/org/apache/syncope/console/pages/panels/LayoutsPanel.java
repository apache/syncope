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
package org.apache.syncope.console.pages.panels;

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.to.AttributeTO;
import org.apache.syncope.common.to.SchemaTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.AttributeSchemaType;
import org.apache.syncope.console.commons.LayoutType;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.commons.XMLRolesReader;
import org.apache.syncope.console.rest.ConfigurationRestClient;
import org.apache.syncope.console.rest.SchemaRestClient;
import org.apache.syncope.console.wicket.markup.html.form.AjaxPalettePanel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LayoutsPanel extends Panel {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(LayoutsPanel.class);

    private static final long serialVersionUID = -6804066913177804275L;

    private static final String CANCEL = "cancel";

    private static final String APPLY = "apply";

    @SpringBean
    protected XMLRolesReader xmlRolesReader;

    @SpringBean
    private SchemaRestClient schemaRestClient;

    @SpringBean
    private ConfigurationRestClient confRestClient;

    @SuppressWarnings("unchecked")
    public LayoutsPanel(final String id, final LayoutType layoutType, final NotificationPanel feedbackPanel) {
        super(id);

        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);

        final Form<String> form = new Form<String>("form");
        form.setOutputMarkupId(true);

        final AttributableType type;
        final AttributeTO attributeTO;
        if (layoutType == LayoutType.ADMIN_USER || layoutType == layoutType.SELF_USER) {
            attributeTO = getConfiguration(layoutType, AttributableType.USER);
            type = AttributableType.USER;
        } else if (layoutType == LayoutType.ADMIN_ROLE || layoutType == layoutType.SELF_ROLE) {
            attributeTO = getConfiguration(layoutType, AttributableType.ROLE);
            type = AttributableType.ROLE;
        } else {
            attributeTO = getConfiguration(layoutType, AttributableType.MEMBERSHIP);
            type = AttributableType.MEMBERSHIP;
        }

        form.setModel(new CompoundPropertyModel(attributeTO.getValues()));

        final List<String> fields = getAllFields(type);

        final ListModel<String> selectedFields = new ListModel<String>(!attributeTO.getValues().isEmpty()
                ? attributeTO.getValues() : fields);
        final ListModel<String> availableFields = new ListModel<String>(fields);

        form.add(new AjaxPalettePanel<String>("fields", selectedFields, availableFields, true));

        final AjaxButton submit = new IndicatingAjaxButton(APPLY, new ResourceModel(APPLY)) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                try {
                    if (schemaRestClient.readSchema(
                            AttributableType.CONFIGURATION, layoutType.getParameter()) == null) {
                        final SchemaTO confSchemaTO = new SchemaTO();
                        confSchemaTO.setName(layoutType.getParameter());
                        confSchemaTO.setMultivalue(true);
                        confSchemaTO.setType(AttributeSchemaType.String);
                        schemaRestClient.createSchema(AttributableType.CONFIGURATION, confSchemaTO);
                    }
                    confRestClient.set(attributeTO);
                    info(getString(Constants.OPERATION_SUCCEEDED));
                } catch (Exception e) {
                    LOG.error("While save layout configuration", e);
                    error(getString(Constants.ERROR) + ": " + e.getMessage());
                }
                feedbackPanel.refresh(target);
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                LOG.error("While save layout configuration");
                error(getString(Constants.ERROR) + ": While save layout configuration");
                feedbackPanel.refresh(target);
            }
        };

        form.add(submit);

        final IndicatingAjaxButton cancel = new IndicatingAjaxButton(CANCEL, new ResourceModel(CANCEL)) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                target.add(container);
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
            }
        };

        cancel.setDefaultFormProcessing(false);
        form.add(cancel);
        container.add(form);
        add(container);
    }

    private AttributeTO getConfiguration(
            final LayoutType layoutType, final AttributableType type) {
        AttributeTO attributeTO = confRestClient.read(layoutType.getParameter());
        if (attributeTO != null) {
            return attributeTO;
        } else {
            attributeTO = new AttributeTO();
            attributeTO.setSchema(layoutType.getParameter());
            attributeTO.getValues().addAll(getAllFields(type));
            return attributeTO;
        }
    }

    private List<String> getAllFields(final AttributableType type) {
        final List<String> fields = new ArrayList<String>();
        for (SchemaTO item : schemaRestClient.getSchemas(type)) {
            fields.add(item.getName());
        }
        return fields;
    }
}
