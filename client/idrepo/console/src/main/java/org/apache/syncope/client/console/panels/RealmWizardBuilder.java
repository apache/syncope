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
package org.apache.syncope.client.console.panels;

import java.io.Serializable;
import org.apache.syncope.client.console.rest.RealmRestClient;
import org.apache.syncope.client.console.wizards.BaseAjaxWizardBuilder;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;

public class RealmWizardBuilder extends BaseAjaxWizardBuilder<RealmTO> {

    private static final long serialVersionUID = 5945391813567245081L;

    protected final RealmRestClient realmRestClient;

    protected String parentPath;

    public RealmWizardBuilder(final RealmRestClient realmRestClient, final PageReference pageRef) {
        super(new RealmTO(), pageRef);
        this.realmRestClient = realmRestClient;
    }

    @Override
    protected Serializable onApplyInternal(final RealmTO modelObject) {
        ProvisioningResult<RealmTO> result;
        if (modelObject.getKey() == null) {
            result = realmRestClient.create(this.parentPath, modelObject);
        } else {
            result = realmRestClient.update(modelObject);
        }
        return result;
    }

    @Override
    protected WizardModel buildModelSteps(final RealmTO modelObject, final WizardModel wizardModel) {
        wizardModel.add(new Realm(modelObject));
        return wizardModel;
    }

    public static class Realm extends WizardStep {

        private static final long serialVersionUID = -2123790676338327104L;

        public Realm(final RealmTO modelObject) {
            RealmDetails realmDetail = new RealmDetails("details", modelObject);
            realmDetail.add(new AttributeAppender("style", "overflow-x:hidden;"));
            add(realmDetail);
        }
    }

    public void setParentPath(final String parentPath) {
        this.parentPath = parentPath;
    }
}
