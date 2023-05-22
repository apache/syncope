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
package org.apache.syncope.client.enduser.panels.any;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.password.strength.PasswordStrengthBehavior;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.ui.commons.ajax.markup.html.LabelInfo;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.FieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.SyncopePasswordStrengthConfig;
import org.apache.syncope.client.ui.commons.wizards.any.PasswordPanel;
import org.apache.syncope.client.ui.commons.wizards.any.UserWrapper;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;

public class UserDetails extends Details<UserTO> {

    private static final long serialVersionUID = 6592027822510220463L;

    protected final AjaxTextFieldPanel username;

    protected final UserTO userTO;

    public UserDetails(final String id, final UserWrapper wrapper, final PageReference pageRef) {
        super(id, pageRef);

        userTO = wrapper.getInnerObject();
        // ------------------------
        // Username
        // ------------------------
        username = new AjaxTextFieldPanel("username", "username", new PropertyModel<>(userTO, "username"), false);

        if (wrapper.getPreviousUserTO() != null && StringUtils.
                compare(wrapper.getPreviousUserTO().getUsername(), wrapper.getInnerObject().getUsername()) != 0) {
            username.showExternAction(new LabelInfo("externalAction", wrapper.getPreviousUserTO().getUsername()));
        }

        username.addRequiredLabel();
        add(username);
        // ------------------------

        // ------------------------
        // Realm
        // ------------------------
        add(buildDestinationRealm());
    }

    protected FieldPanel<String> buildDestinationRealm() {
        AjaxDropDownChoicePanel<String> destinationRealm = new AjaxDropDownChoicePanel<>(
                "destinationRealm", "destinationRealm", new PropertyModel<>(userTO, "realm"), false);
        destinationRealm.setNullValid(false);
        destinationRealm.setChoices(List.of(
                Optional.ofNullable(userTO.getRealm()).orElse(SyncopeConstants.ROOT_REALM)));
        return destinationRealm;
    }

    protected static class EditUserPasswordPanel extends Panel {

        private static final long serialVersionUID = -8198836979773590078L;

        protected EditUserPasswordPanel(final String id, final UserWrapper wrapper) {
            super(id);
            setOutputMarkupId(true);
            add(new Label("warning", new ResourceModel("password.change.warning")));
            add(new PasswordPanel(
                    "passwordPanel",
                    wrapper,
                    false,
                    wrapper.getInnerObject().getKey() == null,
                    new PasswordStrengthBehavior(new SyncopePasswordStrengthConfig())));
        }
    }
}
