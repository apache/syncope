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

import java.io.InputStream;
import org.apache.wicket.Request;
import org.apache.wicket.Response;
import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.syncope.console.pages.Login;
import org.syncope.console.pages.WelcomePage;

/**
 * SyncopeApplication class.
 */
public class SyncopeApplication extends WebApplication implements ApplicationContextAware
{
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
    public Class getHomePage()
    {
        return (user == null) ? Login.class :  WelcomePage.class;
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
          this.applicationContext = applicationContext;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }
    
    public InputStream getAuthenticationFile(){
    return getServletContext().getResourceAsStream(file);
    }
}


