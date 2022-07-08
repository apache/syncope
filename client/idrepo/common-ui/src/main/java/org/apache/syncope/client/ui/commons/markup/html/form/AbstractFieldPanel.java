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
package org.apache.syncope.client.ui.commons.markup.html.form;

import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractFieldPanel<T> extends Panel {

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractFieldPanel.class);

    private static final long serialVersionUID = 5958017546318855690L;

    public static final String LABEL = "field-label";

    protected boolean isRequiredLabelAdded = false;

    protected String name;

    public AbstractFieldPanel(final String id, final String name, final IModel<T> model) {
        super(id, model);
        this.name = name;

        add(new Fragment("required", "emptyFragment", AbstractFieldPanel.this));
        add(new Fragment("externalAction", "emptyFragment", AbstractFieldPanel.this));

        addLabel();
        setOutputMarkupId(true);
    }

    public final AbstractFieldPanel<T> addLabel() {
        return addLabel(this.name);
    }

    public final AbstractFieldPanel<T> addLabel(final String name) {
        addOrReplace(new Label(LABEL, new ResourceModel(name, name)));
        return this;
    }

    public AbstractFieldPanel<T> hideLabel() {
        Optional.ofNullable(get(LABEL)).ifPresent(label -> label.setVisible(false));
        return this;
    }

    public AbstractFieldPanel<T> showExternAction(final Component component) {
        Fragment fragment = new Fragment("externalAction", "externalActionFragment", AbstractFieldPanel.this);
        addOrReplace(fragment);
        fragment.add(component.setRenderBodyOnly(false));
        return this;
    }

    public boolean isRequired() {
        return false;
    }

    public AbstractFieldPanel<T> setRequired(final boolean required) {
        return this;
    }

    public AbstractFieldPanel<T> addRequiredLabel() {
        if (!isRequired()) {
            setRequired(true);
        }

        Fragment fragment = new Fragment("required", "requiredFragment", this);
        fragment.add(new Label("requiredLabel", "*"));
        replace(fragment);

        this.isRequiredLabelAdded = true;

        return this;
    }

    public AbstractFieldPanel<T> removeRequiredLabel() {
        if (isRequired()) {
            setRequired(false);
        }

        replace(new Fragment("required", "emptyFragment", this));

        this.isRequiredLabelAdded = false;

        return this;
    }

    protected String externalActionIcon() {
        return StringUtils.EMPTY;
    }

    public abstract AbstractFieldPanel<T> setModelObject(T object);

    public String getName() {
        return this.name;
    }

    public abstract AbstractFieldPanel<T> setReadOnly(boolean readOnly);
}
