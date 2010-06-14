package org.syncope.console;

import org.apache.wicket.Request;
import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebSession;

/**
 * Custom Syncope Session class.
 * @author lbrandolini
 */
public class SyncopeSession extends WebSession
{

    public static SyncopeSession get()
    {
        return ( SyncopeSession ) Session.get();
    }
    private SyncopeUser user;

    public SyncopeSession( Request request )
    {
        super( request );

        setLocale( request.getLocale() );
    }

    public synchronized SyncopeUser getUser()
    {
        return user;
    }

    public synchronized boolean isAuthenticated()
    {
        return ( user != null );
    }

    public synchronized void setUser( SyncopeUser user )
    {
        this.user = user;
        dirty();
    }
}
