package org.apache.syncope.core.persistence.api.entity;

import org.apache.syncope.common.lib.types.AuditLoggerName;

import java.io.Serializable;

public interface AuditEntry extends Serializable {

    String getWho();

    AuditLoggerName getLogger();

    Object getBefore();

    Object getOutput();

    Object[] getInput();
}
