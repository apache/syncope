package org.syncope.console.pages;

import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;

/**
 * Users WebPage.
 * @author lbrandolini
 */
public class Users extends BasePage{

    public Users(PageParameters parameters) {
        super(parameters);

        add(new TextField("search",new Model(getString("search"))));

        add(new Button("newUserBtn",new Model(getString("newUserBtn"))));

    }
}
