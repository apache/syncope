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
package org.apache.syncope.core.persistence.neo4j.content;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.TransformerHandler;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.persistence.common.content.AbstractXMLContentExporter;
import org.apache.syncope.core.persistence.common.content.MultiParentNode;
import org.apache.syncope.core.persistence.common.content.MultiParentNodeOp;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jAuditEvent;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jImplementationRelationship;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jJobStatus;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jRealm;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jSchema;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jInboundTask;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jMacroTask;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jMacroTaskCommandRelationship;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jProvisioningTask;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jSchedTask;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.types.Node;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.mapping.RelationshipDescription;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class XMLContentExporter extends AbstractXMLContentExporter {

    protected static final Set<String> LABELS_TO_BE_EXCLUDED = Set.of(
            Neo4jSchema.NODE, Neo4jPolicy.NODE, Neo4jProvisioningTask.NODE, Neo4jInboundTask.NODE,
            Neo4jJobStatus.NODE, Neo4jAuditEvent.NODE);

    protected static final Comparator<Record> REALM_COMPARATOR =
            Comparator.comparing(record -> record.get("n").asNode().get("fullPath").asString());

    protected final DomainHolder<Driver> domainHolder;

    protected final Neo4jMappingContext mappingContext;

    public XMLContentExporter(
            final DomainHolder<Driver> domainHolder,
            final Neo4jMappingContext mappingContext) {

        this.domainHolder = domainHolder;
        this.mappingContext = mappingContext;
    }

    protected List<Neo4jPersistentEntity<?>> persistentEntities(final String[] elements) {
        Map<String, Neo4jPersistentEntity<?>> entities = mappingContext.getPersistentEntities().stream().
                filter(e -> !LABELS_TO_BE_EXCLUDED.contains(e.getPrimaryLabel())
                && !e.getPrimaryLabel().startsWith("Abstract")
                && !e.getPrimaryLabel().contains("PlainAttr")).
                collect(Collectors.toMap(
                        Neo4jPersistentEntity::getPrimaryLabel, Function.identity(), (first, second) -> first));

        if (ArrayUtils.isNotEmpty(elements)) {
            entities.entrySet().removeIf(e -> !ArrayUtils.contains(elements, e.getKey()));
        }

        Set<MultiParentNode> roots = new HashSet<>();
        Map<String, MultiParentNode> exploited = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        entities.forEach((label, entity) -> {
            MultiParentNode node = Optional.ofNullable(exploited.get(label)).
                    orElseGet(() -> {
                        MultiParentNode n = new MultiParentNode(label);
                        roots.add(n);
                        exploited.put(label, n);
                        return n;
                    });

            Set<String> refEntityLabels = new HashSet<>();
            entity.getRelationships().stream().filter(r -> r.getDirection() == Relationship.Direction.OUTGOING).
                    forEach(r -> refEntityLabels.add(r.getTarget().getPrimaryLabel()));

            refEntityLabels.stream().
                    filter(refEntityLabel -> !label.equalsIgnoreCase(refEntityLabel)).
                    forEach(refEntityLabel -> {

                        MultiParentNode pkNode = Optional.ofNullable(exploited.get(refEntityLabel)).
                                orElseGet(() -> {
                                    MultiParentNode n = new MultiParentNode(refEntityLabel);
                                    roots.add(n);
                                    exploited.put(refEntityLabel, n);
                                    return n;
                                });

                        pkNode.addChild(node);

                        roots.remove(node);
                    });
        });

        List<String> sortedEntityLabels = new ArrayList<>(entities.size());
        MultiParentNodeOp.traverseTree(roots, sortedEntityLabels);

        // remove from sortedEntityLabels any table possibly added during lookup 
        sortedEntityLabels.retainAll(entities.keySet());

        LOG.debug("Entities after retainAll {}", sortedEntityLabels);

        Collections.reverse(sortedEntityLabels);

        return sortedEntityLabels.stream().map(entities::get).toList();
    }

    protected void exportNode(
            final Neo4jPersistentEntity<?> entity,
            final Record record,
            final Session session,
            final TransformerHandler handler) throws SAXException {

        LOG.debug("Export entity {}", entity.getPrimaryLabel());

        Node node = record.get("n").asNode();

        AttributesImpl attrs = new AttributesImpl();
        node.asMap().forEach((key, value) -> attrs.addAttribute("", "", key, "CDATA", value.toString()));

        handler.startElement("", "", entity.getPrimaryLabel(), attrs);
        handler.endElement("", "", entity.getPrimaryLabel());

        for (org.neo4j.driver.types.Relationship rel : record.get("rels").asList().stream().
                map(org.neo4j.driver.types.Relationship.class::cast).toList()) {

            Optional<RelationshipDescription> relDesc = entity.getRelationships().stream().
                    filter(r -> r.getType().equals(rel.type()) && r.getDirection() == Relationship.Direction.OUTGOING).
                    findFirst();
            if (relDesc.isPresent()) {
                AttributesImpl rattrs = new AttributesImpl();
                rattrs.addAttribute("", "", "type", "CDATA", rel.type());
                rattrs.addAttribute("", "", "left", "CDATA", node.get("id").asString());

                String rightId = session.run(
                        "MATCH (n:" + relDesc.get().getTarget().getPrimaryLabel() + ") "
                        + "WHERE elementId(n) = $endNodeElementId RETURN n.id",
                        Map.of("endNodeElementId", rel.endNodeElementId())).
                        single().get("n.id").asString();
                rattrs.addAttribute("", "", "right", "CDATA", rightId);

                Optional.ofNullable(relDesc.get().getRelationshipPropertiesEntity()).
                        filter(e -> Neo4jImplementationRelationship.class.getSimpleName().equals(e.getPrimaryLabel())
                        || Neo4jMacroTaskCommandRelationship.class.getSimpleName().equals(e.getPrimaryLabel())).
                        flatMap(rpe -> session.run(
                        "MATCH (n {id: $left})-[r:" + relDesc.get().getType() + "]-" + "(m {id: $right}) "
                        + "RETURN r.index",
                        Map.of("left", node.get("id").asString(), "right", rightId)).stream().findFirst()).
                        ifPresent(r -> rattrs.addAttribute("", "", "index", "CDATA", r.get("r.index").asString()));

                String elementName = entity.getPrimaryLabel()
                        + "_"
                        + relDesc.get().getTarget().getPrimaryLabel();
                handler.startElement("", "", elementName, rattrs);
                handler.endElement("", "", elementName);
            }
        }
    }

    @Override
    public void export(
            final String domain,
            final int threshold,
            final OutputStream os,
            final String... elements) throws SAXException, TransformerConfigurationException {

        TransformerHandler handler = start(os);

        for (Neo4jPersistentEntity<?> entity : persistentEntities(elements)) {
            try (Session session = domainHolder.getDomains().get(domain).session()) {
                StringBuilder query = new StringBuilder("MATCH (n:" + entity.getPrimaryLabel() + ")-[r]-() ");
                if (Neo4jSchedTask.NODE.equals(entity.getPrimaryLabel())) {
                    query.append("WHERE NOT n:").append(Neo4jMacroTask.NODE).
                            append(" AND NOT n:").append(Neo4jInboundTask.NODE).
                            append(" AND NOT n:").append(Neo4jProvisioningTask.NODE).append(' ');
                }
                query.append("RETURN n, collect(r) AS rels ORDER BY n.id");

                Stream<Record> records = session.run(query.toString()).stream().limit(threshold);
                if (Neo4jRealm.NODE.equals(entity.getPrimaryLabel())) {
                    records = records.sorted(REALM_COMPARATOR);
                }
                for (Record record : records.toList()) {
                    exportNode(entity, record, session, handler);
                }
            } catch (Exception e) {
                LOG.error("While exporting database content", e);
            }
        }

        end(handler);
    }
}
