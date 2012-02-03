/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.console.pages;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.LoggerFactory;
import org.syncope.client.to.ConfigurationTO;
import org.syncope.client.to.LoggerTO;
import org.syncope.console.commons.Constants;
import org.syncope.console.commons.PreferenceManager;
import org.syncope.console.commons.SortableDataProviderComparator;
import org.syncope.console.rest.ConfigurationRestClient;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.syncope.console.pages.panels.PoliciesPanel;
import org.syncope.types.PolicyType;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.util.resource.StringResourceStream;
import org.syncope.client.to.NotificationTO;
import org.syncope.client.to.WorkflowDefinitionTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.console.rest.NotificationRestClient;
import org.syncope.console.rest.WorkflowRestClient;
import org.syncope.console.wicket.markup.html.form.ActionLink;
import org.syncope.console.wicket.markup.html.form.ActionLinksPanel;
import org.syncope.types.LoggerLevel;

/**
 * Configurations WebPage.
 */
public class Configuration extends BasePage {

    private static final long serialVersionUID = -2838270869037702214L;

    @SpringBean
    private ConfigurationRestClient confRestClient;

    @SpringBean
    private NotificationRestClient notificationRestClient;

    @SpringBean
    private WorkflowRestClient wfRestClient;

    @SpringBean
    private PreferenceManager prefMan;

    private final ModalWindow createConfigWin;

    private final ModalWindow editConfigWin;

    private static final int CONFIG_WIN_HEIGHT = 200;

    private static final int CONFIG_WIN_WIDTH = 350;

    private final ModalWindow createNotificationWin;

    private final ModalWindow editNotificationWin;

    private static final int NOTIFICATION_WIN_HEIGHT = 500;

    private static final int NOTIFICATION_WIN_WIDTH = 900;

    private WebMarkupContainer confContainer;

    private WebMarkupContainer notificationContainer;

    private int confPaginatorRows;

    private int notificationPaginatorRows;

    public Configuration(final PageParameters parameters) {
        super(parameters);

        add(createConfigWin = new ModalWindow("createConfigurationWin"));
        add(editConfigWin = new ModalWindow("editConfigurationWin"));
        setupSyncopeConf();

        add(new PoliciesPanel("passwordPoliciesPanel", PolicyType.PASSWORD));
        add(new PoliciesPanel("accountPoliciesPanel", PolicyType.ACCOUNT));
        add(new PoliciesPanel("syncPoliciesPanel", PolicyType.SYNC));

        add(createNotificationWin = new ModalWindow("createNotificationWin"));
        add(editNotificationWin = new ModalWindow("editNotificationWin"));
        setupNotification();

        // Workflow definition stuff
        final WorkflowDefinitionTO workflowDef = wfRestClient.getDefinition();

        WebMarkupContainer workflowDefContainer =
                new WebMarkupContainer("workflowDefContainer");

        Form wfForm = new Form("workflowDefForm",
                new CompoundPropertyModel(workflowDef));

        TextArea<WorkflowDefinitionTO> workflowDefArea =
                new TextArea<WorkflowDefinitionTO>("workflowDefArea",
                new PropertyModel<WorkflowDefinitionTO>(
                workflowDef, "xmlDefinition"));
        wfForm.add(workflowDefArea);

        IndicatingAjaxButton submit = new IndicatingAjaxButton(
                "apply", new Model<String>(getString("submit"))) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target,
                    final Form form) {

                try {
                    wfRestClient.updateDefinition(workflowDef);
                    info(getString("operation_succeded"));
                } catch (SyncopeClientCompositeErrorException scee) {
                    error(getString("error") + ":" + scee.getMessage());
                }
                target.add(feedbackPanel);
            }

