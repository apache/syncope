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

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.commons.RealmsUtils;
import org.apache.syncope.client.console.rest.RealmRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxSearchFieldPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxNumberFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.to.ConnIdBundle;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.types.ConnPoolConf;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class ConnectorDetailsPanel extends WizardStep {

    private static final long serialVersionUID = -2435937897614232137L;

    @SpringBean
    protected RealmRestClient realmRestClient;

    public ConnectorDetailsPanel(final ConnInstanceTO connInstanceTO, final List<ConnIdBundle> bundles) {
        super();
        setOutputMarkupId(true);

        boolean fullRealmsTree = SyncopeWebApplication.get().fullRealmsTree(realmRestClient);

        AutoCompleteSettings settings = new AutoCompleteSettings();
        settings.setShowCompleteListOnFocusGain(fullRealmsTree);
        settings.setShowListOnEmptyInput(fullRealmsTree);

        AjaxSearchFieldPanel realm = new AjaxSearchFieldPanel(
                "adminRealm", "adminRealm", new PropertyModel<>(connInstanceTO, "adminRealm"), settings) {

            private static final long serialVersionUID = -6390474600233486704L;

            @Override
            protected Iterator<String> getChoices(final String input) {
                return (RealmsUtils.checkInput(input)
                        ? (realmRestClient.search(fullRealmsTree
                                ? RealmsUtils.buildBaseQuery()
                                : RealmsUtils.buildKeywordQuery(input)).getResult())
                        : List.<RealmTO>of()).stream().
                        map(RealmTO::getFullPath).iterator();
            }
        };
        add(realm.addRequiredLabel().setOutputMarkupId(true));

        AjaxTextFieldPanel displayName = new AjaxTextFieldPanel(
                "displayName",
                "displayName",
                new PropertyModel<>(connInstanceTO, "displayName"), false);
        add(displayName.addRequiredLabel().setOutputMarkupId(true));

        AjaxTextFieldPanel location = new AjaxTextFieldPanel(
                "location", "location", new PropertyModel<>(connInstanceTO, "location"), false);
        add(location.addRequiredLabel().setOutputMarkupId(true).setEnabled(false));

        AjaxDropDownChoicePanel<String> bundleName = new AjaxDropDownChoicePanel<>(
                "bundleName",
                "bundleName",
                new PropertyModel<>(connInstanceTO, "bundleName"), false);
        bundleName.setEnabled(connInstanceTO.getKey() == null || connInstanceTO.isErrored());
        bundleName.setChoices(bundles.stream().map(ConnIdBundle::getBundleName).
                distinct().sorted().collect(Collectors.toList()));
        bundleName.getField().setOutputMarkupId(true);
        add(bundleName.addRequiredLabel().setOutputMarkupId(true));

        AjaxDropDownChoicePanel<String> connectorName = new AjaxDropDownChoicePanel<>(
                "connectorName",
                "connectorName",
                new PropertyModel<>(connInstanceTO, "connectorName"), false);
        connectorName.setEnabled(connInstanceTO.getBundleName() == null || connInstanceTO.isErrored());
        Optional.ofNullable(connInstanceTO.getConnectorName()).ifPresent(v -> connectorName.setChoices(List.of(v)));
        connectorName.getField().setOutputMarkupId(true);
        add(connectorName.addRequiredLabel().setOutputMarkupId(true));

        AjaxDropDownChoicePanel<String> version = new AjaxDropDownChoicePanel<>(
                "version", "version", new PropertyModel<>(connInstanceTO, "version"), false);
        version.setEnabled(connInstanceTO.getConnectorName() == null || connInstanceTO.isErrored());
        Optional.ofNullable(connInstanceTO.getVersion()).ifPresent(v -> version.setChoices(List.of(v)));
        version.getField().setOutputMarkupId(true);
        add(version.addRequiredLabel().setOutputMarkupId(true));

        bundleName.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                connectorName.setEnabled(true);

                List<Pair<String, String>> connectors = bundles.stream().
                        filter(bundle -> bundle.getBundleName().equals(connInstanceTO.getBundleName())).
                        map(bundle -> Pair.of(bundle.getConnectorName(), bundle.getVersion())).
                    toList();
                if (connectors.size() == 1) {
                    Pair<String, String> entry = connectors.getFirst();

                    connInstanceTO.setConnectorName(entry.getLeft());
                    connectorName.getField().setModelObject(entry.getLeft());
                    connectorName.setChoices(List.of(entry.getLeft()));

                    connInstanceTO.setVersion(entry.getRight());
                    version.getField().setModelObject(entry.getRight());
                    version.setChoices(List.of(entry.getRight()));
                } else {
                    connectorName.setChoices(connectors.stream().
                            map(Pair::getLeft).distinct().sorted().collect(Collectors.toList()));

                    List<String> versions = connectors.stream().
                            map(Pair::getRight).distinct().sorted().collect(Collectors.toList());
                    version.setChoices(versions);

                    if (versions.size() == 1) {
                        connInstanceTO.setVersion(versions.getFirst());
                        version.getField().setModelObject(versions.getFirst());
                    } else {
                        connInstanceTO.setVersion(null);
                        version.getField().setModelObject(null);
                    }
                }

                target.add(version);
                target.add(connectorName);
            }
        });

        connectorName.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                List<String> versions = bundles.stream().
                        filter(bundle -> bundle.getBundleName().equals(connInstanceTO.getBundleName())
                        && bundle.getConnectorName().equals(connInstanceTO.getConnectorName())).
                        map(ConnIdBundle::getVersion).collect(Collectors.toList());
                if (versions.size() == 1) {
                    connInstanceTO.setVersion(versions.getFirst());
                    version.getField().setModelObject(versions.getFirst());
                }
                version.setChoices(versions);

                target.add(version);
            }
        });

        if (connInstanceTO.getPoolConf() == null) {
            connInstanceTO.setPoolConf(new ConnPoolConf());
        }

        add(new AjaxNumberFieldPanel.Builder<Integer>().min(0).max(Integer.MAX_VALUE).build(
                "connRequestTimeout", "connRequestTimeout", Integer.class,
                new PropertyModel<>(connInstanceTO, "connRequestTimeout")));

        add(new AjaxNumberFieldPanel.Builder<Integer>().min(0).max(Integer.MAX_VALUE).build(
                "poolMaxObjects", "poolMaxObjects", Integer.class,
                new PropertyModel<>(connInstanceTO.getPoolConf(), "maxObjects")));

        add(new AjaxNumberFieldPanel.Builder<Integer>().min(0).max(Integer.MAX_VALUE).build(
                "poolMinIdle", "poolMinIdle", Integer.class,
                new PropertyModel<>(connInstanceTO.getPoolConf(), "minIdle")));

        add(new AjaxNumberFieldPanel.Builder<Integer>().min(0).max(Integer.MAX_VALUE).build(
                "poolMaxIdle", "poolMaxIdle", Integer.class,
                new PropertyModel<>(connInstanceTO.getPoolConf(), "maxIdle")));

        add(new AjaxNumberFieldPanel.Builder<Long>().min(0L).max(Long.MAX_VALUE).build(
                "poolMaxWait", "poolMaxWait", Long.class,
                new PropertyModel<>(connInstanceTO.getPoolConf(), "maxWait")));

        add(new AjaxNumberFieldPanel.Builder<Long>().min(0L).max(Long.MAX_VALUE).build(
                "poolMinEvictableIdleTime", "poolMinEvictableIdleTime", Long.class,
                new PropertyModel<>(connInstanceTO.getPoolConf(), "minEvictableIdleTimeMillis")));
    }
}
