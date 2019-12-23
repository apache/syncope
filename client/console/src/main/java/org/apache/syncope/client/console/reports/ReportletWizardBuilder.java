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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Serializable;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.panels.BeanPanel;
import org.apache.syncope.client.console.rest.ImplementationRestClient;
import org.apache.syncope.client.console.rest.ReportRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wizards.AjaxWizardBuilder;
import org.apache.syncope.common.lib.report.ReportletConf;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.ImplementationType;
import org.apache.wicket.PageReference;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;

public class ReportletWizardBuilder extends AjaxWizardBuilder<ReportletWrapper> {

    private static final long serialVersionUID = 5945391813567245081L;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ImplementationRestClient implementationClient = new ImplementationRestClient();

    private final ReportRestClient restClient = new ReportRestClient();

    private final String report;

    public ReportletWizardBuilder(
            final String report,
            final ReportletWrapper reportlet,
            final PageReference pageRef) {
        super(reportlet, pageRef);
        this.report = report;
    }

    @Override
    protected Serializable onApplyInternal(final ReportletWrapper modelObject) {
        if (modelObject.getImplementationEngine() == ImplementationEngine.JAVA) {
            ImplementationTO reportlet = implementationClient.read(
                    ImplementationType.REPORTLET, modelObject.getImplementationKey());
            try {
                reportlet.setBody(MAPPER.writeValueAsString(modelObject.getConf()));
                implementationClient.update(reportlet);
            } catch (Exception e) {
                throw new WicketRuntimeException(e);
            }
        }

        ReportTO reportTO = restClient.read(report);
        if (modelObject.isNew()) {
            reportTO.getReportlets().add(modelObject.getImplementationKey());
        }

        restClient.update(reportTO);
        return modelObject;
    }

    @Override
    protected WizardModel buildModelSteps(
            final ReportletWrapper modelObject, final WizardModel wizardModel) {
        wizardModel.add(new Profile(modelObject));
        wizardModel.add(new Configuration(modelObject));
        return wizardModel;
    }

    public class Profile extends WizardStep {

        private static final long serialVersionUID = -3043839139187792810L;

        public Profile(final ReportletWrapper reportlet) {
            final AjaxDropDownChoicePanel<String> conf = new AjaxDropDownChoicePanel<>(
                    "reportlet", getString("reportlet"), new PropertyModel<String>(reportlet, "implementationKey"));

            conf.setChoices(implementationClient.list(ImplementationType.REPORTLET).stream().
                    map(EntityTO::getKey).sorted().collect(Collectors.toList()));
            conf.addRequiredLabel();
            conf.setNullValid(false);
            conf.setEnabled(reportlet.isNew());
            conf.add(new AjaxEventBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -7133385027739964990L;

                @Override
                protected void onEvent(final AjaxRequestTarget target) {
                    ImplementationTO impl = implementationClient.read(
                            ImplementationType.REPORTLET, conf.getModelObject());
                    reportlet.setImplementationEngine(impl.getEngine());
                    if (impl.getEngine() == ImplementationEngine.JAVA) {
                        try {
                            ReportletConf conf = MAPPER.readValue(impl.getBody(), ReportletConf.class);
                            reportlet.setConf(conf);
                        } catch (Exception e) {
                            LOG.error("During deserialization", e);
                        }
                    }
                }
            });
            add(conf);
        }
    }

    public class Configuration extends WizardStep {

        private static final long serialVersionUID = -785981096328637758L;

        public Configuration(final ReportletWrapper reportlet) {
            final LoadableDetachableModel<Serializable> bean = new LoadableDetachableModel<Serializable>() {

                private static final long serialVersionUID = 2092144708018739371L;

                @Override
                protected Serializable load() {
                    return reportlet.getConf();
                }
            };
            add(new BeanPanel<>("bean", bean, reportlet.getSCondWrapper(), "name", "reportlet").
                    setRenderBodyOnly(true));
        }
    }
}
