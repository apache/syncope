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
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.layout.UserForm;
import org.apache.syncope.client.console.layout.UserFormLayoutInfo;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.patch.PasswordPatch;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.PageReference;

public class UserWizardBuilder extends AnyWizardBuilder<UserTO> implements UserForm {

    private static final long serialVersionUID = 6716803168859873877L;

    private final UserRestClient userRestClient = new UserRestClient();

    public UserWizardBuilder(
            final UserTO userTO,
            final List<String> anyTypeClasses,
            final UserFormLayoutInfo formLayoutInfo,
            final PageReference pageRef) {

        super(new UserWrapper(userTO), anyTypeClasses, formLayoutInfo, pageRef);
    }

    @Override
    protected Serializable onApplyInternal(final AnyWrapper<UserTO> modelObject) {
        UserTO inner = modelObject.getInnerObject();

        ProvisioningResult<UserTO> actual;
        if (inner.getKey() == null) {
            actual = userRestClient.create(inner, modelObject instanceof UserWrapper
                    ? UserWrapper.class.cast(modelObject).isStorePasswordInSyncope()
                    : StringUtils.isNotBlank(inner.getPassword()));
        } else {
            UserPatch patch = AnyOperations.diff(inner, getOriginalItem().getInnerObject(), false);

            if (StringUtils.isNotBlank(inner.getPassword())) {
                PasswordPatch passwordPatch = new PasswordPatch.Builder().
                        value(inner.getPassword()).onSyncope(true).resources(inner.getResources()).build();
                patch.setPassword(passwordPatch);
            }

            // update just if it is changed
            if (patch.isEmpty()) {
                actual = new ProvisioningResult<>();
                actual.setEntity(inner);
            } else {
                actual = userRestClient.update(getOriginalItem().getInnerObject().getETagValue(), patch);
            }
        }

        return actual;
    }

    @Override
    protected Details<UserTO> addOptionalDetailsPanel(final AnyWrapper<UserTO> modelObject) {

        return new UserDetails(
                UserWrapper.class.cast(modelObject), mode == AjaxWizard.Mode.TEMPLATE,
                modelObject.getInnerObject().getKey() != null,
                UserFormLayoutInfo.class.cast(formLayoutInfo).isPasswordManagement(),
                pageRef);
    }

    /**
     * Overrides default setItem() in order to clean statusModel as well.
     *
     * @param item item to be set.
     * @return the current wizard.
     */
    @Override
    public UserWizardBuilder setItem(final AnyWrapper<UserTO> item) {
        super.setItem(item == null ? null : new UserWrapper(item.getInnerObject()));
        return this;
    }
}
