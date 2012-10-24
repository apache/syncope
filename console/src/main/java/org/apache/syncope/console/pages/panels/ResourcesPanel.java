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

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.syncope.client.to.AbstractAttributableTO;
import org.apache.syncope.client.to.ResourceTO;
import org.apache.syncope.console.rest.ResourceRestClient;
import org.apache.syncope.console.wicket.markup.html.form.AjaxPalettePanel;

public class ResourcesPanel extends Panel {

    private static final long serialVersionUID = -8728071019777410008L;

    @SpringBean
    private ResourceRestClient resourceRestClient;

    public <T extends AbstractAttributableTO> ResourcesPanel(final String id, final T entityTO) {
        super(id);
        final IModel<List<String>> allResources = new allResourcesModel(resourceRestClient);

        final AjaxPalettePanel resourcesPalette = new AjaxPalettePanel("resourcesPalette", new PropertyModel(entityTO,
                "resources"), new ListModel<String>(allResources.getObject()));

        add(resourcesPalette);
    }
    
    private static class allResourcesModel extends LoadableDetachableModel<List<String>> {
        private static final long serialVersionUID = 5275935387613157437L;

        private ResourceRestClient client;
        
        public allResourcesModel(ResourceRestClient resourceRestClient) {
            this.client = resourceRestClient;
        }

        @Override
        protected List<String> load() {
            final List<String> resourceNames = new ArrayList<String>();

            for (ResourceTO resourceTO : client.getAllResources()) {
                resourceNames.add(resourceTO.getName());
            }
            return resourceNames;
        }
    }
}
