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

import org.apache.syncope.client.console.panels.mapping.SCIMExtensionMappingPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.scim.SCIMConf;
import org.apache.syncope.common.lib.scim.SCIMExtensionAnyConf;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

public abstract class SCIMConfExtensionAnyPanel extends SCIMConfTabPanel {

    private static final long serialVersionUID = 2459231778083046011L;
    
    protected SCIMConfExtensionAnyPanel(final String id, final SCIMConf scimConf) {
        super(id);

        SCIMExtensionAnyConf scimExtensionAnyConf = getExtensionAnyConf(scimConf);

        AjaxTextFieldPanel namePanel = new AjaxTextFieldPanel("name", "name", new PropertyModel<>("name", "name") {

            private static final long serialVersionUID = 7389942851813193481L;

            @Override
            public String getObject() {
                return scimExtensionAnyConf.getName();
            }

            @Override
            public void setObject(final String object) {
                scimExtensionAnyConf.setName(object);
            }
        });
        add(namePanel);

        AjaxTextFieldPanel descriptionPanel = new AjaxTextFieldPanel(
                "description", "description", new PropertyModel<>("description", "description") {

            private static final long serialVersionUID = -5911179251497048661L;

            @Override
            public String getObject() {
                return scimExtensionAnyConf.getDescription();
            }

            @Override
            public void setObject(final String object) {
                scimExtensionAnyConf.setDescription(object);
            }
        });
        add(descriptionPanel);

        SCIMExtensionMappingPanel extensionMappingPanel = new SCIMExtensionMappingPanel(
                "mapping", new ListModel<>(scimExtensionAnyConf.getAttributes()), getAnyType());
        Form<SCIMExtensionAnyConf> form = new Form<>("form", new Model<>(scimExtensionAnyConf));
        form.add(extensionMappingPanel);
        add(form);
    }

    abstract SCIMExtensionAnyConf getExtensionAnyConf(SCIMConf scimConf);

    abstract AnyTypeKind getAnyType();
}
