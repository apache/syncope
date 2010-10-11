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


import org.apache.wicket.Page;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PageableListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.to.ResourceTO;
import org.syncope.console.commons.Constants;
import org.syncope.console.commons.Utility;
import org.syncope.console.rest.ResourcesRestClient;

/**
 * Resources WebPage.
 */
public class Resources extends BasePage {

    @SpringBean(name = "resourcesRestClient")
    ResourcesRestClient restClient;

    @SpringBean(name = "utility")
    Utility utility;

    final ModalWindow createResourceWin;
    final ModalWindow editResourceWin;

    final int WIN_INITIAL_HEIGHT = 515;
    final int WIN_INITIAL_WIDTH = 775;

    WebMarkupContainer resourcesContainer;

    /** Response flag set by the Modal Window after the operation is completed:
     *  TRUE if the operation succedes, FALSE otherwise
     */
    boolean operationResult = false;
    FeedbackPanel feedbackPanel;

    private int paginatorRows;

    public Resources(PageParameters parameters) {
        super(parameters);

        add(createResourceWin = new ModalWindow("createResourceWin"));
        add(editResourceWin = new ModalWindow("editResourceWin"));

        feedbackPanel = new FeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId( true );

        add(feedbackPanel);

        IModel resources = new LoadableDetachableModel() {

            protected Object load() {
                return restClient.getAllResources().getResources();
            }
        };

        paginatorRows = utility.getPaginatorRowsToDisplay(Constants
                .CONF_RESOURCES_PAGINATOR_ROWS);

        final PageableListView resourcesView = new PageableListView("resources",
                resources, paginatorRows) {

            @Override
            protected void populateItem(final ListItem item) {
                final ResourceTO resourceTO =
                        (ResourceTO) item.getDefaultModelObject();

                item.add(new Label("name", resourceTO.getName()));


                AjaxLink editLink = new AjaxLink("editLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        final ResourceTO resourceTO =
                                (ResourceTO) item.getDefaultModelObject();

                        editResourceWin.setPageCreator(new ModalWindow
                                .PageCreator() {

                            public Page createPage() {
                                ResourceModalPage form = new ResourceModalPage
                                        (Resources.this, editResourceWin,
                                        resourceTO, false);
                                return form;
                            }
                        });

                        editResourceWin.show(target);
                    }
                };

                item.add(editLink);

                AjaxLink deleteLink = new AjaxLink("deleteLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        restClient.deleteResource(resourceTO.getName());

                        info(getString("operation_succeded"));
                        target.addComponent(feedbackPanel);

                        target.addComponent(resourcesContainer);
                    }
                };

                item.add(deleteLink);
            }
        };

        add(new AjaxPagingNavigator("resourcesNavigator", resourcesView)
                .setOutputMarkupId(true));

        resourcesContainer = new WebMarkupContainer("resourcesContainer");
        resourcesContainer.add(resourcesView);
        resourcesContainer.setOutputMarkupId(true);

        add(resourcesContainer);

        setWindowClosedCallback(createResourceWin, resourcesContainer);
        setWindowClosedCallback(editResourceWin, resourcesContainer);

        createResourceWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createResourceWin.setInitialHeight(WIN_INITIAL_HEIGHT);
        createResourceWin.setInitialWidth(WIN_INITIAL_WIDTH);
        createResourceWin.setPageMapName("create-res-modal");
        createResourceWin.setCookieName("create-res-modal");

        editResourceWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editResourceWin.setInitialHeight(WIN_INITIAL_HEIGHT);
        editResourceWin.setInitialWidth(WIN_INITIAL_WIDTH);
        editResourceWin.setPageMapName("edit-res-modal");
        editResourceWin.setCookieName("edit-res-modal");

        add(new AjaxLink("createResourceLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {

                createResourceWin.setPageCreator(new ModalWindow.PageCreator() {

                    public Page createPage() {
                        ResourceModalPage windows = new ResourceModalPage
                                (Resources.this, editResourceWin,
                                new ResourceTO(), true);
                        return windows;
                    }
                });

                createResourceWin.show(target);
            }
        });

        Form paginatorForm = new Form("PaginatorForm");

        final DropDownChoice rowsChooser = new DropDownChoice("rowsChooser",
        new PropertyModel(this,"paginatorRows"),utility.paginatorRowsChooser());

        rowsChooser.add(new AjaxFormComponentUpdatingBehavior( "onchange" ){
          protected void onUpdate( AjaxRequestTarget target )
            {
              utility.updatePaginatorRows(Constants.CONF_RESOURCES_PAGINATOR_ROWS,
                      paginatorRows);

              resourcesView.setRowsPerPage(paginatorRows);
              
              target.addComponent(resourcesContainer);
              target.addComponent(getPage().get("resourcesNavigator"));
            }

          });

        paginatorForm.add(rowsChooser);
        add(paginatorForm);
    }

    public boolean isOperationResult() {
        return operationResult;
    }

    public void setOperationResult(boolean operationResult) {
        this.operationResult = operationResult;
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
}