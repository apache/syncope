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
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.to.UserRequestTO;
import org.syncope.client.to.WorkflowFormTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.console.SyncopeSession;
import org.syncope.console.commons.Constants;
import org.syncope.console.commons.PreferenceManager;
import org.syncope.console.commons.SortableDataProviderComparator;
import org.syncope.console.rest.ApprovalRestClient;
import org.syncope.console.rest.UserRequestRestClient;
import org.syncope.console.rest.UserRestClient;
import org.syncope.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.syncope.console.wicket.markup.html.form.ActionLink;
import org.syncope.console.wicket.markup.html.form.ActionLinksPanel;
import org.syncope.types.UserRequestType;

public class Todo extends BasePage {

    private static final long serialVersionUID = -7122136682275797903L;

    @SpringBean
    private ApprovalRestClient approvalRestClient;

    @SpringBean
    private UserRestClient userRestClient;

    @SpringBean
    private UserRequestRestClient userRequestRestClient;

    private final ModalWindow editApprovalWin;

    private final ModalWindow editUserRequestWin;

    private static final int APPROVAL_WIN_HEIGHT = 400;

    private static final int APPROVAL_WIN_WIDTH = 600;

    private final static int USER_REQUEST_WIN_HEIGHT = 550;

    private final static int USER_REQUESTL_WIN_WIDTH = 800;

    @SpringBean
    private PreferenceManager prefMan;

    private WebMarkupContainer approvalContainer;

    private WebMarkupContainer userRequestContainer;

    private int approvalPaginatorRows;

    private int userRequestPaginatorRows;

    public Todo(final PageParameters parameters) {
        super(parameters);

        add(editApprovalWin = new ModalWindow("editApprovalWin"));
        add(editUserRequestWin = new ModalWindow("editUserRequestWin"));

        setupApproval();
        setupUserRequest();
    }

