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
package org.syncope.console.pages.panels;

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
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.syncope.client.to.PasswordPolicyTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.console.commons.Constants;
import org.syncope.console.commons.PreferenceManager;
import org.syncope.console.commons.SortableDataProviderComparator;
import org.syncope.console.commons.XMLRolesReader;
import org.syncope.console.pages.PasswordPolicyModalPage;
import org.syncope.console.rest.PolicyRestClient;
import org.syncope.console.wicket.ajax.markup.html.IndicatingDeleteOnConfirmAjaxLink;
import org.syncope.console.wicket.markup.html.form.DeleteLinkPanel;
import org.syncope.console.wicket.markup.html.form.EditLinkPanel;

public class PasswordPoliciesPanel extends Panel {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(
            PasswordPoliciesPanel.class);

    private final static int MODAL_WIN_HEIGHT = 400;

    private final static int MODAL_WIN_WIDTH = 1000;

    private static final long serialVersionUID = -6804066913177804275L;

    @SpringBean
    private PolicyRestClient policyRestClient;

    @SpringBean
    protected XMLRolesReader xmlRolesReader;

    @SpringBean
    private PreferenceManager prefMan;

    final private int paginatorRows = prefMan.getPaginatorRows(
            getWebRequest(), Constants.PREF_POLICY_PAGINATOR_ROWS);

    protected boolean modalResult = false;

    public PasswordPoliciesPanel(final String id) {
        super(id);

        // Modal window for editing user attributes
        final ModalWindow mwindow =
                new ModalWindow("editModalWin");
        mwindow.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        mwindow.setInitialHeight(MODAL_WIN_HEIGHT);
        mwindow.setInitialWidth(MODAL_WIN_WIDTH);
        mwindow.setCookieName("policy-modal");
        add(mwindow);

        // Container for user list
        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        setWindowClosedCallback(mwindow, container);

        final List<IColumn> columns = new ArrayList<IColumn>();

        columns.add(new PropertyColumn(
                new ResourceModel("id"), "id", "id"));

        columns.add(new PropertyColumn(
                new ResourceModel("description"), "description", "description"));

        columns.add(new PropertyColumn(
                new ResourceModel("type"), "type", "type"));

        columns.add(new AbstractColumn<PasswordPolicyTO>(
                new ResourceModel("edit")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<PasswordPolicyTO>> cellItem,
                    final String componentId,
                    final IModel<PasswordPolicyTO> model) {

                final PasswordPolicyTO passwordPolicyTO = model.getObject();

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
                                final PasswordPolicyModalPage page =
                                        new PasswordPolicyModalPage(
                                        mwindow, passwordPolicyTO);
                                return page;
                            }
                        });

                        mwindow.show(target);
                    }
                };

                final EditLinkPanel panel =
                        new EditLinkPanel(componentId, model);
                panel.add(editLink);

                MetaDataRoleAuthorizationStrategy.authorize(
                        panel, ENABLE, xmlRolesReader.getAllAllowedRoles(
                        "Policies", "read"));

                cellItem.add(panel);
            }
        });

        columns.add(new AbstractColumn<PasswordPolicyTO>(
                new ResourceModel("delete")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<PasswordPolicyTO>> cellItem,
                    final String componentId,
                    final IModel<PasswordPolicyTO> model) {
                final PasswordPolicyTO passwordPolicyTO = model.getObject();

                AjaxLink deleteLink = new IndicatingDeleteOnConfirmAjaxLink(
                        "deleteLink") {

                    private static final long serialVersionUID =
                            -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {

                            policyRestClient.delete(passwordPolicyTO.getId());
                            info(getString("operation_succeded"));

                        } catch (SyncopeClientCompositeErrorException e) {
                            error(getString("operation_error"));

                            LOG.error("While deleting resource {}({})",
                                    new Object[]{
                                        passwordPolicyTO.getId(),
                                        passwordPolicyTO.getDescription()},
                                    e);
                        }

                        target.add(container);
                        target.add(getPage().get("feedback"));
                    }
                };

                final DeleteLinkPanel panel =
                        new DeleteLinkPanel(componentId, model);
                panel.add(deleteLink);

                MetaDataRoleAuthorizationStrategy.authorize(
                        panel, ENABLE, xmlRolesReader.getAllAllowedRoles(
                        "Policies", "delete"));

                cellItem.add(panel);
            }
        });

        final AjaxFallbackDefaultDataTable table =
                new AjaxFallbackDefaultDataTable(
                "datatable", columns, new PolicyDataProvider(), paginatorRows);

        container.add(table);

        final IndicatingAjaxLink createButton =
                new IndicatingAjaxLink("createLink") {

                    private static final long serialVersionUID =
                            -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {

                        mwindow.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID =
                                    -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                final PasswordPolicyModalPage page =
                                        new PasswordPolicyModalPage(
                                        mwindow, new PasswordPolicyTO());
                                return page;
                            }
                        });

                        mwindow.show(target);
                    }
                };

        add(createButton);

        MetaDataRoleAuthorizationStrategy.authorize(
                createButton, ENABLE, xmlRolesReader.getAllAllowedRoles(
                "Policies", "create"));

        final Form paginatorForm = new Form("PaginatorForm");

        final DropDownChoice rowsChooser = new DropDownChoice("rowsChooser",
                new PropertyModel(this, "paginatorRows"),
                prefMan.getPaginatorChoices());

        rowsChooser.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                prefMan.set(getWebRequest(), (WebResponse) getResponse(),
                        Constants.PREF_POLICY_PAGINATOR_ROWS,
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
    private void setWindowClosedCallback(
            final ModalWindow window,
            final WebMarkupContainer container) {

        window.setWindowClosedCallback(
                new ModalWindow.WindowClosedCallback() {

                    private static final long serialVersionUID =
                            8804221891699487139L;

                    @Override
                    public void onClose(AjaxRequestTarget target) {
                        target.add(container);
                    }
                });
    }

    private class PolicyDataProvider
            extends SortableDataProvider<PasswordPolicyTO> {

        private static final long serialVersionUID = -6976327453925166730L;

        private SortableDataProviderComparator comparator;

        public PolicyDataProvider() {
            super();

            //Default sorting
            setSort("description", SortOrder.ASCENDING);

            comparator =
                    new SortableDataProviderComparator<PasswordPolicyTO>(this);
        }

        @Override
        public int size() {
            return policyRestClient.getPasswordPolicies().size();
        }

        @Override
        public Iterator<PasswordPolicyTO> iterator(
                final int first, final int count) {

            final List<PasswordPolicyTO> policies =
                    policyRestClient.getPasswordPolicies();

            Collections.sort(policies, comparator);

            return policies.subList(first, first + count).iterator();
        }

        @Override
        public IModel<PasswordPolicyTO> model(final PasswordPolicyTO object) {
            return new CompoundPropertyModel<PasswordPolicyTO>(object);
        }
    }
}
