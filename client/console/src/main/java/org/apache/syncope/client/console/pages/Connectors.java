package org.apache.syncope.client.console.pages;

import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.panels.ConnectorDirectoryPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.console.wizards.resources.ConnectorWizardBuilder;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import java.io.Serializable;

public class Connectors extends Panel {

    private static final long serialVersionUID = 305521359617401936L;

    private final WizardMgtPanel<Serializable> connectorDirectoryPanel;

    public Connectors(final String id, final PageReference pageRef) {
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
                send(Connectors.this, Broadcast.DEPTH,
                        new ConnectorDirectoryPanel.ConnectorSearchEvent(target, keywordModel.getObject()));
            }
        };
        search.setOutputMarkupId(true);
        form.add(search);
        form.setDefaultButton(search);

        connectorDirectoryPanel =
                new ConnectorDirectoryPanel.Builder(pageRef).
                        addNewItemPanelBuilder(new ConnectorWizardBuilder(
                                new ConnInstanceTO(), pageRef), true).
                        build("connectorDirectoryPanel");
        connectorDirectoryPanel.setOutputMarkupId(true);

        content.add(connectorDirectoryPanel);
    }
}
