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

import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.syncope.client.to.ConfigurationTO;
import org.syncope.console.commons.Utility;
import org.syncope.console.rest.ConfigurationsRestClient;

/**
 * Configurations WebPage.
 */
public class Configuration extends BasePage{

    @SpringBean(name = "configurationsRestClient")
    ConfigurationsRestClient restClient;

    @SpringBean(name = "utility")
    Utility utility;

    final ModalWindow createConfigWin;
    final ModalWindow editConfigWin;

    final int WIN_USER_HEIGHT = 680;
    final int WIN_USER_WIDTH = 1133;

    WebMarkupContainer container;

    /** Response flag set by the Modal Window after the operation
     * is completed  */
    boolean operationResult = false;

    FeedbackPanel feedbackPanel;
    
    public Configuration(PageParameters parameters) {
        super(parameters);

        feedbackPanel = new FeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId( true );

        add(feedbackPanel);

        add(createConfigWin = new ModalWindow("createConfigurationWin"));
        add(editConfigWin = new ModalWindow("editConfigurationWin"));

        final IModel configurations = new LoadableDetachableModel() {

            protected Object load() {
                return restClient.getAllConfigurations().getConfigurations();
            }
        };

        PageableListView configurationsView = new PageableListView
                ("configurations",configurations, 
                utility.getPaginatorRowsToDisplay("configuration")) {

            @Override
            protected void populateItem(final ListItem item) {

                final ConfigurationTO configurationTO =
                        (ConfigurationTO)item.getModelObject();

                item.add(new Label("key",configurationTO.getConfKey()));
                item.add(new Label("value",configurationTO.getConfValue()));

                AjaxLink editLink = new AjaxLink("editLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        editConfigWin.setPageCreator(
                                new ModalWindow.PageCreator() {

                            public Page createPage() {
                                ConfigurationModalPage window =
                                        new ConfigurationModalPage
                                        (Configuration.this, editConfigWin, configurationTO, false);
                                return window;
                            }
                        });

                        editConfigWin.show(target);
                    }
                };

                item.add(editLink);

                AjaxLink deleteLink = new AjaxLink("deleteLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        try {
                            restClient.deleteConfiguration(configurationTO.getConfKey());
                        } catch (UnsupportedEncodingException ex) {
                            Logger.getLogger(Configuration.class.getName()).log(Level.SEVERE, null, ex);
                            error(ex.getMessage());
                            return;
                        }

                        info(getString("operation_succeded"));
                        target.addComponent(feedbackPanel);

                        target.addComponent(container);
                    }
                };

                item.add(deleteLink);
                
            }
        };

        add(new AjaxPagingNavigator("configurationsNavigator", configurationsView));

        container = new WebMarkupContainer("container");
        container.add(configurationsView);
        container.setOutputMarkupId(true);

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

        add(container);

        add(new AjaxLink("createConfigurationLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {

                createConfigWin.setPageCreator(new ModalWindow.PageCreator() {

                    public Page createPage() {
                        ConfigurationModalPage window = new ConfigurationModalPage(Configuration.this,
                                createConfigWin, new ConfigurationTO(), true);
                        return window;
                    }
                });

                createConfigWin.show(target);
            }
        });
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
