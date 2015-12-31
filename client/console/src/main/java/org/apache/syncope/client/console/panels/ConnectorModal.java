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

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.client.console.topology.TopologyNode;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.common.lib.to.ConnBundleTO;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

/**
 * Modal window with Connector form.
 */
public class ConnectorModal extends AbstractResourceModal<Serializable> {

    private static final long serialVersionUID = -2025535531121434050L;

    private final ConnectorRestClient connectorRestClient = new ConnectorRestClient();

    private final List<ConnBundleTO> bundles;

    public ConnectorModal(
            final BaseModal<Serializable> modal,
            final PageReference pageRef,
            final IModel<ConnInstanceTO> model) {

        super(modal, pageRef);

        this.bundles = CollectionUtils.select(connectorRestClient.getAllBundles(),
                new Predicate<ConnBundleTO>() {

            @Override
            public boolean evaluate(final ConnBundleTO object) {
                return object.getLocation().equals(model.getObject().getLocation());
            }
        }, new ArrayList<ConnBundleTO>());

        //--------------------------------
        // Connector details panel
        //--------------------------------
        tabs.add(new AbstractTab(new ResourceModel("general", "general")) {

            private static final long serialVersionUID = -5861786415855103549L;

            @Override
            public Panel getPanel(final String panelId) {
                return new ConnectorDetailsPanel(panelId, model, bundles);
            }
        });
        //--------------------------------

        //--------------------------------
        // Connector configuration panel
        //--------------------------------
        tabs.add(new AbstractTab(new ResourceModel("configuration", "configuration")) {

            private static final long serialVersionUID = -5861786415855103549L;

            @Override
            public Panel getPanel(final String panelId) {
                return new ConnectorConfPanel(panelId, model, bundles) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void check(final AjaxRequestTarget target) {
                        if (connectorRestClient.check(model.getObject())) {
                            info(getString("success_connection"));
                        } else {
                            error(getString("error_connection"));
                        }
                        modal.getNotificationPanel().refresh(target);
                    }

                };
            }
        });
        //--------------------------------

        //--------------------------------
        // Connector configuration panel
        //--------------------------------
        tabs.add(new AbstractTab(new ResourceModel("capabilities", "capabilities")) {

            private static final long serialVersionUID = -5861786415855103549L;

            @Override
            public Panel getPanel(final String panelId) {
                return new ConnectorCapabilitiesPanel(panelId, model);
            }
        });
        //--------------------------------
    }

    @Override
    public void onError(final AjaxRequestTarget target, final Form<?> form) {
        modal.getNotificationPanel().refresh(target);
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
        final ConnInstanceTO connInstanceTO = (ConnInstanceTO) form.getModelObject();

        final ConnBundleTO bundleTO = ConnectorModal.getBundle(connInstanceTO, bundles);

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

        try {
            if (connInstanceTO.getKey() == 0) {
                final ConnInstanceTO actual = connectorRestClient.create(connInstanceTO);
                send(pageRef.getPage(), Broadcast.BREADTH, new CreateEvent(
                        actual.getKey(),
                        actual.getDisplayName(),
                        TopologyNode.Kind.CONNECTOR,
                        URI.create(actual.getLocation()).toASCIIString(),
                        target));
            } else {
                connectorRestClient.update(connInstanceTO);
            }
            info(getString(Constants.OPERATION_SUCCEEDED));
            modal.close(target);
        } catch (Exception e) {
            LOG.error("Failure managing resource {}", connInstanceTO, e);
            error(getString(Constants.ERROR) + ": " + e.getMessage());
            modal.getNotificationPanel().refresh(target);
        }
    }

    protected static ConnBundleTO getBundle(final ConnInstanceTO connInstanceTO, final List<ConnBundleTO> bundles) {
        return IterableUtils.find(bundles, new Predicate<ConnBundleTO>() {

            @Override
            public boolean evaluate(final ConnBundleTO bundle) {
                return bundle.getBundleName().equals(connInstanceTO.getBundleName())
                        && bundle.getVersion().equals(connInstanceTO.getVersion());
            }
        });
    }
}
