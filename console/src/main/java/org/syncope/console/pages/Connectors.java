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

import java.util.List;
import org.apache.wicket.Page;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PageableListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.to.ConnectorInstanceTO;
import org.syncope.client.to.ResourceTO;
import org.syncope.console.rest.ConnectorsRestClient;
import org.syncope.console.rest.ResourcesRestClient;

/**
 * Connectors WebPage.
 */
public class Connectors extends BasePage {

    @SpringBean(name = "connectorsRestClient")
    ConnectorsRestClient restClient;

    @SpringBean(name = "resourcesRestClient")
    ResourcesRestClient resourcesRestClient;

    final ModalWindow createConnectorWin;
    final ModalWindow editConnectorWin;
    
    WebMarkupContainer connectorsContainer;

    /** Response flag set by the Modal Window after the operation is completed
     */
    boolean operationResult = false;
    FeedbackPanel feedbackPanel;

    /** Navigator's rows to display for single view */
    final int ROWS_TO_DISPLAY = 5;
    
    public Connectors(PageParameters parameters) {
        super(parameters);

        add(createConnectorWin = new ModalWindow("createConnectorWin"));
        add(editConnectorWin = new ModalWindow("editConnectorWin"));

        feedbackPanel = new FeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId( true );

        add(feedbackPanel);

        IModel connectors = new LoadableDetachableModel() {

            protected Object load() {
                return restClient.getAllConnectors().getInstances();
            }
        };        

        PageableListView connectorsView = new PageableListView("connectors",
                connectors, ROWS_TO_DISPLAY) {

            @Override
            protected void populateItem(final ListItem item) {
                final ConnectorInstanceTO connectorTO =
                        (ConnectorInstanceTO) item.getDefaultModelObject();

                item.add(new Label("id", connectorTO.getId()+""));
                item.add(new Label("name", connectorTO.getConnectorName()));
                item.add(new Label("version", connectorTO.getVersion()));
                item.add(new Label("bundleName", connectorTO.getBundleName()));


                AjaxLink editLink = new AjaxLink("editLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        final ConnectorInstanceTO connectorTO =
                                (ConnectorInstanceTO) item.getDefaultModelObject();

                        editConnectorWin.setPageCreator(new ModalWindow.PageCreator() {

                            public Page createPage() {
                                ConnectorsModalPage form = new ConnectorsModalPage
                                        (Connectors.this, editConnectorWin, connectorTO, false);
                                return form;
                            }
                        });

                        editConnectorWin.show(target);
                    }
                };

                item.add(editLink);

                AjaxLink deleteLink = new AjaxLink("deleteLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        if(!checkDeleteIsForbidden(connectorTO)){
                        restClient.deleteConnector(connectorTO.getId());
                        info(getString("operation_succeded"));
                        }

                        else
                            error(getString("delete_error"));
                            
                        target.addComponent(connectorsContainer);
                        target.addComponent(feedbackPanel);
                    
                }};

                item.add(deleteLink);
            }
        };

        add(new AjaxPagingNavigator("connectorsNavigator", connectorsView));

        connectorsContainer = new WebMarkupContainer("connectorsContainer");
        connectorsContainer.add(connectorsView);
        connectorsContainer.setOutputMarkupId(true);

        add(connectorsContainer);

        setWindowClosedCallback(createConnectorWin, connectorsContainer);
        setWindowClosedCallback(editConnectorWin, connectorsContainer);

        createConnectorWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createConnectorWin.setPageMapName("create-conn-modal");
        createConnectorWin.setCookieName("create-conn-modal");

        editConnectorWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editConnectorWin.setPageMapName("edit-conn-modal");
        editConnectorWin.setCookieName("edit-conn-modal");

        add(new AjaxLink("createConnectorLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {

                createConnectorWin.setPageCreator(new ModalWindow.PageCreator() {

                    public Page createPage() {
                        ConnectorsModalPage form = new ConnectorsModalPage(Connectors.this,
                                editConnectorWin, new ConnectorInstanceTO(), true);
                        return form;
                    }
                });

                createConnectorWin.show(target);
            }
        });

    }

    /**
     * Check if the delete action is forbidden
     * @param ConnectorInstanceTO object to check
     * @return true if the action is forbidden, false otherwise
     */
    public boolean checkDeleteIsForbidden(ConnectorInstanceTO connectorTO){

        boolean forbidden = false;
        List<ResourceTO> resources = resourcesRestClient.getAllResources().getResources();

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
    public void setWindowClosedCallback(ModalWindow window, final WebMarkupContainer container) {

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
}