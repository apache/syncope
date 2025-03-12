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

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.Attributable;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;

public class AjaxCheckBoxPanel extends FieldPanel<Boolean> {

    private static final long serialVersionUID = 5664138233103884310L;

    public AjaxCheckBoxPanel(final String id, final String name, final IModel<Boolean> model) {
        this(id, name, model, true);
    }

    public AjaxCheckBoxPanel(
            final String id, final String name, final IModel<Boolean> model, final boolean enableOnChange) {

        super(id, name, model);

        field = new CheckBox("checkboxField", model);
        add(field.setLabel(new ResourceModel(name, name)).setOutputMarkupId(true));

        if (enableOnChange && !isReadOnly()) {
            field.add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    // nothing to do
                }
            });
        }
    }

    @Override
    public FieldPanel<Boolean> setNewModel(final List<Serializable> list) {
        setNewModel(new Model<>() {

            private static final long serialVersionUID = 527651414610325237L;

            @Override
            public Boolean getObject() {
                Boolean value = null;

                if (list != null && !list.isEmpty()) {
                    value = Boolean.TRUE.toString().equalsIgnoreCase(list.getFirst().toString());
                }

                return value;
            }

            @Override
            public void setObject(final Boolean object) {
                list.clear();
                if (object != null) {
                    list.add(object.toString());
                }
            }
        });

        return this;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public FieldPanel<Boolean> setNewModel(final ListItem item) {
        IModel<Boolean> model = new Model<>() {

            private static final long serialVersionUID = 6799404673615637845L;

            @Override
            public Boolean getObject() {
                Boolean bool = null;

                final Object obj = item.getModelObject();

                if (obj != null && !obj.toString().isEmpty()) {
                    if (obj instanceof String) {
                        bool = Boolean.TRUE.toString().equalsIgnoreCase(obj.toString());
                    } else if (obj instanceof final Boolean b) {
                        // Don't parse anything
                        bool = b;
                    }
                }

                return bool;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void setObject(final Boolean object) {
                item.setModelObject(Optional.ofNullable(object)
                        .map(Object::toString).orElseGet(Boolean.FALSE::toString));
            }
        };

        field.setModel(model);
        return this;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public FieldPanel<Boolean> setNewModel(final Attributable attributable, final String schema) {
        field.setModel(new Model() {

            private static final long serialVersionUID = -4214654722524358000L;

            @Override
            public Serializable getObject() {
                return attributable.getPlainAttr(schema).map(Attr::getValues).filter(Predicate.not(List::isEmpty)).
                        map(values -> Boolean.TRUE.toString().equalsIgnoreCase(values.getFirst())).
                        orElse(null);
            }

            @Override
            public void setObject(final Serializable object) {
                attributable.getPlainAttr(schema).ifPresent(plainAttr -> {
                    plainAttr.getValues().clear();
                    plainAttr.getValues().add(object == null ? Boolean.FALSE.toString() : object.toString());
                });
            }
        });

        return this;
    }
}
