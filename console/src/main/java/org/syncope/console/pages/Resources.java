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

import java.util.ArrayList;
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
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.to.ResourceTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.console.commons.Constants;
import org.syncope.console.commons.PreferenceManager;
import org.syncope.console.commons.SortableDataProviderComparator;
import org.syncope.console.rest.ResourceRestClient;
import org.syncope.console.wicket.ajax.markup.html.IndicatingDeleteOnConfirmAjaxLink;
import org.syncope.console.wicket.markup.html.form.DeleteLinkPanel;
import org.syncope.console.wicket.markup.html.form.EditLinkPanel;

/**
 * Resources WebPage.
 */
public class Resources extends BasePage {

    @SpringBean
    private ResourceRestClient restClient;

    @SpringBean
    private PreferenceManager prefMan;

    private final ModalWindow createResourceWin;

    private final ModalWindow editResourceWin;

    private static final int WIN_HEIGHT = 500;

    private static final int WIN_WIDTH = 775;

    private WebMarkupContainer container;

    /*
    Response flag set by the Modal Window after the operation is completed:
    TRUE if the operation succedes, FALSE otherwise
     */
    private boolean operationResult = false;

    private int paginatorRows;

    public Resources(PageParameters parameters) {
        super(parameters);

        add(createResourceWin = new ModalWindow("createResourceWin"));
        add(editResourceWin = new ModalWindow("editResourceWin"));

        add(feedbackPanel);

        paginatorRows = prefMan.getPaginatorRows(
                getWebRequestCycle().getWebRequest(),
                Constants.PREF_RESOURCES_PAGINATOR_ROWS);

        List<IColumn> columns = new ArrayList<IColumn>();

        columns.add(new PropertyColumn(new Model(getString("name")),
                "name", "name"));

        columns.add(new AbstractColumn<ResourceTO>(new Model<String>(
                getString("edit"))) {

            public void populateItem(Item<ICellPopulator<ResourceTO>> cellItem,
                    String componentId, IModel<ResourceTO> model) {
                final ResourceTO resourceTO = model.getObject();

                AjaxLink editLink = new IndicatingAjaxLink("editLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        editResourceWin.setPageCreator(new ModalWindow.PageCreator() {

                            public Page createPage() {
                                ResourceModalPage form = new ResourceModalPage(
                                        Resources.this, editResourceWin,
                                        resourceTO, false);
                                return form;
                            }
                        });

                        editResourceWin.show(target);
                    }
                };

                EditLinkPanel panel = new EditLinkPanel(componentId, model);
                panel.add(editLink);

                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                        xmlRolesReader.getAllAllowedRoles(
                        "Resources", "read"));

                cellItem.add(panel);
            }
        });

        columns.add(new AbstractColumn<ResourceTO>(new Model<String>(getString(
                "delete"))) {

            public void populateItem(Item<ICellPopulator<ResourceTO>> cellItem,
                    String componentId, IModel<ResourceTO> model) {
                final ResourceTO resourceTO = model.getObject();

                AjaxLink deleteLink = new IndicatingDeleteOnConfirmAjaxLink(
                        "deleteLink") {

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {

                            restClient.delete(resourceTO.getName());
                            info(getString("operation_succeded"));

                        } catch (SyncopeClientCompositeErrorException e) {
                            error(getString("operation_error"));

                            LOG.error("While deleting resource "
                                    + resourceTO.getName(), e);
                        }

                        target.addComponent(feedbackPanel);
                        target.addComponent(container);
                    }
                };
                DeleteLinkPanel panel = new DeleteLinkPanel(componentId, model);
                panel.add(deleteLink);

                String allowedRoles = xmlRolesReader.getAllAllowedRoles(
                        "Resources", "delete");

                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                        allowedRoles);

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
        createResourceWin.setInitialHeight(WIN_HEIGHT);
        createResourceWin.setInitialWidth(WIN_WIDTH);
        createResourceWin.setPageMapName("create-res-modal");
        createResourceWin.setCookieName("create-res-modal");

        editResourceWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editResourceWin.setInitialHeight(WIN_HEIGHT);
        editResourceWin.setInitialWidth(WIN_WIDTH);
        editResourceWin.setPageMapName("edit-res-modal");
        editResourceWin.setCookieName("edit-res-modal");

        add(new IndicatingAjaxLink("createResourceLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {

                createResourceWin.setPageCreator(new ModalWindow.PageCreator() {

                    @Override
                    public Page createPage() {
                        ResourceModalPage windows = new ResourceModalPage(
                                Resources.this, editResourceWin,
                                new ResourceTO(), true);
                        return windows;
                    }
                });

                createResourceWin.show(target);
            }
        });

        Form paginatorForm = new Form("PaginatorForm");

        final DropDownChoice rowsChooser = new DropDownChoice("rowsChooser",
                new PropertyModel(this, "paginatorRows"),
                prefMan.getPaginatorChoices());

        rowsChooser.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                prefMan.set(getWebRequestCycle().getWebRequest(),
                        getWebRequestCycle().getWebResponse(),
                        Constants.PREF_RESOURCES_PAGINATOR_ROWS,
                        String.valueOf(paginatorRows));
                table.setRowsPerPage(paginatorRows);

                target.addComponent(container);
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
                        if (operationResult) {
                            info(getString("operation_succeded"));
                            target.addComponent(feedbackPanel);
                            operationResult = false;
                        }
                    }
                });
    }

    class ResourcesProvider extends SortableDataProvider<ResourceTO> {

        private SortableDataProviderComparator<ResourceTO> comparator;

        public ResourcesProvider() {
            //Default sorting
            setSort("name", true);
            comparator =
                    new SortableDataProviderComparator<ResourceTO>(this);
        }

        @Override
        public Iterator<ResourceTO> iterator(int first, int count) {
            List<ResourceTO> list = getResourcesListDB();

            Collections.sort(list, comparator);

            return list.subList(first, first + count).iterator();
        }

        @Override
        public int size() {
            return getResourcesListDB().size();
        }

        @Override
        public IModel<ResourceTO> model(final ResourceTO resource) {
            return new AbstractReadOnlyModel<ResourceTO>() {

                @Override
                public ResourceTO getObject() {
                    return resource;
                }
            };
        }

        public List<ResourceTO> getResourcesListDB() {
            return restClient.getAllResources();
        }
    }
}
