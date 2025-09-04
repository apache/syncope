package org.apache.syncope.client.console.panels;

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.audit.AuditHistoryModal;
import org.apache.syncope.client.console.commons.AMConstants;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.AuditRestClient;
import org.apache.syncope.client.console.rest.PasswordManagementRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.BooleanPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wizards.PasswordManagementWizardBuilder;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.to.PasswordManagementTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class PasswordManagementDirectoryPanel extends DirectoryPanel<PasswordManagementTO, PasswordManagementTO,
        PasswordManagementDirectoryPanel.PasswordManagementProvider, PasswordManagementRestClient> {
    private static final long serialVersionUID = 1005345990563741296L;

    @SpringBean
    protected AuditRestClient auditRestClient;

    protected final BaseModal<Serializable> historyModal;

    public PasswordManagementDirectoryPanel(
            final String id,
            final PasswordManagementRestClient restClient,
            final PageReference pageRef) {

        super(id, restClient, pageRef);

        disableCheckBoxes();

        addNewItemPanelBuilder(new PasswordManagementWizardBuilder(
                new PasswordManagementTO(), restClient, pageRef), true);

        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, AMEntitlement.AUTH_MODULE_CREATE);

        modal.size(Modal.Size.Extra_large);
        initResultTable();

        historyModal = new BaseModal<>(Constants.OUTER);
        historyModal.size(Modal.Size.Large);
        addOuterObject(historyModal);
    }

    @Override
    protected PasswordManagementProvider dataProvider() {
        return new PasswordManagementProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return AMConstants.PREF_AUTHMODULE_PAGINATOR_ROWS;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of();
    }

    @Override
    protected List<IColumn<PasswordManagementTO, String>> getColumns() {
        List<IColumn<PasswordManagementTO, String>> columns = new ArrayList<>();
        columns.add(new PropertyColumn<>(
                new StringResourceModel(Constants.KEY_FIELD_NAME, this),
                Constants.KEY_FIELD_NAME, Constants.KEY_FIELD_NAME));
        columns.add(new PropertyColumn<>(new ResourceModel(Constants.DESCRIPTION_FIELD_NAME),
                Constants.DESCRIPTION_FIELD_NAME, Constants.DESCRIPTION_FIELD_NAME));
        columns.add(new PropertyColumn<>(new ResourceModel("type"), "conf") {

            private static final long serialVersionUID = -1822504503325964706L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<PasswordManagementTO>> item,
                    final String componentId,
                    final IModel<PasswordManagementTO> rowModel) {

                item.add(new Label(componentId, rowModel.getObject().getConf() == null
                        ? StringUtils.EMPTY
                        : StringUtils.substringBefore(
                        rowModel.getObject().getConf().getClass().getSimpleName(), "PasswordManagementConf")));
            }
        });
        columns.add(new BooleanPropertyColumn<>(
                new StringResourceModel("enabled", this),
                "enabled",
                "enabled"));
        return columns;
    }

    @Override
    public ActionsPanel<PasswordManagementTO> getActions(final IModel<PasswordManagementTO> model) {
        ActionsPanel<PasswordManagementTO> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final PasswordManagementTO ignore) {
                send(PasswordManagementDirectoryPanel.this, Broadcast.EXACT, new AjaxWizard.EditItemActionEvent<>(
                        restClient.read(model.getObject().getKey()), target));
            }
        }, ActionLink.ActionType.EDIT, AMEntitlement.PASSWORD_MANAGEMENT_UPDATE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final PasswordManagementTO ignore) {
                try {
                    model.setObject(restClient.read(model.getObject().getKey()));
                    boolean enabled = Boolean.parseBoolean(model.getObject().getEnabled());
                    model.getObject().setEnabled(String.valueOf(!enabled));
                    restClient.update(model.getObject());

                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (Exception e) {
                    LOG.error("While actioning object {}", model.getObject().getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.ENABLE_PM, AMEntitlement.PASSWORD_MANAGEMENT_UPDATE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -5432034353017728756L;

            @Override
            public void onClick(final AjaxRequestTarget target, final PasswordManagementTO ignore) {
                model.setObject(restClient.read(model.getObject().getKey()));

                target.add(historyModal.setContent(new AuditHistoryModal<>(
                        OpEvent.CategoryType.LOGIC,
                        "PasswordManagementLogic",
                        model.getObject(),
                        AMEntitlement.PASSWORD_MANAGEMENT_UPDATE,
                        auditRestClient) {

                    private static final long serialVersionUID = -3712506022627033822L;

                    @Override
                    protected void restore(final String json, final AjaxRequestTarget target) {
                        try {
                            PasswordManagementTO updated = MAPPER.readValue(json, PasswordManagementTO.class);
                            restClient.update(updated);

                            SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                        } catch (Exception e) {
                            LOG.error("While restoring AuthModule {}", model.getObject().getKey(), e);
                            SyncopeConsoleSession.get().onException(e);
                        }
                        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                    }
                }));

                historyModal.header(new Model<>(getString("auditHistory.title", new Model<>(model.getObject()))));

                historyModal.show(true);
            }
        }, ActionLink.ActionType.VIEW_AUDIT_HISTORY, String.format("%s,%s", AMEntitlement.PASSWORD_MANAGEMENT_READ,
                IdRepoEntitlement.AUDIT_LIST));

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final PasswordManagementTO ignore) {
                try {
                    restClient.delete(model.getObject().getKey());

                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (Exception e) {
                    LOG.error("While deleting {}", model.getObject().getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, AMEntitlement.PASSWORD_MANAGEMENT_DELETE, true);

        return panel;
    }

    protected final class PasswordManagementProvider extends DirectoryDataProvider<PasswordManagementTO> {

        private static final long serialVersionUID = 4972693840864025955L;

        private final SortableDataProviderComparator<PasswordManagementTO> comparator;

        private PasswordManagementProvider(final int paginatorRows) {
            super(paginatorRows);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<PasswordManagementTO> iterator(final long first, final long count) {
            List<PasswordManagementTO> passwordManagements = restClient.list();
            passwordManagements.sort(comparator);
            return passwordManagements.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return restClient.list().size();
        }

        @Override
        public IModel<PasswordManagementTO> model(final PasswordManagementTO object) {
            return new CompoundPropertyModel<>(object);
        }
    }
}
