package org.syncope.console.pages;


import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

/**
 * Syncope Login page.
 * @author lbrandolini
 */
public class Login extends WebPage
{
    
public Form form;
public TextField usernameField;
public TextField passwordField;
private List languages = Arrays.asList(new Locale[] { Locale.ENGLISH, Locale.ITALIAN});
public Locale selected;


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
        

        final DropDownChoice<String> language = new DropDownChoice<String>("language", new PropertyModel<String>(this, "selected"), languages);
        form.add(language);

        language.add(new AjaxFormComponentUpdatingBehavior("onchange")
        {
            @Override
            protected void onUpdate(AjaxRequestTarget target)
            {
                if(language.getDefaultModelObject()!=null){
                getSession().setLocale((Locale)language.getDefaultModelObject());
                }
            }
        });

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
