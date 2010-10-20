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
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table
        .AjaxFallbackDefaultDataTable;
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
import org.syncope.client.to.ResourceTO;
import org.syncope.console.commons.Constants;
import org.syncope.console.commons.Utility;
import org.syncope.console.rest.ResourcesRestClient;
import org.syncope.console.wicket.markup.html.form.DeleteLinkPanel;
import org.syncope.console.wicket.markup.html.form.EditLinkPanel;

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

    WebMarkupContainer container;

    /*
     Response flag set by the Modal Window after the operation is completed:
     TRUE if the operation succedes, FALSE otherwise
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

        paginatorRows = utility.getPaginatorRowsToDisplay(Constants
                .CONF_RESOURCES_PAGINATOR_ROWS);

        List<IColumn> columns = new ArrayList<IColumn>();

        columns.add(new PropertyColumn(new Model(getString("name")),
                "name", "name"));

        columns.add(new AbstractColumn<ResourceTO>(new Model<String>(
                getString("edit")))
        {
            public void populateItem(Item<ICellPopulator<ResourceTO>>
                    cellItem, String componentId, IModel<ResourceTO> model)
            {
                    final ResourceTO resourceTO = model.getObject();

                    AjaxLink editLink = new AjaxLink("editLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {

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

                    EditLinkPanel panel = new EditLinkPanel(componentId, model);
                    panel.add(editLink);

                    cellItem.add(panel);
            }
        });

        columns.add(new AbstractColumn<ResourceTO>(new Model<String>
                (getString("delete"))) {
            public void populateItem(Item<ICellPopulator<ResourceTO>>
                    cellItem, String componentId, IModel<ResourceTO> model)
            {
                    final ResourceTO resourceTO = model.getObject();

                    AjaxLink deleteLink = new AjaxLink("deleteLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        restClient.deleteResource(resourceTO.getName());

                        info(getString("operation_succeded"));
                        target.addComponent(feedbackPanel);

                        target.addComponent(container);
                    }
                 };
                    DeleteLinkPanel panel = new DeleteLinkPanel(componentId,
                            model);
                    panel.add(deleteLink);

                    cellItem.add(panel);
            }
        });


        final AjaxFallbackDefaultDataTable table =
                new AjaxFallbackDefaultDataTable("datatable", columns,
                new ResourcesProvider(), paginatorRows);

        container = new WebMarkupContainer("container");
        container.add(table);
        container.setOutputMarkupId(true);

        add(container);

        setWindowClosedCallback(createResourceWin, container);
        setWindowClosedCallback(editResourceWin, container);

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

              table.setRowsPerPage(paginatorRows);
              
              target.addComponent(container);
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

    class ResourcesProvider extends SortableDataProvider<ResourceTO> {

        private SortableDataProviderComparator comparator =
                new SortableDataProviderComparator();

        public ResourcesProvider() {
            //Default sorting
            setSort("name",true);
        }

        @Override
        public Iterator<ResourceTO> iterator(int first, int count) {
            List<ResourceTO> list = getResourcesListDB();

            Collections.sort(list, comparator);

            return list.subList(first, first+count).iterator();
        }

        @Override
        public int size() {
            return getResourcesListDB().size();
        }

        @Override
        public IModel<ResourceTO> model(final ResourceTO
                resource) {
            return new AbstractReadOnlyModel<ResourceTO>() {

                @Override
                public ResourceTO getObject() {
                    return resource;
                }
            };
        }

        public List<ResourceTO> getResourcesListDB(){
        List<ResourceTO> list = restClient.getAllResources();

        return list;
        }

        class SortableDataProviderComparator implements
                Comparator<ResourceTO>, Serializable {
            public int compare(final ResourceTO o1,
                    final ResourceTO o2) {
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