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
package org.apache.syncope.client.console.panels.search;

import java.util.List;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.panels.UserDisplayAttributesModalPanel;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.console.wizards.any.AnyWrapper;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.PageReference;

public final class UserSelectionDirectoryPanel extends AnySelectionDirectoryPanel<UserTO, UserRestClient> {

    private static final long serialVersionUID = -1100228004207271272L;

    private UserSelectionDirectoryPanel(final String id, final Builder builder, final boolean wizardInModal) {
        super(id, builder, UserTO.class, wizardInModal);
    }

    @Override
    protected String paginatorRowsKey() {
        return Constants.PREF_USERS_PAGINATOR_ROWS;
    }

    @Override
    protected String[] getDisplayAttributes() {
        return UserDisplayAttributesModalPanel.DEFAULT_SELECTION;
    }

    @Override
    protected String getPrefDetailsView() {
        return Constants.PREF_USERS_DETAILS_VIEW;
    }

    @Override
    protected String getPrefPlainAttributesView() {
        return Constants.PREF_USERS_PLAIN_ATTRS_VIEW;
    }

    @Override
    protected String getPrefDerivedAttributesView() {
        return Constants.PREF_USERS_DER_ATTRS_VIEW;
    }

    public static final class Builder extends AnySelectionDirectoryPanel.Builder<UserTO, UserRestClient> {

        private static final long serialVersionUID = -1555789797531054422L;

        public Builder(final List<AnyTypeClassTO> anyTypeClassTOs, final String type, final PageReference pageRef) {
            super(anyTypeClassTOs, new UserRestClient(), type, pageRef);
            this.filtered = true;
            this.checkBoxEnabled = false;
        }

        @Override
        protected WizardMgtPanel<AnyWrapper<UserTO>> newInstance(final String id, final boolean wizardInModal) {
            return new UserSelectionDirectoryPanel(id, this, wizardInModal);
        }
    }
}
