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
import org.apache.syncope.client.console.rest.AttrRepoRestClient;
import org.apache.syncope.client.console.wizards.mapping.AttrRepoMappingPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxNumberFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.attr.AttrRepoConf;
import org.apache.syncope.common.lib.to.AttrRepoTO;
import org.apache.syncope.common.lib.types.AttrRepoState;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.springframework.util.ClassUtils;

public class AttrRepoWizardBuilder extends BaseAjaxWizardBuilder<AttrRepoTO> {

    private static final long serialVersionUID = -6163230263062920394L;

    protected final LoadableDetachableModel<List<String>> attrRepoConfs;

    protected final AttrRepoRestClient attrRepoRestClient;

    protected final Model<Class<? extends AttrRepoConf>> attrRepoConfClass = Model.of();

    public AttrRepoWizardBuilder(
            final AttrRepoTO defaultItem,
            final AttrRepoRestClient attrRepoRestClient,
            final PageReference pageRef) {

        super(defaultItem, pageRef);

        attrRepoConfs = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return SyncopeWebApplication.get().getLookup().getClasses(AttrRepoConf.class).stream().
                        map(Class::getName).sorted().collect(Collectors.toList());
            }
        };
        this.attrRepoRestClient = attrRepoRestClient;
    }

    @Override
    protected Serializable onApplyInternal(final AttrRepoTO modelObject) {
        if (mode == AjaxWizard.Mode.CREATE) {
            attrRepoRestClient.create(modelObject);
        } else {
            attrRepoRestClient.update(modelObject);
        }
        return modelObject;
    }

    @Override
    protected WizardModel buildModelSteps(final AttrRepoTO modelObject, final WizardModel wizardModel) {
        wizardModel.add(new Profile(modelObject, attrRepoConfs, attrRepoConfClass));
        wizardModel.add(new Configuration(modelObject));
        wizardModel.add(new Mapping(modelObject));
        return wizardModel;
    }

    protected static class Profile extends WizardStep {

        private static final long serialVersionUID = -3043839139187792810L;

        Profile(
                final AttrRepoTO attrRepo,
                final LoadableDetachableModel<List<String>> attrRepoConfs,
                final Model<Class<? extends AttrRepoConf>> attrRepoConfClass) {

            boolean isNew = attrRepo.getConf() == null;
            if (!isNew) {
                attrRepoConfClass.setObject(attrRepo.getConf().getClass());
            }

            AjaxTextFieldPanel key = new AjaxTextFieldPanel(
                    Constants.KEY_FIELD_NAME, Constants.KEY_FIELD_NAME,
                    new PropertyModel<>(attrRepo, Constants.KEY_FIELD_NAME));
            key.addRequiredLabel();
            key.setEnabled(isNew);
            add(key);

            AjaxTextFieldPanel description = new AjaxTextFieldPanel(
                    Constants.DESCRIPTION_FIELD_NAME, getString(Constants.DESCRIPTION_FIELD_NAME),
                    new PropertyModel<>(attrRepo, Constants.DESCRIPTION_FIELD_NAME));
            add(description);

            AjaxDropDownChoicePanel<AttrRepoState> state = new AjaxDropDownChoicePanel<>(
                    "state", getString("state"), new PropertyModel<>(attrRepo, "state"));
            state.setChoices(List.of(AttrRepoState.values()));
            state.addRequiredLabel();
            state.setNullValid(false);
            add(state);

            add(new AjaxNumberFieldPanel.Builder<Integer>().build(
                    "order",
                    "order",
                    Integer.class,
                    new PropertyModel<>(attrRepo, "order")).addRequiredLabel());

            AjaxDropDownChoicePanel<String> conf = new AjaxDropDownChoicePanel<>("conf", getString("type"), isNew
                    ? Model.of()
                    : Model.of(attrRepo.getConf().getClass().getName()));
            conf.setChoices(attrRepoConfs.getObject());
            conf.addRequiredLabel();
            conf.setNullValid(false);
            conf.setEnabled(isNew);
            conf.add(new AjaxEventBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -7133385027739964990L;

                @SuppressWarnings("unchecked")
                @Override
                protected void onEvent(final AjaxRequestTarget target) {
                    try {
                        Class<? extends AttrRepoConf> clazz =
                                (Class<? extends AttrRepoConf>) ClassUtils.resolveClassName(
                                        conf.getModelObject(), ClassUtils.getDefaultClassLoader());

                        attrRepo.setConf(clazz.getConstructor().newInstance());
                        attrRepoConfClass.setObject(clazz);
                    } catch (Exception e) {
                        LOG.error("During deserialization", e);
                    }
                }
            });
            add(conf);
        }
    }

    protected class Configuration extends WizardStep {

        private static final long serialVersionUID = -785981096328637758L;

        Configuration(final AttrRepoTO attrRepo) {
            add(new BeanPanel<>("bean", new PropertyModel<>(attrRepo, "conf"), pageRef).setRenderBodyOnly(true));
        }
    }

    protected static final class Mapping extends WizardStep {

        private static final long serialVersionUID = 3454904947720856253L;

        Mapping(final AttrRepoTO attrRepo) {
            setTitleModel(Model.of("Mapping"));
            setSummaryModel(Model.of(StringUtils.EMPTY));
            add(new AttrRepoMappingPanel("mapping", attrRepo));
        }
    }
}
