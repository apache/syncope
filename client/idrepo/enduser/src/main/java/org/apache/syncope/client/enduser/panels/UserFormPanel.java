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
import org.apache.syncope.client.enduser.layout.UserFormLayoutInfo;
import org.apache.syncope.client.enduser.pages.BasePage;
import org.apache.syncope.client.enduser.pages.Dashboard;
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
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.IEventSink;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class UserFormPanel extends AnyFormPanel implements UserForm {

    private static final long serialVersionUID = 6763365006334514387L;

    private final UserSelfRestClient userSelfRestClient = new UserSelfRestClient();

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
            ProvisioningResult<UserTO> result;
            PageParameters parameters = new PageParameters();
            try {
                AnyWrapper<UserTO> updatedWrapper = form.getModelObject();
                UserTO userTO = updatedWrapper.getInnerObject();

                fixPlainAndVirAttrs(userTO, getOriginalItem().getInnerObject());
                UserUR req = AnyOperations.diff(userTO, getOriginalItem().getInnerObject(), false);

                // update just if it is changed
                if (req.isEmpty()) {
                    result = new ProvisioningResult<>();
                    result.setEntity(userTO);
                } else {
                    result = userSelfRestClient.update(getOriginalItem().getInnerObject().getETagValue(), req);
                    LOG.debug("User {} has been modified", result.getEntity().getUsername());
                }
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
            parameters.add(
                    EnduserConstants.LANDING_PAGE,
                    SyncopeWebApplication.get().getPageClass("profile", Dashboard.class).getName());
            setResponsePage(SelfResult.class, parameters);
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
