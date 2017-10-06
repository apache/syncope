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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleApplication;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.console.init.ConsoleInitializer;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.ImplementationRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.info.JavaImplInfo;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.ImplementationType;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.io.IOUtils;

public class ImplementationModalPanel extends AbstractModalPanel<ImplementationTO> {

    private static final long serialVersionUID = 5283548960927517342L;

    private enum ViewMode {
        JAVA_CLASS,
        JSON_BODY,
        GROOVY_BODY

    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ImplementationRestClient restClient = new ImplementationRestClient();

    private final ImplementationTO implementation;

    private final ViewMode viewMode;

    private boolean create = false;

    public ImplementationModalPanel(
            final BaseModal<ImplementationTO> modal,
            final ImplementationTO implementation,
            final PageReference pageRef) {

        super(modal, pageRef);
        this.implementation = implementation;
        this.viewMode = implementation.getEngine() == ImplementationEngine.GROOVY
                ? ViewMode.GROOVY_BODY
                : implementation.getType() == ImplementationType.REPORTLET
                || implementation.getType() == ImplementationType.ACCOUNT_RULE
                || implementation.getType() == ImplementationType.PASSWORD_RULE
                ? ViewMode.JSON_BODY
                : ViewMode.JAVA_CLASS;
        this.create = implementation.getKey() == null;

        add(new AjaxTextFieldPanel(
                "key", "key", new PropertyModel<>(implementation, "key"), false).
                addRequiredLabel().setEnabled(create));

        List<String> classes = Collections.emptyList();
        if (viewMode == ViewMode.JAVA_CLASS) {
            Optional<JavaImplInfo> javaClasses = SyncopeConsoleSession.get().getPlatformInfo().
                    getJavaImplInfo(implementation.getType());
            classes = javaClasses.isPresent()
                    ? new ArrayList<>(javaClasses.get().getClasses())
                    : new ArrayList<>();
        } else if (viewMode == ViewMode.JSON_BODY) {
            ClassPathScanImplementationLookup implementationLookup =
                    (ClassPathScanImplementationLookup) SyncopeConsoleApplication.get().
                            getServletContext().getAttribute(ConsoleInitializer.CLASSPATH_LOOKUP);

            switch (implementation.getType()) {
                case REPORTLET:
                    classes = implementationLookup.getReportletConfs().keySet().stream().
                            collect(Collectors.toList());
                    break;

                case ACCOUNT_RULE:
                    classes = implementationLookup.getAccountRuleConfs().keySet().stream().
                            collect(Collectors.toList());
                    break;

                case PASSWORD_RULE:
                    classes = implementationLookup.getPasswordRuleConfs().keySet().stream().
                            collect(Collectors.toList());
                    break;

                default:
            }
        }
        Collections.sort(classes);

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
                if (node.has("@class")) {
                    jsonClass.setModelObject(node.get("@class").asText());
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

            String templateClassName = null;

            switch (implementation.getType()) {
                case REPORTLET:
                    templateClassName = "MyReportlet";
                    break;

                case ACCOUNT_RULE:
                    templateClassName = "MyAccountRule";
                    break;

                case PASSWORD_RULE:
                    templateClassName = "MyPasswordRule";
                    break;

                case ITEM_TRANSFORMER:
                    templateClassName = "MyItemTransformer";
                    break;

                case TASKJOB_DELEGATE:
                    templateClassName = "MySchedTaskJobDelegate";
                    break;

                case RECON_FILTER_BUILDER:
                    templateClassName = "MyReconFilterBuilder";
                    break;

                case LOGIC_ACTIONS:
                    templateClassName = "MyLogicActions";
                    break;

                case PROPAGATION_ACTIONS:
                    templateClassName = "MyPropagationActions";
                    break;

                case PULL_ACTIONS:
                    templateClassName = "MyPullActions";
                    break;

                case PUSH_ACTIONS:
                    templateClassName = "MyPushActions";
                    break;

                case PULL_CORRELATION_RULE:
                    templateClassName = "MyPullCorrelationRule";
                    break;

                case VALIDATOR:
                    templateClassName = "MyValidator";
                    break;

                case RECIPIENTS_PROVIDER:
                    templateClassName = "MyRecipientsProvider";
                    break;

                default:
            }

            if (templateClassName != null) {
                try {
                    implementation.setBody(StringUtils.substringAfter(IOUtils.toString(getClass().
                            getResourceAsStream(
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
                ClassPathScanImplementationLookup implementationLookup =
                        (ClassPathScanImplementationLookup) SyncopeConsoleApplication.get().
                                getServletContext().getAttribute(ConsoleInitializer.CLASSPATH_LOOKUP);

                Class<?> clazz = null;
                switch (implementation.getType()) {
                    case REPORTLET:
                        clazz = implementationLookup.getReportletConfs().get(jsonClass.getModelObject());
                        break;

                    case ACCOUNT_RULE:
                        clazz = implementationLookup.getAccountRuleConfs().get(jsonClass.getModelObject());
                        break;

                    case PASSWORD_RULE:
                        clazz = implementationLookup.getPasswordRuleConfs().get(jsonClass.getModelObject());
                        break;

                    default:
                }

                if (clazz != null) {
                    try {
                        target.appendJavaScript("editor.getDoc().setValue('"
                                + MAPPER.writeValueAsString(clazz.newInstance())
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
    public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
        try {
            if (create) {
                restClient.create(implementation);
            } else {
                restClient.update(implementation);
            }

            modal.close(target);
            SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
        } catch (Exception e) {
            LOG.error("While creating or updating AttrTO", e);
            SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage())
                    ? e.getClass().getName()
                    : e.getMessage());
        }
        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
    }

}
