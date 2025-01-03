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
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxNumberFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.AbstractLDAPConf;
import org.apache.syncope.common.lib.auth.AuthModuleConf;
import org.apache.syncope.common.lib.auth.LDAPDependantAuthModuleConf;
import org.apache.syncope.common.lib.to.AuthModuleTO;
import org.apache.syncope.common.lib.types.AuthModuleState;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.springframework.util.ClassUtils;

public class AuthModuleWizardBuilder extends BaseAjaxWizardBuilder<AuthModuleTO> {

    private static final long serialVersionUID = -6163230263062920394L;

    protected final LoadableDetachableModel<List<String>> authModuleConfs = new LoadableDetachableModel<>() {

        private static final long serialVersionUID = 5275935387613157437L;

        @Override
        protected List<String> load() {
            return SyncopeWebApplication.get().getLookup().getClasses(AuthModuleConf.class).stream().
                    map(Class::getName).sorted().collect(Collectors.toList());
        }
    };

    protected final AuthModuleRestClient authModuleRestClient;

    protected final Model<Class<? extends AuthModuleConf>> authModuleConfClass = Model.of();

    public AuthModuleWizardBuilder(
            final AuthModuleTO defaultItem,
            final AuthModuleRestClient authModuleRestClient,
            final PageReference pageRef) {

        super(defaultItem, pageRef);
        this.authModuleRestClient = authModuleRestClient;
    }

    @Override
    protected Serializable onApplyInternal(final AuthModuleTO modelObject) {
        if (mode == AjaxWizard.Mode.CREATE) {
            authModuleRestClient.create(modelObject);
        } else {
            authModuleRestClient.update(modelObject);
        }
        return modelObject;
    }

    @Override
    protected WizardModel buildModelSteps(final AuthModuleTO modelObject, final WizardModel wizardModel) {
        wizardModel.add(new Profile(modelObject, authModuleConfs, authModuleConfClass));
        wizardModel.add(new Configuration(modelObject));
        wizardModel.add(new AuthModuleConfLDAP(modelObject, authModuleConfClass));
        wizardModel.add(new Mapping(modelObject));
        return wizardModel;
    }

    protected static class Profile extends WizardStep {

        private static final long serialVersionUID = -3043839139187792810L;

        Profile(
                final AuthModuleTO authModule,
                final LoadableDetachableModel<List<String>> authModuleConfs,
                final Model<Class<? extends AuthModuleConf>> authModuleConfClass) {

            boolean isNew = authModule.getConf() == null;
            if (!isNew) {
                authModuleConfClass.setObject(authModule.getConf().getClass());
            }

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

            AjaxDropDownChoicePanel<AuthModuleState> state = new AjaxDropDownChoicePanel<>(
                    "state", getString("state"), new PropertyModel<>(authModule, "state"));
            state.setChoices(List.of(AuthModuleState.values()));
            state.addRequiredLabel();
            state.setNullValid(false);
            add(state);

            add(new AjaxNumberFieldPanel.Builder<Integer>().build(
                    "order",
                    "order",
                    Integer.class,
                    new PropertyModel<>(authModule, "order")).addRequiredLabel());

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
                        Class<? extends AuthModuleConf> clazz =
                                (Class<? extends AuthModuleConf>) ClassUtils.resolveClassName(
                                        conf.getModelObject(), ClassUtils.getDefaultClassLoader());

                        authModule.setConf(clazz.getConstructor().newInstance());
                        authModuleConfClass.setObject(clazz);
                    } catch (Exception e) {
                        LOG.error("Cannot instantiate {}", conf.getModelObject(), e);
                    }
                }
            });
            add(conf);
        }
    }

    protected class Configuration extends WizardStep {

        private static final long serialVersionUID = -785981096328637758L;

        Configuration(final AuthModuleTO authModule) {
            add(new BeanPanel<>("bean", new PropertyModel<>(authModule, "conf"), pageRef, "ldap").
                    setRenderBodyOnly(true));
        }
    }

    protected class AuthModuleConfLDAP extends WizardStep implements WizardModel.ICondition {

        private static final long serialVersionUID = 5328049907748683944L;

        private final Model<Class<? extends AuthModuleConf>> authModuleConfClass;

        AuthModuleConfLDAP(
                final AuthModuleTO authModule,
                final Model<Class<? extends AuthModuleConf>> authModuleConfClass) {

            this.authModuleConfClass = authModuleConfClass;

            PropertyModel<AbstractLDAPConf> beanPanelModel = new PropertyModel<>(authModule, "conf.ldap");

            AjaxCheckBoxPanel enable = new AjaxCheckBoxPanel("enable", "enableLDAP", new IModel<Boolean>() {

                private static final long serialVersionUID = -7126718045816207110L;

                @Override
                public Boolean getObject() {
                    return beanPanelModel.getObject() != null;
                }

                @Override
                public void setObject(final Boolean object) {
                    // nothing to do
                }
            });
            enable.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    if (beanPanelModel.getObject() == null) {
                        try {
                            beanPanelModel.setObject((AbstractLDAPConf) authModuleConfClass.getObject().
                                    getMethod("ldapInstance", new Class<?>[] {}).invoke(authModule.getConf()));
                        } catch (Exception e) {
                            LOG.warn("Error instantiating beanPanel model object", e);
                        }
                    } else {
                        beanPanelModel.setObject(null);
                    }
                    target.add(AuthModuleConfLDAP.this);
                }
            });
            add(enable);

            add(new BeanPanel<>("bean", beanPanelModel, pageRef).setRenderBodyOnly(true));
            setOutputMarkupId(true);
        }

        @Override
        public boolean evaluate() {
            return LDAPDependantAuthModuleConf.class.isAssignableFrom(authModuleConfClass.getObject());
        }
    }

    protected static final class Mapping extends WizardStep {

        private static final long serialVersionUID = 3454904947720856253L;

        Mapping(final AuthModuleTO authModule) {
            setTitleModel(Model.of("Mapping"));
            setSummaryModel(Model.of(StringUtils.EMPTY));
            add(new AuthModuleMappingPanel("mapping", authModule));
        }
    }
}
