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

import java.util.List;
import org.apache.syncope.client.console.commons.AttrLayoutType;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.SelectChoiceRenderer;
import org.apache.syncope.client.console.commons.XMLRolesReader;
import org.apache.syncope.client.console.rest.ConfigurationRestClient;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.common.lib.to.AttrTO;
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
    public LayoutsPanel(final String id, final AttrLayoutType attrLayoutType, final NotificationPanel feedbackPanel) {
        super(id);

        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);

        final Form<String> form = new Form<String>("form");
        form.setOutputMarkupId(true);

        final AttrTO attrLayout = confRestClient.readAttrLayout(attrLayoutType);
        form.setModel(new CompoundPropertyModel(attrLayout.getValues()));

        final List<String> fields = schemaRestClient.getPlainSchemaNames(attrLayoutType.getAttrType());
        final ListModel<String> selectedFields =
                new ListModel<String>(attrLayout.getValues().isEmpty() ? fields : attrLayout.getValues());
        final ListModel<String> availableFields = new ListModel<String>(fields);

        form.add(new AjaxPalettePanel<String>("fields", selectedFields, availableFields,
                new SelectChoiceRenderer<String>(), true, true));

        final AjaxButton submit = new IndicatingAjaxButton(APPLY, new ResourceModel(APPLY)) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                try {
                    confRestClient.set(attrLayout);
                    info(getString(Constants.OPERATION_SUCCEEDED));
                } catch (Exception e) {
                    LOG.error("While saving layout configuration", e);
                    error(getString(Constants.ERROR) + ": " + e.getMessage());
                }
                feedbackPanel.refresh(target);
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                error(getString(Constants.ERROR) + ": While saving layout configuration");
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
}
