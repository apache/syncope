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
package org.apache.syncope.client.console.pages;

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.BookmarkablePageLinkBuilder;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.panels.RealmDirectoryPanel;
import org.apache.syncope.client.console.panels.search.RealmSearchPanel;
import org.apache.syncope.client.console.panels.search.SearchClause;
import org.apache.syncope.client.console.panels.search.SearchClausePanel;
import org.apache.syncope.client.console.panels.search.SearchUtils;
import org.apache.syncope.client.console.rest.RealmRestClient;
import org.apache.syncope.client.console.tasks.TemplatesTogglePanel;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.panels.WizardModalPanel;
import org.apache.syncope.client.ui.commons.wicket.markup.html.bootstrap.tabs.Accordion;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.to.TemplatableTO;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class Realms extends BasePage {

    private static final long serialVersionUID = -810612679853772333L;

    @SpringBean
    protected RealmRestClient realmRestClient;

    protected final RealmDirectoryPanel realmDirectoryPanel;

    protected RealmSearchPanel searchPanel;

    protected final TemplatesTogglePanel templates;

    protected final BaseModal<Serializable> templateModal;

    public Realms(final PageParameters parameters) {
        super(parameters);

        body.add(BookmarkablePageLinkBuilder.build("dashboard", "dashboardBr", Dashboard.class));

        List<SearchClause> clauses = new ArrayList<>();

        Model<Integer> model = Model.of(-1);
        body.add(new Accordion("accordionPanel",
                List.of(new AbstractTab(new ResourceModel("search.result")) {

                    private static final long serialVersionUID = 1037272333056449377L;

                    @Override
                    public WebMarkupContainer getPanel(final String panelId) {
                        searchPanel = new RealmSearchPanel.Builder(new ListModel<>(clauses), getPageReference()).
                                required(true).enableSearch().build(panelId);
                        return searchPanel;
                    }
                }), model) {

            private static final long serialVersionUID = -3056452800492734900L;

            @Override
            protected Component newTitle(final String markupId, final ITab tab, final Accordion.State state) {
                return new AjaxLink<Integer>(markupId) {

                    private static final long serialVersionUID = 6250423506463465679L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        model.setObject(model.getObject() == 0 ? -1 : 0);
                    }
                }.setBody(tab.getTitle()).setEscapeModelStrings(false);
            }
        });

        realmDirectoryPanel = new RealmDirectoryPanel("directoryPanel", realmRestClient, getPageReference());
        realmDirectoryPanel.setOutputMarkupId(true);
        body.add(realmDirectoryPanel);

        templates = new TemplatesTogglePanel(BaseModal.CONTENT_ID, this, getPageReference()) {

            private static final long serialVersionUID = 4828350561653999922L;

            @Override
            protected Serializable onApplyInternal(
                    final TemplatableTO targetObject, final String type, final AnyTO anyTO) {

                targetObject.getTemplates().put(type, anyTO);
                realmRestClient.update(RealmTO.class.cast(targetObject));
                return targetObject;
            }
        };
        body.add(templates);

        templateModal = new BaseModal<>("templateModal") {

            private static final long serialVersionUID = 5787433530654262016L;

            @Override
            protected void onConfigure() {
                super.onConfigure();
                setFooterVisible(false);
            }
        };
        templateModal.size(Modal.Size.Extra_large);
        body.add(templateModal);

        templateModal.setWindowClosedCallback(target -> templateModal.show(false));
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof SearchClausePanel.SearchEvent payload && searchPanel != null) {
            List<SearchClause> clauses = searchPanel.getModel().getObject();
            String base = clauses.stream().
                    filter(Realms::isBaseClause).
                    map(SearchClause::getValue).
                    filter(StringUtils::isNotBlank).
                    findFirst().
                    orElse(StringUtils.EMPTY);

            List<SearchClause> fiqlClauses = clauses.stream()
                    .filter(clause -> !isBaseClause(clause))
                    .filter(Realms::isValid)
                    .toList();

            realmDirectoryPanel.search(base, Optional.ofNullable(SearchUtils.buildFIQL(
                    fiqlClauses,
                    SyncopeClient.getRealmFiqlSearchConditionBuilder())).orElse(StringUtils.EMPTY),
                    payload.getTarget());
        } else if (event.getPayload() instanceof AjaxWizard.NewItemEvent<?> newItemEvent) {
            WizardModalPanel<?> modalPanel = newItemEvent.getModalPanel();

            if (event.getPayload() instanceof AjaxWizard.NewItemActionEvent && modalPanel != null) {
                final IModel<Serializable> model = new CompoundPropertyModel<>(modalPanel.getItem());
                templateModal.setFormModel(model);
                templateModal.header(newItemEvent.getTitleModel());
                newItemEvent.getTarget().ifPresent(t -> t.add(templateModal.setContent(modalPanel)));
                templateModal.show(true);
            } else if (event.getPayload() instanceof AjaxWizard.NewItemCancelEvent) {
                newItemEvent.getTarget().ifPresent(templateModal::close);
            } else if (event.getPayload() instanceof AjaxWizard.NewItemFinishEvent) {
                SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                newItemEvent.getTarget().ifPresent(t -> {
                    ((BasePage) getPage()).getNotificationPanel().refresh(t);
                    templateModal.close(t);
                });
            }
        } else if (event.getPayload() instanceof TemplatesTogglePanel.ShowTemplatesTogglePanelEvent action) {
            templates.setTargetObject(action.realmTO());
            templates.toggle(action.target(), true);
        } else {
            super.onEvent(event);
        }
    }

    private static boolean isBaseClause(final SearchClause clause) {
        return clause.getType() == SearchClause.Type.CUSTOM && "fullPath".equalsIgnoreCase(clause.getProperty());
    }

    private static boolean isValid(final SearchClause clause) {
        return clause.getProperty() != null && clause.getComparator() != null;
    }
}
