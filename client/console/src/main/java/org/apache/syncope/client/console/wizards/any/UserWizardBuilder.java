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
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.commons.status.StatusBean;
import org.apache.syncope.client.console.commons.status.StatusUtils;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;

public class UserWizardBuilder extends AnyWizardBuilder<UserTO> {

    private static final long serialVersionUID = 6716803168859873877L;

    private final UserRestClient userRestClient = new UserRestClient();

    private final IModel<List<StatusBean>> statusModel;

    /**
     * Construct.
     *
     * @param userTO any
     * @param anyTypeClasses any type classes
     * @param pageRef Caller page reference.
     */
    public UserWizardBuilder(
            final UserTO userTO,
            final List<String> anyTypeClasses,
            final PageReference pageRef) {
        super(userTO, anyTypeClasses, pageRef);
        statusModel = new ListModel<>(new ArrayList<StatusBean>());
    }

    @Override
    protected Serializable onApplyInternal(final AnyHandler<UserTO> modelObject) {
        final ProvisioningResult<UserTO> actual;

        final UserTO inner = modelObject.getInnerObject();

        if (inner.getKey() == null) {
            actual = userRestClient.create(inner, StringUtils.isNotBlank(inner.getPassword()));
        } else {
            final UserPatch patch = AnyOperations.diff(inner, getOriginalItem().getInnerObject(), false);
            if (!statusModel.getObject().isEmpty()) {
                patch.setPassword(StatusUtils.buildPasswordPatch(inner.getPassword(), statusModel.getObject()));
            }

            // update user just if it is changed
            if (!patch.isEmpty()) {
                actual = userRestClient.update(getOriginalItem().getInnerObject().getETagValue(), patch);
            } else {
                actual = new ProvisioningResult<>();
                actual.setAny(inner);
            }
        }

        return actual;
    }

    @Override
    protected UserWizardBuilder addOptionalDetailsPanel(
            final AnyHandler<UserTO> modelObject, final WizardModel wizardModel) {

        wizardModel.add(new UserDetails(
                modelObject, statusModel, false, false, pageRef,
                modelObject.getInnerObject().getKey() != null));
        return this;
    }

    /**
     * Overrides default setItem() in order to clean statusModel as well.
     *
     * @param item item to be set.
     * @return the current wizard.
     */
    @Override
    public UserWizardBuilder setItem(final AnyHandler<UserTO> item) {
        super.setItem(item);
        statusModel.getObject().clear();
        return this;
    }
}
