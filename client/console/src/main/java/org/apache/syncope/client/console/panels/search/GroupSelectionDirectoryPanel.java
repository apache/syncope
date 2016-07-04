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
import org.apache.syncope.client.console.panels.GroupDisplayAttributesModalPanel;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.console.wizards.any.AnyWrapper;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.wicket.PageReference;

public final class GroupSelectionDirectoryPanel extends AnySelectionDirectoryPanel<GroupTO, GroupRestClient> {

    private static final long serialVersionUID = -1100228004207271271L;

    private GroupSelectionDirectoryPanel(final String id, final Builder builder, final boolean wizardInModal) {
        super(id, builder, GroupTO.class, wizardInModal);
    }

    @Override
    protected String paginatorRowsKey() {
        return Constants.PREF_GROUP_PAGINATOR_ROWS;
    }

    @Override
    protected String[] getDisplayAttributes() {
        return GroupDisplayAttributesModalPanel.DEFAULT_SELECTION;
    }

    @Override
    public String getPrefDetailsView() {
        return Constants.PREF_GROUP_DETAILS_VIEW;
    }

    @Override
    public String getPrefPlainAttributesView() {
        return Constants.PREF_GROUP_PLAIN_ATTRS_VIEW;
    }

    @Override
    public String getPrefDerivedAttributesView() {
        return Constants.PREF_GROUP_DER_ATTRS_VIEW;
    }

    public static final class Builder extends AnySelectionDirectoryPanel.Builder<GroupTO, GroupRestClient> {

        private static final long serialVersionUID = -8774023867045850683L;

        public Builder(final List<AnyTypeClassTO> anyTypeClassTOs, final String type, final PageReference pageRef) {
            super(anyTypeClassTOs, new GroupRestClient(), type, pageRef);
            this.filtered = true;
            this.checkBoxEnabled = false;
        }

        @Override
        protected WizardMgtPanel<AnyWrapper<GroupTO>> newInstance(final String id, final boolean wizardInModal) {
            return new GroupSelectionDirectoryPanel(id, this, wizardInModal);
        }
    }
}
