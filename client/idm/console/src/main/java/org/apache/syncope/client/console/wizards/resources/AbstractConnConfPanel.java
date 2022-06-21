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
package org.apache.syncope.client.console.wizards.resources;

import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.wicket.markup.html.list.ConnConfPropertyListView;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.wizard.WizardModel.ICondition;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.ResourceModel;

public abstract class AbstractConnConfPanel<T extends EntityTO> extends WizardStep implements ICondition {

    private static final long serialVersionUID = -2025535531121434050L;

    protected LoadableDetachableModel<List<ConnConfProperty>> model;

    protected final WebMarkupContainer propertiesContainer;

    protected final AjaxButton check;

    protected final T modelObject;

    public AbstractConnConfPanel(final T model) {
        super();
        this.modelObject = model;
        setOutputMarkupId(true);

        propertiesContainer = new WebMarkupContainer("connectorPropertiesContainer");
        propertiesContainer.setOutputMarkupId(true);
        add(propertiesContainer);

        check = new IndicatingAjaxButton("check", new ResourceModel("check")) {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onSubmit(final AjaxRequestTarget target) {
                final Pair<Boolean, String> result = check(target);
                if (result.getLeft()) {
                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                } else {
                    SyncopeConsoleSession.get().error(getString("error_connection") + ": " + result.getRight());
                }
                ((BasePage) getPage()).getNotificationPanel().refresh(target);
            }
        };
        propertiesContainer.add(check);
    }

    protected void setConfPropertyListView(final IModel<List<ConnConfProperty>> model, final boolean withOverridable) {
        propertiesContainer.addOrReplace(new ConnConfPropertyListView(
                "connectorProperties", model, withOverridable).setOutputMarkupId(true));
    }

    protected abstract Pair<Boolean, String> check(AjaxRequestTarget taget);

    protected abstract List<ConnConfProperty> getConnProperties(T instance);

    @Override
    public boolean evaluate() {
        return model != null && model.getObject() != null && !model.getObject().isEmpty();
    }
}
