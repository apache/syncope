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

import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.rest.RoleRestClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.markup.html.LabelInfo;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.ui.commons.wizards.any.UserWrapper;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.wicket.Component;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.ActionPermissions;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.wizard.WizardModel.ICondition;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class Roles extends WizardStep implements ICondition {

    private static final long serialVersionUID = 552437609667518888L;

    @SpringBean
    protected RoleRestClient roleRestClient;

    protected final List<String> allRoles;

    protected final UserTO userTO;

    protected WebMarkupContainer dynrolesContainer;

    public <T extends AnyTO> Roles(final UserWrapper modelObject) {
        if (modelObject.getPreviousUserTO() != null
                && !modelObject.getInnerObject().getRoles().equals(modelObject.getPreviousUserTO().getRoles())) {

            add(new LabelInfo("changed", StringUtils.EMPTY));
        } else {
            add(new Label("changed", StringUtils.EMPTY));
        }

        userTO = modelObject.getInnerObject();

        // -----------------------------------------------------------------
        // Pre-Authorizations
        // -----------------------------------------------------------------
        ActionPermissions permissions = new ActionPermissions();
        setMetaData(MetaDataRoleAuthorizationStrategy.ACTION_PERMISSIONS, permissions);
        permissions.authorize(RENDER,
                new org.apache.wicket.authroles.authorization.strategies.role.Roles(IdRepoEntitlement.ROLE_LIST));
        // -----------------------------------------------------------------

        this.setOutputMarkupId(true);

        allRoles = getManagedRoles();

        add(buildRolesSelector(modelObject));

        dynrolesContainer = new WebMarkupContainer("dynrolesContainer");
        dynrolesContainer.setOutputMarkupId(true);
        dynrolesContainer.setOutputMarkupPlaceholderTag(true);
        add(dynrolesContainer);

        dynrolesContainer.add(new AjaxPalettePanel.Builder<String>().build("dynroles",
                new PropertyModel<>(userTO, "dynRoles"),
                new ListModel<>(allRoles)).hideLabel().setEnabled(false).setOutputMarkupId(true));
    }

    protected List<String> getManagedRoles() {
        return SyncopeWebApplication.get().getSecuritySettings().getAuthorizationStrategy().
                isActionAuthorized(this, RENDER)
                ? roleRestClient.list().stream().map(RoleTO::getKey).sorted().collect(Collectors.toList())
                : List.of();
    }

    protected Component buildRolesSelector(final UserWrapper modelObject) {
        return new AjaxPalettePanel.Builder<String>().
                withFilter().
                setAllowOrder(true).
                build("roles",
                        new PropertyModel<>(modelObject.getInnerObject(), "roles"),
                        new AjaxPalettePanel.Builder.Query<>() {

                    private static final long serialVersionUID = 3900199363626636719L;

                    @Override
                    public List<String> execute(final String filter) {
                        if (StringUtils.isEmpty(filter) || "*".equals(filter)) {
                            return allRoles.size() > Constants.MAX_ROLE_LIST_SIZE
                                    ? allRoles.subList(0, Constants.MAX_ROLE_LIST_SIZE)
                                    : allRoles;

                        }
                        return allRoles.stream().
                                filter(role -> StringUtils.containsIgnoreCase(role, filter)).
                                collect(Collectors.toList());
                    }
                }).
                hideLabel().
                setOutputMarkupId(true);
    }

    @Override
    public final boolean evaluate() {
        return CollectionUtils.isNotEmpty(allRoles)
                && SyncopeWebApplication.get().getSecuritySettings().getAuthorizationStrategy().
                        isActionAuthorized(this, RENDER);
    }
}
