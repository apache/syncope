package org.syncope.console.pages;

import java.util.Locale;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;

/**
 * Syncope Login page.
 * @author lbrandolini
 */
public class Login extends WebPage
{
    
public Form form;
public TextField usernameField;
public TextField passwordField;


    public Login( PageParameters parameters )
    {
        super( parameters );

        form = new Form( "login" );

        usernameField = new TextField("username", new Model());
        usernameField.setMarkupId("username");
        form.add(usernameField);

        passwordField = new PasswordTextField("password", new Model());
        passwordField.setMarkupId("password");
        form.add(passwordField);
        
        Button submitButton = new Button("submit",new Model(getString("submit")))
        {

            @Override
            public void onSubmit() {
               System.out.println("Submit");
               setResponsePage(new HomePage(null));
            }
            
        };

        submitButton.setDefaultFormProcessing(false);
        form.add(submitButton);

        form.add(new Button("reset",new Model(getString("reset"))));

        add(form);
    }
}
