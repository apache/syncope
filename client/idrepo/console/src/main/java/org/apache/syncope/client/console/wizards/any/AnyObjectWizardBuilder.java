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
import org.apache.syncope.client.console.layout.AnyObjectForm;
import org.apache.syncope.client.console.layout.AnyObjectFormLayoutInfo;
import org.apache.syncope.client.console.rest.AnyObjectRestClient;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.wicket.PageReference;

public class AnyObjectWizardBuilder extends AnyWizardBuilder<AnyObjectTO> implements AnyObjectForm {

    private static final long serialVersionUID = -2480279868319546243L;

    protected AnyObjectRestClient anyObjectRestClient;

    public AnyObjectWizardBuilder(
            final AnyObjectTO anyObjectTO,
            final List<String> anyTypeClasses,
            final AnyObjectFormLayoutInfo formLayoutInfo,
            final AnyObjectRestClient anyObjectRestClient,
            final PageReference pageRef) {

        super(Optional.ofNullable(anyObjectTO).map(AnyObjectWrapper::new).
                orElse(null), anyTypeClasses, formLayoutInfo, pageRef);
        this.anyObjectRestClient = anyObjectRestClient;
    }

    /**
     * Constructor to be used for Remediation details only.
     *
     * @param previousAnyObjectTO previous anyObject status.
     * @param anyObjectTO new anyObject status to be approved.
     * @param anyTypeClasses any type classes.
     * @param formLayoutInfo from layout.
     * @param pageRef reference page.
     */
    public AnyObjectWizardBuilder(
            final AnyObjectTO previousAnyObjectTO,
            final AnyObjectTO anyObjectTO,
            final List<String> anyTypeClasses,
            final AnyObjectFormLayoutInfo formLayoutInfo,
            final PageReference pageRef) {

        super(new AnyObjectWrapper(previousAnyObjectTO, anyObjectTO), anyTypeClasses, formLayoutInfo, pageRef);
    }

    @Override
    protected Serializable onApplyInternal(final AnyWrapper<AnyObjectTO> modelObject) {
        final AnyObjectTO inner = modelObject.getInnerObject();

        ProvisioningResult<AnyObjectTO> result;
        if (inner.getKey() == null) {
            AnyObjectCR req = new AnyObjectCR();
            EntityTOUtils.toAnyCR(inner, req);

            result = anyObjectRestClient.create(req);
        } else {
            fixPlainAndVirAttrs(inner, getOriginalItem().getInnerObject());
            AnyObjectUR req = AnyOperations.diff(inner, getOriginalItem().getInnerObject(), false);

            // update just if it is changed
            if (req.isEmpty()) {
                result = new ProvisioningResult<>();
                result.setEntity(inner);
            } else {
                result = anyObjectRestClient.update(getOriginalItem().getInnerObject().getETagValue(), req);
            }
        }

        return result;
    }

    @Override
    protected Optional<Details<AnyObjectTO>> addOptionalDetailsPanel(final AnyWrapper<AnyObjectTO> modelObject) {
        return Optional.of(new AnyObjectDetails(
                modelObject,
                mode == AjaxWizard.Mode.TEMPLATE,
                modelObject.getInnerObject().getKey() != null,
                pageRef));
    }
}
