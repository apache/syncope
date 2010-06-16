package org.syncope.console.pages;

import org.apache.wicket.PageParameters;

/**
 * Syncope Logout.
 * @author lbrandolini
 */
public class Logout extends BasePage
{

    public Logout( PageParameters parameters )
    {
        super( parameters );

        getSyncopeSession().setUser(null);
        
        getSyncopeSession().invalidate();
        getRequestCycle().setRedirect(true);

        setResponsePage(getApplication().getHomePage());
        
    }

}
