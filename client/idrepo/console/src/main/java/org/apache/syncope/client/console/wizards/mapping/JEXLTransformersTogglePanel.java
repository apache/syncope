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
package org.apache.syncope.client.console.wizards.mapping;

import java.io.Serializable;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.panels.TogglePanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.to.Item;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

public class JEXLTransformersTogglePanel extends TogglePanel<Serializable> {

    private static final long serialVersionUID = -1284019117452782479L;

    private final AjaxTextFieldPanel propagationJEXLTransformer;

    private final AjaxTextFieldPanel pullJEXLTransformer;

    public JEXLTransformersTogglePanel(final WebMarkupContainer container, final PageReference pageRef) {
        super(Constants.OUTER, "jexlTransformersTogglePanel", pageRef);

        Form<?> form = new Form<>("form");
        addInnerObject(form);

        propagationJEXLTransformer = new AjaxTextFieldPanel(
                "propagationJEXLTransformer",
                "Propagation",
                Model.of(""));
        form.add(propagationJEXLTransformer.enableJexlHelp("value.toLowercase()", "'PREFIX' + value"));

        pullJEXLTransformer = new AjaxTextFieldPanel(
                "pullJEXLTransformer",
                "Pull",
                Model.of(""));
        form.add(pullJEXLTransformer.enableJexlHelp("value.toLowercase()", "'PREFIX' + value"));

        form.add(new AjaxSubmitLink("submit", form) {

            private static final long serialVersionUID = 4617041491286858973L;

            @Override
            public void onSubmit(final AjaxRequestTarget target) {
                toggle(target, false);
                target.add(container);
            }
        });
    }

    public JEXLTransformersTogglePanel setItem(final AjaxRequestTarget target, final Item item) {
        this.propagationJEXLTransformer.setNewModel(new PropertyModel<>(item, "propagationJEXLTransformer"));
        this.pullJEXLTransformer.setNewModel(new PropertyModel<>(item, "pullJEXLTransformer"));
        setHeader(target, StringUtils.EMPTY);
        return this;
    }
}
