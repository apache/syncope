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
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
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
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
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

    private static final long serialVersionUID = -3789252860990261728L;

    @SpringBean
    private ResourceRestClient restClient;

    @SpringBean
    private PreferenceManager prefMan;

    private final ModalWindow createResourceWin;

    private final ModalWindow mwindow;

    private static final int WIN_HEIGHT = 500;

    private static final int WIN_WIDTH = 900;

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
        add(mwindow = new ModalWindow("editResourceWin"));

        add(feedbackPanel);

        paginatorRows = prefMan.getPaginatorRows(getRequest(),
                Constants.PREF_RESOURCES_PAGINATOR_ROWS);

        List<IColumn> columns = new ArrayList<IColumn>();

        columns.add(new PropertyColumn(
                new ResourceModel("name"), "name", "name"));
        columns.add(new PropertyColumn(
                new ResourceModel("propagationPrimary"),
                "propagationPrimary", "propagationPrimary"));
        columns.add(new PropertyColumn(
                new ResourceModel("propagationPriority"),
                "propagationPriority", "propagationPriority"));

        columns.add(new AbstractColumn<ResourceTO>(new ResourceModel("edit")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<ResourceTO>> cellItem,
                    final String componentId,
                    final IModel<ResourceTO> model) {
                final ResourceTO resourceTO = model.getObject();

                AjaxLink editLink = new IndicatingAjaxLink("editLink") {

                    private static final long serialVersionUID =
                            -7978723352517770644L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        mwindow.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID =
                                    -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                ResourceModalPage form = new ResourceModalPage(
                                        Resources.this.getPageReference(),
                                        mwindow, resourceTO, false);
                                return form;
                            }
                        });

                        mwindow.show(target);
                    }
                };

                final EditLinkPanel panel =
                        new EditLinkPanel(componentId, model);
                panel.add(editLink);

                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                        xmlRolesReader.getAllAllowedRoles(
                        "Resources", "read"));

                cellItem.add(panel);
            }
        });

        columns.add(new AbstractColumn<ResourceTO>(new ResourceModel("delete")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<ResourceTO>> cellItem,
                    final String componentId,
                    final IModel<ResourceTO> model) {
                final ResourceTO resourceTO = model.getObject();

                AjaxLink deleteLink = new IndicatingDeleteOnConfirmAjaxLink(
                        "deleteLink") {

                    private static final long serialVersionUID =
                            -7978723352517770644L;

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

                        target.add(feedbackPanel);
                        target.add(container);
                    }
                };

                final DeleteLinkPanel panel =
                        new DeleteLinkPanel(componentId, model);
                panel.add(deleteLink);

                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                        xmlRolesReader.getAllAllowedRoles(
                        "Resources", "delete"));

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
        setWindowClosedCallback(mwindow, container);

        createResourceWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createResourceWin.setInitialHeight(WIN_HEIGHT);
        createResourceWin.setInitialWidth(WIN_WIDTH);
        createResourceWin.setCookieName("create-res-modal");

        mwindow.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        mwindow.setInitialHeight(WIN_HEIGHT);
        mwindow.setInitialWidth(WIN_WIDTH);
        mwindow.setCookieName("edit-res-modal");

        add(new IndicatingAjaxLink("createResourceLink") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(AjaxRequestTarget target) {

                createResourceWin.setPageCreator(new ModalWindow.PageCreator() {

                    private static final long serialVersionUID =
                            -7834632442532690940L;

                    @Override
                    public Page createPage() {
                        final ResourceModalPage windows = new ResourceModalPage(
                                Resources.this.getPageReference(), mwindow,
                                new ResourceTO(), true);
                        return windows;
                    }
                });

                createResourceWin.show(target);
            }
        });

        final Form paginatorForm = new Form("PaginatorForm");

        final DropDownChoice rowsChooser = new DropDownChoice("rowsChooser",
                new PropertyModel(this, "paginatorRows"),
                prefMan.getPaginatorChoices());

        rowsChooser.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                prefMan.set(getRequest(), getResponse(),
                        Constants.PREF_RESOURCES_PAGINATOR_ROWS,
                        String.valueOf(paginatorRows));

                table.setItemsPerPage(paginatorRows);
                target.add(container);
            }
        });

        paginatorForm.add(rowsChooser);
        add(paginatorForm);
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

                    @Override
                    public void onClose(AjaxRequestTarget target) {
                        target.add(container);
                        if (operationResult) {
                            info(getString("operation_succeded"));
                            target.add(feedbackPanel);
                            operationResult = false;
                        }
                    }
                });
    }

    public void setOperationResult(boolean result) {
        operationResult = result;
    }

    class ResourcesProvider extends SortableDataProvider<ResourceTO> {

        private static final long serialVersionUID = -9055916672926643975L;

        private SortableDataProviderComparator<ResourceTO> comparator;

        public ResourcesProvider() {
            //Default sorting
            setSort("name", SortOrder.ASCENDING);
            comparator =
                    new SortableDataProviderComparator<ResourceTO>(this);
        }

        @Override
        public Iterator<ResourceTO> iterator(final int first, final int count) {
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
