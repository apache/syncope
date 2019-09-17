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
package org.apache.syncope.client.enduser.wizards.any;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.ui.commons.layout.UserForm;
import org.apache.syncope.client.enduser.layout.UserFormLayoutInfo;
import org.apache.syncope.client.enduser.rest.UserSelfRestClient;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.client.ui.commons.wizards.any.UserWrapper;
import org.apache.syncope.client.ui.commons.wizards.exception.CaptchaNotMatchingException;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.request.PasswordPatch;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.PageReference;

public class UserWizardBuilder extends AnyWizardBuilder implements UserForm {

    private static final long serialVersionUID = 6716803168859873877L;

    protected final UserSelfRestClient userSelfRestClient = new UserSelfRestClient();

    /**
     * Constructor to be used for templating only.
     *
     * @param anyTypeClasses any type classes.
     * @param formLayoutInfo form layout.
     * @param pageRef reference page.
     */
    public UserWizardBuilder(
            final List<String> anyTypeClasses,
            final UserFormLayoutInfo formLayoutInfo,
            final PageReference pageRef) {

        super(new UserWrapper(null), anyTypeClasses, formLayoutInfo, pageRef);
    }

    /**
     * Constructor to be used for Approval and Remediation details only.
     *
     * @param previousUserTO previous user status.
     * @param userTO new user status to be approved.
     * @param anyTypeClasses any type classes.
     * @param formLayoutInfo from layout.
     * @param pageRef reference page.
     */
    public UserWizardBuilder(
            final UserTO previousUserTO,
            final UserTO userTO,
            final List<String> anyTypeClasses,
            final UserFormLayoutInfo formLayoutInfo,
            final PageReference pageRef) {

        super(new UserWrapper(previousUserTO, userTO), anyTypeClasses, formLayoutInfo, pageRef);
    }

    @Override
    protected Serializable onApplyInternal(final AnyWrapper<UserTO> modelObject) {
        // captcha check
        if (captcha != null && captcha.evaluate() && !captcha.captchaCheck()) {
            throw new CaptchaNotMatchingException();
        }
        UserTO inner = modelObject.getInnerObject();

        ProvisioningResult<UserTO> result;
        if (inner.getKey() == null) {
            UserCR req = new UserCR();
            EntityTOUtils.toAnyCR(inner, req);
            req.setStorePassword(modelObject instanceof UserWrapper
                    ? UserWrapper.class.cast(modelObject).isStorePasswordInSyncope()
                    : StringUtils.isNotBlank(inner.getPassword()));

            result = UserSelfRestClient.create(req);
        } else {
            fixPlainAndVirAttrs(inner, getOriginalItem().getInnerObject());
            UserUR userUR = AnyOperations.diff(inner, getOriginalItem().getInnerObject(), false);

            if (StringUtils.isNotBlank(inner.getPassword())) {
                PasswordPatch passwordPatch = new PasswordPatch.Builder().
                        value(inner.getPassword()).onSyncope(true).resources(inner.getResources()).build();
                userUR.setPassword(passwordPatch);
            }

            // update just if it is changed
            if (userUR.isEmpty()) {
                result = new ProvisioningResult<>();
                result.setEntity(inner);
            } else {
                result = userSelfRestClient.update(getOriginalItem().getInnerObject().getETagValue(), userUR);
            }
        }

        return result;
    }

    /**
     * Overrides default setItem() in order to clean statusModel as well.
     *
     * @param item item to be set.
     * @return the current wizard.
     */
    @Override
    public UserWizardBuilder setItem(final AnyWrapper<UserTO> item) {
        super.setItem(Optional.ofNullable(item)
            .map(userTOAnyWrapper -> new UserWrapper(userTOAnyWrapper.getInnerObject())).orElse(null));
        return this;
    }

}
