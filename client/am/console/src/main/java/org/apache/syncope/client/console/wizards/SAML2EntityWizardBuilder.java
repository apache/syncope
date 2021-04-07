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
package org.apache.syncope.client.console.wizards;

import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.wicket.markup.html.form.XMLEditorPanel;
import org.apache.syncope.common.lib.to.SAML2EntityTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.PropertyModel;

abstract class SAML2EntityWizardBuilder<T extends SAML2EntityTO> extends BaseAjaxWizardBuilder<T> {

    private static final long serialVersionUID = 1L;

    protected SAML2EntityWizardBuilder(final T defaultItem, final PageReference pageRef) {
        super(defaultItem, pageRef);
    }

    protected class Metadata extends WizardStep {

        private static final long serialVersionUID = -3043839139187792810L;

        Metadata(final T entity, final PageReference pageRef) {
            add(new XMLEditorPanel(null, new PropertyModel<>(entity, "metadata"), false, pageRef));
        }

        @Override
        public String getTitle() {
            return "Metadata";
        }
    }

    protected abstract class Pem extends WizardStep {

        private static final long serialVersionUID = -3043839139187792810L;

        protected final String property;

        Pem(final String property) {
            this.property = property;
        }

        @Override
        public String getTitle() {
            return StringUtils.capitalize(property);
        }
    }
}
