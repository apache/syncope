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
package org.apache.syncope.client.console.wizards;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.panels.BeanPanel;
import org.apache.syncope.client.console.rest.AuthModuleRestClient;
import org.apache.syncope.client.console.wizards.mapping.AuthModuleMappingPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.auth.AuthModuleConf;
import org.apache.syncope.common.lib.to.AuthModuleTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.springframework.util.ClassUtils;

public class AuthModuleWizardBuilder extends BaseAjaxWizardBuilder<AuthModuleTO> {

    private static final long serialVersionUID = 1L;

    private final LoadableDetachableModel<List<String>> authModuleConfs;

    public AuthModuleWizardBuilder(final AuthModuleTO defaultItem, final PageReference pageRef) {

        super(defaultItem, pageRef);

        authModuleConfs = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return SyncopeWebApplication.get().getLookup().getClasses(AuthModuleConf.class).stream().
                    map(Class::getName).sorted().collect(Collectors.toList());
            }
        };
    }

    @Override
    protected Serializable onApplyInternal(final AuthModuleTO modelObject) {
        if (mode == AjaxWizard.Mode.CREATE) {
            AuthModuleRestClient.create(modelObject);
        } else {
            AuthModuleRestClient.update(modelObject);
        }
        return modelObject;
    }

    @Override
    protected WizardModel buildModelSteps(final AuthModuleTO modelObject, final WizardModel wizardModel) {
        wizardModel.add(new Profile(modelObject, authModuleConfs));
        wizardModel.add(new Configuration(modelObject));
        wizardModel.add(new Mapping(modelObject));
        return wizardModel;
    }

    public static class Profile extends WizardStep {

        private static final long serialVersionUID = -3043839139187792810L;

        Profile(final AuthModuleTO authModule, final LoadableDetachableModel<List<String>> authModuleConfs) {
            boolean isNew = authModule.getConf() == null;

            AjaxTextFieldPanel key = new AjaxTextFieldPanel(
                    Constants.KEY_FIELD_NAME, Constants.KEY_FIELD_NAME,
                    new PropertyModel<>(authModule, Constants.KEY_FIELD_NAME));
            key.addRequiredLabel();
            key.setEnabled(isNew);
            add(key);

            AjaxTextFieldPanel description = new AjaxTextFieldPanel(
                    Constants.DESCRIPTION_FIELD_NAME, getString(Constants.DESCRIPTION_FIELD_NAME),
                    new PropertyModel<>(authModule, Constants.DESCRIPTION_FIELD_NAME));
            add(description);

            AjaxDropDownChoicePanel<String> conf = new AjaxDropDownChoicePanel<>("conf", getString("type"), isNew
                    ? Model.of()
                    : Model.of(authModule.getConf().getClass().getName()));
            conf.setChoices(authModuleConfs.getObject());
            conf.addRequiredLabel();
            conf.setNullValid(false);
            conf.setEnabled(isNew);
            conf.add(new AjaxEventBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -7133385027739964990L;

                @SuppressWarnings("unchecked")
                @Override
                protected void onEvent(final AjaxRequestTarget target) {
                    try {
                        Class<? extends AuthModuleConf> authModuleConfClass =
                                (Class<? extends AuthModuleConf>) ClassUtils.resolveClassName(
                                        conf.getModelObject(), ClassUtils.getDefaultClassLoader());

                        authModule.setConf(authModuleConfClass.getConstructor().newInstance());
                    } catch (Exception e) {
                        LOG.error("During deserialization", e);
                    }
                }
            });
            add(conf);
        }
    }

    private static class Configuration extends WizardStep {

        private static final long serialVersionUID = -785981096328637758L;

        Configuration(final AuthModuleTO authModule) {
            add(new BeanPanel<>("bean", new PropertyModel<>(authModule, "conf")).setRenderBodyOnly(true));
        }
    }

    private static final class Mapping extends WizardStep {

        private static final long serialVersionUID = 3454904947720856253L;

        Mapping(final AuthModuleTO authModule) {
            setTitleModel(Model.of("Mapping"));
            setSummaryModel(Model.of(StringUtils.EMPTY));
            add(new AuthModuleMappingPanel("mapping", authModule));
        }
    }
}
