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
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.to.ConnectorInstanceTO;
import org.syncope.client.to.ResourceTO;
import org.syncope.console.commons.Constants;
import org.syncope.console.commons.Utility;
import org.syncope.console.rest.ConnectorsRestClient;
import org.syncope.console.rest.ResourcesRestClient;
import org.syncope.console.wicket.markup.html.form.DeleteLinkPanel;
import org.syncope.console.wicket.markup.html.form.EditLinkPanel;

/**
 * Connectors WebPage.
 */
public class Connectors extends BasePage {

    @SpringBean(name = "connectorsRestClient")
    ConnectorsRestClient restClient;

    @SpringBean(name = "resourcesRestClient")
    ResourcesRestClient resourcesRestClient;

    @SpringBean(name = "utility")
    Utility utility;

    final ModalWindow createConnectorWin;
    final ModalWindow editConnectorWin;
    
    WebMarkupContainer container;
    /*
     Response flag set by the Modal Window after the operation is completed
     */
    boolean operationResult = false;
    FeedbackPanel feedbackPanel;

    private int paginatorRows;
    
    public Connectors(PageParameters parameters) {
        super(parameters);

        add(createConnectorWin = new ModalWindow("createConnectorWin"));
        add(editConnectorWin = new ModalWindow("editConnectorWin"));

        feedbackPanel = new FeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId( true );

        add(feedbackPanel);

        paginatorRows = utility.getPaginatorRowsToDisplay(Constants
                    .CONF_CONNECTORS_PAGINATOR_ROWS);

        List<IColumn> columns = new ArrayList<IColumn>();

        columns.add(new PropertyColumn(new Model(getString("id")),
                "id", "id"));

        columns.add(new PropertyColumn(new Model(getString("name")),
                "connectorName", "connectorName"));

        columns.add(new PropertyColumn(new Model(getString("version")),
                "version", "version"));

        columns.add(new PropertyColumn(new Model(getString("bundleName")),
                "bundleName", "bundleName"));

        columns.add(new AbstractColumn<ConnectorInstanceTO>(new Model<String>(
                getString("edit")))
        {
            public void populateItem(Item<ICellPopulator<ConnectorInstanceTO>>
                    cellItem, String componentId, IModel<ConnectorInstanceTO>
                    model)
            {
                    final ConnectorInstanceTO connectorTO = model.getObject();

                    AjaxLink editLink = new AjaxLink("editLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        editConnectorWin.setPageCreator(new ModalWindow
                                .PageCreator() {

                            public Page createPage() {
                                ConnectorsModalPage form = 
                                        new ConnectorsModalPage(Connectors.this,
                                        editConnectorWin, connectorTO, false);
                                return form;
                            }
                        });

                        editConnectorWin.show(target);
                    }
                };

                EditLinkPanel panel = new EditLinkPanel(componentId, model);
                panel.add(editLink);

                String allowedRoles = xmlRolesReader.getAllAllowedRoles(
                        "Connectors","read");
                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                        allowedRoles);

                cellItem.add(panel);
            }
        });

        columns.add(new AbstractColumn<ConnectorInstanceTO>(new Model<String>
                (getString("delete")))
        {
            public void populateItem(Item<ICellPopulator<ConnectorInstanceTO>>
                    cellItem, String componentId, IModel<ConnectorInstanceTO>
                    model)
            {
                    final ConnectorInstanceTO connectorTO = model.getObject();

                    AjaxLink deleteLink = new AjaxLink("deleteLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        if(!checkDeleteIsForbidden(connectorTO)){
                        restClient.deleteConnector(connectorTO.getId());
                        info(getString("operation_succeded"));
                        }

                        else
                            error(getString("delete_error"));

                        target.addComponent(container);
                        target.addComponent(feedbackPanel);

                    }

                    @Override
                    protected IAjaxCallDecorator getAjaxCallDecorator() {
                        return new AjaxPreprocessingCallDecorator(super.getAjaxCallDecorator()) {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public CharSequence preDecorateScript(CharSequence script) {
                                return "if (confirm('"+getString("confirmDelete")+"'))"
                                        +"{"+script+"}";
                            }
                        };
                    }
                    };

                DeleteLinkPanel panel = new DeleteLinkPanel(componentId, model);
                panel.add(deleteLink);

                String allowedRoles = xmlRolesReader.getAllAllowedRoles(
                        "Connectors","delete");
                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                        allowedRoles);

                cellItem.add(panel);
            }
        });


        final AjaxFallbackDefaultDataTable table =
                new AjaxFallbackDefaultDataTable("datatable", columns,
                new ConnectorsProvider(), paginatorRows);

        container = new WebMarkupContainer("container");
        container.add(table);
        container.setOutputMarkupId(true);

        add(container);

        setWindowClosedCallback(createConnectorWin, container);
        setWindowClosedCallback(editConnectorWin, container);

        createConnectorWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createConnectorWin.setPageMapName("create-conn-modal");
        createConnectorWin.setCookieName("create-conn-modal");

        editConnectorWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editConnectorWin.setPageMapName("edit-conn-modal");
        editConnectorWin.setCookieName("edit-conn-modal");

       AjaxLink createConnectorLink = new AjaxLink("createConnectorLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {

                createConnectorWin.setPageCreator(new ModalWindow.PageCreator() {

                    public Page createPage() {
                        ConnectorsModalPage form = new ConnectorsModalPage(
                                Connectors.this, editConnectorWin,
                                new ConnectorInstanceTO(), true);
                        return form;
                    }
                });

                createConnectorWin.show(target);
            }
        };

        String allowedRoles = xmlRolesReader.getAllAllowedRoles(
                "Connectors","create");
        MetaDataRoleAuthorizationStrategy.authorize(createConnectorLink, ENABLE,
                allowedRoles);
        
        add(createConnectorLink);

        Form paginatorForm = new Form("PaginatorForm");

        final DropDownChoice rowsChooser = new DropDownChoice("rowsChooser",
        new PropertyModel(this,"paginatorRows"),utility.paginatorRowsChooser());

        rowsChooser.add(new AjaxFormComponentUpdatingBehavior( "onchange" ){
          protected void onUpdate( AjaxRequestTarget target )
            {
              utility.updatePaginatorRows(
                      Constants.CONF_CONNECTORS_PAGINATOR_ROWS, paginatorRows);

              table.setRowsPerPage(paginatorRows);

              target.addComponent(container);
            }

          });

        paginatorForm.add(rowsChooser);
        add(paginatorForm);
    }

    /**
     * Check if the delete action is forbidden
     * @param ConnectorInstanceTO object to check
     * @return true if the action is forbidden, false otherwise
     */
    public boolean checkDeleteIsForbidden(ConnectorInstanceTO connectorTO){

        boolean forbidden = false;
        List<ResourceTO> resources = resourcesRestClient.getAllResources();

        for(ResourceTO resourceTO : resources) {
            if(resourceTO.getConnectorId() == connectorTO.getId())
                forbidden = true;
        }

        return forbidden;
    }

    /**
     * Set a WindowClosedCallback for a ModalWindow instance.
     * @param current window
     * @param container to refresh
     */
    public void setWindowClosedCallback(ModalWindow window,
            final WebMarkupContainer container) {

        window.setWindowClosedCallback(
                new ModalWindow.WindowClosedCallback() {

                    public void onClose(AjaxRequestTarget target) {
                        target.addComponent(container);
                        if(operationResult){
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

    class ConnectorsProvider extends SortableDataProvider<ConnectorInstanceTO> {

        private SortableDataProviderComparator comparator =
                new SortableDataProviderComparator();

        public ConnectorsProvider() {
            //Default sorting
            setSort("id",true);
        }

        @Override
        public Iterator<ConnectorInstanceTO> iterator(int first, int count) {
            List<ConnectorInstanceTO> list = getConnectorsListDB();

            Collections.sort(list, comparator);

            return list.subList(first, first+count).iterator();
        }

        @Override
        public int size() {
            return getConnectorsListDB().size();
        }

        @Override
        public IModel<ConnectorInstanceTO> model(final ConnectorInstanceTO
                connector) {
            return new AbstractReadOnlyModel<ConnectorInstanceTO>() {

                @Override
                public ConnectorInstanceTO getObject() {
                    return connector;
                }
            };
        }

        public List<ConnectorInstanceTO> getConnectorsListDB(){
        List<ConnectorInstanceTO> list = restClient.getAllConnectors();

        return list;
        }

        class SortableDataProviderComparator implements
                Comparator<ConnectorInstanceTO>, Serializable {
            public int compare(final ConnectorInstanceTO o1,
                    final ConnectorInstanceTO o2) {
                    PropertyModel<Comparable> model1 =
                            new PropertyModel<Comparable>(o1, getSort()
                            .getProperty());
                    PropertyModel<Comparable> model2 =
                            new PropertyModel<Comparable>(o2, getSort()
                            .getProperty());

                    int result = 1;

                    if(model1.getObject() == null && model2.getObject() == null)
                        result = 0;
                    else if(model1.getObject() == null)
                        result = 1;
                    else if(model2.getObject() == null)
                        result = -1;
                    else
                        result = ((Comparable)model1.getObject()).compareTo(
                                model2.getObject());

                    result = getSort().isAscending() ? result : -result;

                    return result;
            }
	}
    }
}