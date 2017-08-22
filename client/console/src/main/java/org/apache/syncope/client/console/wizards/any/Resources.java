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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleApplication;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.wicket.ajax.markup.html.LabelInfo;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.ActionPermissions;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

public class Resources extends WizardStep implements WizardModel.ICondition {

    private static final long serialVersionUID = 552437609667518888L;

    private final ListModel<String> available;

    public <T extends AnyTO> Resources(final AnyWrapper<T> modelObject) {
        final T entityTO = modelObject.getInnerObject();

        if (modelObject instanceof UserWrapper
                && UserWrapper.class.cast(modelObject).getPreviousUserTO() != null
                && !modelObject.getInnerObject().getResources().equals(
                        UserWrapper.class.cast(modelObject).getPreviousUserTO().getResources())) {

            add(new LabelInfo("changed", StringUtils.EMPTY));
        } else {
            add(new Label("changed", StringUtils.EMPTY));
        }

        // -----------------------------------------------------------------
        // Pre-Authorizations
        // -----------------------------------------------------------------
        final ActionPermissions permissions = new ActionPermissions();
        setMetaData(MetaDataRoleAuthorizationStrategy.ACTION_PERMISSIONS, permissions);
        permissions.authorize(RENDER,
                new org.apache.wicket.authroles.authorization.strategies.role.Roles(StandardEntitlement.RESOURCE_LIST));
        // -----------------------------------------------------------------

        this.setOutputMarkupId(true);
        this.available = new ListModel<>(Collections.<String>emptyList());

        add(new AjaxPalettePanel.Builder<String>().build("resources",
                new PropertyModel<List<String>>(entityTO, "resources") {

            private static final long serialVersionUID = 3799387950428254072L;

            @Override
            public List<String> getObject() {
                return new ArrayList<>(entityTO.getResources());
            }

            @Override
            public void setObject(final List<String> object) {
                entityTO.getResources().clear();
                entityTO.getResources().addAll(object);
            }
        }, available).hideLabel().setOutputMarkupId(true));
    }

    @Override
    public boolean evaluate() {
        if (SyncopeConsoleApplication.get().getSecuritySettings().getAuthorizationStrategy().
                isActionAuthorized(this, RENDER)) {
            available.setObject(new ResourceRestClient().list().stream().
                    map(EntityTO::getKey).collect(Collectors.toList()));
            return !available.getObject().isEmpty();
        } else {
            return false;
        }
    }
}
