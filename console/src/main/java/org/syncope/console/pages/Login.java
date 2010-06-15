package org.syncope.console.pages;

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
private TextField usernameField;
private TextField passwordField;


    public Login( PageParameters parameters )
    {
        super( parameters );

        form = new Form( "login" );

        usernameField = new TextField("username");
        usernameField.setMarkupId("username");
        form.add(usernameField);

        passwordField = new PasswordTextField("password");
        passwordField.setMarkupId("password");
        form.add(passwordField);
        
        Button submitButton = new Button("submit",new Model(getString("submit"))){

            @Override
            public void onSubmit() {
               System.out.println("Submit");
               setResponsePage(HomePage.class);
            }
            
        };

        submitButton.setDefaultFormProcessing(false);
        form.add(submitButton);

        form.add(new Button("reset",new Model(getString("reset"))));

        add(form);
    }
}
