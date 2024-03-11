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
package org.apache.syncope.common.lib.to;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.ws.rs.PathParam;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.types.OpEvent;

public class AuditEventTO implements EntityTO {

    private static final long serialVersionUID = -4744825079853638110L;

    private String key;

    private OpEvent opEvent;

    private String who;

    private OffsetDateTime when;

    private String before;

    private final List<String> inputs = new ArrayList<>();

    private String output;

    private String throwable;

    @Override
    public String getKey() {
        return key;
    }

    @PathParam("key")
    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    public OpEvent getOpEvent() {
        return opEvent;
    }

    public void setOpEvent(final OpEvent opEvent) {
        this.opEvent = opEvent;
    }

    public String getWho() {
        return who;
    }

    public void setWho(final String who) {
        this.who = who;
    }

    public OffsetDateTime getWhen() {
        return when;
    }

    public void setWhen(final OffsetDateTime when) {
        this.when = when;
    }

    public String getBefore() {
        return before;
    }

    public void setBefore(final String before) {
        this.before = before;
    }

    @JacksonXmlElementWrapper(localName = "inputs")
    @JacksonXmlProperty(localName = "input")
    public List<String> getInputs() {
        return inputs;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(final String output) {
        this.output = output;
    }

    public String getThrowable() {
        return throwable;
    }

    public void setThrowable(final String throwable) {
        this.throwable = throwable;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(key).
                append(opEvent).
                append(who).
                append(when).
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
        final AuditEventTO other = (AuditEventTO) obj;
        return new EqualsBuilder().
                append(key, other.key).
                append(opEvent, other.opEvent).
                append(who, other.who).
                append(when, other.when).
                append(before, other.before).
                append(inputs, other.inputs).
                append(output, other.output).
                append(throwable, other.throwable).
                build();
    }
}
