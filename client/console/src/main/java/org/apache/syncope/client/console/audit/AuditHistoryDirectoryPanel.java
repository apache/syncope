package org.apache.syncope.client.console.audit;

import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.panels.AjaxDataTablePanel;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.panels.ModalPanel;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.rest.AuditHistoryRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.CollectionPanel;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AuditEntryTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class AuditHistoryDirectoryPanel extends
    DirectoryPanel<AuditEntryBean, AuditEntryBean, DirectoryDataProvider<AuditEntryBean>, AuditHistoryRestClient>
    implements ModalPanel {

    private static final long serialVersionUID = -8248734710505211261L;

    private final BaseModal<?> baseModal;

    private final MultilevelPanel multiLevelPanelRef;

    private final AnyTO anyTO;

    private final AnyTypeKind anyTypeKind;

    public AuditHistoryDirectoryPanel(
        final BaseModal<?> baseModal,
        final MultilevelPanel multiLevelPanelRef,
        final PageReference pageRef,
        final AnyTO anyTO) {

        super(MultilevelPanel.FIRST_LEVEL_ID, pageRef);
        disableCheckBoxes();
        
        this.baseModal = baseModal;
        this.multiLevelPanelRef = multiLevelPanelRef;
        this.anyTO = anyTO;

        if (anyTO instanceof UserTO) {
            anyTypeKind = AnyTypeKind.USER;
        } else if (anyTO instanceof GroupTO) {
            anyTypeKind = AnyTypeKind.GROUP;
        } else {
            anyTypeKind = AnyTypeKind.ANY_OBJECT;
        }
        this.restClient = new AuditHistoryRestClient();
        initResultTable();
    }

    @Override
    protected DirectoryDataProvider<AuditEntryBean> dataProvider() {
        return new AuditHistoryProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return Constants.PREF_AUDIT_HISTORY_PAGINATOR_ROWS;
    }

    @Override
    protected List<IColumn<AuditEntryBean, String>> getColumns() {
        final List<IColumn<AuditEntryBean, String>> columns = new ArrayList<>();
        columns.add(new PropertyColumn<>(
            new StringResourceModel("who", this), "who"));
        columns.add(new DatePropertyColumn<>(
            new StringResourceModel("date", this), null, "date"));
        return columns;
    }

    @Override
    protected void resultTableCustomChanges(
        final AjaxDataTablePanel.Builder<AuditEntryBean, String> resultTableBuilder) {
        resultTableBuilder.setMultiLevelPanel(baseModal, multiLevelPanelRef);
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return Collections.emptyList();
    }

    private class AuditHistoryProvider extends DirectoryDataProvider<AuditEntryBean> {
        private static final long serialVersionUID = 415113175628260864L;

        AuditHistoryProvider(final int paginatorRows) {
            super(paginatorRows);
        }

        @Override
        public Iterator<? extends AuditEntryBean> iterator(final long first, final long count) {
            return getAuditEntryBeans(first, count).iterator();
        }

        @Override
        public long size() {
            return restClient.count(anyTO.getKey());
        }

        @Override
        public IModel<AuditEntryBean> model(final AuditEntryBean auditEntryBean) {
            return new IModel<AuditEntryBean>() {
                private static final long serialVersionUID = -7802635613997243712L;

                @Override
                public AuditEntryBean getObject() {
                    return auditEntryBean;
                }
            };
        }

        private List<AuditEntryBean> getAuditEntryBeans(final long first, final long count) {
            int page = (int) first / paginatorRows;
            List<AuditEntryTO> search = restClient.search(anyTO.getKey(),
                Math.max(page, 0) + 1, paginatorRows,
                new SortParam<>("event_date", false),
                new ArrayList<>(), new ArrayList<>());
            return search
                .stream()
                .map(entry -> {
                    AuditEntryBean bean = new AuditEntryBean(anyTO);
                    bean.setBefore(entry.getBefore());
                    bean.setDate(entry.getDate());
                    bean.setEvent(entry.getEvent());
                    bean.setInputs(entry.getInputs());
                    bean.setLoggerName(entry.getLoggerName());
                    bean.setOutput(entry.getOutput());
                    bean.setResult(entry.getResult());
                    bean.setSubCategory(entry.getSubCategory());
                    bean.setThrowable(entry.getThrowable());
                    bean.setWho(entry.getWho());
                    return bean;
                })
                .collect(Collectors.toList());
        }
    }
}
