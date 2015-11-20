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

import java.util.Arrays;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.common.lib.to.AbstractSchemaTO;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

public class SchemaModalPanel extends AbstractModalPanel {

    private static final long serialVersionUID = -4681998932778822125L;

    private AbstractSchemaDetailsPanel schemaPanel;

    private final boolean createFlag;

    public SchemaModalPanel(
            final BaseModal<AbstractSchemaTO> modal,
            final PageReference pageRef, final boolean createFlag) {
        super(modal, pageRef);

        this.createFlag = createFlag;
        final BaseModal<AbstractSchemaTO> schemaModal = modal;

        final Panel panel = this;
        final Form<SchemaType> kindForm = new Form<>("kindForm");
        add(kindForm);

        final AjaxDropDownChoicePanel<SchemaType> kind = new AjaxDropDownChoicePanel<>(
                "kind", getString("kind"), new Model<SchemaType>());
        kind.setChoices(Arrays.asList(SchemaType.values()));
        kind.setOutputMarkupId(true);

        SchemaType schemaType = SchemaType.PLAIN;
        if (!createFlag) {
            schemaType = SchemaType.fromToClass(schemaModal.getFormModel().getClass());
            kind.setModelObject(schemaType);
            kind.setEnabled(false);
        }

        ((DropDownChoice) kind.getField()).setNullValid(false);
        kindForm.add(kind);

        kind.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                schemaPanel = getSchemaPanel("details", kind.getModelObject(), modal);
                panel.addOrReplace(schemaPanel);
                target.add(panel);
            }
        });

        schemaPanel = getSchemaPanel("details", schemaType, modal);
        schemaPanel.setOutputMarkupId(true);
        addOrReplace(schemaPanel);
    }

    private AbstractSchemaDetailsPanel getSchemaPanel(final String id,
            final SchemaType schemaType, final BaseModal<AbstractSchemaTO> modal) {
        final AbstractSchemaDetailsPanel panel;

        if (createFlag) {
            try {
                final Class<? extends AbstractSchemaTO> schemaTOClass = schemaType.getToClass();
                modal.setFormModel((AbstractSchemaTO) schemaTOClass.newInstance());
            } catch (InstantiationException | IllegalAccessException ex) {
                LOG.error("SchemaType not found", ex);
            }
        }

        switch (schemaType) {
            case DERIVED:
                panel = new DerSchemaDetails(id, pageRef, modal);
                break;
            case VIRTUAL:
                panel = new VirSchemaDetails(id, pageRef, modal);
                break;
            case PLAIN:
            default:
                panel = new PlainSchemaDetails(id, pageRef, modal);
        }
        panel.setOutputMarkupId(true);
        return panel;
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
        schemaPanel.getOnSubmit(target, modal, form, pageRef, createFlag);
    }
}
