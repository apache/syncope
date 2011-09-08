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

import org.syncope.console.pages.panels.PasswordPoliciesPanel;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.Page;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
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
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClientException;
import org.syncope.client.to.ConfigurationTO;
import org.syncope.client.to.LoggerTO;
import org.syncope.console.commons.Constants;
import org.syncope.console.commons.PreferenceManager;
import org.syncope.console.commons.SortableDataProviderComparator;
import org.syncope.console.rest.ConfigurationRestClient;
import org.syncope.console.wicket.ajax.markup.html.IndicatingDeleteOnConfirmAjaxLink;
import org.syncope.console.wicket.markup.html.form.DeleteLinkPanel;
import org.syncope.console.wicket.markup.html.form.EditLinkPanel;

/**
 * Configurations WebPage.
 */
public class Configuration extends BasePage {

    private static final long serialVersionUID = -2838270869037702214L;

    @SpringBean
    private ConfigurationRestClient restClient;

    @SpringBean
    private PreferenceManager prefMan;

    private final ModalWindow createConfigWin;

    private final ModalWindow editConfigWin;

    private static final int WIN_HEIGHT = 200;

    private static final int WIN_WIDTH = 350;

    private WebMarkupContainer confContainer;

    /**
     * Response flag set by the Modal Window after the operation
     * is completed.
     */
    private boolean operationResult = false;

    private int paginatorRows;

    public Configuration(final PageParameters parameters) {
        super(parameters);

        add(createConfigWin = new ModalWindow("createConfigurationWin"));
        add(editConfigWin = new ModalWindow("editConfigurationWin"));

        paginatorRows = prefMan.getPaginatorRows(
                getWebRequestCycle().getWebRequest(),
                Constants.PREF_CONFIGURATION_PAGINATOR_ROWS);

        List<IColumn> confColumns = new ArrayList<IColumn>();

        confColumns.add(new PropertyColumn(new ResourceModel("key"),
                "key", "key"));

        confColumns.add(new PropertyColumn(new ResourceModel("value"),
                "value", "value"));

        confColumns.add(new AbstractColumn<ConfigurationTO>(
                new ResourceModel("edit")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<ConfigurationTO>> cellItem,
                    final String componentId,
                    final IModel<ConfigurationTO> model) {

                final ConfigurationTO configurationTO = model.getObject();
                AjaxLink editLink = new IndicatingAjaxLink("editLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        editConfigWin.setPageCreator(
                                new ModalWindow.PageCreator() {

                                    public Page createPage() {
                                        ConfigurationModalPage window =
                                                new ConfigurationModalPage(
                                                Configuration.this,
                                                editConfigWin,
                                                configurationTO, false);
                                        return window;
                                    }
                                });

                        editConfigWin.show(target);
                    }
                };

                EditLinkPanel panel = new EditLinkPanel(componentId, model);
                panel.add(editLink);

                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                        xmlRolesReader.getAllAllowedRoles(
                        "Configuration", "read"));

                cellItem.add(panel);
            }
        });

        confColumns.add(new AbstractColumn<ConfigurationTO>(
                new ResourceModel("delete")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<ConfigurationTO>> cellItem,
                    final String componentId, IModel<ConfigurationTO> model) {

                final ConfigurationTO configurationTO = model.getObject();

                AjaxLink deleteLink = new IndicatingDeleteOnConfirmAjaxLink(
                        "deleteLink") {

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {
                            restClient.deleteConfiguration(configurationTO.getKey());
                        } catch (UnsupportedEncodingException e) {
                            LOG.error("While deleting a conf key", e);
                            error(e.getMessage());
                            return;
                        }

                        info(getString("operation_succeded"));
                        target.addComponent(feedbackPanel);

                        target.addComponent(confContainer);
                    }
                };

                DeleteLinkPanel panel = new DeleteLinkPanel(componentId, model);
                panel.add(deleteLink);

                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                        xmlRolesReader.getAllAllowedRoles(
                        "Configuration", "delete"));

                cellItem.add(panel);
            }
        });


        final AjaxFallbackDefaultDataTable confTable =
                new AjaxFallbackDefaultDataTable("syncopeconf", confColumns,
                new SyncopeConfProvider(), paginatorRows);

        confContainer = new WebMarkupContainer("confContainer");
        confContainer.add(confTable);
        confContainer.setOutputMarkupId(true);

        add(confContainer);

        createConfigWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createConfigWin.setInitialHeight(WIN_HEIGHT);
        createConfigWin.setInitialWidth(WIN_WIDTH);
        createConfigWin.setPageMapName("create-configuration-modal");
        createConfigWin.setCookieName("create-configuration-modal");

        editConfigWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editConfigWin.setInitialHeight(WIN_HEIGHT);
        editConfigWin.setInitialWidth(WIN_HEIGHT);
        editConfigWin.setPageMapName("edit-configuration-modal");
        editConfigWin.setCookieName("edit-configuration-modal");

        setWindowClosedCallback(createConfigWin, confContainer);
        setWindowClosedCallback(editConfigWin, confContainer);

        AjaxLink createConfigurationLink = new AjaxLink(
                "createConfigurationLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {

                createConfigWin.setPageCreator(new ModalWindow.PageCreator() {

                    @Override
                    public Page createPage() {
                        ConfigurationModalPage window =
                                new ConfigurationModalPage(Configuration.this,
                                createConfigWin, new ConfigurationTO(), true);
                        return window;
                    }
                });

                createConfigWin.show(target);
            }
        };

        String allowedRoles = xmlRolesReader.getAllAllowedRoles(
                "Configuration", "create");
        MetaDataRoleAuthorizationStrategy.authorize(createConfigurationLink,
                ENABLE, allowedRoles);

        add(createConfigurationLink);

        Form paginatorForm = new Form("PaginatorForm");

        final DropDownChoice rowsChooser = new DropDownChoice("rowsChooser",
                new PropertyModel(this, "paginatorRows"),
                prefMan.getPaginatorChoices());

        rowsChooser.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                prefMan.set(getWebRequestCycle().getWebRequest(),
                        getWebRequestCycle().getWebResponse(),
                        Constants.PREF_CONFIGURATION_PAGINATOR_ROWS,
                        String.valueOf(paginatorRows));
                confTable.setRowsPerPage(paginatorRows);

                target.addComponent(confContainer);
            }
        });

        paginatorForm.add(rowsChooser);
        add(paginatorForm);

        add(paginatorForm);

