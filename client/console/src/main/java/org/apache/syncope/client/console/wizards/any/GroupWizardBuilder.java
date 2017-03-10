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
import org.apache.syncope.client.console.layout.GroupForm;
import org.apache.syncope.client.console.layout.GroupFormLayoutInfo;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.client.console.wizards.AjaxWizardBuilder;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.patch.GroupPatch;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.wicket.PageReference;

public class GroupWizardBuilder extends AnyWizardBuilder<GroupTO> implements GroupForm {

    private static final long serialVersionUID = 5945391813567245081L;

    private final GroupRestClient groupRestClient = new GroupRestClient();

    public GroupWizardBuilder(
            final GroupTO groupTO,
            final List<String> anyTypeClasses,
            final GroupFormLayoutInfo formLayoutInfo,
            final PageReference pageRef) {

        super(groupTO == null ? null : new GroupWrapper(groupTO), anyTypeClasses, formLayoutInfo, pageRef);
    }

    /**
     * This method has been overridden to manage asynchronous translation of FIQL string to search clases list and
     * viceversa.
     *
     * @param item wizard backend item.
     * @return the current builder.
     */
    @Override
    public AjaxWizardBuilder<AnyWrapper<GroupTO>> setItem(final AnyWrapper<GroupTO> item) {
        return (AjaxWizardBuilder<AnyWrapper<GroupTO>>) (item != null
                ? super.setItem(new GroupWrapper(item.getInnerObject()))
                : super.setItem(null));
    }

    @Override
    protected Serializable onApplyInternal(final AnyWrapper<GroupTO> modelObject) {
        GroupTO inner = modelObject instanceof GroupWrapper
                ? GroupWrapper.class.cast(modelObject).fillDynamicConditions()
                : modelObject.getInnerObject();

        ProvisioningResult<GroupTO> actual;
        if (inner.getKey() == null) {
            actual = groupRestClient.create(inner);
        } else {
            GroupPatch patch = AnyOperations.diff(inner, getOriginalItem().getInnerObject(), false);
            // update just if it is changed
            if (patch.isEmpty()) {
                actual = new ProvisioningResult<>();
                actual.setEntity(inner);
            } else {
                actual = groupRestClient.update(getOriginalItem().getInnerObject().getETagValue(), patch);
            }
        }

        return actual;
    }

    @Override
    protected Details<GroupTO> addOptionalDetailsPanel(final AnyWrapper<GroupTO> modelObject) {
        return new GroupDetails(
                GroupWrapper.class.cast(modelObject),
                mode == AjaxWizard.Mode.TEMPLATE,
                modelObject.getInnerObject().getKey() != null, pageRef);
    }
}