            @Override
            protected void onError(final AjaxRequestTarget target,
                    final Form form) {

                target.add(feedbackPanel);
            }
        };

        MetaDataRoleAuthorizationStrategy.authorize(
                submit, ENABLE, xmlRolesReader.getAllAllowedRoles(
                "Configuration", "workflowDefUpdate"));
        wfForm.add(submit);

        workflowDefContainer.add(wfForm);

        MetaDataRoleAuthorizationStrategy.authorize(
                workflowDefContainer, ENABLE, xmlRolesReader.getAllAllowedRoles(
                "Configuration", "workflowDefRead"));
        add(workflowDefContainer);

        // Logger stuff
        PropertyListView coreLoggerList =
                new LoggerPropertyList(null,
                "corelogger",
                confRestClient.getLoggers());
        WebMarkupContainer coreLoggerContainer =
                new WebMarkupContainer("coreLoggerContainer");
        coreLoggerContainer.add(coreLoggerList);
        coreLoggerContainer.setOutputMarkupId(true);

        MetaDataRoleAuthorizationStrategy.authorize(
                coreLoggerContainer, ENABLE,
                xmlRolesReader.getAllAllowedRoles(
                "Configuration", "loggerList"));
        add(coreLoggerContainer);

        ConsoleLoggerController consoleLoggerController =
                new ConsoleLoggerController();
        PropertyListView consoleLoggerList =
                new LoggerPropertyList(
                consoleLoggerController,
                "consolelogger",
                consoleLoggerController.getLoggers());
        WebMarkupContainer consoleLoggerContainer =
                new WebMarkupContainer("consoleLoggerContainer");
        consoleLoggerContainer.add(consoleLoggerList);
        consoleLoggerContainer.setOutputMarkupId(true);

        MetaDataRoleAuthorizationStrategy.authorize(
                consoleLoggerContainer, ENABLE,
                xmlRolesReader.getAllAllowedRoles(
                "Configuration", "loggerList"));
        add(consoleLoggerContainer);
    }

    private void setupSyncopeConf() {
        confPaginatorRows = prefMan.getPaginatorRows(getRequest(),
                Constants.PREF_CONFIGURATION_PAGINATOR_ROWS);

        List<IColumn> confColumns = new ArrayList<IColumn>();

        confColumns.add(new PropertyColumn(new ResourceModel("key"),
                "key", "key"));

        confColumns.add(new PropertyColumn(new ResourceModel("value"),
                "value", "value"));

        confColumns.add(new AbstractColumn<ConfigurationTO>(
                new ResourceModel("actions", "")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public String getCssClass() {
                return "action";
            }

            @Override
            public void populateItem(
                    final Item<ICellPopulator<ConfigurationTO>> cellItem,
                    final String componentId,
                    final IModel<ConfigurationTO> model) {

                final ConfigurationTO configurationTO = model.getObject();

                final ActionLinksPanel panel =
                        new ActionLinksPanel(componentId, model);

                panel.add(new ActionLink() {

                    private static final long serialVersionUID =
                            -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {

                        editConfigWin.setPageCreator(
                                new ModalWindow.PageCreator() {

                                    private static final long serialVersionUID =
                                            -7834632442532690940L;

                                    @Override
                                    public Page createPage() {
                                        return new ConfigurationModalPage(
                                                Configuration.this.
                                                getPageReference(),
                                                editConfigWin,
                                                configurationTO, false);
                                    }
                                });

                        editConfigWin.show(target);
                    }
                }, ActionLink.ActionType.EDIT, "Configuration", "read");

                panel.add(new ActionLink() {

                    private static final long serialVersionUID =
                            -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {
                            confRestClient.deleteConfiguration(
                                    configurationTO.getKey());
                        } catch (SyncopeClientCompositeErrorException e) {
                            LOG.error("While deleting a conf key", e);
                            error(e.getMessage());
                            return;
                        }

                        info(getString("operation_succeded"));
                        target.add(feedbackPanel);

                        target.add(confContainer);
                    }
                }, ActionLink.ActionType.DELETE, "Configuration", "delete");

                cellItem.add(panel);
            }
        });

        final AjaxFallbackDefaultDataTable confTable =
                new AjaxFallbackDefaultDataTable("syncopeconf", confColumns,
                new SyncopeConfProvider(), confPaginatorRows);

        confContainer = new WebMarkupContainer("confContainer");
        confContainer.add(confTable);
        confContainer.setOutputMarkupId(true);

        add(confContainer);

        createConfigWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createConfigWin.setInitialHeight(CONFIG_WIN_HEIGHT);
        createConfigWin.setInitialWidth(CONFIG_WIN_WIDTH);
        createConfigWin.setCookieName("create-configuration-modal");

        editConfigWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editConfigWin.setInitialHeight(CONFIG_WIN_HEIGHT);
        editConfigWin.setInitialWidth(CONFIG_WIN_WIDTH);
        editConfigWin.setCookieName("edit-configuration-modal");

        setWindowClosedCallback(createConfigWin, confContainer);
        setWindowClosedCallback(editConfigWin, confContainer);

        AjaxLink createConfigurationLink = new AjaxLink(
                "createConfigurationLink") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {

                createConfigWin.setPageCreator(new ModalWindow.PageCreator() {

                    private static final long serialVersionUID =
                            -7834632442532690940L;

                    @Override
                    public Page createPage() {
                        return new ConfigurationModalPage(
                                Configuration.this.getPageReference(),
                                createConfigWin, new ConfigurationTO(), true);
                    }
                });

                createConfigWin.show(target);
            }
        };

        MetaDataRoleAuthorizationStrategy.authorize(
                createConfigurationLink, ENABLE,
                xmlRolesReader.getAllAllowedRoles("Configuration", "create"));
        add(createConfigurationLink);

        Link dbExportLink = new Link<Void>("dbExportLink") {

            private static final long serialVersionUID = -4331619903296515985L;

            @Override
            public void onClick() {
                StringResourceStream stream =
                        new StringResourceStream(
                        confRestClient.dbContentAsXml(), "text/xml");
                getRequestCycle().scheduleRequestHandlerAfterCurrent(
                        new ResourceStreamRequestHandler(stream).setFileName(
                        "content.xml").setContentDisposition(
                        ContentDisposition.ATTACHMENT));
            }
        };

        MetaDataRoleAuthorizationStrategy.authorize(
                dbExportLink, ENABLE,
                xmlRolesReader.getAllAllowedRoles("Configuration", "read"));
        add(dbExportLink);

        Form confPaginatorForm = new Form("confPaginatorForm");

        final DropDownChoice rowsChooser = new DropDownChoice("rowsChooser",
                new PropertyModel(this, "confPaginatorRows"),
                prefMan.getPaginatorChoices());

        rowsChooser.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                prefMan.set(getRequest(),
                        getResponse(),
                        Constants.PREF_CONFIGURATION_PAGINATOR_ROWS,
                        String.valueOf(confPaginatorRows));
                confTable.setItemsPerPage(confPaginatorRows);

                target.add(confContainer);
            }
        });

        confPaginatorForm.add(rowsChooser);
        add(confPaginatorForm);
    }

    private void setupNotification() {
        notificationPaginatorRows = prefMan.getPaginatorRows(getRequest(),
                Constants.PREF_NOTIFICATION_PAGINATOR_ROWS);

        List<IColumn> notificationCols = new ArrayList<IColumn>();
        notificationCols.add(new PropertyColumn(new ResourceModel("id"),
                "id", "id"));
        notificationCols.add(new PropertyColumn(new ResourceModel("events"),
                "events", "events"));
        notificationCols.add(new PropertyColumn(new ResourceModel("subject"),
                "subject", "subject"));
        notificationCols.add(new PropertyColumn(new ResourceModel("template"),
                "template", "template"));
        notificationCols.add(new PropertyColumn(new ResourceModel("traceLevel"),
                "traceLevel", "traceLevel"));

        notificationCols.add(new AbstractColumn<NotificationTO>(
                new ResourceModel("actions", "")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public String getCssClass() {
                return "action";
            }

            @Override
            public void populateItem(
                    final Item<ICellPopulator<NotificationTO>> cellItem,
                    final String componentId,
                    final IModel<NotificationTO> model) {

                final NotificationTO notificationTO = model.getObject();

                final ActionLinksPanel panel =
                        new ActionLinksPanel(componentId, model);

                panel.add(new ActionLink() {

                    private static final long serialVersionUID =
                            -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {

                        editNotificationWin.setPageCreator(
                                new ModalWindow.PageCreator() {

                                    private static final long serialVersionUID =
                                            -7834632442532690940L;

                                    @Override
                                    public Page createPage() {
                                        return new NotificationModalPage(
                                                Configuration.this.
                                                getPageReference(),
                                                editNotificationWin,
                                                notificationTO, false);
                                    }
                                });

                        editNotificationWin.show(target);
                    }
                }, ActionLink.ActionType.EDIT, "Notification", "read");

                panel.add(new ActionLink() {

                    private static final long serialVersionUID =
                            -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {
                            notificationRestClient.deleteNotification(
                                    notificationTO.getId());
                        } catch (SyncopeClientCompositeErrorException e) {
                            LOG.error("While deleting a notification", e);
                            error(e.getMessage());
                            return;
                        }

                        info(getString("operation_succeded"));
                        target.add(feedbackPanel);

                        target.add(notificationContainer);
                    }
                }, ActionLink.ActionType.DELETE, "Notification", "delete");

                cellItem.add(panel);
            }
        });

        final AjaxFallbackDefaultDataTable notificationTable =
                new AjaxFallbackDefaultDataTable("notificationTable",
                notificationCols,
                new NotificationProvider(), notificationPaginatorRows);

        notificationContainer = new WebMarkupContainer("notificationContainer");
        notificationContainer.add(notificationTable);
        notificationContainer.setOutputMarkupId(true);

        add(notificationContainer);

        createNotificationWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createNotificationWin.setInitialHeight(NOTIFICATION_WIN_HEIGHT);
        createNotificationWin.setInitialWidth(NOTIFICATION_WIN_WIDTH);
        createNotificationWin.setCookieName("create-notification-modal");

        editNotificationWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editNotificationWin.setInitialHeight(NOTIFICATION_WIN_HEIGHT);
        editNotificationWin.setInitialWidth(NOTIFICATION_WIN_WIDTH);
        editNotificationWin.setCookieName("edit-notification-modal");

        setWindowClosedCallback(createNotificationWin, notificationContainer);
        setWindowClosedCallback(editNotificationWin, notificationContainer);

        AjaxLink createNotificationLink =
                new AjaxLink("createNotificationLink") {

                    private static final long serialVersionUID =
                            -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {

                        createNotificationWin.setPageCreator(
                                new ModalWindow.PageCreator() {

                                    private static final long serialVersionUID =
                                            -7834632442532690940L;

                                    @Override
                                    public Page createPage() {
                                        return new NotificationModalPage(
                                                Configuration.this.
                                                getPageReference(),
                                                createNotificationWin,
                                                new NotificationTO(), true);
                                    }
                                });

                        createNotificationWin.show(target);
                    }
                };

        MetaDataRoleAuthorizationStrategy.authorize(
                createNotificationLink, ENABLE,
                xmlRolesReader.getAllAllowedRoles("Notification", "create"));
        add(createNotificationLink);

        Form notificationPaginatorForm = new Form("notificationPaginatorForm");

        final DropDownChoice rowsChooser = new DropDownChoice("rowsChooser",
                new PropertyModel(this, "notificationPaginatorRows"),
                prefMan.getPaginatorChoices());

        rowsChooser.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                prefMan.set(getRequest(),
                        getResponse(),
                        Constants.PREF_NOTIFICATION_PAGINATOR_ROWS,
                        String.valueOf(notificationPaginatorRows));
                notificationTable.setItemsPerPage(notificationPaginatorRows);

                target.add(notificationContainer);
            }
        });

        notificationPaginatorForm.add(rowsChooser);
        add(notificationPaginatorForm);
    }

    private class SyncopeConfProvider
            extends SortableDataProvider<ConfigurationTO> {

        private static final long serialVersionUID = -276043813563988590L;

        private SortableDataProviderComparator<ConfigurationTO> comparator;

        public SyncopeConfProvider() {
            //Default sorting
            setSort("key", SortOrder.ASCENDING);
            comparator =
                    new SortableDataProviderComparator<ConfigurationTO>(this);
        }

        @Override
        public Iterator<ConfigurationTO> iterator(final int first,
                final int count) {

            List<ConfigurationTO> list = confRestClient.getAllConfigurations();

            Collections.sort(list, comparator);

            return list.subList(first, first + count).iterator();
        }

        @Override
        public int size() {
            return confRestClient.getAllConfigurations().size();
        }

        @Override
        public IModel<ConfigurationTO> model(
                final ConfigurationTO configuration) {

            return new AbstractReadOnlyModel<ConfigurationTO>() {

                private static final long serialVersionUID =
                        774694801558497248L;

                @Override
                public ConfigurationTO getObject() {
                    return configuration;
                }
            };
        }
    }

    private class NotificationProvider
            extends SortableDataProvider<NotificationTO> {

        private static final long serialVersionUID = -276043813563988590L;

        private SortableDataProviderComparator<NotificationTO> comparator;

        public NotificationProvider() {
            //Default sorting
            setSort("id", SortOrder.ASCENDING);
            comparator =
                    new SortableDataProviderComparator<NotificationTO>(this);
        }

        @Override
        public Iterator<NotificationTO> iterator(final int first,
                final int count) {

            List<NotificationTO> list =
                    notificationRestClient.getAllNotifications();

            Collections.sort(list, comparator);

            return list.subList(first, first + count).iterator();
        }

        @Override
        public int size() {
            return notificationRestClient.getAllNotifications().size();
        }

        @Override
        public IModel<NotificationTO> model(
                final NotificationTO notification) {

            return new AbstractReadOnlyModel<NotificationTO>() {

                private static final long serialVersionUID =
                        774694801558497248L;

                @Override
                public NotificationTO getObject() {
                    return notification;
                }
            };
        }
    }

    private class LoggerPropertyList extends PropertyListView<LoggerTO> {

        private static final long serialVersionUID = 5911412425994616111L;

        private final ConsoleLoggerController consoleLoggerController;

        public LoggerPropertyList(
                final ConsoleLoggerController consoleLoggerController,
                final String id, final List<? extends LoggerTO> list) {

            super(id, list);
            this.consoleLoggerController = consoleLoggerController;
        }

        @Override
        protected void populateItem(final ListItem<LoggerTO> item) {
            item.add(new Label("name"));

            DropDownChoice<LoggerLevel> level =
                    new DropDownChoice<LoggerLevel>("level");
            level.setModel(new IModel<LoggerLevel>() {

                private static final long serialVersionUID =
                        -2350428186089596562L;

                @Override
                public LoggerLevel getObject() {
                    return item.getModelObject().getLevel();
                }

                @Override
                public void setObject(final LoggerLevel object) {
                    item.getModelObject().setLevel(object);
                }

                @Override
                public void detach() {
                }
            });
            level.setChoices(Arrays.asList(LoggerLevel.values()));
            level.setOutputMarkupId(true);
            level.add(new AjaxFormComponentUpdatingBehavior(
                    "onchange") {

                private static final long serialVersionUID =
                        -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    boolean result = getId().equals("corelogger")
                            ? confRestClient.setLoggerLevel(
                            item.getModelObject().getName(),
                            item.getModelObject().getLevel())
                            : consoleLoggerController.setLoggerLevel(
                            item.getModelObject().getName(),
                            item.getModelObject().getLevel());

                    if (result) {
                        info(getString("operation_succeded"));
                    } else {
                        info(getString("operation_error"));

                    }

                    target.add(feedbackPanel);
                }
            });

            MetaDataRoleAuthorizationStrategy.authorize(level, ENABLE,
                    xmlRolesReader.getAllAllowedRoles(
                    "Configuration", "loggerSetLevel"));

            item.add(level);
        }
    }

    private static class ConsoleLoggerController implements Serializable {

        private static final long serialVersionUID = -1550459341476431714L;

        public List<LoggerTO> getLoggers() {
            LoggerContext lc =
                    (LoggerContext) LoggerFactory.getILoggerFactory();
            List<LoggerTO> result =
                    new ArrayList<LoggerTO>(lc.getLoggerList().size());
            LoggerTO loggerTO;
            for (Logger logger : lc.getLoggerList()) {
                if (logger.getLevel() != null) {
                    loggerTO = new LoggerTO();
                    loggerTO.setName(logger.getName());
                    loggerTO.setLevel(LoggerLevel.fromLevel(logger.getLevel()));
                    result.add(loggerTO);
                }
            }

            return result;
        }

        public boolean setLoggerLevel(final String name,
                final LoggerLevel level) {

            LoggerContext lc =
                    (LoggerContext) LoggerFactory.getILoggerFactory();
            Logger logger = lc.getLogger(name);
            if (logger != null) {
                logger.setLevel(level.getLevel());
            }

            return logger != null;
        }
    }
}
