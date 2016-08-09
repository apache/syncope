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

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.client.console.rest.AnyTypeClassRestClient;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.ProvisionTO;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

public class ProvisionAuxClassesPanel extends Panel {

    private static final long serialVersionUID = -3962956154520358784L;

    private final ProvisionTO provisionTO;

    public ProvisionAuxClassesPanel(final String id, final ProvisionTO provisionTO) {
        super(id);
        setOutputMarkupId(true);

        this.provisionTO = provisionTO;
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();

        AnyTypeTO anyType = new AnyTypeRestClient().read(provisionTO.getAnyType());
        List<String> choices = new ArrayList<>();
        for (AnyTypeClassTO aux : new AnyTypeClassRestClient().list()) {
            if (!anyType.getClasses().contains(aux.getKey())) {
                choices.add(aux.getKey());
            }
        }
        addOrReplace(new AjaxPalettePanel.Builder<String>().build("auxClasses",
                new PropertyModel<List<String>>(provisionTO, "auxClasses"),
                new ListModel<>(choices)).hideLabel().setOutputMarkupId(true));
    }

}
