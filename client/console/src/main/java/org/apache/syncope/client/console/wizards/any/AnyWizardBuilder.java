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
import org.apache.syncope.client.console.commons.Mode;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.wizards.AjaxWizardBuilder;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.patch.AnyObjectPatch;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel;

public class AnyWizardBuilder<T extends AnyTO> extends AjaxWizardBuilder<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final AnyTypeRestClient anyTypeRestClient = new AnyTypeRestClient();

    protected final List<String> anyTypeClasses;

    /**
     * Construct.
     *
     * @param id The component id
     * @param anyTO any
     * @param anyTypeClasses
     * @param pageRef Caller page reference.
     */
    public AnyWizardBuilder(
            final String id, final T anyTO, final List<String> anyTypeClasses, final PageReference pageRef) {
        super(id, anyTO, pageRef);
        this.anyTypeClasses = anyTypeClasses;
    }

    @Override
    protected WizardModel buildModelSteps(final T modelObject, final WizardModel wizardModel) {
        wizardModel.add(new PlainAttrs(modelObject, null, Mode.ADMIN, anyTypeClasses.toArray(new String[] {})));
        wizardModel.add(new DerAttrs(modelObject, anyTypeClasses.toArray(new String[] {})));
        wizardModel.add(new VirAttrs(modelObject, anyTypeClasses.toArray(new String[] {})));
        return wizardModel;
    }

    @Override
    protected void onCancelInternal(final T modelObject) {
        // do nothing
    }

    @Override
    protected void onApplyInternal(final T modelObject) {
        if (!(modelObject instanceof AnyObjectTO)) {
            throw new IllegalArgumentException();
        }

        final ProvisioningResult<AnyObjectTO> actual;

        if (modelObject.getKey() == 0) {
            actual = anyTypeRestClient.create(AnyObjectTO.class.cast(modelObject));
        } else {
            final AnyObjectPatch patch = AnyOperations.diff(modelObject, getOriginalItem(), true);

            // update user just if it is changed
            if (!patch.isEmpty()) {
                actual = anyTypeRestClient.update(getOriginalItem().getETagValue(), patch);
            }
        }
    }
}
