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
package org.apache.syncope.client.console.wizards.any;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.layout.LinkedAccountForm;
import org.apache.syncope.client.console.layout.LinkedAccountFormLayoutInfo;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal.ModalEvent;
import org.apache.syncope.client.console.wizards.BaseAjaxWizardBuilder;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.client.ui.commons.wizards.any.EntityWrapper;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.request.LinkedAccountUR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.LinkedAccountTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.model.IModel;

/**
 * Accounts wizard builder.
 */
public class LinkedAccountWizardBuilder extends BaseAjaxWizardBuilder<LinkedAccountTO> implements LinkedAccountForm {

    private static final long serialVersionUID = -9142332740863374891L;

    protected final IModel<UserTO> model;

    protected LinkedAccountFormLayoutInfo formLayoutInfo;

    protected final UserRestClient userRestClient;

    protected final AnyTypeRestClient anyTypeRestClient;

    public LinkedAccountWizardBuilder(
            final IModel<UserTO> model,
            final LinkedAccountFormLayoutInfo formLayoutInfo,
            final UserRestClient userRestClient,
            final AnyTypeRestClient anyTypeRestClient,
            final PageReference pageRef) {

        super(new LinkedAccountTO(), pageRef);

        this.model = model;
        this.formLayoutInfo = formLayoutInfo;
        this.userRestClient = userRestClient;
        this.anyTypeRestClient = anyTypeRestClient;
    }

    @Override
    public AjaxWizard<LinkedAccountTO> build(final String id, final AjaxWizard.Mode mode) {
        return super.build(id, mode);
    }

    @Override
    protected WizardModel buildModelSteps(final LinkedAccountTO modelObject, final WizardModel wizardModel) {
        wizardModel.add(new LinkedAccountDetailsPanel(modelObject));
        if (formLayoutInfo.isCredentials()) {
            wizardModel.add(new LinkedAccountCredentialsPanel(
                    new EntityWrapper<>(modelObject), formLayoutInfo.getWhichCredentials()));
        }

        if (formLayoutInfo.isPlainAttrs()) {
            wizardModel.add(new LinkedAccountPlainAttrsPanel(
                    new EntityWrapper<>(modelObject),
                    model.getObject(),
                    anyTypeRestClient.read(model.getObject().getType()).getClasses(),
                    formLayoutInfo.getWhichPlainAttrs()));
        }

        return wizardModel;
    }

    @Override
    protected Serializable onApplyInternal(final LinkedAccountTO modelObject) {
        fixPlainAttrs(modelObject);

        LinkedAccountUR linkedAccountPatch = new LinkedAccountUR.Builder().linkedAccountTO(modelObject).build();
        linkedAccountPatch.setLinkedAccountTO(modelObject);
        UserUR req = new UserUR();
        req.setKey(model.getObject().getKey());
        req.getLinkedAccounts().add(linkedAccountPatch);
        model.setObject(userRestClient.update(model.getObject().getETagValue(), req).getEntity());

        return modelObject;
    }

    private void fixPlainAttrs(final LinkedAccountTO linkedAccountTO) {
        Set<Attr> validAttrs = new HashSet<>(linkedAccountTO.getPlainAttrs().stream().
                filter(attr -> !attr.getValues().isEmpty()).
                collect(Collectors.toSet()));
        linkedAccountTO.getPlainAttrs().clear();
        linkedAccountTO.getPlainAttrs().addAll(validAttrs);
    }

    @Override
    protected Serializable getCreateCustomPayloadEvent(final Serializable afterObject, final AjaxRequestTarget target) {
        LinkedAccountTO linkedAccountTO = LinkedAccountTO.class.cast(afterObject);
        return new CreateEvent(
                linkedAccountTO.getConnObjectKeyValue(),
                model.getObject(),
                target);
    }

    private static class CreateEvent extends ModalEvent {

        private static final long serialVersionUID = 6416834092156281986L;

        private final String key;

        private final UserTO userTO;

        CreateEvent(
                final String key,
                final UserTO userTO,
                final AjaxRequestTarget target) {

            super(target);
            this.key = key;
            this.userTO = userTO;
        }

        public String getKey() {
            return key;
        }

        public UserTO getUserTO() {
            return userTO;
        }
    }
}
