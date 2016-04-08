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
package org.apache.syncope.client.console.wizards.any;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

public class Resources extends WizardStep {

    private static final long serialVersionUID = 552437609667518888L;

    public <T extends AnyTO> Resources(final T entityTO) {
        this.setOutputMarkupId(true);

        add(new AjaxPalettePanel.Builder<String>().build("resources",
                new PropertyModel<List<String>>(entityTO, "resources") {

            private static final long serialVersionUID = 1L;

            @Override
            public List<String> getObject() {
                return new ArrayList<>(entityTO.getResources());
            }

            @Override
            public void setObject(final List<String> object) {
                entityTO.getResources().clear();
                entityTO.getResources().addAll(object);
            }
        }, new ListModel<>(CollectionUtils.collect(new ResourceRestClient().list(),
                        EntityTOUtils.<String, ResourceTO>keyTransformer(),
                        new ArrayList<String>()))).hideLabel().setOutputMarkupId(true));
    }
}
