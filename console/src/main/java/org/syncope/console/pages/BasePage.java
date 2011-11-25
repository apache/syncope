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

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.syncope.client.to.UserTO;
import org.syncope.console.SyncopeApplication;
import org.syncope.console.SyncopeSession;
import org.syncope.console.commons.XMLRolesReader;
import org.syncope.console.rest.UserRequestRestClient;
import org.syncope.console.wicket.markup.html.form.LinkPanel;

/**
 * Syncope Wicket base-page.
 */
public class BasePage extends WebPage implements IAjaxIndicatorAware {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(
            BasePage.class);

    private static final long serialVersionUID = 1571997737305598502L;

    private final static int EDIT_PROFILE_WIN_HEIGHT = 550;

    private final static int EDIT_PROFILE_WIN_WIDTH = 800;

    @SpringBean
    private UserRequestRestClient profileRestClient;

    @SpringBean
    protected XMLRolesReader xmlRolesReader;

    @SpringBean(name = "version")
    private String version;

    protected FeedbackPanel feedbackPanel;

    /**
     * Response flag set by the Modal Window after the operation is completed.
     */
    protected boolean modalResult = false;

    public BasePage() {
        super();

        pageSetup();
    }

    /**
     * Constructor that is invoked when page is invoked without a
     * session.
     *
     * @param PageParameters parameters
     */
    public BasePage(final PageParameters parameters) {
        super(parameters);

        pageSetup();
    }

    private void pageSetup() {
        ((SyncopeApplication) getApplication()).setupNavigationPane(
                this, xmlRolesReader, version);

        feedbackPanel = new FeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId(true);
        add(feedbackPanel);

        final String kind = getClass().getSimpleName().toLowerCase();
        final Component kindIcon = get(kind);
        if (kindIcon != null) {
            kindIcon.add(new Behavior() {

                private static final long serialVersionUID =
                        1469628524240283489L;

                @Override
                public void onComponentTag(final Component component,
                        final ComponentTag tag) {

                    tag.put("class", kind);
                }
            });
            add(kindIcon);
        }

        // Modal window for editing user profile
        final ModalWindow editProfileModalWin =
                new ModalWindow("editProfileModal");
        editProfileModalWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editProfileModalWin.setInitialHeight(EDIT_PROFILE_WIN_HEIGHT);
        editProfileModalWin.setInitialWidth(EDIT_PROFILE_WIN_WIDTH);
        editProfileModalWin.setCookieName("edit-profile-modal");
        add(editProfileModalWin);

        add(new Label("username", SyncopeSession.get().getUserId()));

        Fragment editProfileFrag;
        if ("admin".equals(SyncopeSession.get().getUserId())) {
            editProfileFrag =
                    new Fragment("editProfile", "adminEmptyFrag", this);
        } else {
            final UserTO userTO = SyncopeSession.get().isAuthenticated()
                    ? profileRestClient.readProfile()
                    : new UserTO();

            editProfileFrag =
                    new Fragment("editProfile", "editProfileFrag", this);

            AjaxLink editProfileLink =
                    new IndicatingAjaxLink("link") {

                        private static final long serialVersionUID =
                                -7978723352517770644L;

                        @Override
                        public void onClick(final AjaxRequestTarget target) {
                            editProfileModalWin.setPageCreator(
                                    new ModalWindow.PageCreator() {

                                        @Override
                                        public Page createPage() {
                                            return new UserModalPage(
                                                    BasePage.this.
                                                    getPageReference(),
                                                    editProfileModalWin,
                                                    userTO, 
                                                    UserModalPage.Mode.SELF);
                                        }
                                    });

                            editProfileModalWin.show(target);
                        }
                    };
            editProfileLink.add(
                    new Label("linkTitle", getString("editProfile")));

            Panel panel = new LinkPanel("editProfile",
                    new ResourceModel("editProfile"));
            panel.add(editProfileLink);
            editProfileFrag.add(panel);
        }
        add(editProfileFrag);
    }

    @Override
    public String getAjaxIndicatorMarkupId() {
        return "veil";
    }

    public FeedbackPanel getFeedbackPanel() {
        return feedbackPanel;
    }

    public boolean isModalResult() {
        return modalResult;
    }

    public void setModalResult(final boolean operationResult) {
        this.modalResult = operationResult;
    }

    /**
     * Set a WindowClosedCallback for a ModalWindow instance.
     * @param window window
     * @param container container
     */
    protected void setWindowClosedCallback(final ModalWindow window,
            final WebMarkupContainer container) {

        window.setWindowClosedCallback(
                new ModalWindow.WindowClosedCallback() {

                    private static final long serialVersionUID =
                            8804221891699487139L;

                    @Override
                    public void onClose(final AjaxRequestTarget target) {
                        target.add(container);
                        if (isModalResult()) {
                            info(getString("operation_succeded"));
                            target.add(feedbackPanel);
                            setModalResult(false);
                        }
                    }
                });
    }
}
