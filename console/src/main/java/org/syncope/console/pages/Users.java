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
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.syncope.client.search.NodeCond;
import org.syncope.client.to.UserTO;
import org.syncope.console.pages.panels.ResultSetPanel;
import org.syncope.console.pages.panels.ResultSetPanel.EventDataWrapper;
import org.syncope.console.pages.panels.UserSearchPanel;

public class Users extends BasePage {

    private static final long serialVersionUID = 134681165644474568L;

    private final static int EDIT_MODAL_WIN_HEIGHT = 550;

    private final static int EDIT_MODAL_WIN_WIDTH = 800;

    protected boolean modalResult = false;

    public Users(final PageParameters parameters) {
        super(parameters);

        // Modal window for editing user attributes
        final ModalWindow editModalWin = new ModalWindow("editModal");
        editModalWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editModalWin.setInitialHeight(EDIT_MODAL_WIN_HEIGHT);
        editModalWin.setInitialWidth(EDIT_MODAL_WIN_WIDTH);
        editModalWin.setCookieName("edit-modal");
        add(editModalWin);

        final ResultSetPanel searchResult = new ResultSetPanel("searchResult",
                true, null, getPageReference());
        add(searchResult);

        final ResultSetPanel listResult = new ResultSetPanel("listResult",
                false, null, getPageReference());
        add(listResult);

        // create new user
        final AjaxLink createLink = new IndicatingAjaxLink("createLink") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                editModalWin.setPageCreator(new ModalWindow.PageCreator() {

                    private static final long serialVersionUID =
                            -7834632442532690940L;

                    @Override
                    public Page createPage() {
                        return new UserModalPage(
                                Users.this.getPageReference(),
                                editModalWin, new UserTO());
                    }
                });

                editModalWin.show(target);
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(createLink, ENABLE,
                xmlRolesReader.getAllAllowedRoles("Users", "create"));
        add(createLink);

        setWindowClosedReloadCallback(editModalWin);

        final Form searchForm = new Form("searchForm");
        add(searchForm);

        final UserSearchPanel searchPanel =
                new UserSearchPanel("searchPanel");
        searchForm.add(searchPanel);

        searchForm.add(new IndicatingAjaxButton(
                "search", new ResourceModel("search")) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target,
                    final Form<?> form) {

                final NodeCond searchCond = searchPanel.buildSearchCond();
                LOG.debug("Node condition " + searchCond);

                doSearch(target, searchCond, searchResult);

                Session.get().getFeedbackMessages().clear();
                target.add(searchPanel.getSearchFeedback());
            }

            @Override
            protected void onError(final AjaxRequestTarget target,
                    final Form form) {

                target.add(searchPanel.getSearchFeedback());
            }
        });
    }

    private void doSearch(final AjaxRequestTarget target,
            final NodeCond searchCond,
            final ResultSetPanel resultsetPanel) {

        if (searchCond == null || !searchCond.checkValidity()) {
            error(getString("search_error"));
            return;
        }

        resultsetPanel.search(searchCond, target);
    }

    public void setModalResult(final boolean modalResult) {
        this.modalResult = modalResult;
    }

    public boolean isModalResult() {
        return modalResult;
    }

    private void setWindowClosedReloadCallback(final ModalWindow window) {
        window.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                final EventDataWrapper data = new EventDataWrapper();
                data.setTarget(target);
                data.setCreate(true);

                send(getPage(), Broadcast.BREADTH, data);

                if (isModalResult()) {
                    // reset modal result
                    setModalResult(false);
                    // set operation succeded
                    getSession().info(getString("operation_succeded"));
                    // refresh feedback panel
                    target.add(feedbackPanel);
                }
            }
        });
    }
}
