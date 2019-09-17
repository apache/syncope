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

import org.apache.syncope.client.ui.commons.wizards.any.UserWrapper;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.rest.RoleRestClient;
import org.apache.syncope.client.ui.commons.ajax.markup.html.LabelInfo;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.ActionPermissions;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.wizard.WizardModel.ICondition;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

public class Roles extends WizardStep implements ICondition {

    private static final long serialVersionUID = 552437609667518888L;

    private final List<String> allRoles;

    public <T extends AnyTO> Roles(final AnyWrapper<?> modelObject) {
        if (!(modelObject.getInnerObject() instanceof UserTO)) {
            throw new IllegalStateException("Invalid instance " + modelObject.getInnerObject());
        }

        if (UserWrapper.class.cast(modelObject).getPreviousUserTO() != null
                && !ListUtils.isEqualList(
                        UserWrapper.class.cast(modelObject).getInnerObject().getRoles(),
                        UserWrapper.class.cast(modelObject).getPreviousUserTO().getRoles())) {
            add(new LabelInfo("changed", StringUtils.EMPTY));
        } else {
            add(new Label("changed", StringUtils.EMPTY));
        }

        final UserTO entityTO = UserTO.class.cast(modelObject.getInnerObject());

        // -----------------------------------------------------------------
        // Pre-Authorizations
        // -----------------------------------------------------------------
        final ActionPermissions permissions = new ActionPermissions();
        setMetaData(MetaDataRoleAuthorizationStrategy.ACTION_PERMISSIONS, permissions);
        permissions.authorize(RENDER,
                new org.apache.wicket.authroles.authorization.strategies.role.Roles(IdRepoEntitlement.ROLE_LIST));
        // -----------------------------------------------------------------

        this.setOutputMarkupId(true);

        allRoles = SyncopeWebApplication.get().getSecuritySettings().getAuthorizationStrategy().
                isActionAuthorized(this, RENDER)
                ? RoleRestClient.list().stream().map(EntityTO::getKey).collect(Collectors.toList())
                : List.of();
        Collections.sort(allRoles);

        add(new AjaxPalettePanel.Builder<String>().build("roles",
                new PropertyModel<List<String>>(entityTO, "roles"),
                new ListModel<>(allRoles)).hideLabel().setOutputMarkupId(true));

        add(new AjaxPalettePanel.Builder<String>().build("dynroles",
                new PropertyModel<List<String>>(entityTO, "dynRoles"),
                new ListModel<>(allRoles)).hideLabel().setEnabled(false).setOutputMarkupId(true));
    }

    @Override
    public final boolean evaluate() {
        return CollectionUtils.isNotEmpty(allRoles)
                && SyncopeWebApplication.get().getSecuritySettings().getAuthorizationStrategy().
                        isActionAuthorized(this, RENDER);
    }
}
