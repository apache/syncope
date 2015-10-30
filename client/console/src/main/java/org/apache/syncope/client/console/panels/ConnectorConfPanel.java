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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.syncope.common.lib.to.ConnBundleTO;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.types.ConnConfPropSchema;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.wicket.model.IModel;

/**
 * Modal window with Connector form.
 */
public abstract class ConnectorConfPanel extends AbstractConnectorConfPanel<ConnInstanceTO> {

    private static final long serialVersionUID = -2025535531121434050L;

    private final List<ConnBundleTO> bundles;

    public ConnectorConfPanel(final String id, final IModel<ConnInstanceTO> model, final List<ConnBundleTO> bundles) {

        super(id, model);
        this.bundles = bundles;

        final List<ConnConfProperty> properties = getConnProperties(model.getObject());
        model.getObject().getConf().clear();
        model.getObject().getConf().addAll(properties);

        setConfPropertyListView("conf", true);
    }

    /**
     * Ge available configuration properties.
     *
     * @param instance connector instance.
     * @return configuration properties.
     */
    @Override
    protected final List<ConnConfProperty> getConnProperties(final ConnInstanceTO instance) {

        final List<ConnConfProperty> res = CollectionUtils.collect(
                ConnectorModal.getBundle(instance, bundles).getProperties(),
                new Transformer<ConnConfPropSchema, ConnConfProperty>() {

                    @Override
                    public ConnConfProperty transform(final ConnConfPropSchema key) {
                        final ConnConfProperty property = new ConnConfProperty();
                        property.setSchema(key);

                        if (instance.getKey() != 0 && instance.getConfMap().containsKey(key.getName())
                        && instance.getConfMap().get(key.getName()).getValues() != null) {
                            property.getValues().addAll(instance.getConfMap().get(key.getName()).getValues());
                            property.setOverridable(instance.getConfMap().get(key.getName()).isOverridable());
                        }

                        if (property.getValues().isEmpty() && !key.getDefaultValues().isEmpty()) {
                            property.getValues().addAll(key.getDefaultValues());
                        }
                        return property;
                    }
                },
                new ArrayList<ConnConfProperty>());

        return res;
    }
}
