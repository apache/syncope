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
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.to.ResourceTO;
import org.syncope.console.rest.ResourcesRestClient;

/**
 * Resources WebPage.
 */
public class Resources extends BasePage {

    @SpringBean(name = "resourcesRestClient")
    ResourcesRestClient restClient;

    final ModalWindow createResourceWin;
    final ModalWindow editResourceWin;

    final int WIN_INITIAL_HEIGHT = 515;
    final int WIN_INITIAL_WIDTH = 775;

    WebMarkupContainer resourcesContainer;

    public Resources(PageParameters parameters) {
        super(parameters);

        add(createResourceWin = new ModalWindow("createResourceWin"));
        add(editResourceWin = new ModalWindow("editResourceWin"));

        IModel resources = new LoadableDetachableModel() {

            protected Object load() {
                return restClient.getAllResources().getResources();
            }
        };

        ListView resourcesView = new ListView("resources", resources) {

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

                        editResourceWin.setPageCreator(new ModalWindow.PageCreator() {

                            public Page createPage() {
                                ResourceModalPage form = new ResourceModalPage
                                        (Resources.this, editResourceWin, resourceTO, false);
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
                        target.addComponent(resourcesContainer);
                    }
                };

                item.add(deleteLink);
            }
        };

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
                        ResourceModalPage form = new ResourceModalPage(Resources.this, editResourceWin, new ResourceTO(), true);
                        return form;
                    }
                });

                createResourceWin.show(target);
            }
        });
    }

    /**
     * Set a WindowClosedCallback for a ModalWindow instance.
     * @param window
     * @param container
     */
    public void setWindowClosedCallback(ModalWindow window, final WebMarkupContainer container) {

        window.setWindowClosedCallback(
                new ModalWindow.WindowClosedCallback() {

                    public void onClose(AjaxRequestTarget target) {
                        target.addComponent(container);
                    }
                });
    }
}