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
package org.apache.syncope.core.provisioning.api.pushpull.stream;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.SequenceWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.provisioning.api.Connector;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.springframework.util.CollectionUtils;

public class StreamConnector implements Connector {

    private final String keyColumn;

    private final String arrayElementsSeparator;

    private final MappingIterator<Map<String, String>> reader;

    private final SequenceWriter writer;

    public StreamConnector(
            final String keyColumn,
            final String arrayElementsSeparator,
            final MappingIterator<Map<String, String>> reader,
            final SequenceWriter writer) {

        this.keyColumn = keyColumn;
        this.arrayElementsSeparator = arrayElementsSeparator;
        this.reader = reader;
        this.writer = writer;
    }

    @Override
    public Uid authenticate(final String username, final String password, final OperationOptions options) {
        return null;
    }

    @Override
    public ConnInstance getConnInstance() {
        return null;
    }

    @Override
    public Uid create(
            final ObjectClass objectClass,
            final Set<Attribute> attrs,
            final OperationOptions options,
            final AtomicReference<Boolean> propagationAttempted) {

        if (writer != null) {
            Map<String, String> row = new HashMap<>();
            attrs.stream().filter(attr -> !AttributeUtil.isSpecial(attr)).forEach(attr -> {
                if (CollectionUtils.isEmpty(attr.getValue()) || attr.getValue().get(0) == null) {
                    row.put(attr.getName(), null);
                } else if (attr.getValue().size() == 1) {
                    row.put(attr.getName(), attr.getValue().get(0).toString());
                } else if (arrayElementsSeparator == null) {
                    row.put(attr.getName(), attr.getValue().toString());
                } else {
                    row.put(
                            attr.getName(),
                            attr.getValue().stream().map(Object::toString).
                                    collect(Collectors.joining(arrayElementsSeparator)));
                }
            });
            try {
                writer.write(row);
            } catch (IOException e) {
                throw new IllegalStateException("Could not object " + row, e);
            }
            propagationAttempted.set(Boolean.TRUE);
        }
        return null;
    }

    @Override
    public Uid update(
            final ObjectClass objectClass,
            final Uid uid,
            final Set<Attribute> attrs,
            final OperationOptions options,
            final AtomicReference<Boolean> propagationAttempted) {

        return null;
    }

    @Override
    public void delete(
            final ObjectClass objectClass,
            final Uid uid,
            final OperationOptions options,
            final AtomicReference<Boolean> propagationAttempted) {

        // nothing to do
    }

    @Override
    public void sync(
            final ObjectClass objectClass,
            final SyncToken token,
            final SyncResultsHandler handler,
            final OperationOptions options) {

        throw new UnsupportedOperationException();
    }

    @Override
    public SyncToken getLatestSyncToken(final ObjectClass objectClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectorObject getObject(
            final ObjectClass objectClass,
            final Attribute connObjectKey,
            final boolean ignoreCaseMatch,
            final OperationOptions options) {

        return null;
    }

    @Override
    public SearchResult search(
            final ObjectClass objectClass,
            final Filter filter,
            final SearchResultsHandler handler,
            final OperationOptions options) {

        SearchResult result = new SearchResult();

        if (reader != null) {
            while (reader.hasNext()) {
                Map<String, String> row = reader.next();

                ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
                builder.setObjectClass(objectClass);
                builder.setUid(row.get(keyColumn));
                builder.setName(row.get(keyColumn));

                row.forEach((key, value) -> builder.addAttribute(arrayElementsSeparator == null
                        ? AttributeBuilder.build(key, value)
                        : AttributeBuilder.build(key,
                                (Object[]) StringUtils.splitByWholeSeparator(value, arrayElementsSeparator))));

                handler.handle(builder.build());
            }
        }

        return result;
    }

    @Override
    public Set<ObjectClassInfo> getObjectClassInfo() {
        return Collections.emptySet();
    }

    @Override
    public void validate() {
        // nothing to do
    }

    @Override
    public void test() {
        // nothing to do
    }

    @Override
    public void dispose() {
        // nothing to do
    }
}
