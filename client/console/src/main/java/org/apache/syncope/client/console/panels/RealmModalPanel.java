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

import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.rest.RealmRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.form.Form;

public class RealmModalPanel extends AbstractModalPanel<RealmTO> {

    private static final long serialVersionUID = -4285220460543213901L;

    private final RealmRestClient realmRestClient = new RealmRestClient();

    private boolean newRealm = false;

    private final String parentPath;

    public RealmModalPanel(
            final BaseModal<RealmTO> modal,
            final PageReference pageRef,
            final RealmTO realmTO,
            final String parentPath,
            final String entitlement) {

        this(modal, pageRef, realmTO, parentPath, entitlement, false);
    }

    public RealmModalPanel(
            final BaseModal<RealmTO> modal,
            final PageReference pageRef,
            final RealmTO realmTO,
            final String parentPath,
            final String entitlement,
            final boolean newRealm) {

        super(modal, pageRef);

        this.newRealm = newRealm;
        this.parentPath = parentPath;

        RealmDetails realmDetail = new RealmDetails("details", realmTO);
        realmDetail.add(new AttributeAppender("style", "overflow-x:hidden;"));
        if (SyncopeConsoleSession.get().owns(entitlement)) {
            MetaDataRoleAuthorizationStrategy.authorize(realmDetail, ENABLE, entitlement);
        }

        add(realmDetail);
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
        try {
            final RealmTO updatedRealmTO = RealmTO.class.cast(form.getModelObject());
            if (newRealm) {
                realmRestClient.create(this.parentPath, updatedRealmTO);
            } else {
                realmRestClient.update(updatedRealmTO);
            }
            modal.close(target);
            info(getString(Constants.OPERATION_SUCCEEDED));
        } catch (Exception e) {
            LOG.error("While creating or updating realm", e);
            error(StringUtils.isBlank(e.getMessage()) ? e.getClass().getName() : e.getMessage());
        }
        SyncopeConsoleSession.get().getNotificationPanel().refresh(target);
    }
}
