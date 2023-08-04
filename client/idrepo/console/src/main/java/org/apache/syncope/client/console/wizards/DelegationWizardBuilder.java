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
package org.apache.syncope.client.console.wizards;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.rest.DelegationRestClient;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.DateOps;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDateTimeFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.to.DelegationTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;

public class DelegationWizardBuilder extends BaseAjaxWizardBuilder<DelegationTO> {

    private static final long serialVersionUID = 16656970898539L;

    protected final UserRestClient userRestClient;

    protected final DelegationRestClient delegationRestClient;

    public DelegationWizardBuilder(
            final DelegationTO defaultItem,
            final UserRestClient userRestClient,
            final DelegationRestClient delegationRestClient,
            final PageReference pageRef) {

        super(defaultItem, pageRef);

        this.userRestClient = userRestClient;
        this.delegationRestClient = delegationRestClient;
    }

    @Override
    protected Serializable onApplyInternal(final DelegationTO modelObject) {
        if (getOriginalItem() == null || StringUtils.isBlank(getOriginalItem().getKey())) {
            delegationRestClient.create(modelObject);
        } else {
            delegationRestClient.update(modelObject);
        }
        return null;
    }

    @Override
    protected WizardModel buildModelSteps(final DelegationTO modelObject, final WizardModel wizardModel) {
        if (getOriginalItem() == null || StringUtils.isBlank(getOriginalItem().getKey())
                && SyncopeConsoleSession.get().owns(IdRepoEntitlement.USER_SEARCH)
                && SyncopeConsoleSession.get().owns(IdRepoEntitlement.DELEGATION_CREATE)) {

            wizardModel.add(new UserSelectionWizardStep(
                    new ResourceModel("delegating"), new PropertyModel<>(modelObject, "delegating"), pageRef));
            wizardModel.add(new UserSelectionWizardStep(
                    new ResourceModel("delegated"), new PropertyModel<>(modelObject, "delegated"), pageRef));
        } else {
            wizardModel.add(new Users(modelObject));
        }

        wizardModel.add(new StartEnd(modelObject));
        wizardModel.add(new Roles(modelObject));

        return wizardModel;
    }

    private class Users extends WizardStep {

        private static final long serialVersionUID = 33859341441696L;

        Users(final DelegationTO modelObject) {
            super();

            setTitleModel(new ResourceModel("users"));

            IModel<String> delegating = new PropertyModel<>(modelObject, "delegating");
            IModel<String> delegated = new PropertyModel<>(modelObject, "delegated");

            boolean isNew = getOriginalItem() == null || StringUtils.isBlank(getOriginalItem().getKey());
            if (!isNew) {
                if (SyncopeConsoleSession.get().owns(IdRepoEntitlement.USER_READ)) {
                    delegating = Model.of(userRestClient.read(delegating.getObject()).getUsername());
                    delegated = Model.of(userRestClient.read(delegated.getObject()).getUsername());
                } else {
                    if (SyncopeConsoleSession.get().getSelfTO().getKey().equals(delegating.getObject())) {
                        delegating = Model.of(SyncopeConsoleSession.get().getSelfTO().getUsername());
                    }
                    if (SyncopeConsoleSession.get().getSelfTO().getKey().equals(delegated.getObject())) {
                        delegated = Model.of(SyncopeConsoleSession.get().getSelfTO().getUsername());
                    }
                }
            }

            boolean isSelfOnly = !SyncopeConsoleSession.get().owns(IdRepoEntitlement.DELEGATION_CREATE);
            if (isSelfOnly) {
                modelObject.setDelegating(SyncopeConsoleSession.get().getSelfTO().getUsername());
            }

            add(new AjaxTextFieldPanel(
                    "delegating",
                    "delegating",
                    delegating,
                    false).addRequiredLabel().
                    setEnabled(isNew && !isSelfOnly));
            add(new AjaxTextFieldPanel(
                    "delegated",
                    "delegated",
                    delegated,
                    false).addRequiredLabel().
                    setEnabled(isNew));
        }
    }

    private static class StartEnd extends WizardStep {

        private static final long serialVersionUID = 16957451737824L;

        StartEnd(final DelegationTO modelObject) {
            super();

            setTitleModel(new ResourceModel("validity"));

            add(new AjaxDateTimeFieldPanel(
                    "start",
                    "start",
                    DateOps.WrappedDateModel.ofOffset(new PropertyModel<>(modelObject, "start")),
                    DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT).
                    addRequiredLabel());

            add(new AjaxDateTimeFieldPanel(
                    "end",
                    "end",
                    DateOps.WrappedDateModel.ofOffset(new PropertyModel<>(modelObject, "end")),
                    DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT));
        }
    }

    private class Roles extends WizardStep implements WizardModel.ICondition {

        private static final long serialVersionUID = 16957451737824L;

        private final List<String> allRoles = new ArrayList<>();

        private final DelegationTO modelObject;

        Roles(final DelegationTO modelObject) {
            super();
            this.modelObject = modelObject;

            setTitleModel(new ResourceModel("roles"));

            add(new AjaxPalettePanel.Builder<String>().
                    withFilter().
                    setAllowOrder(true).
                    build("roles",
                            new PropertyModel<>(modelObject, "roles"),
                            new AjaxPalettePanel.Builder.Query<>() {

                        private static final long serialVersionUID = 3900199363626636719L;

                        @Override
                        public List<String> execute(final String filter) {
                            if (StringUtils.isEmpty(filter) || "*".equals(filter)) {
                                return allRoles.size() > Constants.MAX_ROLE_LIST_SIZE
                                        ? allRoles.subList(0, Constants.MAX_ROLE_LIST_SIZE)
                                        : allRoles;

                            }
                            return allRoles.stream().
                                    filter(role -> StringUtils.containsIgnoreCase(role, filter)).
                                    collect(Collectors.toList());
                        }
                    }).
                    hideLabel().
                    setOutputMarkupId(true));
        }

        @Override
        public boolean evaluate() {
            if (modelObject.getDelegating() != null) {
                allRoles.clear();

                if (SyncopeConsoleSession.get().owns(IdRepoEntitlement.USER_READ)) {
                    allRoles.addAll(userRestClient.read(modelObject.getDelegating()).getRoles());
                } else if (SyncopeConsoleSession.get().getSelfTO().getKey().equals(modelObject.getDelegating())) {
                    allRoles.addAll(SyncopeConsoleSession.get().getSelfTO().getRoles());
                }
            }
            return true;
        }
    }
}
