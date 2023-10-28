/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.client.console.panels;

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.IdRepoConstants;
import org.apache.syncope.client.console.commons.KeywordSearchEvent;
import org.apache.syncope.client.console.panels.CommandDirectoryPanel.CommandDataProvider;
import org.apache.syncope.client.console.rest.CommandRestClient;
import org.apache.syncope.client.console.tasks.ExecMessage;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wizards.CommandWizardBuilder;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.command.CommandTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

public class CommandDirectoryPanel
        extends DirectoryPanel<CommandTO, CommandTO, CommandDataProvider, CommandRestClient> {

    private static final long serialVersionUID = -8723262033772725592L;

    private String keyword;

    public CommandDirectoryPanel(final String id, final CommandRestClient restClient, final PageReference pageRef) {
        super(id, restClient, pageRef);
        disableCheckBoxes();

        modal.size(Modal.Size.Large);

        modal.setWindowClosedCallback(target -> {
            updateResultTable(target);
            modal.show(false);
        });

        addNewItemPanelBuilder(new CommandWizardBuilder(new CommandTO(), restClient, pageRef), false);

        setShowResultPanel(true);

        initResultTable();
    }

    @Override
    protected CommandDataProvider dataProvider() {
        return new CommandDataProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return IdRepoConstants.PREF_COMMAND_PAGINATOR_ROWS;
    }

    @Override
    protected List<IColumn<CommandTO, String>> getColumns() {
        List<IColumn<CommandTO, String>> columns = new ArrayList<>();

        columns.add(new PropertyColumn<>(
                new ResourceModel(Constants.KEY_FIELD_NAME), Constants.KEY_FIELD_NAME, Constants.KEY_FIELD_NAME));

        columns.add(new AbstractColumn<>(new ResourceModel("arguments"), "arguments") {

            private static final long serialVersionUID = -4008579357070833846L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<CommandTO>> cellItem,
                    final String componentId,
                    final IModel<CommandTO> rowModel) {

                cellItem.add(new Label(componentId, rowModel.getObject().getArgs().getClass().getName()));
            }
        });

        return columns;
    }

    @Override
    protected ActionsPanel<CommandTO> getActions(final IModel<CommandTO> model) {
        ActionsPanel<CommandTO> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final CommandTO ignore) {
                send(CommandDirectoryPanel.this, Broadcast.EXACT,
                        new AjaxWizard.EditItemActionEvent<>(model.getObject(), target));
            }
        }, ActionLink.ActionType.EXECUTE, IdRepoEntitlement.COMMAND_RUN);

        return panel;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of();
    }

    @Override
    protected Panel customResultBody(final String panelId, final CommandTO item, final Serializable result) {
        return new ExecMessage(panelId, (String) result);
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof KeywordSearchEvent) {
            KeywordSearchEvent payload = KeywordSearchEvent.class.cast(event.getPayload());

            keyword = payload.getKeyword();
            if (StringUtils.isNotBlank(keyword)) {
                if (!StringUtils.startsWith(keyword, "*")) {
                    keyword = "*" + keyword;
                }
                if (!StringUtils.endsWith(keyword, "*")) {
                    keyword += "*";
                }
            }

            updateResultTable(payload.getTarget());
        } else {
            super.onEvent(event);
        }
    }

    protected final class CommandDataProvider extends DirectoryDataProvider<CommandTO> {

        private static final long serialVersionUID = 6267494272884913376L;

        public CommandDataProvider(final int paginatorRows) {
            super(paginatorRows);
        }

        @Override
        public Iterator<CommandTO> iterator(final long first, final long count) {
            int page = ((int) first / paginatorRows);
            return restClient.search((page < 0 ? 0 : page) + 1, paginatorRows, keyword).iterator();
        }

        @Override
        public long size() {
            return restClient.count(keyword);
        }

        @Override
        public IModel<CommandTO> model(final CommandTO object) {
            return new CompoundPropertyModel<>(object);
        }
    }
}
