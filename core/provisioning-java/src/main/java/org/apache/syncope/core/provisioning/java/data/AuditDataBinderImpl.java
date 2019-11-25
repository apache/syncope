package org.apache.syncope.core.provisioning.java.data;

import org.apache.syncope.common.lib.to.AuditEntryTO;
import org.apache.syncope.core.persistence.api.entity.AuditEntry;
import org.apache.syncope.core.provisioning.api.data.AuditDataBinder;
import org.springframework.stereotype.Component;

@Component
public class AuditDataBinderImpl implements AuditDataBinder {
    @Override
    public AuditEntry create(final AuditEntryTO applicationTO) {
        return null;
    }

    @Override
    public AuditEntryTO getAuditTO(final AuditEntry application) {
        return null;
    }

    @Override
    public AuditEntryTO returnAuditTO(final AuditEntryTO user, final boolean details) {
        return null;
    }
}
