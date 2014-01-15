/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.console.pages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.syncope.console.commons.PageUtils;
import org.apache.syncope.common.to.AccountPolicyTO;
import org.apache.syncope.common.to.PasswordPolicyTO;
import org.apache.syncope.common.to.PolicyTO;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.SyncPolicyTO;
import org.apache.syncope.common.types.AbstractPolicySpec;
import org.apache.syncope.common.types.AccountPolicySpec;
import org.apache.syncope.common.types.PasswordPolicySpec;
import org.apache.syncope.common.types.PolicyType;
import org.apache.syncope.common.types.SyncPolicySpec;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.pages.panels.PolicyBeanPanel;
import org.apache.syncope.console.rest.PolicyRestClient;
import org.apache.syncope.console.rest.RoleRestClient;
import org.apache.syncope.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

/**
 * Modal window with Resource form.
 */
public class PolicyModalPage<T extends PolicyTO> extends BaseModalPage {

    private static final long serialVersionUID = -7325772767481076679L;

    private static final int WIN_HEIGHT = 600;

    private static final int WIN_WIDTH = 1100;

    @SpringBean
    private PolicyRestClient policyRestClient;

    @SpringBean
    private RoleRestClient roleRestClient;

    public PolicyModalPage(final ModalWindow window, final T policyTO) {
        super();

        final Form form = new Form(FORM);
        form.setOutputMarkupId(true);
        add(form);

        final AjaxTextFieldPanel policyid = new AjaxTextFieldPanel("id", "id",
                new PropertyModel<String>(policyTO, "id"));
        policyid.setEnabled(false);
        policyid.setStyleSheet("ui-widget-content ui-corner-all short_fixedsize");
        form.add(policyid);

        final AjaxTextFieldPanel description = new AjaxTextFieldPanel("description", "description",
                new PropertyModel<String>(policyTO, "description"));
        description.addRequiredLabel();
        description.setStyleSheet("ui-widget-content ui-corner-all medium_dynamicsize");
        form.add(description);

        final AjaxDropDownChoicePanel<PolicyType> type = new AjaxDropDownChoicePanel<PolicyType>("type", "type",
                new PropertyModel<PolicyType>(policyTO, "type"));

        switch (policyTO.getType()) {
            case GLOBAL_ACCOUNT:
            case ACCOUNT:
                type.setChoices(Arrays.asList(new PolicyType[] { PolicyType.GLOBAL_ACCOUNT, PolicyType.ACCOUNT }));
                break;

            case GLOBAL_PASSWORD:
            case PASSWORD:
                type.setChoices(Arrays.asList(new PolicyType[] { PolicyType.GLOBAL_PASSWORD, PolicyType.PASSWORD }));
                break;

            case GLOBAL_SYNC:
            case SYNC:
                type.setChoices(Arrays.asList(new PolicyType[] { PolicyType.GLOBAL_SYNC, PolicyType.SYNC }));

            default:
        }

        type.setChoiceRenderer(new PolicyTypeRenderer());

        type.addRequiredLabel();
        form.add(type);

        final AbstractPolicySpec policy = getPolicySpecification(policyTO);

        form.add(new PolicyBeanPanel("panel", policy));

        final ModalWindow mwindow = new ModalWindow("metaEditModalWin");
        mwindow.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        mwindow.setInitialHeight(WIN_HEIGHT);
        mwindow.setInitialWidth(WIN_WIDTH);
        mwindow.setCookieName("meta-edit-modal");
        add(mwindow);

        List<IColumn<String, String>> resColumns = new ArrayList<IColumn<String, String>>();
        resColumns.add(new AbstractColumn<String, String>(new StringResourceModel("name", this, null, "")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public void populateItem(final Item<ICellPopulator<String>> cellItem,
                    final String componentId, final IModel<String> rowModel) {

                cellItem.add(new Label(componentId, rowModel.getObject()));
            }
        });
        resColumns.add(new AbstractColumn<String, String>(new StringResourceModel("actions", this, null, "")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public String getCssClass() {
                return "action";
            }

            @Override
            public void populateItem(final Item<ICellPopulator<String>> cellItem, final String componentId,
                    final IModel<String> model) {

                final String resource = model.getObject();

                final ActionLinksPanel panel = new ActionLinksPanel(componentId, model,
                        PageUtils.getPageReference(getPage()));
                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        mwindow.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID = -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                return new ResourceModalPage(PageUtils.getPageReference(PolicyModalPage.this),
                                        mwindow, resourceRestClient.read(resource), false);
                            }
                        });

                        mwindow.show(target);
                    }
                }, ActionLink.ActionType.EDIT, "Resources");

                cellItem.add(panel);
            }
        });
        ISortableDataProvider<String, String> resDataProvider = new SortableDataProvider<String, String>() {

            private static final long serialVersionUID = 8263758912838836438L;

            @Override
            public Iterator<? extends String> iterator(final long first, final long count) {
                return policyTO.getId() == 0
                        ? Collections.<String>emptyList().iterator()
                        : policyRestClient.getPolicy(policyTO.getType(), policyTO.getId()).
                        getUsedByResources().subList((int) first, (int) first + (int) count).iterator();
            }

            @Override
            public long size() {
                return policyTO.getId() == 0
                        ? 0
                        : policyRestClient.getPolicy(policyTO.getType(), policyTO.getId()).
                        getUsedByResources().size();
            }

            @Override
            public IModel<String> model(final String object) {
                return new Model<String>(object);
            }
        };
        final AjaxFallbackDefaultDataTable<String, String> resources =
                new AjaxFallbackDefaultDataTable<String, String>("resources", resColumns, resDataProvider, 10);
        form.add(resources);

        List<IColumn<RoleTO, String>> roleColumns = new ArrayList<IColumn<RoleTO, String>>();
        roleColumns.add(new PropertyColumn<RoleTO, String>(new ResourceModel("id", "id"), "id", "id"));
        roleColumns.add(new PropertyColumn<RoleTO, String>(new ResourceModel("name", "name"), "name", "name"));
        roleColumns.add(new AbstractColumn<RoleTO, String>(new StringResourceModel("actions", this, null, "")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public String getCssClass() {
                return "action";
            }

            @Override
            public void populateItem(final Item<ICellPopulator<RoleTO>> cellItem, final String componentId,
                    final IModel<RoleTO> model) {

                final RoleTO role = model.getObject();

                final ActionLinksPanel panel = new ActionLinksPanel(componentId, model,
                        PageUtils.getPageReference(getPage()));
                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        mwindow.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID = -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                return new RoleModalPage(mwindow, role);
                            }
                        });

                        mwindow.show(target);
                    }
                }, ActionLink.ActionType.EDIT, "Roles");

                cellItem.add(panel);
            }
        });
        ISortableDataProvider<RoleTO, String> roleDataProvider = new SortableDataProvider<RoleTO, String>() {

            private static final long serialVersionUID = 8263758912838836438L;

            @Override
            public Iterator<? extends RoleTO> iterator(final long first, final long count) {
                List<RoleTO> roles = new ArrayList<RoleTO>();

                if (policyTO.getId() > 0) {
                    for (Long roleId : policyRestClient.getPolicy(policyTO.getType(), policyTO.getId()).
                            getUsedByRoles().subList((int) first, (int) first + (int) count)) {

                        roles.add(roleRestClient.read(roleId));
                    }
                }

                return roles.iterator();
            }

            @Override
            public long size() {
                return policyTO.getId() == 0
                        ? 0
                        : policyRestClient.getPolicy(policyTO.getType(), policyTO.getId()).
                        getUsedByRoles().size();
            }

            @Override
            public IModel<RoleTO> model(final RoleTO object) {
                return new Model<RoleTO>(object);
            }
        };
        final AjaxFallbackDefaultDataTable<RoleTO, String> roles =
                new AjaxFallbackDefaultDataTable<RoleTO, String>("roles", roleColumns, roleDataProvider, 10);
        form.add(roles);
        mwindow.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                target.add(resources);
                target.add(roles);
                if (isModalResult()) {
                    info(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(feedbackPanel);
                    setModalResult(false);
                }
            }
        });

        final AjaxButton submit = new IndicatingAjaxButton(APPLY, new ResourceModel(APPLY)) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                setPolicySpecification(policyTO, policy);

                try {
                    if (policyTO.getId() > 0) {
                        policyRestClient.updatePolicy(policyTO);
                    } else {
                        policyRestClient.createPolicy(policyTO);
                    }

                    window.close(target);
                } catch (Exception e) {
                    LOG.error("While creating policy", e);

                    error(getString(Constants.ERROR) + ":" + e.getMessage());
                    target.add(getPage().get(Constants.FEEDBACK));
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                target.add(getPage().get(Constants.FEEDBACK));
            }
        };

        form.add(submit);

        final IndicatingAjaxButton cancel = new IndicatingAjaxButton(CANCEL, new ResourceModel(CANCEL)) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                window.close(target);
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
            }
        };

        cancel.setDefaultFormProcessing(false);
        form.add(cancel);
    }

    private AbstractPolicySpec getPolicySpecification(final PolicyTO policyTO) {
        AbstractPolicySpec spec;

        switch (policyTO.getType()) {
            case GLOBAL_ACCOUNT:
            case ACCOUNT:
                spec = ((AccountPolicyTO) policyTO).getSpecification() != null
                        ? ((AccountPolicyTO) policyTO).getSpecification()
                        : new AccountPolicySpec();
                break;

            case GLOBAL_PASSWORD:
            case PASSWORD:
                spec = ((PasswordPolicyTO) policyTO).getSpecification() != null
                        ? ((PasswordPolicyTO) policyTO).getSpecification()
                        : new PasswordPolicySpec();
                break;

            case GLOBAL_SYNC:
            case SYNC:
            default:
                spec = ((SyncPolicyTO) policyTO).getSpecification() != null
                        ? ((SyncPolicyTO) policyTO).getSpecification()
                        : new SyncPolicySpec();
        }

        return spec;
    }

    private void setPolicySpecification(final PolicyTO policyTO, final AbstractPolicySpec specification) {
        switch (policyTO.getType()) {
            case GLOBAL_ACCOUNT:
            case ACCOUNT:
                if (!(specification instanceof AccountPolicySpec)) {
                    throw new ClassCastException("policy is type Account, but spec is not: " + specification.getClass().
                            getName());
                }
                ((AccountPolicyTO) policyTO).setSpecification((AccountPolicySpec) specification);
                break;

            case GLOBAL_PASSWORD:
            case PASSWORD:
                if (!(specification instanceof PasswordPolicySpec)) {
                    throw new ClassCastException("policy is type Password, but spec is not: "
                            + specification.getClass().getName());
                }
                ((PasswordPolicyTO) policyTO).setSpecification((PasswordPolicySpec) specification);
                break;

            case GLOBAL_SYNC:
            case SYNC:
                if (!(specification instanceof SyncPolicySpec)) {
                    throw new ClassCastException("policy is type Sync, but spec is not: " + specification.getClass().
                            getName());
                }
                ((SyncPolicyTO) policyTO).setSpecification((SyncPolicySpec) specification);

            default:
        }
    }

    private class PolicyTypeRenderer extends ChoiceRenderer<PolicyType> {

        private static final long serialVersionUID = -8993265421104002134L;

        @Override
        public Object getDisplayValue(final PolicyType object) {
            return getString(object.name());
        }
    };
}
