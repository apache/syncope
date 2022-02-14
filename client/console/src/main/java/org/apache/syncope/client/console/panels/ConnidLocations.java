package org.apache.syncope.client.console.panels;

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.rest.RestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.console.wizards.resources.ConnectorWizardBuilder;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ConnidLocations extends
        DirectoryPanel<Serializable, Serializable, ConnidLocations.ConnidLocationsDataProvider, RestClient> {
    
    public ConnidLocations(final String id, final Builder builder) {
        super(id, builder);

        disableCheckBoxes();
        setShowResultPage(true);

        modal.size(Modal.Size.Large);
        initResultTable();
    }

    @Override
    protected ConnidLocationsDataProvider dataProvider() {
        return new ConnidLocationsDataProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return Constants.PREF_DYNREALM_PAGINATOR_ROWS;
    }

    @Override
    protected List<IColumn<Serializable, String>> getColumns() {
        final List<IColumn<Serializable, String>> columns = new ArrayList<>();

        columns.add(new AbstractColumn<Serializable, String>(
                new ResourceModel(Constants.KEY_FIELD_NAME), Constants.KEY_FIELD_NAME) {
            @Override
            public void populateItem(final Item cellItem, final String componentId, final IModel rowModel) {
                SyncopeConsoleSession.get().getPlatformInfo().getConnIdLocations().forEach(location ->
                        cellItem.add(new Label(componentId, location)));
            }
        });

        return columns;
    }

    @Override
    public ActionsPanel<Serializable> getActions(final IModel<Serializable> model) {
        final ActionsPanel<Serializable> panel = super.getActions(model);
        
        panel.add(new ActionLink<Serializable>() {

            private static final long serialVersionUID = 293293495682202660L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                final ConnInstanceTO modelObject = new ConnInstanceTO();
                modelObject.setLocation((String) ignore);

                final IModel<ConnInstanceTO> model = new CompoundPropertyModel<>(modelObject);
                modal.setFormModel(model);

                target.add(modal.setContent(new ConnectorWizardBuilder(modelObject, pageRef).
                        build(BaseModal.CONTENT_ID, AjaxWizard.Mode.CREATE)));

                modal.header(new Model<>(MessageFormat.format(getString("connector.new"), (String) ignore)));
                modal.show(true);
            }
            
        }, ActionLink.ActionType.CREATE, String.format("%s", StandardEntitlement.CONNECTOR_CREATE));
        
        return panel;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return Collections.emptyList();
    }

    public abstract static class Builder
            extends DirectoryPanel.Builder<Serializable, Serializable, RestClient> {

        private static final long serialVersionUID = 4448348557808690524L;

        public Builder(final PageReference pageRef) {
            super(null, pageRef);
        }

        @Override
        protected WizardMgtPanel<Serializable> newInstance(final String id, final boolean wizardInModal) {
            return new ConnidLocations(id, this);
        }
    }

    protected class ConnidLocationsDataProvider extends DirectoryDataProvider<Serializable> {

        private static final long serialVersionUID = 3161906945317209169L;

        public ConnidLocationsDataProvider(final int paginatorRows) {
            super(paginatorRows);
        }

        @Override
        public Iterator<String> iterator(final long first, final long count) {
            List<String> result = new ArrayList<>(SyncopeConsoleSession.get().getPlatformInfo().getConnIdLocations());
            return result.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return SyncopeConsoleSession.get().getPlatformInfo().getConnIdLocations().size();
        }

        @Override
        public IModel<Serializable> model(final Serializable object) {
            return new CompoundPropertyModel<>(object);
        }
    }
}