    private void setupApproval() {
        approvalContainer = new WebMarkupContainer("approvalContainer");

        approvalPaginatorRows = prefMan.getPaginatorRows(getRequest(),
                Constants.PREF_APPROVAL_PAGINATOR_ROWS);

        List<IColumn> columns = new ArrayList<IColumn>();
        columns.add(new PropertyColumn(new ResourceModel("taskId"),
                "taskId", "taskId"));
        columns.add(new PropertyColumn(new ResourceModel("key"),
                "key", "key"));
        columns.add(new PropertyColumn(new ResourceModel("description"),
                "description", "description"));
        columns.add(new DatePropertyColumn(new ResourceModel("createTime"),
                "createTime", "createTime"));
        columns.add(new DatePropertyColumn(new ResourceModel("dueDate"),
                "dueDate", "dueDate"));
        columns.add(new PropertyColumn(new ResourceModel("owner"),
                "owner", "owner"));
        columns.add(new AbstractColumn<WorkflowFormTO>(
                new ResourceModel("actions", "")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public String getCssClass() {
                return "action";
            }

            @Override
            public void populateItem(
                    final Item<ICellPopulator<WorkflowFormTO>> cellItem,
                    final String componentId,
                    final IModel<WorkflowFormTO> model) {

                final WorkflowFormTO formTO = model.getObject();

                final ActionLinksPanel panel =
                        new ActionLinksPanel(componentId, model);

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {
                            approvalRestClient.claimForm(formTO.getTaskId());
                            info(getString("operation_succeded"));
                        } catch (SyncopeClientCompositeErrorException scee) {
                            error(getString("error") + ":" + scee.getMessage());
                        }
                        target.add(feedbackPanel);
                        target.add(approvalContainer);
                    }
                }, ActionLink.ActionType.CLAIM, "Approval", "claim");

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        editApprovalWin.setPageCreator(
                                new ModalWindow.PageCreator() {

                                    private static final long serialVersionUID = -7834632442532690940L;

                                    @Override
                                    public Page createPage() {
                                        return new ApprovalModalPage(
                                                Todo.this.getPageReference(),
                                                editApprovalWin, formTO);
                                    }
                                });

                        editApprovalWin.show(target);
                    }
                }, ActionLink.ActionType.EDIT, "Approval", "read",
                        SyncopeSession.get().getUserId().equals(
                        formTO.getOwner()));

                cellItem.add(panel);
            }
        });

        final AjaxFallbackDefaultDataTable approvalTable =
                new AjaxFallbackDefaultDataTable("approvalTable", columns,
                new ApprovalProvider(), approvalPaginatorRows);

        approvalContainer.add(approvalTable);
        approvalContainer.setOutputMarkupId(true);

        add(approvalContainer);

        Form approvalPaginatorForm = new Form("approvalPaginatorForm");

        final DropDownChoice rowsChooser = new DropDownChoice("rowsChooser",
                new PropertyModel(this, "approvalPaginatorRows"),
                prefMan.getPaginatorChoices());

        rowsChooser.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                prefMan.set(getRequest(),
                        getResponse(),
                        Constants.PREF_APPROVAL_PAGINATOR_ROWS,
                        String.valueOf(approvalPaginatorRows));
                approvalTable.setItemsPerPage(approvalPaginatorRows);

                target.add(approvalContainer);
            }
        });

        approvalPaginatorForm.add(rowsChooser);
        add(approvalPaginatorForm);

        editApprovalWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editApprovalWin.setInitialHeight(APPROVAL_WIN_HEIGHT);
        editApprovalWin.setInitialWidth(APPROVAL_WIN_WIDTH);
        editApprovalWin.setCookieName("edit-approval-modal");

        setWindowClosedCallback(editApprovalWin, approvalContainer);
    }

    private void setupUserRequest() {
        userRequestContainer = new WebMarkupContainer("userRequestContainer");

        userRequestPaginatorRows = prefMan.getPaginatorRows(getRequest(),
                Constants.PREF_USER_REQUEST_PAGINATOR_ROWS);

        List<IColumn> columns = new ArrayList<IColumn>();
        columns.add(new PropertyColumn(new ResourceModel("id"),
                "id", "id"));
        columns.add(new PropertyColumn(new ResourceModel("type"),
                "type", "type"));
        columns.add(new UserRequestColumn("user"));
        columns.add(new AbstractColumn<UserRequestTO>(
                new ResourceModel("actions", "")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public String getCssClass() {
                return "action";
            }

            @Override
            public void populateItem(
                    final Item<ICellPopulator<UserRequestTO>> cellItem,
                    final String componentId,
                    final IModel<UserRequestTO> model) {

                final UserRequestTO request = model.getObject();

                final ActionLinksPanel panel =
                        new ActionLinksPanel(componentId, model);

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        editUserRequestWin.setPageCreator(
                                new ModalWindow.PageCreator() {

                                    private static final long serialVersionUID = -7834632442532690940L;

                                    @Override
                                    public Page createPage() {
                                        return new UserRequestModalPage(
                                                Todo.this.getPageReference(),
                                                editUserRequestWin,
                                                model.getObject());
                                    }
                                });

                        editUserRequestWin.show(target);
                    }
                }, ActionLink.ActionType.EDIT, "UserRequest", "read",
                        model.getObject().getType() != UserRequestType.DELETE);

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {
                            userRestClient.delete(
                                    model.getObject().getUserId());
                            userRequestRestClient.delete(
                                    model.getObject().getId());
                        } catch (SyncopeClientCompositeErrorException e) {
                            LOG.error("While deleting an user", e);
                            error(e.getMessage());
                            return;
                        }

                        info(getString("operation_succeded"));
                        target.add(feedbackPanel);

                        target.add(userRequestContainer);
                    }
                }, ActionLink.ActionType.DELETE, "Users", "delete",
                        model.getObject().getType() == UserRequestType.DELETE);

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {
                            userRequestRestClient.delete(request.getId());
                        } catch (SyncopeClientCompositeErrorException e) {
                            LOG.error("While deleting an user request", e);
                            error(e.getMessage());
                            return;
                        }

                        info(getString("operation_succeded"));
                        target.add(feedbackPanel);

                        target.add(userRequestContainer);
                    }
                }, ActionLink.ActionType.DELETE, "UserRequest", "delete");

                cellItem.add(panel);
            }
        });

        final AjaxFallbackDefaultDataTable userRequestTable =
                new AjaxFallbackDefaultDataTable("userRequestTable", columns,
                new UserRequestProvider(), userRequestPaginatorRows);

        userRequestContainer.add(userRequestTable);
        userRequestContainer.setOutputMarkupId(true);

        add(userRequestContainer);

        Form userRequestPaginatorForm = new Form("userRequestPaginatorForm");

        final DropDownChoice rowsChooser = new DropDownChoice("rowsChooser",
                new PropertyModel(this, "userRequestPaginatorRows"),
                prefMan.getPaginatorChoices());

        rowsChooser.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                prefMan.set(getRequest(),
                        getResponse(),
                        Constants.PREF_USER_REQUEST_PAGINATOR_ROWS,
                        String.valueOf(userRequestPaginatorRows));
                userRequestTable.setItemsPerPage(userRequestPaginatorRows);

                target.add(userRequestContainer);
            }
        });

        userRequestPaginatorForm.add(rowsChooser);
        add(userRequestPaginatorForm);

        editUserRequestWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editUserRequestWin.setInitialHeight(USER_REQUEST_WIN_HEIGHT);
        editUserRequestWin.setInitialWidth(USER_REQUESTL_WIN_WIDTH);
        editUserRequestWin.setCookieName("edit-userRequest-modal");

        setWindowClosedCallback(editUserRequestWin, userRequestContainer);
    }

    private class ApprovalProvider
            extends SortableDataProvider<WorkflowFormTO> {

        private static final long serialVersionUID = -2311716167583335852L;

        private SortableDataProviderComparator<WorkflowFormTO> comparator;

        public ApprovalProvider() {
            //Default sorting
            setSort("key", SortOrder.ASCENDING);
            comparator =
                    new SortableDataProviderComparator<WorkflowFormTO>(this);
        }

        @Override
        public Iterator<WorkflowFormTO> iterator(final int first,
                final int count) {

            List<WorkflowFormTO> list = approvalRestClient.getForms();

            Collections.sort(list, comparator);

            return list.subList(first, first + count).iterator();
        }

        @Override
        public int size() {
            return approvalRestClient.getForms().size();
        }

        @Override
        public IModel<WorkflowFormTO> model(
                final WorkflowFormTO configuration) {

            return new AbstractReadOnlyModel<WorkflowFormTO>() {

                private static final long serialVersionUID =
                        -2566070996511906708L;

                @Override
                public WorkflowFormTO getObject() {
                    return configuration;
                }
            };
        }
    }

    private class UserRequestProvider
            extends SortableDataProvider<UserRequestTO> {

        private static final long serialVersionUID = -2311716167583335852L;

        private SortableDataProviderComparator<UserRequestTO> comparator;

        public UserRequestProvider() {
            //Default sorting
            setSort("id", SortOrder.ASCENDING);
            comparator =
                    new SortableDataProviderComparator<UserRequestTO>(this);
        }

        @Override
        public Iterator<UserRequestTO> iterator(final int first,
                final int count) {

            List<UserRequestTO> list = userRequestRestClient.list();

            Collections.sort(list, comparator);

            return list.subList(first, first + count).iterator();
        }

        @Override
        public int size() {
            return userRequestRestClient.list().size();
        }

        @Override
        public IModel<UserRequestTO> model(
                final UserRequestTO userRequestTO) {

            return new AbstractReadOnlyModel<UserRequestTO>() {

                private static final long serialVersionUID =
                        -2566070996511906708L;

                @Override
                public UserRequestTO getObject() {
                    return userRequestTO;
                }
            };
        }
    }

    private class UserRequestColumn extends AbstractColumn<UserRequestTO> {

        private static final long serialVersionUID = 8077865338230121496L;

        public UserRequestColumn(final String name) {
            super(new ResourceModel(name, name), name);
        }

        @Override
        public void populateItem(
                final Item<ICellPopulator<UserRequestTO>> cellItem,
                final String componentId, final IModel<UserRequestTO> rowModel) {

            String label = "";
            switch (rowModel.getObject().getType()) {
                case CREATE:
                    label = rowModel.getObject().getUserTO().getUsername();
                    if (label == null) {
                        label = getString("new_user");
                    }
                    break;

                case UPDATE:
                    label = String.valueOf(
                            rowModel.getObject().getUserMod().getId());
                    break;

                case DELETE:
                    label = String.valueOf(rowModel.getObject().getUserId());
                    break;

                default:
            }

            cellItem.add(new Label(componentId, label));
        }
    }
}
