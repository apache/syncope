/*
 *  Copyright (C) 2020 Tirasa (info@tirasa.net)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.syncope.client.enduser.pages;

import com.googlecode.wicket.kendo.ui.widget.notification.Notification;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.select.BootstrapSelect;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.ws.rs.core.HttpHeaders;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.enduser.SyncopeEnduserApplication;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.enduser.init.EnduserInitializer;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.StyledNotificationBehavior;
import org.apache.syncope.client.ui.commons.panels.NotificationPanel;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authentication.IAuthenticationStrategy;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.StatelessForm;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Login extends WebPage {

    private static final long serialVersionUID = 5889157642852559004L;

    private static final Logger LOG = LoggerFactory.getLogger(Login.class);

    private final NotificationPanel notificationPanel;

    protected String notificationMessage;

    protected String notificationLevel;

    private final StatelessForm<Void> form;

    private final TextField<String> usernameField;

    private final TextField<String> passwordField;

    private final BookmarkablePageLink<Void> selfPwdReset;

    private final BookmarkablePageLink<Void> selfRegistration;

    public Login(final PageParameters parameters) {
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
                target.add(form);
            }
        }).add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                target.add(form);
            }
        });
        form.add(languageSelect.setOutputMarkupId(true));

        DomainDropDown domainSelect = new DomainDropDown("domain");
        domainSelect.setOutputMarkupPlaceholderTag(true);
        if (SyncopeEnduserApplication.get().getDomains().size() == 1) {
            domainSelect.setVisible(false);
        }
        domainSelect.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_BLUR) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                target.add(form);
            }
        }).add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                target.add(form);
            }
        });
        form.add(domainSelect.setOutputMarkupId(true));

        AjaxButton submitButton = new AjaxButton("submit", new Model<>(getString("submit"))) {

            private static final long serialVersionUID = 429178684321093953L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                if (SyncopeEnduserApplication.get().getAnonymousUser().equals(usernameField.getRawInput())) {
                    throw new AccessControlException("Illegal username");
                }

                IAuthenticationStrategy strategy = getApplication().getSecuritySettings().getAuthenticationStrategy();

                if (AuthenticatedWebSession.get().signIn(usernameField.getRawInput(), passwordField.getRawInput())) {
                    // If login has been called because the user was not yet logged in, than continue to the
                    // original destination, otherwise to the Home page
                    continueToOriginalDestination();
                    setResponsePage(getApplication().getHomePage());
                } else {
                    SyncopeEnduserSession.get().error(getString("login-error"));
                    notificationPanel.refresh(target);
                }
                strategy.remove();
            }
        };
        submitButton.setDefaultFormProcessing(false);
        form.add(submitButton);
        form.setDefaultButton(submitButton);

        ClassPathScanImplementationLookup classPathScanImplementationLookup =
                (ClassPathScanImplementationLookup) SyncopeEnduserApplication.get().
                        getServletContext().getAttribute(EnduserInitializer.CLASSPATH_LOOKUP);
        List<Panel> ssoLoginFormPanels = new ArrayList<>();
        classPathScanImplementationLookup.getSSOLoginFormPanels().forEach(ssoLoginFormPanel -> {
            try {
                ssoLoginFormPanels.add(ssoLoginFormPanel.getConstructor(String.class).newInstance("ssoLogin"));
            } catch (Exception e) {
                LOG.error("Could not initialize the provided SSO login form panel", e);
            }
        });
        ListView<Panel> ssoLogins = new ListView<Panel>("ssoLogins", ssoLoginFormPanels) {

            private static final long serialVersionUID = -9180479401817023838L;

            @Override
            protected void populateItem(final ListItem<Panel> item) {
                item.add(item.getModelObject());
            }
        };
        form.add(ssoLogins);

        add(form);

        selfPwdReset = new BookmarkablePageLink<>("self-pwd-reset", SelfPasswordReset.class);
        selfPwdReset.getPageParameters().add("domain", SyncopeEnduserSession.get().getDomain());
        selfPwdReset.setOutputMarkupPlaceholderTag(true);
        selfPwdReset.setVisible(SyncopeEnduserSession.get().getPlatformInfo().isPwdResetAllowed());
        add(selfPwdReset.setOutputMarkupId(true));

        selfRegistration = new BookmarkablePageLink<>("self-registration", SelfRegistration.class);
        selfRegistration.getPageParameters().add("domain", SyncopeEnduserSession.get().getDomain());
        selfRegistration.setOutputMarkupPlaceholderTag(true);
        selfRegistration.setVisible(SyncopeEnduserSession.get().getPlatformInfo().isSelfRegAllowed());
        add(selfRegistration.setOutputMarkupId(true));
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

    /**
     * Inner class which implements (custom) Locale DropDownChoice component.
     */
    private class LocaleDropDown extends BootstrapSelect<Locale> {

        private static final long serialVersionUID = 2349382679992357202L;

        private class LocaleRenderer extends ChoiceRenderer<Locale> {

            private static final long serialVersionUID = -3657529581555164741L;

            @Override
            public String getDisplayValue(final Locale locale) {
                return locale.getDisplayName(getLocale());
            }
        }

        LocaleDropDown(final String id) {
            super(id, SyncopeEnduserApplication.SUPPORTED_LOCALES);

            setChoiceRenderer(new LocaleRenderer());
            setModel(new IModel<Locale>() {

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
            List<Locale> filtered = Collections.emptyList();

            String acceptLanguage = ((ServletWebRequest) RequestCycle.get().getRequest()).
                    getHeader(HttpHeaders.ACCEPT_LANGUAGE);
            if (StringUtils.isNotBlank(acceptLanguage)) {
                try {
                    filtered = Locale.filter(
                            Locale.LanguageRange.parse(acceptLanguage), SyncopeEnduserApplication.SUPPORTED_LOCALES);
                } catch (Exception e) {
                    LOG.debug("Could not parse {} HTTP header value '{}'",
                            HttpHeaders.ACCEPT_LANGUAGE, acceptLanguage, e);
                }
            }

            getModel().setObject(filtered.isEmpty()
                    ? Locale.ENGLISH
                    : filtered.get(0));
        }
    }

    /**
     * Inner class which implements (custom) Domain DropDownChoice component.
     */
    private static class DomainDropDown extends BootstrapSelect<String> {

        private static final long serialVersionUID = -7401167913360133325L;

        DomainDropDown(final String id) {
            super(id, SyncopeEnduserApplication.get().getDomains());

            setModel(new IModel<String>() {

                private static final long serialVersionUID = -1124206668056084806L;

                @Override
                public String getObject() {
                    return SyncopeEnduserSession.get().getDomain();
                }

                @Override
                public void setObject(final String object) {
                    SyncopeEnduserSession.get().setDomain(object);
                }

                @Override
                public void detach() {
                    // Empty.
                }
            });

            // set default value to Master Domain
            getModel().setObject(SyncopeConstants.MASTER_DOMAIN);
        }
    }
}
