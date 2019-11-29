/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License; Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing;
 * software distributed under the License is distributed on an
 * "AS IS" BASIS; WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND; either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.provisioning.api;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.syncope.core.persistence.api.entity.AuditEntry;

public class AuditEntryImpl implements AuditEntry {

    private static final long serialVersionUID = -2299082316063743582L;

    private static final String MASKED_VALUE = "<MASKED>";

    private final String who;

    private final AuditLoggerName logger;

    private final Object before;

    private final Object output;

    private final Object[] input;

    private String throwable;

    private Date date;

    @JsonCreator
    public AuditEntryImpl(
            @JsonProperty("who") final String who,
            @JsonProperty("logger") final AuditLoggerName logger,
            @JsonProperty("before") final Object before,
            @JsonProperty("output") final Object output,
            @JsonProperty("input") final Object[] input) {

        super();

        this.who = who;
        this.logger = logger;
        this.before = maskSensitive(before);
        this.output = maskSensitive(output);
        this.input = ArrayUtils.clone(input);
        if (this.input != null) {
            for (int i = 0; i < this.input.length; i++) {
                this.input[i] = maskSensitive(this.input[i]);
            }
        }
    }

    private static Object maskSensitive(final Object object) {
        Object masked;

        if (object instanceof UserTO) {
            masked = SerializationUtils.clone((UserTO) object);
            if (((UserTO) masked).getPassword() != null) {
                ((UserTO) masked).setPassword(MASKED_VALUE);
            }
            if (((UserTO) masked).getSecurityAnswer() != null) {
                ((UserTO) masked).setSecurityAnswer(MASKED_VALUE);
            }
        } else if (object instanceof UserPatch && ((UserPatch) object).getPassword() != null) {
            masked = SerializationUtils.clone((UserPatch) object);
            ((UserPatch) masked).getPassword().setValue(MASKED_VALUE);
        } else {
            masked = object;
        }

        return masked;
    }

    @Override
    public String getWho() {
        return who;
    }

    @Override
    public AuditLoggerName getLogger() {
        return logger;
    }

    @Override
    public Object getBefore() {
        return before;
    }

    @Override
    public Object getOutput() {
        return output;
    }

    @Override
    public Object[] getInput() {
        return input;
    }

    @Override
    public String getThrowable() {
        return throwable;
    }

    public void setThrowable(final String throwable) {
        this.throwable = throwable;
    }

    @Override
    public Date getDate() {
        return date;
    }

    public void setDate(final Date date) {
        this.date = date;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String who;

        private AuditLoggerName logger;

        private Object before;

        private Object output;

        private Object[] input;

        private String throwable;

        private Date date;

        private String key;

        private Builder() {
        }

        public Builder date(final Date date) {
            this.date = date;
            return this;
        }

        public Builder throwable(final String throwable) {
            this.throwable = throwable;
            return this;
        }

        public Builder key(final String key) {
            this.key = key;
            return this;
        }

        public Builder who(final String who) {
            this.who = who;
            return this;
        }

        public Builder logger(final AuditLoggerName logger) {
            this.logger = logger;
            return this;
        }

        public Builder before(final Object before) {
            this.before = before;
            return this;
        }

        public Builder output(final Object output) {
            this.output = output;
            return this;
        }

        public Builder input(final Object[] input) {
            this.input = input;
            return this;
        }

        public AuditEntryImpl build() {
            AuditEntryImpl entry = new AuditEntryImpl(who, logger, before, output, input);
            entry.setDate(date);
            entry.setThrowable(throwable);
            return entry;
        }
    }
}
