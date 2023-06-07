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
import java.util.stream.Collectors;
import org.apache.syncope.client.console.rest.AnyTypeClassRestClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class AnyTypeDetailsPanel extends Panel {

    private static final long serialVersionUID = 8131650329622035501L;

    @SpringBean
    protected AnyTypeClassRestClient anyTypeClassRestClient;

    public AnyTypeDetailsPanel(final String id, final AnyTypeTO anyTypeTO) {
        super(id);

        WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        Form<AnyTypeTO> form = new Form<>("form");
        form.setModel(new CompoundPropertyModel<>(anyTypeTO));
        container.add(form);

        AjaxTextFieldPanel key = new AjaxTextFieldPanel(
                Constants.KEY_FIELD_NAME,
                getString(Constants.KEY_FIELD_NAME),
                new PropertyModel<>(anyTypeTO, Constants.KEY_FIELD_NAME));
        key.addRequiredLabel();
        key.setEnabled(key.getModelObject() == null || key.getModelObject().isEmpty());
        form.add(key);

        AjaxDropDownChoicePanel<AnyTypeKind> kind = new AjaxDropDownChoicePanel<>(
                "kind", getString("kind"), new PropertyModel<>(anyTypeTO, "kind"));
        kind.setChoices(List.of(AnyTypeKind.values()));
        kind.setOutputMarkupId(true);
        if (anyTypeTO.getKind() == null) {
            kind.setModelObject(AnyTypeKind.ANY_OBJECT);
        }
        kind.setEnabled(false);
        form.add(kind);

        form.add(new AjaxPalettePanel.Builder<String>().setAllowOrder(true).build("classes",
                new PropertyModel<>(anyTypeTO, "classes"),
                new ListModel<>(getAvailableAnyTypeClasses())).hideLabel().setOutputMarkupId(true));
    }

    protected List<String> getAvailableAnyTypeClasses() {
        return anyTypeClassRestClient.list().stream().map(AnyTypeClassTO::getKey).collect(Collectors.toList());
    }
}
