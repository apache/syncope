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
package org.apache.syncope.core.persistence.neo4j.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.entity.AuditEvent;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.springframework.data.neo4j.core.schema.Node;

@Node(Neo4jAuditEvent.NODE)
public class Neo4jAuditEvent extends AbstractGeneratedKeyNode implements AuditEvent {

    private static final long serialVersionUID = 5020672457509647068L;

    public static final String NODE = "AuditEvent";

    protected static final TypeReference<List<String>> TYPEREF = new TypeReference<List<String>>() {
    };

    @NotNull
    private String opEvent;

    @NotNull
    private String who;

    @NotNull
    private OffsetDateTime when;

    private String before;

    private String inputs;

    private String output;

    private String throwable;

    @Override
    public String getOpEvent() {
        return opEvent;
    }

    @Override
    public void setOpEvent(final String opEvent) {
        this.opEvent = opEvent;
    }

    @Override
    public String getWho() {
        return who;
    }

    @Override
    public void setWho(final String who) {
        this.who = who;
    }

    @Override
    public OffsetDateTime getWhen() {
        return when;
    }

    @Override
    public void setWhen(final OffsetDateTime when) {
        this.when = when;
    }

    @Override
    public String getBefore() {
        return before;
    }

    @Override
    public void setBefore(final String before) {
        this.before = before;
    }

    @Override
    public void setInputs(final List<String> inputs) {
        this.inputs = Optional.ofNullable(inputs).map(POJOHelper::serialize).orElse(null);
    }

    @Override
    public List<String> getInputs() {
        return Optional.ofNullable(inputs).map(i -> POJOHelper.deserialize(i, TYPEREF)).orElseGet(List::of);
    }

    @Override
    public String getOutput() {
        return output;
    }

    @Override
    public void setOutput(final String output) {
        this.output = output;
    }

    @Override
    public String getThrowable() {
        return throwable;
    }

    @Override
    public void setThrowable(final String throwable) {
        this.throwable = throwable;
    }
}
