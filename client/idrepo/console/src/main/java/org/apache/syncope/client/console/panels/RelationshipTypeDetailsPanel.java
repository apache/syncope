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

import java.util.List;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.RelationshipTypeTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class RelationshipTypeDetailsPanel extends Panel {

    private static final long serialVersionUID = -4962850669086306255L;

    @SpringBean
    protected AnyTypeRestClient anyTypeRestClient;

    public RelationshipTypeDetailsPanel(final String id, final RelationshipTypeTO relationshipTypeTO) {
        super(id);

        WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        Form<RelationshipTypeTO> form = new Form<>("form");
        form.setModel(new CompoundPropertyModel<>(relationshipTypeTO));
        container.add(form);

        AjaxTextFieldPanel key = new AjaxTextFieldPanel(
                Constants.KEY_FIELD_NAME, getString(Constants.KEY_FIELD_NAME),
                new PropertyModel<>(relationshipTypeTO, Constants.KEY_FIELD_NAME));
        key.addRequiredLabel();
        key.setEnabled(key.getModelObject() == null || key.getModelObject().isEmpty());
        form.add(key);

        AjaxTextFieldPanel description = new AjaxTextFieldPanel(
                Constants.DESCRIPTION_FIELD_NAME, getString(Constants.DESCRIPTION_FIELD_NAME),
                new PropertyModel<>(relationshipTypeTO, Constants.DESCRIPTION_FIELD_NAME));
        description.addRequiredLabel();
        form.add(description);

        LoadableDetachableModel<List<String>> anyTypes = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return anyTypeRestClient.listAnyTypes().stream().map(AnyTypeTO::getKey).toList();
            }
        };

        AjaxDropDownChoicePanel<String> leftEndAnyType = new AjaxDropDownChoicePanel<>(
                "leftEndAnyType", "leftEndAnyType", new PropertyModel<>(relationshipTypeTO, "leftEndAnyType"));
        leftEndAnyType.setChoices(anyTypes);
        leftEndAnyType.addRequiredLabel();
        leftEndAnyType.setEnabled(key.getModelObject() == null || key.getModelObject().isEmpty());
        form.add(leftEndAnyType);

        AjaxDropDownChoicePanel<String> rightEndAnyType = new AjaxDropDownChoicePanel<>(
                "rightEndAnyType", "rightEndAnyType", new PropertyModel<>(relationshipTypeTO, "rightEndAnyType"));
        rightEndAnyType.setChoices(anyTypes.getObject().stream().
                filter(t -> !AnyTypeKind.USER.name().equals(t) && !AnyTypeKind.GROUP.name().equals(t)).toList());
        rightEndAnyType.addRequiredLabel();
        rightEndAnyType.setEnabled(key.getModelObject() == null || key.getModelObject().isEmpty());
        form.add(rightEndAnyType);
    }
}
