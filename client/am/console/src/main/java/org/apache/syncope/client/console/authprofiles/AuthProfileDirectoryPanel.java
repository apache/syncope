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
package org.apache.syncope.client.console.authprofiles;

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.authprofiles.AuthProfileDirectoryPanel.AuthProfileProvider;
import org.apache.syncope.client.console.commons.AMConstants;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.panels.ModalDirectoryPanel;
import org.apache.syncope.client.console.rest.AuthProfileRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.BooleanConditionColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.KeyPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.common.lib.to.AuthProfileTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthAccount;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthToken;
import org.apache.syncope.common.lib.wa.ImpersonationAccount;
import org.apache.syncope.common.lib.wa.MfaTrustedDevice;
import org.apache.syncope.common.lib.wa.WebAuthnDeviceCredential;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

public class AuthProfileDirectoryPanel
        extends DirectoryPanel<AuthProfileTO, AuthProfileTO, AuthProfileProvider, AuthProfileRestClient> {

    private static final long serialVersionUID = 2018518567549153364L;

    private final BaseModal<AuthProfileTO> authProfileModal;

    public AuthProfileDirectoryPanel(
            final String id, final AuthProfileRestClient restClient, final PageReference pageRef) {

        super(id, restClient, pageRef);

        authProfileModal = new BaseModal<>(Constants.OUTER) {

            private static final long serialVersionUID = 389935548143327858L;

            @Override
            protected void onConfigure() {
                super.onConfigure();
                setFooterVisible(false);
            }
        };
        authProfileModal.size(Modal.Size.Large);
        authProfileModal.setWindowClosedCallback(target -> {
            updateResultTable(target);
            authProfileModal.show(false);
        });
        addOuterObject(authProfileModal);

        addNewItemPanelBuilder(new CreateAuthProfileWizardBuilder(pageRef), true);
        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, AMEntitlement.AUTH_PROFILE_CREATE);

        disableCheckBoxes();
        initResultTable();
    }

    @Override
    protected AuthProfileProvider dataProvider() {
        return new AuthProfileProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return AMConstants.PREF_AUTHPROFILE_PAGINATOR_ROWS;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of();
    }

    @Override
    protected List<IColumn<AuthProfileTO, String>> getColumns() {
        List<IColumn<AuthProfileTO, String>> columns = new ArrayList<>();

        columns.add(new KeyPropertyColumn<>(
                new StringResourceModel(Constants.KEY_FIELD_NAME, this), Constants.KEY_FIELD_NAME));

        columns.add(new PropertyColumn<>(new ResourceModel("owner"), "owner", "owner"));

        columns.add(new BooleanConditionColumn<>(new StringResourceModel("impersonationAccounts")) {

            private static final long serialVersionUID = -8236820422411536323L;

            @Override
            protected boolean isCondition(final IModel<AuthProfileTO> rowModel) {
                return CollectionUtils.isNotEmpty(rowModel.getObject().getImpersonationAccounts());
            }
        });
        columns.add(new BooleanConditionColumn<>(new StringResourceModel("googleMfaAuthTokens")) {

            private static final long serialVersionUID = -8236820422411536323L;

            @Override
            protected boolean isCondition(final IModel<AuthProfileTO> rowModel) {
                return CollectionUtils.isNotEmpty(rowModel.getObject().getGoogleMfaAuthTokens());
            }
        });
        columns.add(new BooleanConditionColumn<>(new StringResourceModel("googleMfaAuthAccounts")) {

            private static final long serialVersionUID = -8236820422411536323L;

            @Override
            protected boolean isCondition(final IModel<AuthProfileTO> rowModel) {
                return CollectionUtils.isNotEmpty(rowModel.getObject().getGoogleMfaAuthAccounts());
            }
        });
        columns.add(new BooleanConditionColumn<>(new StringResourceModel("mfaTrustedDevices")) {

            private static final long serialVersionUID = -8236820422411536323L;

            @Override
            protected boolean isCondition(final IModel<AuthProfileTO> rowModel) {
                return CollectionUtils.isNotEmpty(rowModel.getObject().getMfaTrustedDevices());
            }
        });
        columns.add(new BooleanConditionColumn<>(new StringResourceModel("webAuthnAccount")) {

            private static final long serialVersionUID = -8236820422411536323L;

            @Override
            protected boolean isCondition(final IModel<AuthProfileTO> rowModel) {
                return CollectionUtils.isNotEmpty(rowModel.getObject().getWebAuthnDeviceCredentials());
            }
        });

        return columns;
    }

    @Override
    public ActionsPanel<AuthProfileTO> getActions(final IModel<AuthProfileTO> model) {
        ActionsPanel<AuthProfileTO> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final AuthProfileTO ignore) {
                model.setObject(restClient.read(model.getObject().getKey()));
                target.add(authProfileModal.setContent(new ModalDirectoryPanel<>(
                        authProfileModal,
                        new AuthProfileItemDirectoryPanel<ImpersonationAccount>(
                                "panel", restClient, authProfileModal, model.getObject(), pageRef) {

                    private static final long serialVersionUID = -5380664539000792237L;

                    @Override
                    protected List<ImpersonationAccount> getItems() {
                        return model.getObject().getImpersonationAccounts();
                    }

                    @Override
                    protected ImpersonationAccount defaultItem() {
                        return new ImpersonationAccount();
                    }

                    @Override
                    protected String sortProperty() {
                        return "impersonated";
                    }

                    @Override
                    protected String paginatorRowsKey() {
                        return AMConstants.PREF_AUTHPROFILE_IMPERSONATED_PAGINATOR_ROWS;
                    }

                    @Override
                    protected List<IColumn<ImpersonationAccount, String>> getColumns() {
                        List<IColumn<ImpersonationAccount, String>> columns = new ArrayList<>();
                        columns.add(new PropertyColumn<>(new ResourceModel("impersonated"),
                                "impersonated", "impersonated"));
                        return columns;
                    }
                }, pageRef)));
                authProfileModal.header(new Model<>(getString("impersonationAccounts", model)));
                authProfileModal.show(true);
            }
        }, ActionLink.ActionType.TYPE_EXTENSIONS, AMEntitlement.AUTH_PROFILE_UPDATE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final AuthProfileTO ignore) {
                model.setObject(restClient.read(model.getObject().getKey()));
                target.add(authProfileModal.setContent(new ModalDirectoryPanel<>(
                        authProfileModal,
                        new AuthProfileItemDirectoryPanel<GoogleMfaAuthToken>(
                                "panel", restClient, authProfileModal, model.getObject(), pageRef) {

                    private static final long serialVersionUID = 7332357430197837993L;

                    @Override
                    protected List<GoogleMfaAuthToken> getItems() {
                        return model.getObject().getGoogleMfaAuthTokens();
                    }

                    @Override
                    protected GoogleMfaAuthToken defaultItem() {
                        return new GoogleMfaAuthToken();
                    }

                    @Override
                    protected String sortProperty() {
                        return "issueDate";
                    }

                    @Override
                    protected String paginatorRowsKey() {
                        return AMConstants.PREF_AUTHPROFILE_GOOGLEMFAAUTHTOKENS_PAGINATOR_ROWS;
                    }

                    @Override
                    protected List<IColumn<GoogleMfaAuthToken, String>> getColumns() {
                        List<IColumn<GoogleMfaAuthToken, String>> columns = new ArrayList<>();
                        columns.add(new DatePropertyColumn<>(
                                new ResourceModel("issueDate"), "issueDate", "issueDate"));
                        columns.add(new PropertyColumn<>(
                                new ResourceModel("otp"), "otp", "otp"));
                        return columns;
                    }
                }, pageRef)));
                authProfileModal.header(new Model<>(getString("googleMfaAuthTokens", model)));
                authProfileModal.show(true);
            }
        }, ActionLink.ActionType.EDIT_APPROVAL, AMEntitlement.AUTH_PROFILE_UPDATE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final AuthProfileTO ignore) {
                model.setObject(restClient.read(model.getObject().getKey()));
                target.add(authProfileModal.setContent(new ModalDirectoryPanel<>(
                        authProfileModal,
                        new AuthProfileItemDirectoryPanel<GoogleMfaAuthAccount>(
                                "panel", restClient, authProfileModal, model.getObject(), pageRef) {

                    private static final long serialVersionUID = -670769282358547044L;

                    @Override
                    protected List<GoogleMfaAuthAccount> getItems() {
                        return model.getObject().getGoogleMfaAuthAccounts();
                    }

                    @Override
                    protected GoogleMfaAuthAccount defaultItem() {
                        return new GoogleMfaAuthAccount();
                    }

                    @Override
                    protected String sortProperty() {
                        return "id";
                    }

                    @Override
                    protected String paginatorRowsKey() {
                        return AMConstants.PREF_AUTHPROFILE_GOOGLEMFAAUTHACCOUNTS_PAGINATOR_ROWS;
                    }

                    @Override
                    protected List<IColumn<GoogleMfaAuthAccount, String>> getColumns() {
                        List<IColumn<GoogleMfaAuthAccount, String>> columns = new ArrayList<>();
                        columns.add(new PropertyColumn<>(new ResourceModel("id"), "id", "id"));
                        columns.add(new DatePropertyColumn<>(
                                new ResourceModel("registrationDate"), "registrationDate", "registrationDate"));
                        columns.add(new PropertyColumn<>(new ResourceModel("name"), "name", "name"));
                        return columns;
                    }
                }, pageRef)));
                authProfileModal.header(new Model<>(getString("googleMfaAuthAccounts", model)));
                authProfileModal.show(true);
            }
        }, ActionLink.ActionType.EXECUTE, AMEntitlement.AUTH_PROFILE_UPDATE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final AuthProfileTO ignore) {
                model.setObject(restClient.read(model.getObject().getKey()));
                target.add(authProfileModal.setContent(new ModalDirectoryPanel<>(
                        authProfileModal,
                        new AuthProfileItemDirectoryPanel<MfaTrustedDevice>(
                                "panel", restClient, authProfileModal, model.getObject(), pageRef) {

                    private static final long serialVersionUID = 5788448799796630011L;

                    @Override
                    protected List<MfaTrustedDevice> getItems() {
                        return model.getObject().getMfaTrustedDevices();
                    }

                    @Override
                    protected MfaTrustedDevice defaultItem() {
                        return new MfaTrustedDevice();
                    }

                    @Override
                    protected String sortProperty() {
                        return "id";
                    }

                    @Override
                    protected String paginatorRowsKey() {
                        return AMConstants.PREF_AUTHPROFILE_MFA_TRUSTED_FDEVICES_PAGINATOR_ROWS;
                    }

                    @Override
                    protected List<IColumn<MfaTrustedDevice, String>> getColumns() {
                        List<IColumn<MfaTrustedDevice, String>> columns = new ArrayList<>();
                        columns.add(new PropertyColumn<>(new ResourceModel("id"), "id", "id"));
                        columns.add(new PropertyColumn<>(new ResourceModel("name"), "name", "name"));
                        columns.add(new DatePropertyColumn<>(
                                new ResourceModel("recordDate"), "recordDate", "recordDate"));
                        columns.add(new DatePropertyColumn<>(
                                new ResourceModel("expirationDate"), "expirationDate", "expirationDate"));
                        return columns;
                    }
                }, pageRef)));
                authProfileModal.header(new Model<>(getString("mfaTrustedDevices", model)));
                authProfileModal.show(true);
            }
        }, ActionLink.ActionType.DOWN, AMEntitlement.AUTH_PROFILE_UPDATE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final AuthProfileTO ignore) {
                model.setObject(restClient.read(model.getObject().getKey()));
                target.add(authProfileModal.setContent(new ModalDirectoryPanel<>(
                        authProfileModal,
                        new AuthProfileItemDirectoryPanel<WebAuthnDeviceCredential>(
                                "panel", restClient, authProfileModal, model.getObject(), pageRef) {

                    private static final long serialVersionUID = 6820212423488933184L;

                    @Override
                    protected List<WebAuthnDeviceCredential> getItems() {
                        return model.getObject().getWebAuthnDeviceCredentials();
                    }

                    @Override
                    protected WebAuthnDeviceCredential defaultItem() {
                        return new WebAuthnDeviceCredential();
                    }

                    @Override
                    protected String sortProperty() {
                        return "identifier";
                    }

                    @Override
                    protected String paginatorRowsKey() {
                        return AMConstants.PREF_AUTHPROFILE_WEBAUTHNDEVICECREDENTIALS_PAGINATOR_ROWS;
                    }

                    @Override
                    protected List<IColumn<WebAuthnDeviceCredential, String>> getColumns() {
                        List<IColumn<WebAuthnDeviceCredential, String>> columns = new ArrayList<>();
                        columns.add(new PropertyColumn<>(
                                new ResourceModel("identifier"), "identifier", "identifier"));
                        columns.add(new PropertyColumn<>(
                                new ResourceModel("json"), "json", "json"));
                        return columns;
                    }
                }, pageRef)));
                authProfileModal.header(new Model<>(getString("webAuthnDeviceCredentials", model)));
                authProfileModal.show(true);
            }
        }, ActionLink.ActionType.HTML, AMEntitlement.AUTH_PROFILE_UPDATE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final AuthProfileTO ignore) {
                try {
                    restClient.delete(model.getObject().getKey());

                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (Exception e) {
                    LOG.error("While deleting {}", model.getObject().getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, AMEntitlement.AUTH_PROFILE_DELETE, true);

        return panel;
    }

    protected final class AuthProfileProvider extends DirectoryDataProvider<AuthProfileTO> {

        private static final long serialVersionUID = -185944053385660794L;

        private AuthProfileProvider(final int paginatorRows) {
            super(paginatorRows);
            setSort("owner", SortOrder.ASCENDING);
        }

        @Override
        public Iterator<AuthProfileTO> iterator(final long first, final long count) {
            int page = ((int) first / paginatorRows);
            return restClient.list((page < 0 ? 0 : page) + 1, paginatorRows).iterator();
        }

        @Override
        public long size() {
            return restClient.count();
        }

        @Override
        public IModel<AuthProfileTO> model(final AuthProfileTO object) {
            return new CompoundPropertyModel<>(object);
        }
    }

    private class CreateAuthProfileWizardBuilder extends AuthProfileWizardBuilder<AuthProfileTO> {

        private static final long serialVersionUID = -2478221092672979490L;

        private class NewAuthProfileStep extends AuthProfileWizardBuilder<AuthProfileTO>.Step {

            private static final long serialVersionUID = 6290450377240300418L;

            NewAuthProfileStep(final AuthProfileTO modelObject) {
                super(modelObject);

                AjaxTextFieldPanel owner = new AjaxTextFieldPanel(
                        "bean", "owner", new PropertyModel<>(modelObject, "owner"));
                owner.addRequiredLabel();
                addOrReplace(owner);
            }
        }

        CreateAuthProfileWizardBuilder(final PageReference pageRef) {
            super(new AuthProfileTO(), new StepModel<>(), pageRef);
        }

        @Override
        protected WizardModel buildModelSteps(final AuthProfileTO modelObject, final WizardModel wizardModel) {
            wizardModel.add(new NewAuthProfileStep(modelObject));
            return wizardModel;
        }

        @Override
        protected Serializable onApplyInternal(final AuthProfileTO modelObject) {
            restClient.create(modelObject);
            return modelObject;
        }
    }
}
