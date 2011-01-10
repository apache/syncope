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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.Page;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxPreprocessingCallDecorator;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.web.client.RestClientException;
import org.syncope.client.to.ConfigurationTO;
import org.syncope.console.commons.Constants;
import org.syncope.console.commons.Utility;
import org.syncope.console.rest.ConfigurationRestClient;
import org.syncope.console.wicket.markup.html.form.DeleteLinkPanel;
import org.syncope.console.wicket.markup.html.form.EditLinkPanel;

/**
 * Configurations WebPage.
 */
public class Configuration extends BasePage {

    @SpringBean
    private ConfigurationRestClient restClient;

    @SpringBean
    private Utility utility;

    private final ModalWindow createConfigWin;

    private final ModalWindow editConfigWin;

    private static final int WIN_USER_HEIGHT = 680;

    private static final int WIN_USER_WIDTH = 1133;

    private WebMarkupContainer container;

    /* 
    Response flag set by the Modal Window after the operation
    is completed  */
    private boolean operationResult = false;

    private int paginatorRows;

    public Configuration(PageParameters parameters) {
        super(parameters);

        add(createConfigWin = new ModalWindow("createConfigurationWin"));
        add(editConfigWin = new ModalWindow("editConfigurationWin"));

        paginatorRows = utility.getPaginatorRowsToDisplay(
                Constants.CONF_CONFIGURATION_PAGINATOR_ROWS);

        List<IColumn> columns = new ArrayList<IColumn>();

        columns.add(new PropertyColumn(new Model(getString("key")),
                "confKey", "confKey"));

        columns.add(new PropertyColumn(new Model(getString("value")),
                "confValue", "confValue"));

        columns.add(new AbstractColumn<ConfigurationTO>(new Model<String>(
                getString("edit"))) {

            public void populateItem(Item<ICellPopulator<ConfigurationTO>> cellItem, String componentId, IModel<ConfigurationTO> model) {
                final ConfigurationTO configurationTO = model.getObject();
                AjaxLink editLink = new AjaxLink("editLink") {

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

                String allowedRoles = xmlRolesReader.getAllAllowedRoles(
                        "Configuration", "read");

                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                        allowedRoles);

                cellItem.add(panel);
            }
        });

        columns.add(new AbstractColumn<ConfigurationTO>(new Model<String>(getString(
                "delete"))) {

            public void populateItem(Item<ICellPopulator<ConfigurationTO>> cellItem, String componentId, IModel<ConfigurationTO> model) {
                final ConfigurationTO configurationTO = model.getObject();

                AjaxLink deleteLink = new AjaxLink("deleteLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        try {
                            restClient.deleteConfiguration(configurationTO.
                                    getConfKey());
                        } catch (UnsupportedEncodingException e) {
                            LOG.error("While deleting a conf key", e);
                            error(e.getMessage());
                            return;
                        }

                        info(getString("operation_succeded"));
                        target.addComponent(feedbackPanel);

                        target.addComponent(container);
                    }

                    @Override
                    protected IAjaxCallDecorator getAjaxCallDecorator() {
                        return new AjaxPreprocessingCallDecorator(super.
                                getAjaxCallDecorator()) {

                            private static final long serialVersionUID = 1L;

                            @Override
                            public CharSequence preDecorateScript(CharSequence script) {
                                return "if (confirm('" + getString(
                                        "confirmDelete") + "'))"
                                        + "{" + script + "}";
                            }
                        };
                    }
                };

                DeleteLinkPanel panel = new DeleteLinkPanel(componentId, model);
                panel.add(deleteLink);

                String allowedRoles = xmlRolesReader.getAllAllowedRoles(
                        "Configuration", "delete");

                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                        allowedRoles);

                cellItem.add(panel);
            }
        });


        final AjaxFallbackDefaultDataTable table =
                new AjaxFallbackDefaultDataTable("datatable", columns,
                new ConfigurationsProvider(), paginatorRows);

        container = new WebMarkupContainer("container");
        container.add(table);
        container.setOutputMarkupId(true);

        add(container);

        createConfigWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createConfigWin.setInitialHeight(WIN_USER_HEIGHT);
        createConfigWin.setInitialWidth(WIN_USER_WIDTH);
        createConfigWin.setPageMapName("create-configuration-modal");
        createConfigWin.setCookieName("create-configuration-modal");

        editConfigWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editConfigWin.setInitialHeight(WIN_USER_HEIGHT);
        editConfigWin.setInitialWidth(WIN_USER_HEIGHT);
        editConfigWin.setPageMapName("edit-configuration-modal");
        editConfigWin.setCookieName("edit-configuration-modal");

        setWindowClosedCallback(createConfigWin, container);
        setWindowClosedCallback(editConfigWin, container);

        AjaxLink createConfigurationLink = new AjaxLink(
                "createConfigurationLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {

                createConfigWin.setPageCreator(new ModalWindow.PageCreator() {

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
                new PropertyModel(this, "paginatorRows"), utility.
                paginatorRowsChooser());

        rowsChooser.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            protected void onUpdate(AjaxRequestTarget target) {
                utility.updatePaginatorRows(
                        Constants.CONF_CONFIGURATION_PAGINATOR_ROWS,
                        paginatorRows);
                table.setRowsPerPage(paginatorRows);

                target.addComponent(container);
            }
        });

        paginatorForm.add(rowsChooser);
        add(paginatorForm);

        add(paginatorForm);
    }

    /**
     * Set a WindowClosedCallback for a ModalWindow instance.
     * @param window
     * @param container
     */
    public void setWindowClosedCallback(ModalWindow window,
            final WebMarkupContainer container) {

        window.setWindowClosedCallback(
                new ModalWindow.WindowClosedCallback() {

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

    class ConfigurationsProvider extends SortableDataProvider<ConfigurationTO> {

        private SortableDataProviderComparator comparator =
                new SortableDataProviderComparator();

        public ConfigurationsProvider() {
            //Default sorting
            setSort("confKey", true);
        }

        @Override
        public Iterator<ConfigurationTO> iterator(int first, int count) {
            List<ConfigurationTO> list = getConfigurationsListDB();

            Collections.sort(list, comparator);

            return list.subList(first, first + count).iterator();
        }

        @Override
        public int size() {
            return getConfigurationsListDB().size();
        }

        @Override
        public IModel<ConfigurationTO> model(final ConfigurationTO configuration) {
            return new AbstractReadOnlyModel<ConfigurationTO>() {

                @Override
                public ConfigurationTO getObject() {
                    return configuration;
                }
            };
        }

        public List<ConfigurationTO> getConfigurationsListDB() {
            List<ConfigurationTO> list = null;

            try {
                list = restClient.getAllConfigurations();
            } catch (RestClientException rce) {
                throw rce;
            }
            return list;
        }

        class SortableDataProviderComparator implements
                Comparator<ConfigurationTO>, Serializable {

            public int compare(final ConfigurationTO o1,
                    final ConfigurationTO o2) {
                PropertyModel<Comparable> model1 =
                        new PropertyModel<Comparable>(o1,
                        getSort().getProperty());
                PropertyModel<Comparable> model2 =
                        new PropertyModel<Comparable>(o2,
                        getSort().getProperty());

                int result = 1;

                if (model1.getObject() == null && model2.getObject() == null) {
                    result = 0;
                } else if (model1.getObject() == null) {
                    result = 1;
                } else if (model2.getObject() == null) {
                    result = -1;
                } else {
                    result = ((Comparable) model1.getObject()).compareTo(
                            model2.getObject());
                }

                result = getSort().isAscending() ? result : -result;

                return result;
            }
        }
    }
}
