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

import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.enduser.SyncopeWebApplication;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.layout.UserFormLayoutInfo;
import org.apache.syncope.client.enduser.pages.BasePage;
import org.apache.syncope.client.enduser.pages.Login;
import org.apache.syncope.client.enduser.pages.SelfResult;
import org.apache.syncope.client.enduser.panels.any.Details;
import org.apache.syncope.client.enduser.panels.any.SelfUserDetails;
import org.apache.syncope.client.enduser.rest.UserSelfRestClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.client.ui.commons.wizards.any.UserWrapper;
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.SecurityQuestionTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.service.SecurityQuestionService;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import java.util.List;
import org.apache.syncope.client.enduser.commons.EnduserConstants;

public class UserSelfFormPanel extends UserFormPanel {

    private static final long serialVersionUID = 6763365006334514387L;

    private final UserSelfRestClient userSelfRestClient = new UserSelfRestClient();

    private TextField<String> securityQuestion;

    private String usernameText;

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
        // captcha check
        boolean checked = true;
        if (SyncopeWebApplication.get().isCaptchaEnabled()) {
            checked = captcha.check();
        }
        if (!checked) {
            SyncopeEnduserSession.get().error(getString(Constants.CAPTCHA_ERROR));
            ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
        } else {
            ProvisioningResult<UserTO> result;
            PageParameters parameters = new PageParameters();
            try {
                AnyWrapper<UserTO> updatedWarapper = form.getModelObject();
                UserTO userTO = updatedWarapper.getInnerObject();

                UserCR req = new UserCR();
                EntityTOUtils.toAnyCR(userTO, req);
                req.setStorePassword(updatedWarapper instanceof UserWrapper
                        ? UserWrapper.class.cast(updatedWarapper).isStorePasswordInSyncope()
                        : StringUtils.isNotBlank(userTO.getPassword()));

                result = userSelfRestClient.create(req);
                LOG.debug("User {} has been created", result.getEntity().getUsername());

                parameters.add(EnduserConstants.STATUS, Constants.OPERATION_SUCCEEDED);
                parameters.add(Constants.NOTIFICATION_TITLE_PARAM, getString("self.profile.change.success"));
                parameters.add(Constants.NOTIFICATION_MSG_PARAM, getString("self.profile.change.success.msg"));
            } catch (SyncopeClientException sce) {
                parameters.add(EnduserConstants.STATUS, Constants.ERROR);
                parameters.add(Constants.NOTIFICATION_TITLE_PARAM, getString("self.profile.change.error"));
                parameters.add(Constants.NOTIFICATION_MSG_PARAM, getString("self.profile.change.error.msg"));
                SyncopeEnduserSession.get().onException(sce);
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
            parameters.add(EnduserConstants.LANDING_PAGE, Login.class);
            setResponsePage(SelfResult.class, parameters);
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
