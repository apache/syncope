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
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.RealmsUtils;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.console.rest.RealmRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxSearchFieldPanel;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxSpinnerFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.to.ConnBundleTO;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.ConnPoolConfTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.PropertyModel;

public class ConnectorDetailsPanel extends WizardStep {

    private static final long serialVersionUID = -2435937897614232137L;

    public ConnectorDetailsPanel(final ConnInstanceTO connInstanceTO, final List<ConnBundleTO> bundles) {
        super();
        setOutputMarkupId(true);

        boolean isSearchEnabled = RealmsUtils.isSearchEnabled();

        final AutoCompleteSettings settings = new AutoCompleteSettings();
        settings.setShowCompleteListOnFocusGain(!isSearchEnabled);
        settings.setShowListOnEmptyInput(!isSearchEnabled);

        AjaxSearchFieldPanel realm = new AjaxSearchFieldPanel(
                "adminRealm", "adminRealm", new PropertyModel<>(connInstanceTO, "adminRealm"), settings) {

            private static final long serialVersionUID = -6390474600233486704L;

            @Override
            protected Iterator<String> getChoices(final String input) {
                return (isSearchEnabled
                        ? RealmRestClient.search(RealmsUtils.buildQuery(input)).getResult()
                        : RealmRestClient.list()).
                        stream().filter(realm -> SyncopeConsoleSession.get().getAuthRealms().stream().anyMatch(
                                authRealm -> realm.getFullPath().startsWith(authRealm))).
                        map(RealmTO::getFullPath).collect(Collectors.toList()).iterator();
            }
        };

        realm.setOutputMarkupId(true);
        realm.addRequiredLabel();
        add(realm);

        AjaxTextFieldPanel displayName = new AjaxTextFieldPanel(
                "displayName", "displayName", new PropertyModel<>(connInstanceTO, "displayName"), false);
        displayName.setOutputMarkupId(true);
        displayName.addRequiredLabel();
        add(displayName);

        final AjaxDropDownChoicePanel<String> bundleName = new AjaxDropDownChoicePanel<>(
                "bundleName",
                "bundleName",
                new PropertyModel<>(connInstanceTO, "bundleName"), false);

        if (StringUtils.isNotBlank(connInstanceTO.getLocation())) {
            AjaxTextFieldPanel location = new AjaxTextFieldPanel(
                    "location", "location", new PropertyModel<>(connInstanceTO, "location"), false);
            location.addRequiredLabel();
            location.setOutputMarkupId(true);
            location.setEnabled(false);
            add(location);
        } else {
            final AjaxDropDownChoicePanel<String> location = new AjaxDropDownChoicePanel<>(
                    "location", "location", new PropertyModel<>(connInstanceTO, "location"), false);
            location.setChoices(new ArrayList<>(SyncopeConsoleSession.get().getPlatformInfo().getConnIdLocations()));
            location.addRequiredLabel();
            location.setOutputMarkupId(true);
            location.getField().setOutputMarkupId(true);
            add(location);

            location.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -5609231641453245929L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    ((DropDownChoice<String>) location.getField()).setNullValid(false);
                    bundleName.setEnabled(true);

                    List<ConnBundleTO> bundles = ConnectorRestClient.getAllBundles().stream().
                            filter(object -> object.getLocation().equals(connInstanceTO.getLocation())).
                            collect(Collectors.toList());

                    List<String> listBundles = getBundles(connInstanceTO, bundles);
                    if (listBundles.size() == 1) {
                        connInstanceTO.setBundleName(listBundles.get(0));
                        bundleName.getField().setModelObject(listBundles.get(0));
                    }
                    bundleName.setChoices(listBundles);

                    target.add(bundleName);
                }
            });
        }

        ((DropDownChoice<String>) bundleName.getField()).setNullValid(true);

        List<String> bundleNames = new ArrayList<>();
        bundles.stream().
                filter(bundle -> (!bundleNames.contains(bundle.getBundleName()))).
                forEachOrdered(bundle -> bundleNames.add(bundle.getBundleName()));

        bundleName.setChoices(bundleNames);
        bundleName.addRequiredLabel();
        bundleName.setOutputMarkupId(true);
        bundleName.setEnabled(connInstanceTO.getKey() == null);
        bundleName.getField().setOutputMarkupId(true);
        add(bundleName);

        final AjaxDropDownChoicePanel<String> version = new AjaxDropDownChoicePanel<>(
                "version", "version", new PropertyModel<>(connInstanceTO, "version"), false);
        version.setChoices(getVersions(connInstanceTO, bundles));
        version.addRequiredLabel();
        version.setEnabled(connInstanceTO.getBundleName() != null);
        version.setOutputMarkupId(true);
        version.getField().setOutputMarkupId(true);
        add(version);

        bundleName.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                ((DropDownChoice<String>) bundleName.getField()).setNullValid(false);
                version.setEnabled(true);

                List<String> versions;
                if (bundles.isEmpty()) {
                    List<ConnBundleTO> bundles = ConnectorRestClient.getAllBundles().stream().
                            filter(object -> object.getLocation().equals(connInstanceTO.getLocation())).
                            collect(Collectors.toList());
                    versions = getVersions(connInstanceTO, bundles);
                } else {
                    versions = getVersions(connInstanceTO, bundles);
                }
                if (versions.size() == 1) {
                    connInstanceTO.setVersion(versions.get(0));
                    version.getField().setModelObject(versions.get(0));
                }
                version.setChoices(versions);

                target.add(version);
            }
        });

        if (connInstanceTO.getPoolConf() == null) {
            connInstanceTO.setPoolConf(new ConnPoolConfTO());
        }

        add(new AjaxSpinnerFieldPanel.Builder<Integer>().min(0).max(Integer.MAX_VALUE).build(
                "connRequestTimeout", "connRequestTimeout", Integer.class,
                new PropertyModel<>(connInstanceTO, "connRequestTimeout")));

        add(new AjaxSpinnerFieldPanel.Builder<Integer>().min(0).max(Integer.MAX_VALUE).build(
                "poolMaxObjects", "poolMaxObjects", Integer.class,
                new PropertyModel<>(connInstanceTO.getPoolConf(), "maxObjects")));

        add(new AjaxSpinnerFieldPanel.Builder<Integer>().min(0).max(Integer.MAX_VALUE).build(
                "poolMinIdle", "poolMinIdle", Integer.class,
                new PropertyModel<>(connInstanceTO.getPoolConf(), "minIdle")));

        add(new AjaxSpinnerFieldPanel.Builder<Integer>().min(0).max(Integer.MAX_VALUE).build(
                "poolMaxIdle", "poolMaxIdle", Integer.class,
                new PropertyModel<>(connInstanceTO.getPoolConf(), "maxIdle")));

        add(new AjaxSpinnerFieldPanel.Builder<Long>().min(0L).max(Long.MAX_VALUE).build(
                "poolMaxWait", "poolMaxWait", Long.class,
                new PropertyModel<>(connInstanceTO.getPoolConf(), "maxWait")));

        add(new AjaxSpinnerFieldPanel.Builder<Long>().min(0L).max(Long.MAX_VALUE).build(
                "poolMinEvictableIdleTime", "poolMinEvictableIdleTime", Long.class,
                new PropertyModel<>(connInstanceTO.getPoolConf(), "minEvictableIdleTimeMillis")));
    }

    private static List<String> getVersions(final ConnInstanceTO connInstanceTO, final List<ConnBundleTO> bundles) {
        return bundles.stream().filter(object -> object.getLocation().equals(connInstanceTO.getLocation())
                        && object.getBundleName().equals(connInstanceTO.getBundleName())).
                map(ConnBundleTO::getVersion).collect(Collectors.toList());
    }

    private List<String> getBundles(final ConnInstanceTO connInstanceTO, final List<ConnBundleTO> bundles) {
        return bundles.stream().filter(object -> object.getLocation().equals(connInstanceTO.getLocation())).
                map(ConnBundleTO::getBundleName).collect(Collectors.toList());
    }
}
