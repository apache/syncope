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

import java.util.Collections;
import java.util.List;
import org.apache.syncope.client.console.commons.status.StatusBean;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.patch.GroupPatch;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.model.util.ListModel;

public class GroupWizardBuilder extends AnyWizardBuilder<GroupTO> {

    private static final long serialVersionUID = 1L;

    private final GroupRestClient groupRestClient = new GroupRestClient();

    /**
     * Construct.
     *
     * @param id The component id
     * @param groupTO any
     * @param anyTypeClasses
     * @param pageRef Caller page reference.
     */
    public GroupWizardBuilder(
            final String id, final GroupTO groupTO, final List<String> anyTypeClasses, final PageReference pageRef) {
        super(id, groupTO, anyTypeClasses, pageRef);
    }

    @Override
    protected void onApplyInternal(final GroupTO modelObject) {
        final ProvisioningResult<GroupTO> actual;

        if (modelObject.getKey() == 0) {
            actual = groupRestClient.create(modelObject);
        } else {
            final GroupPatch patch = AnyOperations.diff(modelObject, getOriginalItem(), true);

            // update user just if it is changed
            if (!patch.isEmpty()) {
                actual = groupRestClient.update(getOriginalItem().getETagValue(), patch);
            }
        }
    }

    @Override
    protected GroupWizardBuilder addOptionalDetailsPanel(final GroupTO modelObject, final WizardModel wizardModel) {
        wizardModel.add(new GroupDetails(modelObject,
                new ListModel<>(Collections.<StatusBean>emptyList()), false, pageRef, modelObject.getKey() > 0));
        return this;
    }
}
