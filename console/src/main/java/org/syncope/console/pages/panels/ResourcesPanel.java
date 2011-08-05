/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.console.pages.panels;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.to.AbstractAttributableTO;
import org.syncope.client.to.ResourceTO;
import org.syncope.console.commons.SelectChoiceRenderer;
import org.syncope.console.rest.ResourceRestClient;

public class ResourcesPanel extends Panel {

    @SpringBean
    private ResourceRestClient resourceRestClient;

    final IModel<List<String>> allResources =
            new LoadableDetachableModel<List<String>>() {

                @Override
                protected List<String> load() {
                    final List<String> resourceNames =
                            new ArrayList<String>();

                    for (ResourceTO resourceTO :
                            resourceRestClient.getAllResources()) {
                        resourceNames.add(resourceTO.getName());
                    }
                    return resourceNames;
                }
            };

    public <T extends AbstractAttributableTO> ResourcesPanel(
            final String id, final T entityTO) {
        super(id);

        final Palette<String> resourcesPalette = new Palette(
                "resourcesPalette",
                new PropertyModel(entityTO, "resources"),
                new ListModel<String>(allResources.getObject()),
                new SelectChoiceRenderer(), 8, false);
        add(resourcesPalette);
    }
}
