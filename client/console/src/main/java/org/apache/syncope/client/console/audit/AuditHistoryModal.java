package org.apache.syncope.client.console.audit;

import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.panels.ModalPanel;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.rest.AuditHistoryRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.markup.html.panel.Panel;

public class AuditHistoryModal<T extends AnyTO> extends Panel implements ModalPanel {
    private static final long serialVersionUID = 1066124171682570080L;

    protected final DirectoryPanel<AuditEntryBean, AuditEntryBean, ?, ?> directoryPanel;

    public AuditHistoryModal(
        final BaseModal<?> baseModal,
        final PageReference pageReference,
        final T entity) {

        super(BaseModal.CONTENT_ID);

        final MultilevelPanel mlp = new MultilevelPanel("history");
        mlp.setOutputMarkupId(true);
        this.directoryPanel = getDirectoryPanel(mlp, baseModal, pageReference, entity);
        add(mlp.setFirstLevel(this.directoryPanel));
    }

    protected DirectoryPanel<AuditEntryBean, AuditEntryBean,
        DirectoryDataProvider<AuditEntryBean>, AuditHistoryRestClient> getDirectoryPanel(
        final MultilevelPanel mlp,
        final BaseModal<?> baseModal,
        final PageReference pageReference,
        final T entity) {

        return new AuditHistoryDirectoryPanel(baseModal, mlp, pageReference, entity);
    }
}
