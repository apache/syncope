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
package org.apache.syncope.client.console.notifications;

import org.apache.syncope.client.console.events.EventCategoryPanel;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.panels.search.AbstractSearchPanel;
import org.apache.syncope.client.console.panels.search.AnyObjectSearchPanel;
import org.apache.syncope.client.console.panels.search.GroupSearchPanel;
import org.apache.syncope.client.console.panels.search.SearchClause;
import org.apache.syncope.client.console.panels.search.UserSearchPanel;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.LoggerRestClient;
import org.apache.syncope.client.console.rest.NotificationRestClient;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiPanel;
import org.apache.syncope.client.console.wizards.AjaxWizardBuilder;
import org.apache.syncope.common.lib.to.DerSchemaTO;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.NotificationTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.VirSchemaTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.validation.validator.EmailAddressValidator;

public class NotificationWizardBuilder extends AjaxWizardBuilder<NotificationWrapper> {

    private static final long serialVersionUID = -1975312550059578553L;

    private final NotificationRestClient restClient = new NotificationRestClient();

    private final SchemaRestClient schemaRestClient = new SchemaRestClient();

    private final LoggerRestClient loggerRestClient = new LoggerRestClient();

    /**
     * Construct.
     *
     * @param notificationTO notification.
     * @param pageRef Caller page reference.
     */
    public NotificationWizardBuilder(final NotificationTO notificationTO, final PageReference pageRef) {
        super(new NotificationWrapper(notificationTO), pageRef);
    }

    @Override
    protected Serializable onApplyInternal(final NotificationWrapper modelObject) {
        modelObject.fillRecipientConditions();
        modelObject.fillAboutConditions();

        final boolean createFlag = modelObject.getInnerObject().getKey() == null;
        if (createFlag) {
            restClient.create(modelObject.getInnerObject());
        } else {
            restClient.update(modelObject.getInnerObject());
        }

        return null;
    }

    @Override
    protected WizardModel buildModelSteps(final NotificationWrapper modelObject, final WizardModel wizardModel) {
        wizardModel.add(new NotificationWizardBuilder.Details(modelObject));
        wizardModel.add(new NotificationWizardBuilder.Recipients(modelObject));
        wizardModel.add(new NotificationWizardBuilder.Events(modelObject));
        wizardModel.add(new NotificationWizardBuilder.Abouts(modelObject));
        return wizardModel;
    }

    public class Details extends WizardStep {

        private static final long serialVersionUID = -7709805590497687958L;

        public Details(final NotificationWrapper modelObject) {
            NotificationTO notificationTO = modelObject.getInnerObject();
            boolean createFlag = notificationTO.getKey() == null;

            AjaxTextFieldPanel sender = new AjaxTextFieldPanel("sender", getString("sender"),
                    new PropertyModel<>(notificationTO, "sender"));
            sender.addRequiredLabel();
            sender.addValidator(EmailAddressValidator.getInstance());
            add(sender);

            AjaxTextFieldPanel subject = new AjaxTextFieldPanel("subject", getString("subject"),
                    new PropertyModel<>(notificationTO, "subject"));
            subject.addRequiredLabel();
            add(subject);

            AjaxDropDownChoicePanel<String> template = new AjaxDropDownChoicePanel<>(
                    "template", getString("template"),
                    new PropertyModel<>(notificationTO, "template"));
            template.setChoices(restClient.listTemplates().stream().
                    map(EntityTO::getKey).collect(Collectors.toList()));

            template.addRequiredLabel();
            add(template);

            AjaxDropDownChoicePanel<TraceLevel> traceLevel = new AjaxDropDownChoicePanel<>(
                    "traceLevel", getString("traceLevel"),
                    new PropertyModel<>(notificationTO, "traceLevel"));
            traceLevel.setChoices(Arrays.asList(TraceLevel.values()));
            traceLevel.addRequiredLabel();
            add(traceLevel);

            final AjaxCheckBoxPanel isActive = new AjaxCheckBoxPanel("isActive",
                    getString("isActive"), new PropertyModel<>(notificationTO, "active"));
            if (createFlag) {
                isActive.getField().setDefaultModelObject(Boolean.TRUE);
            }
            add(isActive);
        }

    }

