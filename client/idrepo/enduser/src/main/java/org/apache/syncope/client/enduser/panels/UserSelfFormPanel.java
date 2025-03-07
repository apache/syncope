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
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.SyncopeWebApplication;
import org.apache.syncope.client.enduser.commons.EnduserConstants;
import org.apache.syncope.client.enduser.commons.ProvisioningUtils;
import org.apache.syncope.client.enduser.layout.UserFormLayoutInfo;
import org.apache.syncope.client.enduser.pages.BasePage;
import org.apache.syncope.client.enduser.pages.SelfResult;
import org.apache.syncope.client.enduser.panels.any.Details;
import org.apache.syncope.client.enduser.panels.any.SelfUserDetails;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.client.ui.commons.wizards.any.UserWrapper;
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.SecurityQuestionTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.rest.api.service.SecurityQuestionService;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;

public class UserSelfFormPanel extends UserFormPanel {

    private static final long serialVersionUID = 6763365006334514387L;

    protected TextField<String> securityQuestion;

    protected String usernameText;

    public UserSelfFormPanel(
            final String id,
            final UserTO previousUserTO,
            final UserTO userTO,
            final List<String> anyTypeClasses,
            final UserFormLayoutInfo formLayoutInfo,
            final PageReference pageReference) {
        super(id, previousUserTO, userTO, anyTypeClasses, formLayoutInfo, pageReference);
    }

    @Override
    protected Details<UserTO> addOptionalDetailsPanel(final UserWrapper modelObject) {
        return new SelfUserDetails(
                EnduserConstants.CONTENT_PANEL,
                UserWrapper.class.cast(modelObject),
                UserFormLayoutInfo.class.cast(formLayoutInfo).isPasswordManagement(),
                pageRef);
    }

    @Override
    protected void onFormSubmit(final AjaxRequestTarget target) {
        // first check captcha
        if (SyncopeWebApplication.get().isCaptchaEnabled() && !captcha.check()) {
            SyncopeEnduserSession.get().error(getString(Constants.CAPTCHA_ERROR));
            ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
        } else {
            UserTO userTO = form.getModelObject().getInnerObject();
            try {
                // create and set page parameters according to provisioning result
                UserCR req = new UserCR();
                EntityTOUtils.toAnyCR(userTO, req);
                req.setStorePassword(form.getModelObject() instanceof UserWrapper
                        ? UserWrapper.class.cast(form.getModelObject()).isStorePasswordInSyncope()
                        : StringUtils.isNotBlank(userTO.getPassword()));
                // perform request and pass propagation statuses to SelfResult page
                ProvisioningResult<UserTO> provisioningResult = userSelfRestClient.create(req);
                setResponsePage(new SelfResult(provisioningResult,
                        ProvisioningUtils.managePageParams(UserSelfFormPanel.this, "profile.change",
                                !SyncopeWebApplication.get().isReportPropagationErrors()
                                || provisioningResult.getPropagationStatuses().stream()
                                        .allMatch(ps -> ExecStatus.SUCCESS == ps.getStatus()))));
            } catch (SyncopeClientException e) {
                LOG.error("While creating user {}", userTO.getUsername(), e);
                SyncopeEnduserSession.get().onException(e);
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }
    }

    protected void loadSecurityQuestion(final PageReference pageRef, final AjaxRequestTarget target) {
        try {
            SecurityQuestionTO securityQuestionTO = SyncopeEnduserSession.get().getService(
                    SecurityQuestionService.class).readByUser(usernameText);
            // set security question field model
            securityQuestion.setModel(Model.of(securityQuestionTO.getContent()));
            target.add(securityQuestion);
        } catch (Exception e) {
            LOG.error("Unable to get security question for [{}]", usernameText, e);
            SyncopeEnduserSession.get().onException(e);
            ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
        }
    }
}
