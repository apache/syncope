package org.apache.syncope.core.provisioning.java.data;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.syncope.common.lib.to.AuditEntryTO;
import org.apache.syncope.core.persistence.api.entity.AuditEntry;
import org.apache.syncope.core.provisioning.api.data.AuditDataBinder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class AuditDataBinderImpl implements AuditDataBinder {

    @Override
    public AuditEntryTO getAuditTO(final AuditEntry auditEntry) {
        AuditEntryTO auditTO = new AuditEntryTO();
        auditTO.setKey(auditEntry.getKey());
        auditTO.setWho(auditEntry.getWho());
        auditTO.setDate(auditEntry.getDate());
        auditTO.setThrowable(auditEntry.getThrowable());
        auditTO.setLoggerName(auditEntry.getLogger().toLoggerName());

        if (StringUtils.isNotBlank(auditEntry.getLogger().getSubcategory())) {
            auditTO.setSubCategory(auditEntry.getLogger().getSubcategory());
        }
        if (StringUtils.isNotBlank(auditEntry.getLogger().getEvent())) {
            auditTO.setEvent(auditEntry.getLogger().getEvent());
        }
        if (auditEntry.getLogger().getResult() != null) {
            auditTO.setResult(auditEntry.getLogger().getResult().name());
        }

        if (auditEntry.getBefore() != null) {
            String before = ToStringBuilder.reflectionToString(
                auditEntry.getBefore(), ToStringStyle.JSON_STYLE);
            auditTO.setBefore(before);
        }

        if (auditEntry.getInput() != null) {
            auditTO.getInputs().addAll(Arrays.stream(auditEntry.getInput())
                .map(input -> ToStringBuilder.reflectionToString(input, ToStringStyle.JSON_STYLE))
                .collect(Collectors.toList()));
        }

        if (auditEntry.getOutput() != null) {
            auditTO.setOutput(ToStringBuilder.reflectionToString(
                auditEntry.getOutput(), ToStringStyle.JSON_STYLE));
        }
        
        return auditTO;
    }

    @Override
    public AuditEntryTO returnAuditTO(final AuditEntryTO user) {
        return user;
    }
}
