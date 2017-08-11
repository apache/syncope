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
package org.apache.syncope.client.console.panels;

import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.wicket.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.common.lib.AbstractBaseBean;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.log.LoggerTO;
import org.apache.syncope.common.lib.types.LoggerLevel;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractLogsPanel<T extends AbstractBaseBean> extends Panel {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractLogsPanel.class);

    private static final long serialVersionUID = -6313532280206208227L;

    protected WebMarkupContainer loggerContainer;

    public AbstractLogsPanel(
            final String id,
            final PageReference pageRef,
            final List<LoggerTO> loggerTOs) {

        super(id);

        loggerContainer = new WebMarkupContainer("loggerContainer");
        loggerContainer.setOutputMarkupId(true);
        add(loggerContainer);

        ListViewPanel.Builder<LoggerTO> builder = new ListViewPanel.Builder<LoggerTO>(LoggerTO.class, pageRef) {

            private static final long serialVersionUID = 6957788356709885298L;

            @Override
            protected Component getValueComponent(final String key, final LoggerTO loggerTO) {
                if ("level".equalsIgnoreCase(key)) {
                    final AjaxDropDownChoicePanel<LoggerLevel> loggerTOs = new AjaxDropDownChoicePanel<>(
                            "field", getString("level"), Model.of(loggerTO.getLevel()), false);
                    MetaDataRoleAuthorizationStrategy.authorize(loggerTOs, ENABLE, StandardEntitlement.LOG_SET_LEVEL);

                    loggerTOs.hideLabel();
                    loggerTOs.setChoices(Arrays.asList(LoggerLevel.values()));
                    loggerTOs.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                        private static final long serialVersionUID = -1107858522700306810L;

                        @Override
                        protected void onUpdate(final AjaxRequestTarget target) {
                            try {
                                loggerTO.setLevel(loggerTOs.getModelObject());
                                update(loggerTO);
                                SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                                target.add(loggerTOs);
                            } catch (SyncopeClientException e) {
                                LOG.error("Error updating the logger level", e);
                                SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage()) ? e.getClass().
                                        getName() : e.getMessage());
                            }
                            ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                        }
                    });
                    return loggerTOs;
                } else {
                    return super.getValueComponent(key, loggerTO);
                }
            }
        };

        builder.setItems(loggerTOs).
                setModel(new ListModel<>(loggerTOs)).
                includes("key", "level").
                withChecks(ListViewPanel.CheckAvailability.NONE).
                setReuseItem(false);

        loggerContainer.add(builder.build("logger"));
    }

    protected abstract void update(final LoggerTO loggerTO);
}
