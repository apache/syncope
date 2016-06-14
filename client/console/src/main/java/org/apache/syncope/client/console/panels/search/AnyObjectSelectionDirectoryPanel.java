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
import org.apache.syncope.client.console.panels.AnyObjectDisplayAttributesModalPanel;
import org.apache.syncope.client.console.rest.AnyObjectRestClient;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.console.wizards.any.AnyWrapper;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.wicket.PageReference;

public final class AnyObjectSelectionDirectoryPanel
        extends AnySelectionDirectoryPanel<AnyObjectTO, AnyObjectRestClient> {

    private static final long serialVersionUID = -1100228004207271272L;

    private AnyObjectSelectionDirectoryPanel(final String id, final Builder builder, final boolean wizardInModal) {
        super(id, builder, AnyObjectTO.class, wizardInModal);
    }

    @Override
    protected String paginatorRowsKey() {
        return Constants.PREF_ANYOBJECT_PAGINATOR_ROWS;
    }

    @Override
    protected String[] getDisplayAttributes() {
        return AnyObjectDisplayAttributesModalPanel.DEFAULT_SELECTION;
    }

    @Override
    public String getPrefDetailsView() {
        return String.format(Constants.PREF_ANY_OBJECT_DETAILS_VIEW, type);
    }

    @Override
    public String getPrefPlainAttributesView() {
        return String.format(Constants.PREF_ANY_OBJECT_PLAIN_ATTRS_VIEW, type);
    }

    @Override
    public String getPrefDerivedAttributesView() {
        return String.format(Constants.PREF_ANY_OBJECT_DER_ATTRS_VIEW, type);
    }

    public static final class Builder extends AnySelectionDirectoryPanel.Builder<AnyObjectTO, AnyObjectRestClient> {

        private static final long serialVersionUID = 5155811461060452446L;

        public Builder(final List<AnyTypeClassTO> anyTypeClassTOs, final String type, final PageReference pageRef) {
            super(anyTypeClassTOs, new AnyObjectRestClient(), type, pageRef);
            this.filtered = true;
            this.checkBoxEnabled = false;
        }

        @Override
        protected WizardMgtPanel<AnyWrapper<AnyObjectTO>> newInstance(final String id, final boolean wizardInModal) {
            return new AnyObjectSelectionDirectoryPanel(id, this, wizardInModal);
        }
    }
}
