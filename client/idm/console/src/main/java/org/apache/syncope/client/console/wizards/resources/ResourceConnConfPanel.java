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
package org.apache.syncope.client.console.wizards.resources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public abstract class ResourceConnConfPanel extends AbstractConnConfPanel<ResourceTO> {

    private static final long serialVersionUID = -7982691107029848579L;

    @SpringBean
    protected ConnectorRestClient connectorRestClient;

    public ResourceConnConfPanel(final ResourceTO resourceTO) {
        super(resourceTO);

        model = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = -2965284931860212687L;

            @Override
            protected List<ConnConfProperty> load() {
                List<ConnConfProperty> confOverride = getConnProperties(resourceTO);
                resourceTO.setConfOverride(Optional.of(confOverride));
                return confOverride;
            }
        };

        setConfPropertyListView(model, true);

        check.setEnabled(!model.getObject().isEmpty());
        check.setVisible(!model.getObject().isEmpty());
    }

    /**
     * Get overridable properties.
     *
     * @param resourceTO resource instance.
     * @return overridable properties.
     */
    @Override
    protected List<ConnConfProperty> getConnProperties(final ResourceTO resourceTO) {
        List<ConnConfProperty> props = new ArrayList<>();

        if (resourceTO.getConnector() != null) {
            connectorRestClient.read(resourceTO.getConnector()).getConf().stream().
                    filter(ConnConfProperty::isOverridable).
                    forEachOrdered(props::add);
        }

        resourceTO.getConfOverride().ifPresent(confOverride -> {
            Map<String, ConnConfProperty> valuedProps = new HashMap<>();
            confOverride.forEach(prop -> valuedProps.put(prop.getSchema().getName(), prop));

            for (int i = 0; i < props.size(); i++) {
                if (valuedProps.containsKey(props.get(i).getSchema().getName())) {
                    props.set(i, valuedProps.get(props.get(i).getSchema().getName()));
                }
            }
        });

        return props;
    }

    public AjaxButton getCheck() {
        return check;
    }
}
