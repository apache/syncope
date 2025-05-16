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
package org.apache.syncope.client.enduser.pages;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.syncope.client.enduser.rest.AuthProfileRestClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.annotations.ExtPage;
import org.apache.syncope.client.ui.commons.markup.html.form.IndicatingOnConfirmAjaxLink;
import org.apache.syncope.common.lib.to.AuthProfileTO;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthAccount;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthToken;
import org.apache.syncope.common.lib.wa.ImpersonationAccount;
import org.apache.syncope.common.lib.wa.MfaTrustedDevice;
import org.apache.syncope.common.lib.wa.WebAuthnDeviceCredential;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

@ExtPage(label = "Auth Profile", icon = "fa fa-passport", listEntitlement = "")
public class AuthProfile extends BaseExtPage {

    private static final long serialVersionUID = -3147262161518280928L;

    protected static final String AUTH_PROFILE = "page.authProfile";

    protected static final int ROWS_PER_PAGE = 5;

    @SpringBean
    protected AuthProfileRestClient restClient;

    public AuthProfile(final PageParameters parameters) {
        super(parameters, AUTH_PROFILE);

        Optional<AuthProfileTO> authProfile = restClient.read();

        WebMarkupContainer container = new WebMarkupContainer("content");
        contentWrapper.add(container.setOutputMarkupId(true));

        DataView<ImpersonationAccount> impersonationAccounts = new DataView<>(
                "impersonationAccounts", new ListDataProvider<>(
                        authProfile.map(AuthProfileTO::getImpersonationAccounts).orElseGet(() -> List.of()))) {

            private static final long serialVersionUID = 6127875313385810666L;

            @Override
            public void populateItem(final Item<ImpersonationAccount> item) {
                item.add(new Label("impersonated", item.getModelObject().getImpersonated()));
                item.add(new IndicatingOnConfirmAjaxLink<>(
                        "impersonationAccountDelete", Constants.CONFIRM_DELETE, true) {

                    private static final long serialVersionUID = 1632838687547839512L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        authProfile.ifPresent(p -> {
                            p.getImpersonationAccounts().remove(item.getModelObject());
                            restClient.update(p);
                            target.add(container);
                        });
                    }
                });
            }
        };
        impersonationAccounts.setItemsPerPage(ROWS_PER_PAGE);
        container.add(impersonationAccounts.setOutputMarkupPlaceholderTag(true));
        container.add(new AjaxPagingNavigator("impersonationAccountsNavigator", impersonationAccounts));

