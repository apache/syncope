/*
 * Copyright 2015 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.syncope.client.console.wizards;

import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.panels.ResourceMappingPanel;
import org.apache.syncope.common.lib.to.ProvisionTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

public class ProvisionWizard extends AjaxWizard<ProvisionTO> {

    private static final long serialVersionUID = 1L;

    private final ResourceTO resourceTO;

    /**
     * The object type specification step.
     */
    private final class ObjectType extends WizardStep {

        private static final long serialVersionUID = 1L;

        /**
         * Construct.
         */
        public ObjectType() {
            super(new ResourceModel("type.title", StringUtils.EMPTY),
                    new ResourceModel("type.summary", StringUtils.EMPTY), new Model<ProvisionTO>(getItem()));

            add(new TextField<String>(
                    "type", new PropertyModel<String>(getItem(), "anyType")).setRequired(true));
            add(new TextField<String>(
                    "class", new PropertyModel<String>(getItem(), "objectClass")).setRequired(true));
        }
    }

    /**
     * Mapping definition step.
     */
    private final class Mapping extends WizardStep {

        private static final long serialVersionUID = 1L;

        /**
         * Construct.
         */
        public Mapping() {
            setTitleModel(new ResourceModel("mapping.title", "Mapping"));
            setSummaryModel(new StringResourceModel("mapping.summary", this, new Model<ProvisionTO>(getItem())));

            add(new ResourceMappingPanel("mapping", resourceTO, getItem()));
        }
    }

    /**
     * AccountLink specification step.
     */
    private final class AccountLink extends WizardStep {

        private static final long serialVersionUID = 1L;

        /**
         * Construct.
         */
        public AccountLink() {
            super(new ResourceModel("link.title", StringUtils.EMPTY),
                    new ResourceModel("link.summary", StringUtils.EMPTY));
        }
    }

    /**
     * Construct.
     *
     * @param id The component id
     * @param resourceTO external resource to be updated.
     * @param pageRef Caller page reference.
     */
    public ProvisionWizard(final String id, final ResourceTO resourceTO, final PageReference pageRef) {
        super(id, new ProvisionTO(), pageRef);
        this.resourceTO = resourceTO;

        setDefaultModel(new CompoundPropertyModel<ProvisionWizard>(this));

        final WizardModel model = new WizardModel();
        model.add(new ObjectType());
        model.add(new Mapping());
        model.add(new AccountLink());

        init(model);
    }

    @Override
    protected void onCancelInternal() {
        // d nothing
    }

    @Override
    protected void onApplyInternal() {
        // do nothing
    }
}
