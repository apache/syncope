package org.syncope.console.pages;

import java.util.Locale;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.syncope.console.SyncopeSession;
import org.syncope.console.SyncopeUser;

/**
 * Syncope Wicket base-page.
 * @author lbrandolini
 */
public class BasePage extends WebPage
{

    private static final long serialVersionUID = 1L;

    /**
     * Constructor that is invoked when page is invoked without a sessadd(new BookmarkablePageLink("roles", Roles.class));ion.
     *
     * @param parameters
     *            Page parameters
     */
    public BasePage( final PageParameters parameters )
    {
        getSession().setLocale(Locale.ITALIAN);

        add(new BookmarkablePageLink("users", Users.class));

        add(new BookmarkablePageLink("roles", Roles.class));

        add(new BookmarkablePageLink("resources", Resources.class));

        add(new BookmarkablePageLink("connectors", Connectors.class));

        add(new BookmarkablePageLink("report", Report.class));

        add(new BookmarkablePageLink("configuration", Configuration.class));

        add(new BookmarkablePageLink("logout", Logout.class));
    }

    /** Returns the current SyncopeSession */
    public SyncopeSession getSyncopeSession()
    {
        return ( SyncopeSession ) getSession();
    }

    /** Returns the current SyncopeUser logged-in from the session */
    public SyncopeUser getSyncopeUser()
    {
        return ( SyncopeUser ) getSyncopeSession().getUser();

    }
}
