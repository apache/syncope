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
package org.apache.syncope.client.enduser.panels;

import java.util.List;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.SyncopeWebApplication;
import org.apache.syncope.client.enduser.commons.EnduserConstants;
import org.apache.syncope.client.enduser.commons.ProvisioningUtils;
import org.apache.syncope.client.enduser.layout.UserFormLayoutInfo;
import org.apache.syncope.client.enduser.pages.BasePage;
import org.apache.syncope.client.enduser.pages.SelfResult;
import org.apache.syncope.client.enduser.panels.any.Details;
import org.apache.syncope.client.enduser.panels.any.UserDetails;
import org.apache.syncope.client.enduser.rest.UserSelfRestClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.layout.UserForm;
import org.apache.syncope.client.ui.commons.panels.WizardModalPanel;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.client.ui.commons.wizards.ModalPanelBuilder;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.client.ui.commons.wizards.any.UserWrapper;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.IEventSink;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class UserFormPanel extends AnyFormPanel implements UserForm {

    private static final long serialVersionUID = 6763365006334514387L;

    @SpringBean
    protected UserSelfRestClient userSelfRestClient;

    public UserFormPanel(
            final String id,
            final UserTO userTO,
            final List<String> anyTypeClasses,
            final UserFormLayoutInfo formLayoutInfo,
            final PageReference pageReference) {

        super(id, new UserWrapper(userTO), anyTypeClasses, formLayoutInfo, pageReference);

        UserWrapper modelObj = newModelObject();
        buildLayout(modelObj);
    }

    public UserFormPanel(
            final String id,
            final UserTO previousUserTO,
            final UserTO userTO,
            final List<String> anyTypeClasses,
            final UserFormLayoutInfo formLayoutInfo,
            final PageReference pageReference) {

        super(id, new UserWrapper(previousUserTO, userTO), anyTypeClasses, formLayoutInfo, pageReference);

        UserWrapper modelObj = newModelObject();
        setFormModel(modelObj);
        buildLayout(modelObj);
    }

    @Override
    protected Details<UserTO> addOptionalDetailsPanel(final UserWrapper modelObject) {
        return new UserDetails(
                EnduserConstants.CONTENT_PANEL,
                UserWrapper.class.cast(modelObject),
                pageRef);
    }

    @Override
    protected void onFormSubmit(final AjaxRequestTarget target) {
        // captcha check
        boolean checked = true;
        if (SyncopeWebApplication.get().isCaptchaEnabled()) {
            checked = captcha.check();
        }
        if (!checked) {
            SyncopeEnduserSession.get().error(getString(Constants.CAPTCHA_ERROR));
            ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
        } else {
            try {
                AnyWrapper<UserTO> updatedWrapper = form.getModelObject();
                UserTO userTO = updatedWrapper.getInnerObject();

                fixPlainAndVirAttrs(userTO, getOriginalItem().getInnerObject());
                // update and set page parameters according to provisioning result
                UserUR updateReq = AnyOperations.diff(userTO, getOriginalItem().getInnerObject(), false);
                ProvisioningResult<UserTO> provisioningResult = updateReq.isEmpty()
                        ? new ProvisioningResult<>()
                        : userSelfRestClient.update(getOriginalItem().getInnerObject().getETagValue(), updateReq);
                setResponsePage(new SelfResult(provisioningResult,
                        ProvisioningUtils.managePageParams(UserFormPanel.this, "profile.change",
                                !SyncopeWebApplication.get().isReportPropagationErrors()
                                || provisioningResult.getPropagationStatuses().stream()
                                        .allMatch(ps -> ExecStatus.SUCCESS == ps.getStatus()))));
            } catch (SyncopeClientException e) {
                LOG.error("While changing password for {}",
                        SyncopeEnduserSession.get().getSelfTO().getUsername(), e);
                SyncopeEnduserSession.get().onException(e);
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }
    }

    @Override
    public IEventSink getEventSink() {
        return null;
    }

    @Override
    public ModalPanelBuilder<AnyWrapper<UserTO>> setEventSink(final IEventSink eventSink) {
        return null;
    }

    @Override
    public ModalPanelBuilder<AnyWrapper<UserTO>> setItem(final AnyWrapper<UserTO> item) {
        return null;
    }

    @Override
    public AnyWrapper<UserTO> getDefaultItem() {
        return null;
    }

    @Override
    public WizardModalPanel<AnyWrapper<UserTO>> build(final String id, final int index, final AjaxWizard.Mode mode) {
        return null;
    }
}
