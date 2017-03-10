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
import org.apache.syncope.client.console.layout.AnyObjectForm;
import org.apache.syncope.client.console.layout.AnyObjectFormLayoutInfo;
import org.apache.syncope.client.console.rest.AnyObjectRestClient;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.patch.AnyObjectPatch;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.wicket.PageReference;

public class AnyObjectWizardBuilder extends AnyWizardBuilder<AnyObjectTO> implements AnyObjectForm {

    private static final long serialVersionUID = -2480279868319546243L;

    private final AnyObjectRestClient anyObjectRestClient = new AnyObjectRestClient();

    public AnyObjectWizardBuilder(
            final AnyObjectTO anyObjectTO,
            final List<String> anyTypeClasses,
            final AnyObjectFormLayoutInfo formLayoutInfo,
            final PageReference pageRef) {

        super(anyObjectTO, anyTypeClasses, formLayoutInfo, pageRef);
    }

    @Override
    protected Serializable onApplyInternal(final AnyWrapper<AnyObjectTO> modelObject) {
        final AnyObjectTO inner = modelObject.getInnerObject();

        ProvisioningResult<AnyObjectTO> actual;
        if (inner.getKey() == null) {
            actual = anyObjectRestClient.create(inner);
        } else {
            AnyObjectPatch patch = AnyOperations.diff(inner, getOriginalItem().getInnerObject(), false);

            // update just if it is changed
            if (patch.isEmpty()) {
                actual = new ProvisioningResult<>();
                actual.setEntity(inner);
            } else {
                actual = anyObjectRestClient.update(getOriginalItem().getInnerObject().getETagValue(), patch);
            }
        }

        return actual;
    }

    @Override
    protected Details<AnyObjectTO> addOptionalDetailsPanel(final AnyWrapper<AnyObjectTO> modelObject) {
        return new AnyObjectDetails(
                modelObject,
                mode == AjaxWizard.Mode.TEMPLATE,
                modelObject.getInnerObject().getKey() != null, pageRef);
    }
}
