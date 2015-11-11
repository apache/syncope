/*
 * Copyright 2015 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.syncope.client.console.wizards.any;

import java.util.List;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.model.Model;

public class UserWizardBuilder extends AnyWizardBuilder<UserTO> {

    private static final long serialVersionUID = 1L;

    private final UserRestClient userRestClient = new UserRestClient();

    /**
     * Construct.
     *
     * @param id The component id
     * @param userTO any
     * @param anyTypeClasses
     * @param pageRef Caller page reference.
     */
    public UserWizardBuilder(
            final String id, final UserTO userTO, final List<String> anyTypeClasses, final PageReference pageRef) {
        super(id, userTO, anyTypeClasses, pageRef);
    }

    @Override
    protected WizardModel buildModelSteps(final UserTO modelObject, final WizardModel wizardModel) {
        wizardModel.add(new UserDetails(modelObject, false, false));
        return super.buildModelSteps(modelObject, wizardModel);
    }

    @Override
    protected void onApplyInternal(final UserTO modelObject) {
        Model<Boolean> storePassword = new Model<>(true);

        final ProvisioningResult<UserTO> actual;

        if (modelObject.getKey() == 0) {
            actual = userRestClient.create(modelObject, storePassword.getObject());
        } else {
            final UserPatch patch = AnyOperations.diff(modelObject, getOriginalItem(), true);

//            if (statusPanel != null) {
//                patch.setPwdPropRequest(statusPanel.getStatusMod());
//            }
            // update user just if it is changed
            if (!patch.isEmpty()) {
                actual = userRestClient.update(getOriginalItem().getETagValue(), patch);
            }
        }
    }
}
