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

import java.io.Serializable;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.client.console.topology.TopologyNode;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.to.ConnIdBundle;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.PropertyModel;

/**
 * Modal window with Connector form.
 */
public class ConnectorWizardBuilder extends AbstractResourceWizardBuilder<ConnInstanceTO> {

    private static final long serialVersionUID = -2025535531121434050L;

    protected final List<ConnIdBundle> bundles;

    protected final ConnectorRestClient connectorRestClient;

    public ConnectorWizardBuilder(
            final ConnInstanceTO defaultItem,
            final ConnectorRestClient connectorRestClient,
            final PageReference pageRef) {

        super(defaultItem, pageRef);

        this.bundles = connectorRestClient.getAllBundles().stream().
                filter(object -> object.getLocation().equals(defaultItem.getLocation())).collect(Collectors.toList());
        this.connectorRestClient = connectorRestClient;
    }

    @Override
    protected WizardModel buildModelSteps(final Serializable modelObject, final WizardModel wizardModel) {
        ConnInstanceTO connInstanceTO = ConnInstanceTO.class.cast(modelObject);
        wizardModel.add(new ConnectorDetailsPanel(connInstanceTO, bundles));
        wizardModel.add(new ConnectorConfPanel(connInstanceTO, bundles) {

            private static final long serialVersionUID = -5886691077681158494L;

            @Override
            protected Pair<Boolean, String> check(final AjaxRequestTarget target) {
                ConnInstanceTO connInstanceTO = ConnInstanceTO.class.cast(modelObject);
                ConnIdBundle bundleTO = ConnectorWizardBuilder.getBundle(connInstanceTO, bundles);

                connInstanceTO.setConnectorName(bundleTO.getConnectorName());
                connInstanceTO.setBundleName(bundleTO.getBundleName());
                connInstanceTO.setVersion(bundleTO.getVersion());

                return connectorRestClient.check(connInstanceTO);
            }

            @Override
            protected void onComponentTag(final ComponentTag tag) {
                tag.append("class", "scrollable-tab-content", " ");
            }

        });
        wizardModel.add(new ConnCapabilitiesPanel(new PropertyModel<>(connInstanceTO, "capabilities")));
        return wizardModel;
    }

    @Override
    protected Serializable onApplyInternal(final Serializable modelObject) {
        ConnInstanceTO connInstanceTO = ConnInstanceTO.class.cast(modelObject);
        ConnIdBundle bundleTO = ConnectorWizardBuilder.getBundle(connInstanceTO, bundles);

        connInstanceTO.setConnectorName(bundleTO.getConnectorName());
        connInstanceTO.setBundleName(bundleTO.getBundleName());
        connInstanceTO.setVersion(bundleTO.getVersion());

        // Reset pool configuration if all fields are null
        if (connInstanceTO.getPoolConf() != null
                && connInstanceTO.getPoolConf().getMaxIdle() == null
                && connInstanceTO.getPoolConf().getMaxObjects() == null
                && connInstanceTO.getPoolConf().getMaxWait() == null
                && connInstanceTO.getPoolConf().getMinEvictableIdleTimeMillis() == null
                && connInstanceTO.getPoolConf().getMinIdle() == null) {

            connInstanceTO.setPoolConf(null);
        }

        ConnInstanceTO connInstance;
        if (mode == AjaxWizard.Mode.CREATE) {
            connInstance = connectorRestClient.create(connInstanceTO);
        } else {
            connectorRestClient.update(connInstanceTO);
            connInstance = connInstanceTO;
        }

        return connInstance;
    }

    @Override
    protected Serializable getCreateCustomPayloadEvent(final Serializable afterObject, final AjaxRequestTarget target) {
        ConnInstanceTO connInstance = ConnInstanceTO.class.cast(afterObject);
        return new CreateEvent(
                connInstance.getKey(),
                connInstance.getDisplayName(),
                TopologyNode.Kind.CONNECTOR,
                URI.create(connInstance.getLocation()).toASCIIString(),
                target);
    }

    protected static ConnIdBundle getBundle(final ConnInstanceTO connInstanceTO, final List<ConnIdBundle> bundles) {
        return bundles.stream().
                filter(bundle -> bundle.getBundleName().equals(connInstanceTO.getBundleName())
                && bundle.getConnectorName().equals(connInstanceTO.getConnectorName())
                && bundle.getVersion().equals(connInstanceTO.getVersion())).
                findFirst().orElse(null);
    }
}
