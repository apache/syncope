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
package org.apache.syncope.client.console.reports;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.panels.BeanPanel;
import org.apache.syncope.client.console.panels.search.SearchUtils;
import org.apache.syncope.client.console.rest.ReportRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wizards.AjaxWizardBuilder;
import org.apache.syncope.common.lib.report.AbstractReportletConf;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.PropertyModel;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.apache.wicket.model.LoadableDetachableModel;

public class ReportletWizardBuilder extends AjaxWizardBuilder<ReportletDirectoryPanel.ReportletWrapper> {

    private static final long serialVersionUID = 5945391813567245081L;

    private final ReportRestClient restClient = new ReportRestClient();

    private final String report;

    public ReportletWizardBuilder(
            final String report,
            final ReportletDirectoryPanel.ReportletWrapper reportlet,
            final PageReference pageRef) {
        super(reportlet, pageRef);
        this.report = report;
    }

    @Override
    protected Serializable onApplyInternal(final ReportletDirectoryPanel.ReportletWrapper modelObject) {
        modelObject.getConf().setName(modelObject.getName());

        final ReportTO reportTO = restClient.read(report);

        if (modelObject.isNew()) {
            reportTO.getReportletConfs().add(modelObject.getConf());
        } else {
            reportTO.getReportletConfs().removeAll(
                    reportTO.getReportletConfs().stream().
                            filter(object -> object.getName().equals(modelObject.getOldName())).
                            collect(Collectors.toList()));
            reportTO.getReportletConfs().add(modelObject.getConf());
        }

        BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(modelObject.getConf());
        modelObject.getSCondWrapper().entrySet().forEach(entry -> {
            wrapper.setPropertyValue(entry.getKey(),
                    SearchUtils.buildFIQL(entry.getValue().getRight(), entry.getValue().getLeft()));
        });

        restClient.update(reportTO);
        return modelObject;
    }

    @Override
    protected WizardModel buildModelSteps(
            final ReportletDirectoryPanel.ReportletWrapper modelObject, final WizardModel wizardModel) {
        wizardModel.add(new Profile(modelObject));
        wizardModel.add(new Configuration(modelObject));
        return wizardModel;
    }

    public class Profile extends WizardStep {

        private static final long serialVersionUID = -3043839139187792810L;

        public Profile(final ReportletDirectoryPanel.ReportletWrapper reportlet) {

            final AjaxTextFieldPanel name = new AjaxTextFieldPanel(
                    "name", "reportlet", new PropertyModel<>(reportlet, "name"), false);
            name.addRequiredLabel();
            name.setEnabled(true);
            add(name);

            final AjaxDropDownChoicePanel<String> conf = new AjaxDropDownChoicePanel<>(
                    "configuration", getString("configuration"), new PropertyModel<String>(reportlet, "conf") {

                private static final long serialVersionUID = -6427731218492117883L;

                @Override
                public String getObject() {
                    return reportlet.getConf() == null ? null : reportlet.getConf().getClass().getName();
                }

                @Override
                public void setObject(final String object) {
                    AbstractReportletConf conf = null;

                    try {
                        conf = AbstractReportletConf.class.cast(Class.forName(object).newInstance());
                    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                        LOG.warn("Error retrieving reportlet configuration instance", e);
                    }

                    reportlet.setConf(conf);
                }
            });

            conf.setChoices(new ArrayList<>(SyncopeConsoleSession.get().getPlatformInfo().getReportletConfs()));

            conf.addRequiredLabel();
            add(conf);
        }
    }

    public class Configuration extends WizardStep {

        private static final long serialVersionUID = -785981096328637758L;

        public Configuration(final ReportletDirectoryPanel.ReportletWrapper reportlet) {
            final LoadableDetachableModel<Serializable> bean = new LoadableDetachableModel<Serializable>() {

                private static final long serialVersionUID = 2092144708018739371L;

                @Override
                protected Serializable load() {
                    return reportlet.getConf();
                }
            };

            add(new BeanPanel<>("bean", bean, reportlet.getSCondWrapper(), "name", "reportletClassName").
                    setRenderBodyOnly(true));
        }
    }
}
