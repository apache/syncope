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

import java.util.List;

import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.patch.AnyObjectPatch;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.wicket.PageReference;

public class AnyObjectWizardBuilder extends AnyWizardBuilder<AnyObjectTO> implements Serializable {

    private static final long serialVersionUID = -2480279868319546243L;

    /**
     * Construct.
     *
     * @param id The component id
     * @param anyObjectTO any object TO.
     * @param anyTypeClasses any type classes
     * @param pageRef Caller page reference.
     */
    public AnyObjectWizardBuilder(
            final String id,
            final AnyObjectTO anyObjectTO,
            final List<String> anyTypeClasses,
            final PageReference pageRef) {
        super(id, anyObjectTO, anyTypeClasses, pageRef);
    }

    @Override
    protected Serializable onApplyInternal(final AnyHandler<AnyObjectTO> modelObject) {
        final AnyObjectTO inner = modelObject.getInnerObject();

        final ProvisioningResult<AnyObjectTO> actual;

        if (inner.getKey() == 0) {
            actual = anyTypeRestClient.create(AnyObjectTO.class.cast(inner));
        } else {
            final AnyObjectPatch patch = AnyOperations.diff(inner, getOriginalItem().getInnerObject(), false);

            // update user just if it is changed
            if (!patch.isEmpty()) {
                actual = anyTypeRestClient.update(getOriginalItem().getInnerObject().getETagValue(), patch);
            } else {
                actual = new ProvisioningResult<>();
                actual.setAny(inner);
            }
        }

        return actual;
    }
}
