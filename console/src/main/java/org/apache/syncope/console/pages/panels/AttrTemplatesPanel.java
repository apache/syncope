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
package org.apache.syncope.console.pages.panels;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.SchemaType;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.commons.SelectChoiceRenderer;
import org.apache.syncope.console.rest.RoleRestClient;
import org.apache.syncope.console.rest.SchemaRestClient;
import org.apache.syncope.console.wicket.markup.html.form.NonI18nPalette;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.form.palette.component.Recorder;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class AttrTemplatesPanel extends Panel {

    public enum Type {

        rAttrTemplates,
        rDerAttrTemplates,
        rVirAttrTemplates,
        mAttrTemplates,
        mDerAttrTemplates,
        mVirAttrTemplates;

    }

    private static final long serialVersionUID = 1016028222120619000L;

    @SpringBean
    private SchemaRestClient schemaRestClient;

    @SpringBean
    private RoleRestClient roleRestClient;

    private final RoleTO roleTO;

    private final NonI18nPalette<String> rAttrTemplates;

    private final NonI18nPalette<String> rDerAttrTemplates;

    private final NonI18nPalette<String> rVirAttrTemplates;

    public AttrTemplatesPanel(final String id, final RoleTO roleTO) {
        super(id);
        this.roleTO = roleTO;

        rAttrTemplates = buildPalette(Type.rAttrTemplates,
                schemaRestClient.getSchemaNames(AttributableType.ROLE, SchemaType.NORMAL));
        this.add(rAttrTemplates);
        rDerAttrTemplates = buildPalette(Type.rDerAttrTemplates,
                schemaRestClient.getSchemaNames(AttributableType.ROLE, SchemaType.DERIVED));
        this.add(rDerAttrTemplates);
        rVirAttrTemplates = buildPalette(Type.rVirAttrTemplates,
                schemaRestClient.getSchemaNames(AttributableType.ROLE, SchemaType.VIRTUAL));
        this.add(rVirAttrTemplates);

        this.add(buildPalette(Type.mAttrTemplates,
                schemaRestClient.getSchemaNames(AttributableType.MEMBERSHIP, SchemaType.NORMAL)));
        this.add(buildPalette(Type.mDerAttrTemplates,
                schemaRestClient.getSchemaNames(AttributableType.MEMBERSHIP, SchemaType.DERIVED)));
        this.add(buildPalette(Type.mVirAttrTemplates,
                schemaRestClient.getSchemaNames(AttributableType.MEMBERSHIP, SchemaType.VIRTUAL)));
    }

    private NonI18nPalette<String> buildPalette(final Type type, final List<String> allSchemas) {
        if (allSchemas != null && !allSchemas.isEmpty()) {
            Collections.sort(allSchemas);
        }
        ListModel<String> availableSchemas = new ListModel<String>(allSchemas);

        return new NonI18nPalette<String>(type.name(), new PropertyModel<List<String>>(roleTO, type.name()),
                availableSchemas, new SelectChoiceRenderer<String>(), 8, false) {

            private static final long serialVersionUID = 2295567122085510330L;

            @Override
            protected Recorder<String> newRecorderComponent() {
                final Recorder<String> recorder = super.newRecorderComponent();

                switch (type) {
                    case rAttrTemplates:
                    case rDerAttrTemplates:
                    case rVirAttrTemplates:
                        recorder.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                            private static final long serialVersionUID = -1107858522700306810L;

                            @Override
                            protected void onUpdate(final AjaxRequestTarget target) {
                                send(getPage(), Broadcast.BREADTH, new RoleAttrTemplatesChange(type, target));
                            }
                        });
                        break;

                    default:
                }

                return recorder;
            }
        };
    }

    public Collection<String> getSelected(final Type type) {
        Collection<String> selected;
        switch (type) {
            case rAttrTemplates:
                selected = this.rAttrTemplates.getModelCollection();
                break;

            case rDerAttrTemplates:
                selected = this.rDerAttrTemplates.getModelCollection();
                break;

            case rVirAttrTemplates:
                selected = this.rVirAttrTemplates.getModelCollection();
                break;

            default:
                selected = Collections.emptyList();
        }

        return selected;
    }

    public static class RoleAttrTemplatesChange {

        private final Type type;

        private final AjaxRequestTarget target;

        public RoleAttrTemplatesChange(final Type type, final AjaxRequestTarget target) {
            this.type = type;
            this.target = target;
        }

        public Type getType() {
            return type;
        }

        public AjaxRequestTarget getTarget() {
            return target;
        }
    }
}
