package org.apache.syncope.client.console.pages;

import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.panels.ResourceDirectoryPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.console.wizards.resources.ResourceWizardBuilder;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import java.io.Serializable;

public class Resources extends Panel {

    private static final long serialVersionUID = 7240865652350993779L;

    private final WizardMgtPanel<Serializable> resourceDirectoryPanel;

    public Resources(final String id, final PageReference pageRef) {
        super(id);

        WebMarkupContainer content = new WebMarkupContainer("content");
        content.setOutputMarkupId(true);
        add(content);

        Model<String> keywordModel = new Model<>(StringUtils.EMPTY);

        WebMarkupContainer searchBoxContainer = new WebMarkupContainer("searchBox");
        content.add(searchBoxContainer);

        Form<?> form = new Form<>("form");
        searchBoxContainer.add(form);

        AjaxTextFieldPanel filter = new AjaxTextFieldPanel("filter", "filter", keywordModel, true);
        form.add(filter.hideLabel().setOutputMarkupId(true));

        AjaxButton search = new AjaxButton("search") {

            private static final long serialVersionUID = 8390605330558248736L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                send(Resources.this, Broadcast.DEPTH,
                        new ResourceDirectoryPanel.ResourceSearchEvent(target, keywordModel.getObject()));
            }
        };
        search.setOutputMarkupId(true);
        form.add(search);
        form.setDefaultButton(search);

        resourceDirectoryPanel =
                new ResourceDirectoryPanel.Builder(pageRef).
                        addNewItemPanelBuilder(new ResourceWizardBuilder(
                                new ResourceTO(), pageRef), true).
                        build("resourceDirectoryPanel");
        resourceDirectoryPanel.setOutputMarkupId(true);

        content.add(resourceDirectoryPanel);
    }
}
