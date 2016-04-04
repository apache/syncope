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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.wicket.model.IModel;

public abstract class ResourceConnConfPanel extends AbstractConnConfPanel<ResourceTO> {

    private static final long serialVersionUID = -7982691107029848579L;

    private final ConnectorRestClient restClient = new ConnectorRestClient();

    private final boolean createFlag;

    public ResourceConnConfPanel(final String id, final IModel<ResourceTO> model, final boolean createFlag) {
        super(id, model);

        this.createFlag = createFlag;

        final List<ConnConfProperty> confOverride = getConnProperties(model.getObject());
        model.getObject().getConfOverride().clear();
        model.getObject().getConfOverride().addAll(confOverride);

        setConfPropertyListView("confOverride", false);

        check.setEnabled(!confOverride.isEmpty());
        check.setVisible(!confOverride.isEmpty());
    }

    /**
     * Get overridable properties.
     *
     * @param resourceTO resource instance.
     * @return overridable properties.
     */
    @Override
    protected final List<ConnConfProperty> getConnProperties(final ResourceTO resourceTO) {
        List<ConnConfProperty> props = new ArrayList<>();

        if (resourceTO.getConnector() != null && resourceTO.getConnector() > 0) {
            for (ConnConfProperty property : restClient.read(resourceTO.getConnector()).getConf()) {
                if (property.isOverridable()) {
                    props.add(property);
                }
            }
        }
        if (createFlag || resourceTO.getConfOverride().isEmpty()) {
            resourceTO.getConfOverride().clear();
        } else {
            Map<String, ConnConfProperty> valuedProps = new HashMap<>();
            for (ConnConfProperty prop : resourceTO.getConfOverride()) {
                valuedProps.put(prop.getSchema().getName(), prop);
            }

            for (int i = 0; i < props.size(); i++) {
                if (valuedProps.containsKey(props.get(i).getSchema().getName())) {
                    props.set(i, valuedProps.get(props.get(i).getSchema().getName()));
                }
            }
        }

        return props;
    }
}
