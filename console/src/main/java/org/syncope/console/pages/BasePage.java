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

import org.apache.wicket.PageParameters;
import org.apache.wicket.authorization.strategies.role.annotations.AuthorizeAction;
import org.apache.wicket.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.console.SyncopeSession;
import org.syncope.console.SyncopeUser;
import org.syncope.console.commons.XMLRolesReader;

/**
 * Syncope Wicket base-page.
 */
public class BasePage extends CustomizableBasePage {

    private static final long serialVersionUID = 1L;

    @SpringBean(name = "xmlRolesReader")
    protected XMLRolesReader xmlRolesReader;

    /**
     * Constructor that is invoked when page is invoked without a
     * sessadd(new BookmarkablePageLink("roles", Roles.class));ion.
     *
     * @param PageParameters parameters
     *            
     */
    public BasePage(final PageParameters parameters) {
        super(parameters);

        BookmarkablePageLink schemaLink = new BookmarkablePageLink("schema",
                Schema.class);

        String allowedSchemaRoles = xmlRolesReader.getAllAllowedRoles("Schema",
                        "list");

        MetaDataRoleAuthorizationStrategy.authorize(schemaLink, ENABLE,
                        allowedSchemaRoles);
        
        add(schemaLink);

        BookmarkablePageLink usersLink = new BookmarkablePageLink("users",
                Users.class);

        String allowedUsersRoles = xmlRolesReader.getAllAllowedRoles("Users",
                        "list");

        MetaDataRoleAuthorizationStrategy.authorize(usersLink, ENABLE,
                        allowedUsersRoles);

        add(usersLink);

        BookmarkablePageLink rolesLink= new BookmarkablePageLink("roles",
                Roles.class);

        String allowedRoleRoles = xmlRolesReader.getAllAllowedRoles("Roles",
                        "list");

        MetaDataRoleAuthorizationStrategy.authorize(rolesLink, ENABLE,
                        allowedRoleRoles);
        
        add(rolesLink);

        BookmarkablePageLink resourcesLink = new BookmarkablePageLink(
                "resources", Resources.class);

        String allowedResourcesRoles = xmlRolesReader.getAllAllowedRoles(
                "Resources","list");

        MetaDataRoleAuthorizationStrategy.authorize(resourcesLink, ENABLE,
                        allowedResourcesRoles);

        add(resourcesLink);

        BookmarkablePageLink connectorsLink =
                new BookmarkablePageLink("connectors", Connectors.class);

        String allowedConnectorsRoles = xmlRolesReader.getAllAllowedRoles(
                "Connectors","list");

        MetaDataRoleAuthorizationStrategy.authorize(connectorsLink, ENABLE,
                        allowedConnectorsRoles);

        add(connectorsLink);

        BookmarkablePageLink reportLink = new BookmarkablePageLink(
                "report", Report.class);

        String allowedReportRoles = xmlRolesReader.getAllAllowedRoles(
                "Report","list");

        MetaDataRoleAuthorizationStrategy.authorize(reportLink, ENABLE,
                        allowedReportRoles);

        add(reportLink);

        BookmarkablePageLink configurationLink = new BookmarkablePageLink(
                "configuration", Configuration.class);

        String allowedConfigurationRoles = xmlRolesReader.getAllAllowedRoles(
                "Configuration","list");

        MetaDataRoleAuthorizationStrategy.authorize(configurationLink, ENABLE,
                        allowedConfigurationRoles);

        add(configurationLink);

        BookmarkablePageLink taskLink = new BookmarkablePageLink("task",
                Tasks.class);
        
        String allowedTasksRoles = xmlRolesReader.getAllAllowedRoles(
                "Tasks","list");

        MetaDataRoleAuthorizationStrategy.authorize(taskLink, ENABLE,
                        allowedTasksRoles);   
        
        add(taskLink);

        add(new BookmarkablePageLink("logout", Logout.class));
    }

    /**
     * @return the current SyncopeSession
     */
    public SyncopeSession getSyncopeSession() {
        return (SyncopeSession) getSession();
    }

    /**
     * @return the current SyncopeUser logged-in from the session
     */
    public SyncopeUser getSyncopeUser() {
        return (SyncopeUser) getSyncopeSession().getUser();

    }
}
