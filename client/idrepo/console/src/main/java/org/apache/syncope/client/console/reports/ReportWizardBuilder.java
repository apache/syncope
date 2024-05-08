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

import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.panels.BeanPanel;
import org.apache.syncope.client.console.panels.search.SearchUtils;
import org.apache.syncope.client.console.rest.ImplementationRestClient;
import org.apache.syncope.client.console.rest.ReportRestClient;
import org.apache.syncope.client.console.tasks.CrontabPanel;
import org.apache.syncope.client.console.wizards.BaseAjaxWizardBuilder;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.MIMETypesLoader;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.report.ReportConf;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.wicket.PageReference;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

public class ReportWizardBuilder extends BaseAjaxWizardBuilder<ReportTO> {

    private static final long serialVersionUID = 5945391813567245081L;

    protected static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    protected final ImplementationRestClient implementationRestClient;

    protected final ReportRestClient reportRestClient;

    protected final MIMETypesLoader mimeTypesLoader;

    protected final Model<ReportConfWrapper> conf = new Model<>();

    protected CrontabPanel crontabPanel;

    public ReportWizardBuilder(
            final ReportTO reportTO,
            final ImplementationRestClient implementationRestClient,
            final ReportRestClient reportRestClient,
            final MIMETypesLoader mimeTypesLoader,
            final PageReference pageRef) {

        super(reportTO, pageRef);

        this.implementationRestClient = implementationRestClient;
        this.reportRestClient = reportRestClient;
        this.mimeTypesLoader = mimeTypesLoader;
    }

    @Override
    protected Serializable onApplyInternal(final ReportTO modelObject) {
        if (conf.getObject() != null) {
            try {
                ImplementationTO implementation = implementationRestClient.read(
                        IdRepoImplementationType.REPORT_DELEGATE, modelObject.getJobDelegate());
                if (implementation.getEngine() == ImplementationEngine.JAVA) {
                    BeanWrapper confWrapper = PropertyAccessorFactory.forBeanPropertyAccess(conf.getObject().getConf());
                    conf.getObject().getSCondWrapper().forEach((fieldName, pair) -> confWrapper.setPropertyValue(
                            fieldName, SearchUtils.buildFIQL(pair.getRight(), pair.getLeft())));

                    implementation.setBody(MAPPER.writeValueAsString(conf.getObject().getConf()));
                    implementationRestClient.update(implementation);
                }
            } catch (Exception e) {
                throw new WicketRuntimeException(e);
            }
        }

        modelObject.setCronExpression(crontabPanel.getCronExpression());

        if (modelObject.getKey() == null) {
            reportRestClient.create(modelObject);
        } else {
            reportRestClient.update(modelObject);
        }

        return modelObject;
    }

    protected void setConf(final String jobDelegate) {
        try {
            ImplementationTO implementation = implementationRestClient.read(
                    IdRepoImplementationType.REPORT_DELEGATE, jobDelegate);
            if (implementation.getEngine() == ImplementationEngine.JAVA) {
                conf.setObject(new ReportConfWrapper());
                conf.getObject().setConf(MAPPER.readValue(implementation.getBody(), ReportConf.class));
            } else {
                conf.setObject(null);
            }
        } catch (Exception e) {
            LOG.error("Could not read or parse {}", jobDelegate, e);
        }
    }

    @Override
    protected WizardModel buildModelSteps(final ReportTO modelObject, final WizardModel wizardModel) {
        Optional.ofNullable(modelObject.getJobDelegate()).ifPresent(this::setConf);

        Configuration configuration = new Configuration();
        wizardModel.add(new Profile(modelObject, configuration));
        wizardModel.add(configuration);
        wizardModel.add(new Schedule(modelObject));
        return wizardModel;
    }

    protected class Profile extends WizardStep {

        private static final long serialVersionUID = -3043839139187792810L;

        private final IModel<List<String>> reportJobDelegates = SyncopeWebApplication.get().
                getImplementationInfoProvider().getReportJobDelegates();

        public Profile(final ReportTO modelObject, final Configuration configuration) {
            AjaxTextFieldPanel name = new AjaxTextFieldPanel(
                    Constants.NAME_FIELD_NAME, Constants.NAME_FIELD_NAME,
                    new PropertyModel<>(modelObject, Constants.NAME_FIELD_NAME), false);
            add(name.addRequiredLabel().setEnabled(true));

            AjaxCheckBoxPanel active = new AjaxCheckBoxPanel(
                    "active", "active", new PropertyModel<>(modelObject, "active"), false);
            add(active);

            AjaxTextFieldPanel mimeType = new AjaxTextFieldPanel(
                    "mimeType", "mimeType", new PropertyModel<>(modelObject, "mimeType"));
            mimeType.setChoices(mimeTypesLoader.getMimeTypes());
            add(mimeType.addRequiredLabel());

            AjaxTextFieldPanel fileExt = new AjaxTextFieldPanel(
                    "fileExt", "fileExt", new PropertyModel<>(modelObject, "fileExt"));
            add(fileExt.addRequiredLabel());
            mimeType.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -6139318907146065915L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    Optional.ofNullable(mimeTypesLoader.getFileExt(mimeType.getModelObject())).
                            ifPresent(fileExt::setModelObject);
                    target.add(fileExt);
                }
            });

            AjaxDropDownChoicePanel<String> jobDelegate = new AjaxDropDownChoicePanel<>(
                    "jobDelegate", "jobDelegate", new PropertyModel<>(modelObject, "jobDelegate"), false);
            jobDelegate.setChoices(reportJobDelegates.getObject());
            add(jobDelegate.addRequiredLabel().setEnabled(modelObject.getKey() == null));
            jobDelegate.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -6139318907146065915L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    setConf(jobDelegate.getModelObject());
                    configuration.update();
                }
            });
        }
    }

    protected class Configuration extends WizardStep implements WizardModel.ICondition {

        private static final long serialVersionUID = -785981096328637758L;

        public Configuration() {
            update();
        }

        protected void update() {
            if (conf.getObject() == null) {
                addOrReplace(new Label("bean", Model.of()));
            } else {
                addOrReplace(new BeanPanel<>(
                        "bean",
                        new PropertyModel<>(conf.getObject(), "conf"),
                        conf.getObject().getSCondWrapper(),
                        pageRef).
                        setRenderBodyOnly(true));
            }
        }

        @Override
        public boolean evaluate() {
            return conf.getObject() != null;
        }
    }

    protected class Schedule extends WizardStep {

        private static final long serialVersionUID = -785981096328637758L;

        public Schedule(final ReportTO modelObject) {
            crontabPanel = new CrontabPanel(
                    "schedule", new PropertyModel<>(modelObject, "cronExpression"), modelObject.getCronExpression());
            add(crontabPanel);
        }
    }
}
