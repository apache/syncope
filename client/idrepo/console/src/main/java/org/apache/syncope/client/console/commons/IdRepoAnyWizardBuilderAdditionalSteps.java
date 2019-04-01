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
package org.apache.syncope.client.console.commons;

import org.apache.syncope.client.ui.commons.layout.AbstractAnyFormLayout;
import org.apache.syncope.client.ui.commons.wizards.any.AnyForm;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.wicket.extensions.wizard.WizardModel;

public class IdRepoAnyWizardBuilderAdditionalSteps implements AnyWizardBuilderAdditionalSteps {

    private static final long serialVersionUID = -2693000486786910671L;

    @Override
    public <A extends AnyTO> WizardModel buildModelSteps(
            final AnyWrapper<A> modelObject,
            final WizardModel wizardModel,
            final AbstractAnyFormLayout<A, ? extends AnyForm<A>> formLayoutInfo) {

        return wizardModel;
    }
}
