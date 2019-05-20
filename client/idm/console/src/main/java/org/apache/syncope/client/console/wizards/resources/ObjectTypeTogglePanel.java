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

import java.io.Serializable;
import java.util.List;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.TogglePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;

public abstract class ObjectTypeTogglePanel extends TogglePanel<Serializable> {

    private static final long serialVersionUID = -1366846136630731264L;

    ObjectTypeTogglePanel(
            final String id,
            final ResourceProvision resourceProvision,
            final LoadableDetachableModel<List<String>> anyTypes,
            final PageReference pageRef) {

        super(id, pageRef);

        Form<?> form = new Form<>("objectTypeForm");
        addInnerObject(form);

        PropertyModel<String> typeModel = new PropertyModel<>(resourceProvision, "anyType");

        form.add(new AjaxDropDownChoicePanel<>(
                "type", "type", typeModel, false).
                setNullValid(false).
                setChoices(anyTypes).
                setStyleSheet("form-control").
                hideLabel());

        form.add(new AjaxSubmitLink("changeit", form) {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                ObjectTypeTogglePanel.this.onSubmit(typeModel.getObject(), target);
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

    protected abstract void onSubmit(String type, AjaxRequestTarget target);

    public void setHeaderLabel(final AjaxRequestTarget target) {
        setHeader(target, getString("type"));
    }
}
