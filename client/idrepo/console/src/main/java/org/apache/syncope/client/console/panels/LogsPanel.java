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

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.rest.LoggerConf;
import org.apache.syncope.client.console.rest.LoggerConfOp;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LogLevel;

public class LogsPanel extends Panel {

    private static final long serialVersionUID = -6313532280206208227L;

    private static final Logger LOG = LoggerFactory.getLogger(LogsPanel.class);

    private final IModel<List<LoggerConf>> loggerConfs = new LoadableDetachableModel<>() {

        private static final long serialVersionUID = 5275935387613157437L;

        @Override
        protected List<LoggerConf> load() {
            return loggerConfOp.list();
        }
    };

    private final LoggerConfOp loggerConfOp;

    private final ListViewPanel<LoggerConf> loggerConfsView;

    public LogsPanel(final String id, final LoggerConfOp loggerConfOp, final PageReference pageRef) {
        super(id);
        this.loggerConfOp = loggerConfOp;

        WebMarkupContainer searchBoxContainer = new WebMarkupContainer("searchBox");
        add(searchBoxContainer.setOutputMarkupId(true));

        Form<?> form = new Form<>("form");
        searchBoxContainer.add(form);

        Model<String> keywordModel = new Model<>(StringUtils.EMPTY);

        AjaxTextFieldPanel filter = new AjaxTextFieldPanel("filter", "filter", keywordModel, true);
        form.add(filter.hideLabel().setOutputMarkupId(true).setRenderBodyOnly(true));

        AjaxButton search = new AjaxButton("search") {

            private static final long serialVersionUID = 8390605330558248736L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                send(LogsPanel.this, Broadcast.EXACT, new LoggerConfSearchEvent(target, keywordModel.getObject()));
            }
        };
        form.add(search.setOutputMarkupId(true));
        form.setDefaultButton(search);

        WebMarkupContainer loggerContainer = new WebMarkupContainer("loggerContainer");
        add(loggerContainer.setOutputMarkupId(true));

        ListViewPanel.Builder<LoggerConf> builder = new ListViewPanel.Builder<>(LoggerConf.class, pageRef) {

            private static final long serialVersionUID = 6957788356709885298L;

            @Override
            protected Component getValueComponent(final String key, final LoggerConf loggerConf) {
                if ("level".equalsIgnoreCase(key)) {
                    AjaxDropDownChoicePanel<LogLevel> loggerLevel = new AjaxDropDownChoicePanel<>(
                        "field", getString("level"), Model.of(loggerConf.getLevel()), false);
                    MetaDataRoleAuthorizationStrategy.authorize(loggerLevel, ENABLE, IdRepoEntitlement.LOGGER_UPDATE);

                    loggerLevel.hideLabel();
                    loggerLevel.setChoices(List.of(LogLevel.values()));
                    loggerLevel.setNullValid(false);
                    loggerLevel.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                        private static final long serialVersionUID = -1107858522700306810L;

                        @Override
                        protected void onUpdate(final AjaxRequestTarget target) {
                            try {
                                loggerConf.setLevel(loggerLevel.getModelObject());
                                loggerConfOp.setLevel(loggerConf.getKey(), loggerConf.getLevel());

                                SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                                target.add(loggerLevel);
                            } catch (SyncopeClientException e) {
                                LOG.error("Error updating the logger level", e);
                                SyncopeConsoleSession.get().onException(e);
                            }
                            ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
                        }
                    });
                    return loggerLevel;
                } else {
                    return super.getValueComponent(key, loggerConf);
                }
            }
        };

        builder.setItems(loggerConfs.getObject()).
                setModel(loggerConfs).
                includes(Constants.KEY_FIELD_NAME, "level").
                withChecks(ListViewPanel.CheckAvailability.NONE).
                setCaptionVisible(false).
                setReuseItem(false);

        loggerConfsView = (ListViewPanel<LoggerConf>) builder.build("logger");
        loggerContainer.add(loggerConfsView);
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof LoggerConfSearchEvent) {
            LoggerConfSearchEvent payload = LoggerConfSearchEvent.class.cast(event.getPayload());
            AjaxRequestTarget target = payload.getTarget();

            String keyword = payload.getKeyword();
            if (StringUtils.isBlank(keyword)) {
                loggerConfsView.refreshList(loggerConfs.getObject());
            } else {
                loggerConfsView.refreshList(loggerConfs.getObject().stream().
                        filter(l -> l.getKey().contains(keyword)).collect(Collectors.toList()));
            }
            target.add(loggerConfsView);
        } else {
            super.onEvent(event);
        }
    }

    private static class LoggerConfSearchEvent implements Serializable {

        private static final long serialVersionUID = -282052400565266028L;

        private final AjaxRequestTarget target;

        private final String keyword;

        LoggerConfSearchEvent(final AjaxRequestTarget target, final String keyword) {
            this.target = target;
            this.keyword = keyword;
        }

        public AjaxRequestTarget getTarget() {
            return target;
        }

        public String getKeyword() {
            return keyword;
        }
    }
}
