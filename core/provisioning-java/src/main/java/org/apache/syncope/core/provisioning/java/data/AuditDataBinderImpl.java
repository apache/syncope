package org.apache.syncope.core.provisioning.java.data;

import org.apache.syncope.common.lib.to.AuditTO;
import org.apache.syncope.core.persistence.api.entity.AuditEntry;
import org.apache.syncope.core.provisioning.api.data.AuditDataBinder;
import org.springframework.stereotype.Component;

@Component
public class AuditDataBinderImpl implements AuditDataBinder {
    @Override
    public AuditEntry create(final AuditTO applicationTO) {
        return null;
    }

    @Override
    public AuditTO getAuditTO(final AuditEntry application) {
        return null;
    }
}
