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
import org.apache.syncope.client.console.rest.PasswordModuleRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wizards.PasswordModuleWizardBuilder;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.to.PasswordModuleTO;
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

public class PasswordModuleDirectoryPanel extends DirectoryPanel<
        PasswordModuleTO,
        PasswordModuleTO,
        PasswordModuleDirectoryPanel.PasswordModuleProvider,
        PasswordModuleRestClient> {
    private static final long serialVersionUID = 1005345990563741296L;

    @SpringBean
    protected AuditRestClient auditRestClient;

    protected final BaseModal<Serializable> historyModal;

    public PasswordModuleDirectoryPanel(
            final String id,
            final PasswordModuleRestClient restClient,
            final PageReference pageRef) {

        super(id, restClient, pageRef);

        disableCheckBoxes();

        addNewItemPanelBuilder(new PasswordModuleWizardBuilder(new PasswordModuleTO(), restClient, pageRef), true);

        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, AMEntitlement.AUTH_MODULE_CREATE);

        modal.size(Modal.Size.Extra_large);
        initResultTable();

        historyModal = new BaseModal<>(Constants.OUTER);
        historyModal.size(Modal.Size.Large);
        addOuterObject(historyModal);
    }

    @Override
    protected PasswordModuleProvider dataProvider() {
        return new PasswordModuleProvider(rows);
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
    protected List<IColumn<PasswordModuleTO, String>> getColumns() {
        List<IColumn<PasswordModuleTO, String>> columns = new ArrayList<>();
        columns.add(new PropertyColumn<>(
                new StringResourceModel(Constants.KEY_FIELD_NAME, this),
                Constants.KEY_FIELD_NAME, Constants.KEY_FIELD_NAME));
        columns.add(new PropertyColumn<>(new ResourceModel(Constants.DESCRIPTION_FIELD_NAME),
                Constants.DESCRIPTION_FIELD_NAME, Constants.DESCRIPTION_FIELD_NAME));
        columns.add(new PropertyColumn<>(new ResourceModel("type"), "conf") {

            private static final long serialVersionUID = -1822504503325964706L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<PasswordModuleTO>> item,
                    final String componentId,
                    final IModel<PasswordModuleTO> rowModel) {

                item.add(new Label(componentId, rowModel.getObject().getConf() == null
                        ? StringUtils.EMPTY
                        : StringUtils.substringBefore(
                        rowModel.getObject().getConf().getClass().getSimpleName(), "PasswordModuleConf")));
            }
        });
        columns.add(new PropertyColumn<>(new ResourceModel("state"), "state", "state"));
        return columns;
    }

    @Override
    public ActionsPanel<PasswordModuleTO> getActions(final IModel<PasswordModuleTO> model) {
        ActionsPanel<PasswordModuleTO> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final PasswordModuleTO ignore) {
                send(PasswordModuleDirectoryPanel.this, Broadcast.EXACT, new AjaxWizard.EditItemActionEvent<>(
                        restClient.read(model.getObject().getKey()), target));
            }
        }, ActionLink.ActionType.EDIT, AMEntitlement.PASSWORD_MODULE_UPDATE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -5432034353017728756L;

            @Override
            public void onClick(final AjaxRequestTarget target, final PasswordModuleTO ignore) {
                model.setObject(restClient.read(model.getObject().getKey()));

                target.add(historyModal.setContent(new AuditHistoryModal<>(
                        OpEvent.CategoryType.LOGIC,
                        "PasswordModuleLogic",
                        model.getObject(),
                        AMEntitlement.PASSWORD_MODULE_UPDATE,
                        auditRestClient) {

                    private static final long serialVersionUID = -3712506022627033822L;

                    @Override
                    protected void restore(final String json, final AjaxRequestTarget target) {
                        try {
                            PasswordModuleTO updated = MAPPER.readValue(json, PasswordModuleTO.class);
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
        }, ActionLink.ActionType.VIEW_AUDIT_HISTORY, String.format("%s,%s", AMEntitlement.PASSWORD_MODULE_READ,
                IdRepoEntitlement.AUDIT_LIST));

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final PasswordModuleTO ignore) {
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
        }, ActionLink.ActionType.DELETE, AMEntitlement.PASSWORD_MODULE_DELETE, true);

        return panel;
    }

    protected final class PasswordModuleProvider extends DirectoryDataProvider<PasswordModuleTO> {

        private static final long serialVersionUID = -185944053385660794L;

        private final SortableDataProviderComparator<PasswordModuleTO> comparator;

        private PasswordModuleProvider(final int paginatorRows) {
            super(paginatorRows);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<PasswordModuleTO> iterator(final long first, final long count) {
            List<PasswordModuleTO> passwordModules = restClient.list();
            passwordModules.sort(comparator);
            return passwordModules.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return restClient.list().size();
        }

        @Override
        public IModel<PasswordModuleTO> model(final PasswordModuleTO object) {
            return new CompoundPropertyModel<>(object);
        }
    }
}