        DataView<GoogleMfaAuthToken> googleMfaAuthTokens = new DataView<>(
                "googleMfaAuthTokens", new ListDataProvider<>(
                        authProfile.map(AuthProfileTO::getGoogleMfaAuthTokens).orElseGet(() -> List.of()))) {

            private static final long serialVersionUID = 6127875313385810666L;

            @Override
            public void populateItem(final Item<GoogleMfaAuthToken> item) {
                item.add(new Label("otp", item.getModelObject().getOtp()));
                item.add(new Label("issueDate", item.getModelObject().getIssueDate()));
                item.add(new IndicatingOnConfirmAjaxLink<>(
                        "googleMfaAuthTokenDelete", Constants.CONFIRM_DELETE, true) {

                    private static final long serialVersionUID = 1632838687547839512L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        authProfile.ifPresent(p -> {
                            p.getGoogleMfaAuthTokens().remove(item.getModelObject());
                            restClient.update(p);
                            target.add(container);
                        });
                    }
                });
            }
        };
        googleMfaAuthTokens.setItemsPerPage(ROWS_PER_PAGE);
        container.add(googleMfaAuthTokens.setOutputMarkupPlaceholderTag(true));
        container.add(new AjaxPagingNavigator("googleMfaAuthTokensNavigator", googleMfaAuthTokens));

        DataView<GoogleMfaAuthAccount> googleMfaAuthAccounts = new DataView<>(
                "googleMfaAuthAccounts", new ListDataProvider<>(
                        authProfile.map(AuthProfileTO::getGoogleMfaAuthAccounts).orElseGet(() -> List.of()))) {

            private static final long serialVersionUID = 6127875313385810666L;

            @Override
            public void populateItem(final Item<GoogleMfaAuthAccount> item) {
                item.add(new Label("id", item.getModelObject().getId()));
                item.add(new Label("name", item.getModelObject().getName()));
                item.add(new Label("secretKey", item.getModelObject().getSecretKey()));
                item.add(new Label("validationCode", item.getModelObject().getValidationCode()));
                item.add(new Label("scratchCodes", item.getModelObject().getScratchCodes().stream().
                        map(String::valueOf).collect(Collectors.joining(", "))));
                item.add(new Label("registrationDate", item.getModelObject().getRegistrationDate()));
                item.add(new IndicatingOnConfirmAjaxLink<>(
                        "googleMfaAuthAccountDelete", Constants.CONFIRM_DELETE, true) {

                    private static final long serialVersionUID = 1632838687547839512L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        authProfile.ifPresent(p -> {
                            p.getGoogleMfaAuthAccounts().remove(item.getModelObject());
                            restClient.update(p);
                            target.add(container);
                        });
                    }
                });
            }
        };
        googleMfaAuthAccounts.setItemsPerPage(ROWS_PER_PAGE);
        container.add(googleMfaAuthAccounts.setOutputMarkupPlaceholderTag(true));
        container.add(new AjaxPagingNavigator("googleMfaAuthAccountsNavigator", googleMfaAuthAccounts));

        DataView<MfaTrustedDevice> mfaTrustedDevices = new DataView<>(
                "mfaTrustedDevices", new ListDataProvider<>(
                        authProfile.map(AuthProfileTO::getMfaTrustedDevices).orElseGet(() -> List.of()))) {

            private static final long serialVersionUID = 6127875313385810666L;

            @Override
            public void populateItem(final Item<MfaTrustedDevice> item) {
                item.add(new Label("id", item.getModelObject().getId()));
                item.add(new Label("name", item.getModelObject().getName()));
                item.add(new Label("deviceFingerprint", item.getModelObject().getDeviceFingerprint()));
                item.add(new Label("recordDate", item.getModelObject().getRecordDate()));
                item.add(new Label("expirationDate", item.getModelObject().getExpirationDate()));
                item.add(new IndicatingOnConfirmAjaxLink<>(
                        "mfaTrustedDeviceDelete", Constants.CONFIRM_DELETE, true) {

                    private static final long serialVersionUID = 1632838687547839512L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        authProfile.ifPresent(p -> {
                            p.getMfaTrustedDevices().remove(item.getModelObject());
                            restClient.update(p);
                            target.add(container);
                        });
                    }
                });
            }
        };
        mfaTrustedDevices.setItemsPerPage(ROWS_PER_PAGE);
        container.add(mfaTrustedDevices.setOutputMarkupPlaceholderTag(true));
        container.add(new AjaxPagingNavigator("mfaTrustedDevicesNavigator", mfaTrustedDevices));

        DataView<WebAuthnDeviceCredential> webAuthnDeviceCredentials = new DataView<>(
                "webAuthnDeviceCredentials", new ListDataProvider<>(
                        authProfile.map(AuthProfileTO::getWebAuthnDeviceCredentials).orElseGet(() -> List.of()))) {

            private static final long serialVersionUID = 6127875313385810666L;

            @Override
            public void populateItem(final Item<WebAuthnDeviceCredential> item) {
                item.add(new Label("identifier", item.getModelObject().getIdentifier()));
                item.add(new IndicatingOnConfirmAjaxLink<>(
                        "webAuthnDeviceCredentialDelete", Constants.CONFIRM_DELETE, true) {

                    private static final long serialVersionUID = 1632838687547839512L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        authProfile.ifPresent(p -> {
                            p.getWebAuthnDeviceCredentials().remove(item.getModelObject());
                            restClient.update(p);
                            target.add(container);
                        });
                    }
                });
            }
        };
        webAuthnDeviceCredentials.setItemsPerPage(ROWS_PER_PAGE);
        container.add(webAuthnDeviceCredentials.setOutputMarkupPlaceholderTag(true));
        container.add(new AjaxPagingNavigator("webAuthnDeviceCredentialsNavigator", webAuthnDeviceCredentials));
    }
}
