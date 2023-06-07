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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.commons.ImplementationInfoProvider.ViewMode;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.ImplementationRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.io.IOUtils;

public class ImplementationModalPanel extends AbstractModalPanel<ImplementationTO> {

    private static final long serialVersionUID = 5283548960927517342L;

    protected static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    @SpringBean
    protected ImplementationRestClient implementationRestClient;

    protected final ImplementationTO implementation;

    protected final ViewMode viewMode;

    protected boolean create = false;

    public ImplementationModalPanel(
            final BaseModal<ImplementationTO> modal,
            final ImplementationTO implementation,
            final PageReference pageRef) {

        super(modal, pageRef);
        this.implementation = implementation;
        this.viewMode = SyncopeWebApplication.get().getImplementationInfoProvider().getViewMode(implementation);
        this.create = implementation.getKey() == null;

        add(new AjaxTextFieldPanel(
                Constants.KEY_FIELD_NAME,
                Constants.KEY_FIELD_NAME,
                new PropertyModel<>(implementation, Constants.KEY_FIELD_NAME), false).
                addRequiredLabel().setEnabled(create));

        List<String> classes = SyncopeWebApplication.get().getImplementationInfoProvider().
                getClasses(implementation, viewMode);

        AjaxDropDownChoicePanel<String> javaClass = new AjaxDropDownChoicePanel<>(
                "javaClass", "Class", new PropertyModel<>(implementation, "body"));
        javaClass.setNullValid(false);
        javaClass.setChoices(classes);
        javaClass.addRequiredLabel();
        javaClass.setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true);
        javaClass.setVisible(viewMode == ViewMode.JAVA_CLASS);
        add(javaClass);

        AjaxDropDownChoicePanel<String> jsonClass = new AjaxDropDownChoicePanel<>(
                "jsonClass", "Class", new Model<>());
        jsonClass.setNullValid(false);
        jsonClass.setChoices(classes);
        jsonClass.addRequiredLabel();
        jsonClass.setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true);
        jsonClass.setVisible(viewMode == ViewMode.JSON_BODY);
        if (viewMode == ViewMode.JSON_BODY && StringUtils.isNotBlank(implementation.getBody())) {
            try {
                JsonNode node = MAPPER.readTree(implementation.getBody());
                if (node.has("_class")) {
                    jsonClass.setModelObject(node.get("_class").asText());
                }
            } catch (IOException e) {
                LOG.error("Could not parse as JSON payload: {}", implementation.getBody(), e);
            }
        }
        jsonClass.setReadOnly(jsonClass.getModelObject() != null);
        add(jsonClass);

        WebMarkupContainer groovyClassContainer = new WebMarkupContainer("groovyClassContainer");
        groovyClassContainer.setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true);
        groovyClassContainer.setVisible(viewMode != ViewMode.JAVA_CLASS);
        add(groovyClassContainer);

        if (StringUtils.isBlank(implementation.getBody())
                && implementation.getEngine() == ImplementationEngine.GROOVY) {

            String templateClassName = SyncopeWebApplication.get().getImplementationInfoProvider().
                    getGroovyTemplateClassName(implementation.getType());
            if (templateClassName != null) {
                try {
                    implementation.setBody(StringUtils.substringAfter(
                            IOUtils.toString(ImplementationModalPanel.class.getResourceAsStream(
                                    "/org/apache/syncope/client/console/implementations/" + templateClassName
                                    + ".groovy")),
                            "*/\n"));
                } catch (IOException e) {
                    LOG.error("Could not load the expected Groovy template {} for {}",
                            templateClassName, implementation.getType(), e);
                }
            }
        }

        TextArea<String> groovyClass = new TextArea<>("groovyClass", new PropertyModel<>(implementation, "body"));
        groovyClass.setMarkupId("groovyClass").setOutputMarkupPlaceholderTag(true);
        groovyClass.setVisible(viewMode != ViewMode.JAVA_CLASS);
        groovyClass.setRequired(true);
        groovyClassContainer.add(groovyClass);

        jsonClass.add(new AjaxEventBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = 5538299138211283825L;

            @Override
            protected void onEvent(final AjaxRequestTarget target) {
                Class<?> clazz = SyncopeWebApplication.get().getImplementationInfoProvider().
                        getClass(implementation.getType(), jsonClass.getModelObject());
                if (clazz != null) {
                    try {
                        target.appendJavaScript("editor.getDoc().setValue('"
                                + MAPPER.writeValueAsString(clazz.getDeclaredConstructor().newInstance())
                                + "');");
                    } catch (Exception e) {
                        LOG.error("Could not generate a value for {}", jsonClass.getModelObject(), e);
                    }
                }
            }
        });
    }

    @Override
    public ImplementationTO getItem() {
        return implementation;
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        if (viewMode != ViewMode.JAVA_CLASS) {
            response.render(OnLoadHeaderItem.forScript(
                    "editor = CodeMirror.fromTextArea("
                    + "document.getElementById('groovyClassForm').children['groovyClass'], {"
                    + "  readOnly: false, "
                    + "  lineNumbers: true, "
                    + "  lineWrapping: true, "
                    + "  matchBrackets: true,"
                    + "  autoCloseBrackets: true,"
                    + (viewMode == ViewMode.GROOVY_BODY ? "  mode: 'text/x-groovy'," : "")
                    + "  autoRefresh: true"
                    + "});"
                    + "editor.on('change', updateTextArea);"));
        }
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target) {
        try {
            if (create) {
                implementationRestClient.create(implementation);
            } else {
                implementationRestClient.update(implementation);
            }

            modal.close(target);
            SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
        } catch (Exception e) {
            LOG.error("While creating or updating Implementation", e);
            SyncopeConsoleSession.get().onException(e);
        }
        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
    }
}
