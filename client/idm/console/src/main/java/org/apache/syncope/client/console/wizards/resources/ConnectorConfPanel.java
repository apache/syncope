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

import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.to.ConnIdBundle;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.wicket.model.LoadableDetachableModel;

public abstract class ConnectorConfPanel extends AbstractConnConfPanel<ConnInstanceTO> {

    private static final long serialVersionUID = -2025535531121434050L;

    private final List<ConnIdBundle> bundles;

    public ConnectorConfPanel(final ConnInstanceTO connInstanceTO, final List<ConnIdBundle> bundles) {
        super(connInstanceTO);
        this.bundles = bundles;

        model = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = -2965284931860212687L;

            @Override
            protected List<ConnConfProperty> load() {
                List<ConnConfProperty> properties = getConnProperties(ConnectorConfPanel.this.modelObject);
                ConnectorConfPanel.this.modelObject.getConf().clear();
                ConnectorConfPanel.this.modelObject.getConf().addAll(properties);
                return properties;
            }
        };

        setConfPropertyListView(model, true);
    }

    /**
     * Get available configuration properties.
     *
     * @param instance connector instance.
     * @return configuration properties.
     */
    @Override
    protected List<ConnConfProperty> getConnProperties(final ConnInstanceTO instance) {
        return ConnectorWizardBuilder.getBundle(instance, bundles).getProperties().stream().map(key -> {
            ConnConfProperty property = new ConnConfProperty();
            property.setSchema(key);

            instance.getConf(key.getName()).ifPresent(conf -> {
                property.getValues().addAll(conf.getValues());
                property.setOverridable(conf.isOverridable());
            });

            if (property.getValues().isEmpty() && !key.getDefaultValues().isEmpty()) {
                property.getValues().addAll(key.getDefaultValues());
            }
            return property;
        }).collect(Collectors.toList());
    }
}