    public class Events extends WizardStep {

        private static final long serialVersionUID = -7709805590497687958L;

        public Events(final NotificationWrapper modelObject) {
            setTitleModel(new ResourceModel("events"));

            add(new EventCategoryPanel(
                    "eventSelection",
                    loggerRestClient.listEvents(),
                    new PropertyModel<List<String>>(modelObject.getInnerObject(), "events")) {

                private static final long serialVersionUID = 6429053774964787735L;

                @Override
                protected List<String> getListAuthRoles() {
                    return Collections.emptyList();
                }

                @Override
                protected List<String> getChangeAuthRoles() {
                    return Collections.emptyList();
                }
            });
        }

    }

    public class About extends Panel {

        private static final long serialVersionUID = -9149543787708482882L;

        public About(final String id, final IModel<Pair<String, List<SearchClause>>> model) {
            super(id, model);
            setOutputMarkupId(true);

            final AjaxDropDownChoicePanel<String> type =
                    new AjaxDropDownChoicePanel<>("about", "anyType", new Model<String>() {

                        private static final long serialVersionUID = -2350296434572623272L;

                        @Override
                        public String getObject() {
                            return model.getObject().getLeft();
                        }

                        @Override
                        public void setObject(final String object) {
                            model.setObject(Pair.of(object, model.getObject().getRight()));
                        }

                    });
            type.setChoices(new AnyTypeRestClient().list());
            type.addRequiredLabel();
            add(type);

            final ListModel<SearchClause> clauseModel = new ListModel<SearchClause>() {

                private static final long serialVersionUID = 3769540249683319782L;

                @Override
                public List<SearchClause> getObject() {
                    return model.getObject().getRight();
                }

                @Override
                public void setObject(final List<SearchClause> object) {
                    model.getObject().setValue(object);
                }

            };

            final WebMarkupContainer searchContainer = new WebMarkupContainer("search");
            add(searchContainer.setOutputMarkupId(true));

            searchContainer.add(getClauseBuilder(model.getObject().getLeft(), clauseModel).build("clauses"));

            type.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    clauseModel.getObject().clear();
                    searchContainer.addOrReplace(getClauseBuilder(type.getModelObject(), clauseModel).build("clauses").
                            setRenderBodyOnly(true));
                    target.add(searchContainer);
                }
            });
        }

        private AbstractSearchPanel.Builder<?> getClauseBuilder(
                final String type, final ListModel<SearchClause> clauseModel) {

            AbstractSearchPanel.Builder<?> clause;

            switch (type) {
                case "USER":
                    clause = new UserSearchPanel.Builder(clauseModel);
                    break;

                case "GROUP":
                    clause = new GroupSearchPanel.Builder(clauseModel);
                    break;

                default:
                    clause = new AnyObjectSearchPanel.Builder(type, clauseModel);
            }

            return clause;
        }
    }

    public class Abouts extends WizardStep {

        private static final long serialVersionUID = -7709805590497687958L;

        public Abouts(final NotificationWrapper modelObject) {
            setTitleModel(new ResourceModel("about"));

            final WebMarkupContainer aboutContainer = new WebMarkupContainer("about");
            aboutContainer.setOutputMarkupId(true);
            add(aboutContainer);

            final IModel<List<Pair<String, List<SearchClause>>>> model =
                    new PropertyModel<>(modelObject, "aboutClauses");

            aboutContainer.add(new MultiPanel<Pair<String, List<SearchClause>>>("abouts", "abouts", model) {

                private static final long serialVersionUID = -2481579077338205547L;

                @Override
                protected Pair<String, List<SearchClause>> newModelObject() {
                    return Pair.<String, List<SearchClause>>of(AnyTypeKind.USER.name(), new ArrayList<>());
                }

                @Override
                protected About getItemPanel(final ListItem<Pair<String, List<SearchClause>>> item) {

                    return new About("panel", new Model<Pair<String, List<SearchClause>>>() {

                        private static final long serialVersionUID = 6799404673615637845L;

                        @Override
                        public Pair<String, List<SearchClause>> getObject() {
                            return item.getModelObject();
                        }

                        @Override
                        public void setObject(final Pair<String, List<SearchClause>> object) {
                            item.setModelObject(object);
                        }

                        @Override
                        public void detach() {
                            // no detach
                        }
                    });
                }
            }.hideLabel());
        }
    }

    public class Recipients extends WizardStep {

        private static final long serialVersionUID = -7709805590497687958L;

        private final IModel<List<String>> recipientProviders = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157447L;

            @Override
            protected List<String> load() {
                return new ArrayList<>(
                        SyncopeConsoleSession.get().getPlatformInfo().getNotificationRecipientsProviders());
            }
        };

        public Recipients(final NotificationWrapper modelObject) {
            setTitleModel(new ResourceModel("recipients"));

            NotificationTO notificationTO = modelObject.getInnerObject();

            AjaxTextFieldPanel recipientAttrName = new AjaxTextFieldPanel(
                    "recipientAttrName", new ResourceModel("recipientAttrName", "recipientAttrName").getObject(),
                    new PropertyModel<>(notificationTO, "recipientAttrName"));
            recipientAttrName.setChoices(getSchemaNames());
            recipientAttrName.addRequiredLabel();
            recipientAttrName.setTitle(getString("intAttrNameInfo.help")
                    + "<div style=\"font-size: 10px;\">"
                    + "<code>groups[groupName].attribute</code>\n"
                    + "<code>anyObjects[anyObjectName].attribute</code>\n"
                    + "<code>memberships[groupName].attribute</code>\n"
                    + "</div>", true);
            add(recipientAttrName);

            AjaxTextFieldPanel staticRecipientsFieldPanel =
                    new AjaxTextFieldPanel("panel", "staticRecipients", new Model<>());
            staticRecipientsFieldPanel.addValidator(EmailAddressValidator.getInstance());
            add(new MultiFieldPanel.Builder<>(
                    new PropertyModel<List<String>>(notificationTO, "staticRecipients")).
                    build("staticRecipients", "staticRecipients", staticRecipientsFieldPanel).hideLabel());

            add(new UserSearchPanel.Builder(
                    new PropertyModel<>(modelObject, "recipientClauses")).
                    required(false).build("recipients"));

            AjaxDropDownChoicePanel<String> recipientsProviderClassName = new AjaxDropDownChoicePanel<>(
                    "recipientsProviderClassName", "recipientsProviderClassName",
                    new PropertyModel<>(notificationTO, "recipientsProviderClassName"), false);
            recipientsProviderClassName.setChoices(recipientProviders.getObject());
            add(recipientsProviderClassName);

            AjaxCheckBoxPanel selfAsRecipient = new AjaxCheckBoxPanel("selfAsRecipient",
                    getString("selfAsRecipient"), new PropertyModel<>(notificationTO, "selfAsRecipient"));
            if (notificationTO.getKey() == null) {
                selfAsRecipient.getField().setDefaultModelObject(Boolean.FALSE);
            }
            add(selfAsRecipient);
        }
    }

    private List<String> getSchemaNames() {
        List<String> result = new ArrayList<>();
        result.add("username");

        result.addAll(schemaRestClient.<PlainSchemaTO>getSchemas(SchemaType.PLAIN, AnyTypeKind.USER.name()).
                stream().map(EntityTO::getKey).collect(Collectors.toList()));
        result.addAll(schemaRestClient.<DerSchemaTO>getSchemas(SchemaType.DERIVED, AnyTypeKind.USER.name()).
                stream().map(EntityTO::getKey).collect(Collectors.toList()));
        result.addAll(schemaRestClient.<VirSchemaTO>getSchemas(SchemaType.VIRTUAL, AnyTypeKind.USER.name()).
                stream().map(EntityTO::getKey).collect(Collectors.toList()));

        Collections.sort(result);
        return result;
    }
}
