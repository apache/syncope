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
package org.syncope.console;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.RestartResponseAtInterceptPageException;
import org.apache.wicket.Session;
import org.apache.wicket.authorization.IUnauthorizedComponentInstantiationListener;
import org.apache.wicket.authorization.UnauthorizedInstantiationException;
import org.apache.wicket.authroles.authorization.strategies.role.IRoleCheckingStrategy;
import org.apache.wicket.authroles.authorization.strategies.role.RoleAuthorizationStrategy;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.resource.ContextRelativeResource;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;
import org.syncope.console.commons.XMLRolesReader;
import org.syncope.console.pages.Configuration;
import org.syncope.console.pages.Login;
import org.syncope.console.pages.Logout;
import org.syncope.console.pages.Reports;
import org.syncope.console.pages.Resources;
import org.syncope.console.pages.Roles;
import org.syncope.console.pages.Schema;
import org.syncope.console.pages.Tasks;
import org.syncope.console.pages.Todo;
import org.syncope.console.pages.Users;
import org.syncope.console.pages.WelcomePage;

/**
 * SyncopeApplication class.
 */
public class SyncopeApplication extends WebApplication
        implements IUnauthorizedComponentInstantiationListener,
        IRoleCheckingStrategy {

    public static final String IMG_PREFIX = "/img/menu/";

    public static final String IMG_NOTSEL = "notsel/";

    public static final String IMG_SUFFIX = ".png";

    @Override
    protected void init() {
        getComponentInstantiationListeners().add(
                new SpringComponentInjector(this));

        getResourceSettings().setThrowExceptionOnMissingResource(true);

        getSecuritySettings().
                setAuthorizationStrategy(new RoleAuthorizationStrategy(this));
        getSecuritySettings().
                setUnauthorizedComponentInstantiationListener(this);

        getMarkupSettings().setStripWicketTags(true);

        getRequestCycleListeners().add(new SyncopeRequestCycleListener());
    }

    public void setupNavigationPane(final WebPage page,
            final XMLRolesReader xmlRolesReader, final boolean notsel,
            final String version) {

        page.add(new Label("version", "Console: " + version
                + "; Core: " + SyncopeSession.get().getCoreVersion()));

        BookmarkablePageLink schemaLink =
                new BookmarkablePageLink("schema", Schema.class);
        MetaDataRoleAuthorizationStrategy.authorizeAll(
                schemaLink, WebPage.ENABLE);
        page.add(schemaLink);
        schemaLink.add(
                new Image("schemaIcon",
                new ContextRelativeResource(IMG_PREFIX
                + (notsel ? IMG_NOTSEL : "") + "schema" + IMG_SUFFIX)));

        BookmarkablePageLink usersLink =
                new BookmarkablePageLink("users", Users.class);
        String allowedUsersRoles =
                xmlRolesReader.getAllAllowedRoles("Users", "list");
        MetaDataRoleAuthorizationStrategy.authorize(
                usersLink, WebPage.ENABLE, allowedUsersRoles);
        page.add(usersLink);
        usersLink.add(new Image("usersIcon",
                new ContextRelativeResource(IMG_PREFIX
                + (notsel ? IMG_NOTSEL : "") + "users" + IMG_SUFFIX)));

        BookmarkablePageLink rolesLink =
                new BookmarkablePageLink("roles", Roles.class);
        MetaDataRoleAuthorizationStrategy.authorizeAll(
                rolesLink, WebPage.ENABLE);
        page.add(rolesLink);
        rolesLink.add(new Image("rolesIcon",
                new ContextRelativeResource(IMG_PREFIX
                + (notsel ? IMG_NOTSEL : "") + "roles" + IMG_SUFFIX)));

        BookmarkablePageLink resourcesLink =
                new BookmarkablePageLink("resources", Resources.class);
        MetaDataRoleAuthorizationStrategy.authorizeAll(
                resourcesLink, WebPage.ENABLE);
        page.add(resourcesLink);
        resourcesLink.add(new Image("resourcesIcon",
                new ContextRelativeResource(
                IMG_PREFIX + (notsel ? IMG_NOTSEL : "") + "resources"
                + IMG_SUFFIX)));

        BookmarkablePageLink todoLink =
                new BookmarkablePageLink("todo", Todo.class);
        MetaDataRoleAuthorizationStrategy.authorize(
                todoLink, WebPage.ENABLE,
                xmlRolesReader.getAllAllowedRoles("Approval", "list"));
        page.add(todoLink);
        todoLink.add(new Image("todoIcon",
                new ContextRelativeResource(IMG_PREFIX
                + (notsel ? IMG_NOTSEL : "") + "todo" + IMG_SUFFIX)));

        BookmarkablePageLink reportLink =
                new BookmarkablePageLink("reports", Reports.class);
        String allowedReportRoles =
                xmlRolesReader.getAllAllowedRoles("Reports", "list");
        MetaDataRoleAuthorizationStrategy.authorize(
                reportLink, WebPage.ENABLE, allowedReportRoles);
        page.add(reportLink);
        reportLink.add(new Image("reportsIcon",
                new ContextRelativeResource(IMG_PREFIX
                + (notsel ? IMG_NOTSEL : "") + "reports" + IMG_SUFFIX)));

        BookmarkablePageLink configurationLink =
                new BookmarkablePageLink("configuration", Configuration.class);
        String allowedConfigurationRoles =
                xmlRolesReader.getAllAllowedRoles("Configuration", "list");
        MetaDataRoleAuthorizationStrategy.authorize(
                configurationLink, WebPage.ENABLE, allowedConfigurationRoles);
        page.add(configurationLink);
        configurationLink.add(new Image("configurationIcon",
                new ContextRelativeResource(IMG_PREFIX
                + (notsel ? IMG_NOTSEL : "") + "configuration" + IMG_SUFFIX)));

        BookmarkablePageLink taskLink =
                new BookmarkablePageLink("tasks", Tasks.class);
        String allowedTasksRoles =
                xmlRolesReader.getAllAllowedRoles("Tasks", "list");
        MetaDataRoleAuthorizationStrategy.authorize(
                taskLink, WebPage.ENABLE, allowedTasksRoles);
        page.add(taskLink);
        taskLink.add(new Image("tasksIcon",
                new ContextRelativeResource(IMG_PREFIX
                + (notsel ? IMG_NOTSEL : "") + "tasks" + IMG_SUFFIX)));

        page.add(new BookmarkablePageLink("logout", Logout.class));
    }

    @Override
    public Session newSession(final Request request, final Response response) {
        return new SyncopeSession(request);
    }

    @Override
    public Class getHomePage() {
        return SyncopeSession.get().isAuthenticated()
                ? WelcomePage.class
                : Login.class;
    }

    @Override
    public void onUnauthorizedInstantiation(final Component component) {
        SyncopeSession.get().invalidate();

        if (component instanceof Page) {
            throw new UnauthorizedInstantiationException(component.getClass());
        }

        throw new RestartResponseAtInterceptPageException(Login.class);
    }

    @Override
    public boolean hasAnyRole(
            final org.apache.wicket.authroles.authorization.strategies.role.Roles roles) {

        return SyncopeSession.get().hasAnyRole(roles);
    }
}
