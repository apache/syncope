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
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.common.util.CollectionUtils;
import org.apache.syncope.client.console.layout.GroupForm;
import org.apache.syncope.client.console.layout.GroupFormLayoutInfo;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizardBuilder;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.GroupUR;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.wicket.PageReference;

public class GroupWizardBuilder extends AnyWizardBuilder<GroupTO> implements GroupForm {

    private static final long serialVersionUID = 5945391813567245081L;

    protected GroupRestClient groupRestClient;

    public GroupWizardBuilder(
            final GroupTO groupTO,
            final List<String> anyTypeClasses,
            final GroupFormLayoutInfo formLayoutInfo,
            final GroupRestClient groupRestClient,
            final PageReference pageRef) {

        super(Optional.ofNullable(groupTO).map(GroupWrapper::new).
                orElse(null), anyTypeClasses, formLayoutInfo, pageRef);
        this.groupRestClient = groupRestClient;
    }

    /**
     * Constructor to be used for Remediation details only.
     *
     * @param previousGroupTO previous group status.
     * @param groupTO new group status to be approved.
     * @param anyTypeClasses any type classes.
     * @param formLayoutInfo from layout.
     * @param pageRef reference page.
     */
    public GroupWizardBuilder(
            final GroupTO previousGroupTO,
            final GroupTO groupTO,
            final List<String> anyTypeClasses,
            final GroupFormLayoutInfo formLayoutInfo,
            final PageReference pageRef) {

        super(new GroupWrapper(previousGroupTO, groupTO), anyTypeClasses, formLayoutInfo, pageRef);
    }

    /**
     * This method has been overridden to manage asynchronous translation of FIQL string to search classes list and
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
        GroupTO updated = modelObject instanceof GroupWrapper
                ? GroupWrapper.class.cast(modelObject).fillDynamicConditions()
                : modelObject.getInnerObject();

        ProvisioningResult<GroupTO> result;
        if (updated.getKey() == null) {
            GroupCR req = new GroupCR();
            EntityTOUtils.toAnyCR(updated, req);
            result = groupRestClient.create(req);
        } else {
            GroupTO original = getOriginalItem().getInnerObject();
            fixPlainAndVirAttrs(updated, original);

            // SYNCOPE-1170
            boolean othersNotEqualsOrBlanks =
                    !updated.getADynMembershipConds().equals(original.getADynMembershipConds())
                    || (StringUtils.isNotBlank(original.getUDynMembershipCond())
                    && StringUtils.isBlank(updated.getUDynMembershipCond()))
                    || (StringUtils.isBlank(original.getUDynMembershipCond())
                    && StringUtils.isNotBlank(updated.getUDynMembershipCond()))
                    || StringUtils.isAllBlank(original.getUDynMembershipCond(), updated.getUDynMembershipCond())
                    || !updated.getUDynMembershipCond().equals(original.getUDynMembershipCond())
                    || !CollectionUtils.diff(updated.getTypeExtensions(), original.getTypeExtensions()).isEmpty();

            GroupUR groupUR = AnyOperations.diff(updated, original, false);

            // update just if it is changed
            if (groupUR.isEmpty() && !othersNotEqualsOrBlanks) {
                result = new ProvisioningResult<>();
                result.setEntity(updated);
            } else {
                result = groupRestClient.update(original.getETagValue(), groupUR);
            }
        }

        return result;
    }

    @Override
    protected Optional<Details<GroupTO>> addOptionalDetailsPanel(final AnyWrapper<GroupTO> modelObject) {
        return Optional.of(new GroupDetails(
                GroupWrapper.class.cast(modelObject),
                mode == AjaxWizard.Mode.TEMPLATE,
                modelObject.getInnerObject().getKey() != null,
                pageRef));
    }
}
