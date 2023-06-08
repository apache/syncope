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
import org.apache.syncope.client.console.commons.IdRepoConstants;
import org.apache.syncope.client.console.panels.AnyObjectDisplayAttributesModalPanel;
import org.apache.syncope.client.console.rest.AnyObjectRestClient;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.wicket.PageReference;

public final class AnyObjectSelectionDirectoryPanel
        extends AnySelectionDirectoryPanel<AnyObjectTO, AnyObjectRestClient> {

    private static final long serialVersionUID = -1100228004207271272L;

    private AnyObjectSelectionDirectoryPanel(final String id, final Builder builder, final boolean wizardInModal) {
        super(id, builder, wizardInModal);
    }

    @Override
    protected String paginatorRowsKey() {
        return IdRepoConstants.PREF_ANYOBJECT_PAGINATOR_ROWS;
    }

    @Override
    protected String[] getDefaultAttributeSelection() {
        return AnyObjectDisplayAttributesModalPanel.DEFAULT_SELECTION;
    }

    public static final class Builder extends AnySelectionDirectoryPanel.Builder<AnyObjectTO, AnyObjectRestClient> {

        private static final long serialVersionUID = 5155811461060452446L;

        public Builder(
                final List<AnyTypeClassTO> anyTypeClassTOs,
                final AnyObjectRestClient restClient,
                final String type,
                final PageReference pageRef) {

            super(anyTypeClassTOs, restClient, type, pageRef);
        }

        @Override
        protected WizardMgtPanel<AnyWrapper<AnyObjectTO>> newInstance(final String id, final boolean wizardInModal) {
            return new AnyObjectSelectionDirectoryPanel(id, this, wizardInModal);
        }
    }
}
