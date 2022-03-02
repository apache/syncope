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
package org.apache.syncope.common.lib.audit;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.BaseBean;
import org.apache.syncope.common.lib.types.AuditLoggerName;

public class AuditEntry implements BaseBean {

    private static final long serialVersionUID = 1215115961911228005L;

    private String who;

    private OffsetDateTime date;

    private AuditLoggerName logger;

    private String before;

    private final List<String> inputs = new ArrayList<>();

    private String output;

    private String throwable;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public String getWho() {
        return who;
    }

    public void setWho(final String who) {
        this.who = who;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public OffsetDateTime getDate() {
        return date;
    }

    public void setDate(final OffsetDateTime date) {
        this.date = date;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public AuditLoggerName getLogger() {
        return logger;
    }

    public void setLogger(final AuditLoggerName logger) {
        this.logger = logger;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public String getBefore() {
        return before;
    }

    @JacksonXmlElementWrapper(localName = "inputs")
    @JacksonXmlProperty(localName = "input")
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public List<String> getInputs() {
        return inputs;
    }

    public void setBefore(final String before) {
        this.before = before;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public String getOutput() {
        return output;
    }

    public void setOutput(final String output) {
        this.output = output;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public String getThrowable() {
        return throwable;
    }

    public void setThrowable(final String throwable) {
        this.throwable = throwable;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
                append(who).
                append(date).
                append(logger).
                append(before).
                append(inputs).
                append(output).
                append(throwable).
                build();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AuditEntry other = (AuditEntry) obj;
        return new EqualsBuilder().
                appendSuper(super.equals(obj)).
                append(who, other.who).
                append(date, other.date).
                append(logger, other.logger).
                append(before, other.before).
                append(inputs, other.inputs).
                append(output, other.output).
                append(throwable, other.throwable).
                build();
    }
}
