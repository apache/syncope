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
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.common.lib.to.SchemaTO;
import org.apache.syncope.common.lib.to.DerSchemaTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.VirSchemaTO;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.PageReference;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;

public class SchemaModalPanel extends AbstractModalPanel<SchemaTO> {

    private static final long serialVersionUID = -4681998932778822125L;

    private final AbstractSchemaDetailsPanel schemaPanel;

    private final SchemaTO schemaTO;

    public SchemaModalPanel(
            final BaseModal<SchemaTO> modal,
            final SchemaTO schemaTO,
            final PageReference pageRef) {
        super(modal, pageRef);

        this.schemaTO = schemaTO;

        final Form<SchemaType> kindForm = new Form<>("kindForm");
        add(kindForm);

        final AjaxDropDownChoicePanel<SchemaType> kind = new AjaxDropDownChoicePanel<>(
                "kind", getString("kind"), new Model<>());
        kind.setChoices(Arrays.asList(SchemaType.values()));
        kind.setOutputMarkupId(true);

        kind.setModelObject(SchemaType.fromToClass(schemaTO.getClass()));
        kind.setEnabled(false);
        kindForm.add(kind);

        schemaPanel = getSchemaPanel("details", SchemaType.fromToClass(schemaTO.getClass()), modal);
        schemaPanel.setOutputMarkupId(true);
        addOrReplace(schemaPanel);
    }

    private AbstractSchemaDetailsPanel getSchemaPanel(final String id,
            final SchemaType schemaType, final BaseModal<SchemaTO> modal) {
        final AbstractSchemaDetailsPanel panel;

        if (schemaTO.getKey() != null) {
            try {
                final Class<? extends SchemaTO> schemaTOClass = schemaType.getToClass();
                modal.setFormModel((SchemaTO) schemaTOClass.newInstance());
            } catch (InstantiationException | IllegalAccessException ex) {
                LOG.error("SchemaType not found", ex);
            }
        }

        switch (schemaType) {
            case DERIVED:
                panel = new DerSchemaDetails(id, pageRef, (DerSchemaTO) schemaTO);
                break;

            case VIRTUAL:
                panel = new VirSchemaDetails(id, pageRef, (VirSchemaTO) schemaTO);
                break;

            case PLAIN:
            default:
                panel = new PlainSchemaDetails(id, pageRef, (PlainSchemaTO) schemaTO);
        }
        panel.setOutputMarkupId(true);
        return panel;
    }
}
