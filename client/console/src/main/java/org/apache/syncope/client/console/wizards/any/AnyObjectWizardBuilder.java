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
package org.apache.syncope.client.console.wizards.any;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wizards.AjaxWizardBuilder;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

public class AnyObjectWizardBuilder extends AjaxWizardBuilder<AnyTO> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final AnyTO anyTO;

    private final LoadableDetachableModel<List<String>> anyTypes = new LoadableDetachableModel<List<String>>() {

        private static final long serialVersionUID = 1L;

        @Override
        protected List<String> load() {
            final List<String> currentlyAdded = new ArrayList<>();
            return currentlyAdded;
        }
    };

    /**
     * The object type specification step.
     */
    private final class ObjectType extends WizardStep {

        private static final long serialVersionUID = 1L;

        /**
         * Construct.
         */
        ObjectType(final AnyTO item) {
            super(new ResourceModel("type.title", StringUtils.EMPTY),
                    new ResourceModel("type.summary", StringUtils.EMPTY), new Model<AnyTO>(item));

            add(new AjaxDropDownChoicePanel<String>("type", "type", new PropertyModel<String>(item, "anyType"), false).
                    setChoices(anyTypes).
                    setStyleSheet("form-control").
                    setRequired(true));

            add(new TextField<String>(
                    "class", new PropertyModel<String>(item, "objectClass")).setRequired(true));
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
        Mapping(final AnyTO item) {
            setTitleModel(new ResourceModel("mapping.title", "Mapping"));
            setSummaryModel(new StringResourceModel("mapping.summary", this, new Model<AnyTO>(item)));
        }
    }

    /**
     * AccountLink specification step.
     */
    private final class ConnObjectLink extends WizardStep {

        private static final long serialVersionUID = 1L;

        /**
         * Construct.
         */
        ConnObjectLink(final AnyTO item) {
            super(new ResourceModel("link.title", StringUtils.EMPTY),
                    new ResourceModel("link.summary", StringUtils.EMPTY));

            final WebMarkupContainer connObjectLinkContainer = new WebMarkupContainer("connObjectLinkContainer");
            connObjectLinkContainer.setOutputMarkupId(true);
            add(connObjectLinkContainer);
        }
    }

    /**
     * Construct.
     *
     * @param id The component id
     * @param anyTO external resource to be updated.
     * @param pageRef Caller page reference.
     */
    public AnyObjectWizardBuilder(final String id, final AnyTO anyTO, final PageReference pageRef) {
        super(id, new AnyObjectTO(), pageRef);
        this.anyTO = anyTO;
    }

    @Override
    protected WizardModel buildModelSteps(final AnyTO modelObject, final WizardModel wizardModel) {
        wizardModel.add(new ObjectType(modelObject));
        wizardModel.add(new Mapping(modelObject));
        wizardModel.add(new ConnObjectLink(modelObject));
        return wizardModel;
    }

    @Override
    protected void onCancelInternal(final AnyTO modelObject) {
        // d nothing
    }

    @Override
    protected void onApplyInternal(final AnyTO modelObject) {
        // do nothing
    }
}
