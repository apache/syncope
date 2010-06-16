package org.syncope.console;

import org.apache.wicket.Request;
import org.apache.wicket.Response;
import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebApplication;
import org.syncope.console.pages.HomePage;
import org.syncope.console.pages.Login;

/**
 * SyncopeApplication class.
 * @author lbrandolini
 */
public class SyncopeApplication extends WebApplication
{
    SyncopeUser user = null;
    
    public SyncopeApplication()
    {
    }

    @Override
    protected void init()
    {
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
        return (user == null) ? Login.class :  HomePage.class;
    }

    /**
     * Use this method to switch from DEVELOPMENT to DEPLOYMENT mode
     * on production enviroment.
     *
     * @return String : Configuration type
     */
    @Override
    public String getConfigurationType()
    {
        return DEVELOPMENT;
    }

}
