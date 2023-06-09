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
package org.apache.syncope.client.console.clientapps;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.panels.AbstractModalPanel;
import org.apache.syncope.client.console.panels.BeanPanel;
import org.apache.syncope.client.console.rest.ClientAppRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.client.ui.commons.panels.WizardModalPanel;
import org.apache.syncope.client.ui.commons.wizards.AbstractModalPanelBuilder;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.clientapps.UsernameAttributeProviderConf;
import org.apache.syncope.common.lib.to.ClientAppTO;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.springframework.util.ClassUtils;

public class UsernameAttributeProviderModalPanelBuilder<T extends ClientAppTO> extends AbstractModalPanelBuilder<T> {

    private static final long serialVersionUID = -4106998301911573852L;

    protected final LoadableDetachableModel<List<String>> usernameAttributeProviderConfs =
            new LoadableDetachableModel<>() {

        private static final long serialVersionUID = 5275935387613157437L;

        @Override
        protected List<String> load() {
            return SyncopeWebApplication.get().getLookup().getClasses(UsernameAttributeProviderConf.class).stream().
                    map(Class::getName).sorted().collect(Collectors.toList());
        }
    };

    protected final BaseModal<T> modal;

    protected final ClientAppType type;

    protected final ClientAppRestClient clientAppRestClient;

    public UsernameAttributeProviderModalPanelBuilder(
            final ClientAppType type,
            final T defaultItem,
            final BaseModal<T> modal,
            final ClientAppRestClient clientAppRestClient,
            final PageReference pageRef) {

        super(defaultItem, pageRef);
        this.type = type;
        this.modal = modal;
        this.clientAppRestClient = clientAppRestClient;
    }

    @Override
    public WizardModalPanel<T> build(final String id, final int index, final AjaxWizard.Mode mode) {
        return new Profile(newModelObject(), modal, pageRef);
    }

    protected class Profile extends AbstractModalPanel<T> {

        private static final long serialVersionUID = 7647959917047450318L;

        protected final T clientAppTO;

        Profile(final T clientAppTO, final BaseModal<T> modal, final PageReference pageRef) {
            super(modal, pageRef);
            modal.setFormModel(clientAppTO);

            this.clientAppTO = clientAppTO;

            AjaxDropDownChoicePanel<String> conf = new AjaxDropDownChoicePanel<>(
                    "conf", "conf", new Model<>());
            Optional.ofNullable(clientAppTO.getUsernameAttributeProviderConf()).
                    ifPresent(uapc -> conf.setModelObject(uapc.getClass().getName()));
            conf.setChoices(usernameAttributeProviderConfs.getObject());
            conf.setNullValid(true);
            add(conf);

            PropertyModel<UsernameAttributeProviderConf> beanPanelModel =
                    new PropertyModel<>(clientAppTO, "usernameAttributeProviderConf");
            BeanPanel<UsernameAttributeProviderConf> bean = new BeanPanel<>("bean", beanPanelModel, pageRef);
            add(bean.setRenderBodyOnly(false));

            conf.add(new AjaxEventBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -7133385027739964990L;

                @SuppressWarnings("unchecked")
                @Override
                protected void onEvent(final AjaxRequestTarget target) {
                    if (conf.getModelObject() == null) {
                        beanPanelModel.setObject(null);
                    } else {
                        try {
                            Class<? extends UsernameAttributeProviderConf> clazz =
                                    (Class<? extends UsernameAttributeProviderConf>) ClassUtils.resolveClassName(
                                            conf.getModelObject(), ClassUtils.getDefaultClassLoader());

                            beanPanelModel.setObject(clazz.getConstructor().newInstance());
                        } catch (Exception e) {
                            LOG.error("Cannot instantiate {}", conf.getModelObject(), e);
                        }
                    }

                    target.add(bean);
                }
            });
        }

        @Override
        public void onSubmit(final AjaxRequestTarget target) {
            try {
                clientAppRestClient.update(type, clientAppTO);

                SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                UsernameAttributeProviderModalPanelBuilder.Profile.this.modal.close(target);
            } catch (Exception e) {
                LOG.error("While creating/updating clientApp", e);
                SyncopeConsoleSession.get().onException(e);
            }
            ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
        }
    }
}
