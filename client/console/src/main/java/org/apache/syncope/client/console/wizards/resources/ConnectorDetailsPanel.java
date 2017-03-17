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
import java.util.HashSet;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.Transformer;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.wicket.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxSpinnerFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.to.ConnBundleTO;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.ConnPoolConfTO;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.PropertyModel;

public class ConnectorDetailsPanel extends WizardStep {

    private static final long serialVersionUID = -2435937897614232137L;

    public ConnectorDetailsPanel(final ConnInstanceTO connInstanceTO, final List<ConnBundleTO> bundles) {
        super();
        setOutputMarkupId(true);

        AjaxTextFieldPanel displayName = new AjaxTextFieldPanel(
                "displayName", "displayName", new PropertyModel<String>(connInstanceTO, "displayName"), false);
        displayName.setOutputMarkupId(true);
        displayName.addRequiredLabel();
        add(displayName);

        AjaxTextFieldPanel location = new AjaxTextFieldPanel(
                "location", "location", new PropertyModel<String>(connInstanceTO, "location"), false);
        location.addRequiredLabel();
        location.setOutputMarkupId(true);
        location.setEnabled(false);
        add(location);

        final AjaxDropDownChoicePanel<String> bundleName = new AjaxDropDownChoicePanel<>(
                "bundleName",
                "bundleName",
                new PropertyModel<String>(connInstanceTO, "bundleName"), false);
        ((DropDownChoice<String>) bundleName.getField()).setNullValid(true);

        List<String> bundleNames = new ArrayList<String>();
        for (ConnBundleTO bundle : bundles) {
            if (!bundleNames.contains(bundle.getBundleName())) {
                bundleNames.add(bundle.getBundleName());
            }
        }

        bundleName.setChoices(bundleNames);
        bundleName.addRequiredLabel();
        bundleName.setOutputMarkupId(true);
        bundleName.setEnabled(connInstanceTO.getKey() == null);
        bundleName.getField().setOutputMarkupId(true);
        add(bundleName);

        final AjaxDropDownChoicePanel<String> version = new AjaxDropDownChoicePanel<>(
                "version", "version", new PropertyModel<String>(connInstanceTO, "version"), false);
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

                List<String> versions = getVersions(connInstanceTO, bundles);
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
                new PropertyModel<Integer>(connInstanceTO, "connRequestTimeout")));

        add(new AjaxSpinnerFieldPanel.Builder<Integer>().min(0).max(Integer.MAX_VALUE).build(
                "poolMaxObjects", "poolMaxObjects", Integer.class,
                new PropertyModel<Integer>(connInstanceTO.getPoolConf(), "maxObjects")));

        add(new AjaxSpinnerFieldPanel.Builder<Integer>().min(0).max(Integer.MAX_VALUE).build(
                "poolMinIdle", "poolMinIdle", Integer.class,
                new PropertyModel<Integer>(connInstanceTO.getPoolConf(), "minIdle")));

        add(new AjaxSpinnerFieldPanel.Builder<Integer>().min(0).max(Integer.MAX_VALUE).build(
                "poolMaxIdle", "poolMaxIdle", Integer.class,
                new PropertyModel<Integer>(connInstanceTO.getPoolConf(), "maxIdle")));

        add(new AjaxSpinnerFieldPanel.Builder<Long>().min(0L).max(Long.MAX_VALUE).build(
                "poolMaxWait", "poolMaxWait", Long.class,
                new PropertyModel<Long>(connInstanceTO.getPoolConf(), "maxWait")));

        add(new AjaxSpinnerFieldPanel.Builder<Long>().min(0L).max(Long.MAX_VALUE).build(
                "poolMinEvictableIdleTime", "poolMinEvictableIdleTime", Long.class,
                new PropertyModel<Long>(connInstanceTO.getPoolConf(), "minEvictableIdleTimeMillis")));
    }

    private List<String> getVersions(final ConnInstanceTO connInstanceTO, final List<ConnBundleTO> bundles) {
        return new ArrayList<>(CollectionUtils.collect(
                CollectionUtils.select(bundles, new Predicate<ConnBundleTO>() {

                    @Override
                    public boolean evaluate(final ConnBundleTO object) {
                        return object.getLocation().equals(connInstanceTO.getLocation())
                                && object.getBundleName().equals(connInstanceTO.getBundleName());
                    }
                }), new Transformer<ConnBundleTO, String>() {

            @Override
            public String transform(final ConnBundleTO input) {
                return input.getVersion();
            }
        }, new HashSet<String>()));
    }
}
