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
package org.apache.syncope.client.ui.commons;

import com.googlecode.wicket.kendo.ui.widget.notification.Notification;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.HttpHeaders;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.ui.commons.panels.NotificationPanel;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.StatelessForm;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseLogin extends WebPage {

    private static final long serialVersionUID = 5889157642852559004L;

    protected static final Logger LOG = LoggerFactory.getLogger(BaseLogin.class);

    @SpringBean
    protected DomainOps domainOps;

    protected final NotificationPanel notificationPanel;

    protected final StatelessForm<Void> form;

    protected final TextField<String> usernameField;

    protected final TextField<String> passwordField;

    protected String notificationMessage;

    protected String notificationLevel;

    protected final LoadableDetachableModel<List<String>> domains = new LoadableDetachableModel<>() {

        private static final long serialVersionUID = 4659376149825914247L;

        @Override
        protected List<String> load() {
            List<String> current = new ArrayList<>();
            current.addAll(domainOps.list().stream().map(Domain::getKey).sorted().toList());
            current.addFirst(SyncopeConstants.MASTER_DOMAIN);
            return current;
        }
    };

    public BaseLogin(final PageParameters parameters) {
        super(parameters);
        setStatelessHint(true);

        notificationPanel = new NotificationPanel(Constants.FEEDBACK);
        add(notificationPanel);

        if (!parameters.get("notificationMessage").isNull()) {
            notificationMessage = parameters.get(Constants.NOTIFICATION_MSG_PARAM).toString();
            notificationLevel = parameters.get(Constants.NOTIFICATION_LEVEL_PARAM).isEmpty()
                    ? Notification.SUCCESS
                    : parameters.get(Constants.NOTIFICATION_LEVEL_PARAM).toString();
        }

        Label exceptionMessage = new Label("exceptionMessage");
        exceptionMessage.setOutputMarkupPlaceholderTag(true);
        exceptionMessage.setVisible(false);
        if (!parameters.get("errorMessage").isNull()) {
            exceptionMessage.setVisible(true);
            exceptionMessage.setDefaultModel(Model.of(parameters.get("errorMessage")));
        }
        add(exceptionMessage);

        form = new StatelessForm<>("login");

        usernameField = new TextField<>("username", new Model<>());
        usernameField.setMarkupId("username");
        form.add(usernameField);

        passwordField = new PasswordTextField("password", new Model<>());
        passwordField.setMarkupId("password");
        form.add(passwordField);

        LocaleDropDown languageSelect = new LocaleDropDown("language");
        languageSelect.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_BLUR) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                getLanguageOnChangeComponents().forEach(target::add);
            }
        }).add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                getLanguageOnChangeComponents().forEach(target::add);
            }
        });
        form.add(languageSelect.setOutputMarkupId(true));

        DomainDropDown domainSelect = new DomainDropDown("domain", domains);
        domainSelect.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_BLUR) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                getDomainOnChangeComponents().forEach(target::add);
            }
        }).add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                getDomainOnChangeComponents().forEach(target::add);
            }
        });
        form.add(domainSelect.setOutputMarkupId(true));

        AjaxButton submitButton = new AjaxButton("submit", new Model<>(getString("submit"))) {

            private static final long serialVersionUID = 429178684321093953L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                authenticate(usernameField.getRawInput(), passwordField.getRawInput(), target);
            }

        };
        submitButton.setDefaultFormProcessing(false);
        form.add(submitButton);
        form.setDefaultButton(submitButton);

        List<Panel> ssoLoginFormPanels = getSSOLoginFormPanels();
        ListView<Panel> ssoLogins = new ListView<>("ssoLogins", ssoLoginFormPanels) {

            private static final long serialVersionUID = -9180479401817023838L;

            @Override
            protected void populateItem(final ListItem<Panel> item) {
                item.add(item.getModelObject());
            }
        };
        form.add(ssoLogins);

        add(form);
    }

    protected Collection<Component> getLanguageOnChangeComponents() {
        return List.of(form);
    }

    protected Collection<Component> getDomainOnChangeComponents() {
        return List.of(form);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        if (StringUtils.isNotBlank(notificationMessage)) {
            response.render(OnLoadHeaderItem.forScript(StyledNotificationBehavior.jQueryShow(notificationMessage,
                    String.format("jQuery('#%s').data('kendoNotification')",
                            notificationPanel.getNotificationMarkupId()), notificationLevel)));
        }
    }

    protected abstract BaseSession getBaseSession();

    protected abstract List<Panel> getSSOLoginFormPanels();

    protected abstract void sendError(String error);

    protected abstract void authenticate(
            String username,
            String password,
            AjaxRequestTarget target)
            throws NotAuthorizedException;

    /**
     * Inner class which implements (custom) Locale DropDownChoice component.
     */
    protected class LocaleDropDown extends DropDownChoice<Locale> {

        private static final long serialVersionUID = 2349382679992357202L;

        protected class LocaleRenderer extends ChoiceRenderer<Locale> {

            private static final long serialVersionUID = -3657529581555164741L;

            @Override
            public String getDisplayValue(final Locale locale) {
                return locale.getDisplayName(getLocale());
            }
        }

        protected LocaleDropDown(final String id) {
            super(id, getBaseSession().getSupportedLocales());

            setChoiceRenderer(new LocaleRenderer());
            setModel(new IModel<>() {

                private static final long serialVersionUID = -6985170095629312963L;

                @Override
                public Locale getObject() {
                    return getSession().getLocale();
                }

                @Override
                public void setObject(final Locale object) {
                    getSession().setLocale(object);
                }

                @Override
                public void detach() {
                    // Empty.
                }
            });

            // set default language selection
            List<Locale> filtered = List.of();

            String acceptLanguage = ((ServletWebRequest) RequestCycle.get().getRequest()).
                    getHeader(HttpHeaders.ACCEPT_LANGUAGE);
            if (StringUtils.isNotBlank(acceptLanguage)) {
                try {
                    filtered = Locale.filter(
                            Locale.LanguageRange.parse(acceptLanguage),
                            getBaseSession().getSupportedLocales());
                } catch (Exception e) {
                    LOG.debug("Could not parse {} HTTP header value '{}'",
                            HttpHeaders.ACCEPT_LANGUAGE, acceptLanguage, e);
                }
            }

            getModel().setObject(filtered.isEmpty()
                    ? Locale.ENGLISH
                    : filtered.getFirst());
        }
    }
}