//        add(new PasswordPolicyPanel("passwordPolicy"));

        add(new PasswordPoliciesPanel("passwordPoliciesPanel"));

        // Logger stuff
        PropertyListView coreLoggerList =
                new LoggerPropertyList(null,
                "corelogger",
                restClient.getLoggers());
        WebMarkupContainer coreLoggerContainer =
                new WebMarkupContainer("coreLoggerContainer");
        coreLoggerContainer.add(coreLoggerList);
        coreLoggerContainer.setOutputMarkupId(true);
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
        add(consoleLoggerContainer);
    }

    /**
     * Set a WindowClosedCallback for a ModalWindow instance.
     * @param window
     * @param container
     */
    private void setWindowClosedCallback(ModalWindow window,
            final WebMarkupContainer container) {

        window.setWindowClosedCallback(
                new ModalWindow.WindowClosedCallback() {

                    private static final long serialVersionUID =
                            8804221891699487139L;

                    public void onClose(AjaxRequestTarget target) {
                        target.addComponent(container);
                        if (operationResult) {
                            info(getString("operation_succeded"));
                            target.addComponent(feedbackPanel);
                            operationResult = false;
                        }
                    }
                });
    }

    public boolean isOperationResult() {
        return operationResult;
    }

    public void setOperationResult(boolean operationResult) {
        this.operationResult = operationResult;
    }

    private class SyncopeConfProvider
            extends SortableDataProvider<ConfigurationTO> {

        private static final long serialVersionUID = -276043813563988590L;

        private SortableDataProviderComparator<ConfigurationTO> comparator;

        public SyncopeConfProvider() {
            //Default sorting
            setSort("key", true);
            comparator =
                    new SortableDataProviderComparator<ConfigurationTO>(this);
        }

        @Override
        public Iterator<ConfigurationTO> iterator(int first, int count) {
            List<ConfigurationTO> list = getAllConfigurations();

            Collections.sort(list, comparator);

            return list.subList(first, first + count).iterator();
        }

        @Override
        public int size() {
            return getAllConfigurations().size();
        }

        @Override
        public IModel<ConfigurationTO> model(
                final ConfigurationTO configuration) {

            return new AbstractReadOnlyModel<ConfigurationTO>() {

                @Override
                public ConfigurationTO getObject() {
                    return configuration;
                }
            };
        }

        private List<ConfigurationTO> getAllConfigurations() {
            List<ConfigurationTO> list = null;

            try {
                list = restClient.getAllConfigurations();
            } catch (RestClientException rce) {
                throw rce;
            }
            return list;
        }
    }

    enum LoggerLevel {

        OFF, ERROR, WARN, INFO, DEBUG, TRACE, ALL
    }

    private class LoggerPropertyList extends PropertyListView<LoggerTO> {

        private static final long serialVersionUID = 5911412425994616111L;

        private final ConsoleLoggerController consoleLoggerController;

        public LoggerPropertyList(
                final ConsoleLoggerController consoleLoggerController,
                final String id,
                final List<? extends LoggerTO> list) {

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
                    return LoggerLevel.valueOf(
                            item.getModelObject().getLevel());
                }

                @Override
                public void setObject(final LoggerLevel object) {
                    item.getModelObject().setLevel(object.toString());
                }

                @Override
                public void detach() {
                }
            });
            level.setChoices(Arrays.asList(LoggerLevel.values()));
            level.setOutputMarkupId(true);
            level.add(new AjaxFormComponentUpdatingBehavior(
                    "onchange") {

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    boolean result = getId().equals("corelogger")
                            ? restClient.setLoggerLevel(
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

                    target.addComponent(feedbackPanel);
                }
            });



            item.add(level);
        }
    }

    private class ConsoleLoggerController implements Serializable {

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
                    loggerTO.setLevel(logger.getLevel().toString());
                    result.add(loggerTO);
                }
            }

            return result;
        }

        public boolean setLoggerLevel(final String name,
                final String level) {

            LoggerContext lc =
                    (LoggerContext) LoggerFactory.getILoggerFactory();
            Logger logger = lc.getLogger(name);
            if (logger != null) {
                logger.setLevel(Level.valueOf(level));
            }

            return logger != null;
        }
    }
}
