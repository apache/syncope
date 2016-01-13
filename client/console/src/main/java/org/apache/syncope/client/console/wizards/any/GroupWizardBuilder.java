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

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import org.apache.syncope.client.console.commons.status.StatusBean;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.console.wizards.AjaxWizardBuilder;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.patch.GroupPatch;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.model.util.ListModel;

public class GroupWizardBuilder extends AnyWizardBuilder<GroupTO> {

    private static final long serialVersionUID = 5945391813567245081L;

    private final GroupRestClient groupRestClient = new GroupRestClient();

    /**
     * Construct.
     *
     * @param id The component id
     * @param groupTO any
     * @param anyTypeClasses any type classes
     * @param pageRef Caller page reference.
     */
    public GroupWizardBuilder(
            final String id, final GroupTO groupTO, final List<String> anyTypeClasses, final PageReference pageRef) {
        super(id, new GroupHandler(groupTO), anyTypeClasses, pageRef);
    }

    /**
     * This method has been overridden to manage asynchronous translation of FIQL string to search clases list and
     * viceversa.
     *
     * @param item wizard backend item.
     * @return the current builder.
     */
    @Override
    public AjaxWizardBuilder<AnyHandler<GroupTO>> setItem(final AnyHandler<GroupTO> item) {
        return (AjaxWizardBuilder<AnyHandler<GroupTO>>) (item == null
                ? super.setItem(item)
                : super.setItem(new GroupHandler(item.getInnerObject())));
    }

    @Override
    protected Serializable onApplyInternal(final AnyHandler<GroupTO> modelObject) {
        final ProvisioningResult<GroupTO> actual;

        GroupTO toBeProcessed = modelObject instanceof GroupHandler
                ? GroupHandler.class.cast(modelObject).fillDynamicConditions()
                : modelObject.getInnerObject();

        if (toBeProcessed.getKey() == 0) {
            actual = groupRestClient.create(toBeProcessed);
        } else {
            final GroupPatch patch = AnyOperations.diff(toBeProcessed, getOriginalItem().getInnerObject(), false);
            // update user just if it is changed
            if (!patch.isEmpty()) {
                actual = groupRestClient.update(getOriginalItem().getInnerObject().getETagValue(), patch);
            } else {
                actual = new ProvisioningResult<>();
                actual.setAny(toBeProcessed);
            }
        }

        return actual;
    }

    @Override
    protected GroupWizardBuilder addOptionalDetailsPanel(
            final AnyHandler<GroupTO> modelObject, final WizardModel wizardModel) {
        wizardModel.add(new GroupDetails(
                GroupHandler.class.cast(modelObject),
                new ListModel<>(Collections.<StatusBean>emptyList()),
                false, pageRef, modelObject.getInnerObject().getKey() > 0));
        return this;
    }
}
