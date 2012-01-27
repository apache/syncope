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
package org.syncope.console.wicket.markup.html.form;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.console.commons.XMLRolesReader;
import org.syncope.console.wicket.ajax.markup.html.IndicatingDeleteOnConfirmAjaxLink;

/**
 * This empty class must exist because there not seems to be alternative to
 * provide specialized HTML for edit links.
 */
public class ActionLinksPanel extends Panel {

    private static final long serialVersionUID = 322966537010107771L;

    /**
     * Role reader for authorizations management.
     */
    @SpringBean
    protected XMLRolesReader xmlRolesReader;

    public ActionLinksPanel(final String componentId, final IModel<?> model) {
        super(componentId, model);

        super.add(new Fragment("panelClaim", "emptyFragment", this));
        super.add(new Fragment("panelCreate", "emptyFragment", this));
        super.add(new Fragment("panelEdit", "emptyFragment", this));
        super.add(new Fragment("panelTemplate", "emptyFragment", this));
        super.add(new Fragment("panelSearch", "emptyFragment", this));
        super.add(new Fragment("panelDelete", "emptyFragment", this));
        super.add(new Fragment("panelExecute", "emptyFragment", this));
        super.add(new Fragment("panelDryRun", "emptyFragment", this));
    }

    public void add(
            final ActionLink link,
            final ActionLink.ActionType type,
            final String pageId,
            final String actionId) {

        add(link, type,
                xmlRolesReader.getAllAllowedRoles(pageId, actionId), true);
    }

    public void add(
            final ActionLink link,
            final ActionLink.ActionType type,
            final String pageId,
            final String actionId,
            final boolean enabled) {

        add(link, type,
                xmlRolesReader.getAllAllowedRoles(pageId, actionId), enabled);
    }

    public void add(
            final ActionLink link,
            final ActionLink.ActionType type,
            final String roles) {
        add(link, type, roles, true);
    }

    public void add(
            final ActionLink link,
            final ActionLink.ActionType type,
            final String roles,
            final boolean enabled) {

        Fragment fragment = null;

        switch (type) {
            case CLAIM:
                fragment = new Fragment(
                        "panelClaim", "fragmentClaim", this);

                fragment.addOrReplace(
                        new IndicatingAjaxLink("claimLink") {

                            private static final long serialVersionUID = -7978723352517770644L;

                            @Override
                            public void onClick(final AjaxRequestTarget target) {
                                link.onClick(target);
                            }
                        });
                break;
            case CREATE:
                fragment = new Fragment(
                        "panelCreate", "fragmentCreate", this);

                fragment.addOrReplace(
                        new IndicatingAjaxLink("createLink") {

                            private static final long serialVersionUID = -7978723352517770644L;

                            @Override
                            public void onClick(final AjaxRequestTarget target) {
                                link.onClick(target);
                            }
                        });
                break;

            case EDIT:
                fragment = new Fragment(
                        "panelEdit", "fragmentEdit", this);

                fragment.addOrReplace(
                        new IndicatingAjaxLink("editLink") {

                            private static final long serialVersionUID = -7978723352517770644L;

                            @Override
                            public void onClick(final AjaxRequestTarget target) {
                                link.onClick(target);
                            }
                        });
                break;
            case TEMPLATE:
                fragment = new Fragment(
                        "panelTemplate", "fragmentTemplate", this);

                fragment.addOrReplace(
                        new IndicatingAjaxLink("templateLink") {

                            private static final long serialVersionUID = -7978723352517770644L;

                            @Override
                            public void onClick(final AjaxRequestTarget target) {
                                link.onClick(target);
                            }
                        });
                break;
            case SEARCH:
                fragment = new Fragment(
                        "panelSearch", "fragmentSearch", this);

                fragment.addOrReplace(
                        new IndicatingAjaxLink("searchLink") {

                            private static final long serialVersionUID = -7978723352517770644L;

                            @Override
                            public void onClick(final AjaxRequestTarget target) {
                                link.onClick(target);
                            }
                        });
                break;
            case EXECUTE:
                fragment = new Fragment(
                        "panelExecute", "fragmentExecute", this);

                fragment.addOrReplace(
                        new IndicatingAjaxLink("executeLink") {

                            private static final long serialVersionUID = -7978723352517770644L;

                            @Override
                            public void onClick(final AjaxRequestTarget target) {
                                link.onClick(target);
                            }
                        });
                break;
            case DRYRUN:
                fragment = new Fragment(
                        "panelDryRun", "fragmentDryRun", this);

                fragment.addOrReplace(
                        new IndicatingAjaxLink("dryRunLink") {

                            private static final long serialVersionUID = -7978723352517770644L;

                            @Override
                            public void onClick(final AjaxRequestTarget target) {
                                link.onClick(target);
                            }
                        });
                break;
            case DELETE:
                fragment = new Fragment(
                        "panelDelete", "fragmentDelete", this);

                fragment.addOrReplace(
                        new IndicatingDeleteOnConfirmAjaxLink("deleteLink") {

                            private static final long serialVersionUID = -7978723352517770644L;

                            @Override
                            public void onClick(final AjaxRequestTarget target) {
                                link.onClick(target);
                            }
                        });

                break;
            default:
            // do nothink
        }

        if (fragment != null) {
            fragment.setEnabled(enabled);
            MetaDataRoleAuthorizationStrategy.authorize(fragment, ENABLE, roles);
            super.addOrReplace(fragment);
        }
    }

    public void remove(ActionLink.ActionType type) {
        switch (type) {
            case CLAIM:
                super.addOrReplace(
                        new Fragment("panelClaim", "emptyFragment", this));
                break;

            case CREATE:
                super.addOrReplace(
                        new Fragment("panelCreate", "emptyFragment", this));
                break;

            case EDIT:
                super.addOrReplace(
                        new Fragment("panelEdit", "emptyFragment", this));
                break;
            case TEMPLATE:
                super.addOrReplace(
                        new Fragment("panelTemplate", "emptyFragment", this));
                break;
            case SEARCH:
                super.addOrReplace(
                        new Fragment("panelSearch", "emptyFragment", this));
                break;
            case EXECUTE:
                super.addOrReplace(
                        new Fragment("panelExecute", "emptyFragment", this));
                break;
            case DRYRUN:
                super.addOrReplace(
                        new Fragment("panelDryRun", "emptyFragment", this));
                break;
            case DELETE:
                super.addOrReplace(
                        new Fragment("panelDelete", "emptyFragment", this));
                break;
            default:
            // do nothink
        }
    }
}
