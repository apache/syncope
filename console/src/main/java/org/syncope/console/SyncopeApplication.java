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
package org.syncope.console;

import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Response;
import org.apache.wicket.Session;
import org.apache.wicket.authentication.AuthenticatedWebApplication;
import org.apache.wicket.authentication.AuthenticatedWebSession;
import org.apache.wicket.authorization.strategies.role.RoleAuthorizationStrategy;
import org.apache.wicket.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.syncope.console.pages.Configuration;
import org.syncope.console.pages.Connectors;
import org.syncope.console.pages.Login;
import org.syncope.console.pages.Report;
import org.syncope.console.pages.Resources;
import org.syncope.console.pages.Roles;
import org.syncope.console.pages.Schema;
import org.syncope.console.pages.Tasks;
import org.syncope.console.pages.Users;
import org.syncope.console.pages.WelcomePage;

/**
 * SyncopeApplication class.
 */
public class SyncopeApplication extends AuthenticatedWebApplication
        implements ApplicationContextAware {
    SyncopeUser user = null;

    String file;
    
    private ApplicationContext applicationContext;

    public SyncopeApplication()
    {
    }

    @Override
    protected void init()
    {
        file = getServletContext().getInitParameter("authenticationFile");
        addComponentInstantiationListener(new SpringComponentInjector(this));
        getResourceSettings().setThrowExceptionOnMissingResource( true );

        getSecuritySettings().setAuthorizationStrategy(
                new RoleAuthorizationStrategy(new SyncopeRolesAuthorizer()));

//        getApplicationSettings().setPageExpiredErrorPage(PageExpiredErrorPage
//                .class);

        setupAuthorizations();
    }

    public void setupAuthorizations() {
        MetaDataRoleAuthorizationStrategy.authorize(Schema.class,
                "SCHEMA_LIST");
        MetaDataRoleAuthorizationStrategy.authorize(Roles.class,
                "ROLES_LIST");
        MetaDataRoleAuthorizationStrategy.authorize(Connectors.class,
                "CONNECTORS_LIST");
        MetaDataRoleAuthorizationStrategy.authorize(Resources.class,
                "RESOURCES_LIST");
        MetaDataRoleAuthorizationStrategy.authorize(Users.class,
                "USER_LIST");
        MetaDataRoleAuthorizationStrategy.authorize(Report.class,
                "REPORT_LIST");
        MetaDataRoleAuthorizationStrategy.authorize(Tasks.class,
                "TASKS_LIST");
        MetaDataRoleAuthorizationStrategy.authorize(Configuration.class,
                "CONFIGURATION_LIST");
    }
    /**
     * Create a new custom SyncopeSession
     * @param request
     * @param response
     * @return Session
     */
    @Override
    public Session newSession( Request request, Response response )
    {
        SyncopeSession session = new SyncopeSession( request );
        
        if ( user != null )
        {
            session.setUser( user );
        }
        
        return session;
    }

    /**
     * @see org.apache.wicket.Application#getHomePage()
     */
    @Override
    public Class getHomePage()
    {
        return (((SyncopeSession)Session.get()).getUser() == null) ?
            Login.class :  WelcomePage.class;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
          this.applicationContext = applicationContext;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }
    
    @Override
    public final RequestCycle newRequestCycle(final Request request,
            final Response response) {
        return new SyncopeRequestCycle(this, (WebRequest) request,
                (WebResponse) response);
    }

    @Override
    protected Class<? extends AuthenticatedWebSession> getWebSessionClass() {
        return SyncopeSession.class;
    }

    @Override
    protected Class<? extends WebPage> getSignInPageClass() {
        return Login.class;
    }
}