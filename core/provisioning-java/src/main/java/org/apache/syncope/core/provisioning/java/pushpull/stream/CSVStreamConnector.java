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
package org.apache.syncope.core.provisioning.java.pushpull.stream;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.syncope.common.rest.api.beans.CSVPullSpec;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.provisioning.api.Connector;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeDelta;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.LiveSyncResultsHandler;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

public class CSVStreamConnector implements Connector, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(CSVStreamConnector.class);

    private final String keyColumn;

    private final String arrayElementsSeparator;

    private final CsvSchema.Builder schemaBuilder;

    private final InputStream in;

    private final OutputStream out;

    private final List<String> columns;

    private MappingIterator<Map<String, String>> reader;

    private SequenceWriter writer;

    public CSVStreamConnector(
            final String keyColumn,
            final String arrayElementsSeparator,
            final CsvSchema.Builder schemaBuilder,
            final InputStream in,
            final OutputStream out,
            final String... columns) {

        this.keyColumn = keyColumn;
        this.arrayElementsSeparator = arrayElementsSeparator;
        this.schemaBuilder = schemaBuilder;
        this.in = in;
        this.out = out;
        this.columns = List.of(columns);
    }

    @Override
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
        }
        if (writer != null) {
            writer.close();
        }
    }

    public MappingIterator<Map<String, String>> reader() throws IOException {
        synchronized (this) {
            if (reader == null) {
                reader = new CsvMapper().
                        enable(CsvParser.Feature.SKIP_EMPTY_LINES).
                        readerFor(Map.class).with(schemaBuilder.build()).readValues(in);
            }
        }
        return reader;
    }

    public List<String> getColumns(final CSVPullSpec spec) throws IOException {
        List<String> fromSpec = new ArrayList<>();
        ((CsvSchema) reader().getParserSchema()).forEach(column -> {
            if (!spec.getIgnoreColumns().contains(column.getName())) {
                fromSpec.add(column.getName());
            }
        });
        return fromSpec;
    }

    public SequenceWriter writer() throws IOException {
        synchronized (this) {
            if (writer == null) {
                writer = new CsvMapper().writerFor(Map.class).with(schemaBuilder.build()).writeValues(out);
            }
        }
        return writer;
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
            final Mutable<Boolean> propagationAttempted) {

        synchronized (schemaBuilder) {
            if (schemaBuilder.size() == 0) {
                attrs.stream().filter(attr -> !AttributeUtil.isSpecial(attr)).map(Attribute::getName).
                        sorted((c1, c2) -> {
                            // sort according to the passed columns, leave any additional column at the end
                            int index1 = columns.indexOf(c1);
                            if (index1 == -1) {
                                index1 = Integer.MAX_VALUE;
                            }
                            int index2 = columns.indexOf(c2);
                            if (index2 == -1) {
                                index2 = Integer.MAX_VALUE;
                            }
                            return Integer.compare(index1, index2);
                        }).
                        forEachOrdered(schemaBuilder::addColumn);
            }
        }

        Map<String, String> row = new LinkedHashMap<>();
        attrs.stream().filter(attr -> !AttributeUtil.isSpecial(attr)).forEach(attr -> {
            if (CollectionUtils.isEmpty(attr.getValue()) || attr.getValue().getFirst() == null) {
                row.put(attr.getName(), null);
            } else if (attr.getValue().size() == 1) {
                row.put(attr.getName(), attr.getValue().getFirst().toString());
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
            writer().write(row);
        } catch (IOException e) {
            throw new ConnectorException("Could not write object " + row, e);
        }
        propagationAttempted.setValue(true);
        return null;
    }

    @Override
    public Uid update(
            final ObjectClass objectClass,
            final Uid uid,
            final Set<Attribute> attrs,
            final OperationOptions options,
            final Mutable<Boolean> propagationAttempted) {

        return null;
    }

    @Override
    public Set<AttributeDelta> updateDelta(
            final ObjectClass objectClass,
            final Uid uid,
            final Set<AttributeDelta> modifications,
            final OperationOptions options,
            final Mutable<Boolean> propagationAttempted) {

        return Set.of();
    }

    @Override
    public void delete(
            final ObjectClass objectClass,
            final Uid uid,
            final OperationOptions options,
            final Mutable<Boolean> propagationAttempted) {

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
    public void livesync(
            final ObjectClass objectClass,
            final LiveSyncResultsHandler handler,
            final OperationOptions options) {

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

        try {
            for (int record = 1; reader().hasNext(); record++) {
                Map<String, String> row = reader().next();

                String keyValue = row.get(keyColumn);
                if (StringUtils.isBlank(keyValue)) {
                    keyValue = "Record " + record;
                }

                ConnectorObjectBuilder builder = new ConnectorObjectBuilder().
                        setObjectClass(objectClass).
                        setUid(keyValue).
                        setName(keyValue);

                row.forEach((key, value) -> builder.addAttribute(arrayElementsSeparator == null
                        ? AttributeBuilder.build(key, value)
                        : AttributeBuilder.build(key,
                                (Object[]) StringUtils.splitByWholeSeparator(value, arrayElementsSeparator))));

                ConnectorObject obj = builder.build();
                if (filter == null || filter.accept(obj)) {
                    handler.handle(obj);
                } else {
                    LOG.debug("Found but not passing the provided filter {}: {}", filter, obj);
                }
            }
        } catch (IOException e) {
            LOG.error("Could not read CSV from provided stream", e);
            throw new ConnectorException(e);
        }

        return result;
    }

    @Override
    public Set<ObjectClassInfo> getObjectClassInfo() {
        return Set.of();
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
