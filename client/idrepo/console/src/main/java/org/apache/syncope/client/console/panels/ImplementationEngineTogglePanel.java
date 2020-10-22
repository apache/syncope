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

import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.PropertyModel;

public abstract class ImplementationEngineTogglePanel extends TogglePanel<Serializable> {

    private static final long serialVersionUID = -7869528596656778267L;

    public ImplementationEngineTogglePanel(
            final String id,
            final ImplementationTO implementation,
            final PageReference pageRef) {

        super(id, pageRef);

        Form<?> form = new Form<>("implementationEngineForm");
        addInnerObject(form);

        PropertyModel<ImplementationEngine> engineModel = new PropertyModel<>(implementation, "engine");

        form.add(new AjaxDropDownChoicePanel<>(
                "engine", "Engine", engineModel, false).
                setNullValid(false).
                setChoices(Arrays.stream(ImplementationEngine.values()).collect(Collectors.toList())).
                setStyleSheet("form-control").
                hideLabel());

        form.add(new AjaxSubmitLink("changeit", form) {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                ImplementationEngineTogglePanel.this.onSubmit(engineModel.getObject(), target);
                target.add(form);
                toggle(target, false);

                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }

            @Override
            protected void onError(final AjaxRequestTarget target) {
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        });

    }

    protected abstract void onSubmit(ImplementationEngine engine, AjaxRequestTarget target);

    public void setHeaderLabel(final AjaxRequestTarget target) {
        setHeader(target, getString("engine"));
    }
}
