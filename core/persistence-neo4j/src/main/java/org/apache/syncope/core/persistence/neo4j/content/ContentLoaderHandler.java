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

import jakarta.xml.bind.DatatypeConverter;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.syncope.core.persistence.api.utils.FormatUtils;
import org.apache.syncope.core.persistence.common.content.AbstractContentLoaderHandler;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jDerSchema;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jImplementationRelationship;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jPlainSchema;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jSchema;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jVirSchema;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jAccessPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jAccountPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jAttrReleasePolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jAuthPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jInboundPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jPasswordPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jPropagationPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jPushPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jTicketExpirationPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jInboundTask;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jLiveSyncTask;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jMacroTask;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jMacroTaskCommandRelationship;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jProvisioningTask;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jPullTask;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jPushTask;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jSchedTask;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.core.env.Environment;
import org.springframework.data.neo4j.core.mapping.GraphPropertyDescription;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.NodeDescription;
import org.xml.sax.Attributes;

/**
 * SAX handler for generating CREATE statements out of given XML file.
 */
public class ContentLoaderHandler extends AbstractContentLoaderHandler {

    protected record Node(String id, Map<String, Object> props) {

    }

    protected record Relationship(String leftId, String rightId, String type, String index) {

    }

    protected record Query(String statement, Map<String, Object> props) {

    }

    protected static String nodelabels(final String primaryLabel) {
        switch (primaryLabel) {
            case Neo4jPlainSchema.NODE -> {
                return Neo4jPlainSchema.NODE + ":" + Neo4jSchema.NODE;
            }
            case Neo4jDerSchema.NODE -> {
                return Neo4jDerSchema.NODE + ":" + Neo4jSchema.NODE;
            }
            case Neo4jVirSchema.NODE -> {
                return Neo4jVirSchema.NODE + ":" + Neo4jSchema.NODE;
            }

            case Neo4jAccessPolicy.NODE -> {
                return Neo4jAccessPolicy.NODE + ":" + Neo4jPolicy.NODE;
            }
            case Neo4jAccountPolicy.NODE -> {
                return Neo4jAccountPolicy.NODE + ":" + Neo4jPolicy.NODE;
            }
            case Neo4jAttrReleasePolicy.NODE -> {
                return Neo4jAttrReleasePolicy.NODE + ":" + Neo4jPolicy.NODE;
            }
            case Neo4jAuthPolicy.NODE -> {
                return Neo4jAuthPolicy.NODE + ":" + Neo4jPolicy.NODE;
            }
            case Neo4jPasswordPolicy.NODE -> {
                return Neo4jPasswordPolicy.NODE + ":" + Neo4jPolicy.NODE;
            }
            case Neo4jPropagationPolicy.NODE -> {
                return Neo4jPropagationPolicy.NODE + ":" + Neo4jPolicy.NODE;
            }
            case Neo4jPushPolicy.NODE -> {
                return Neo4jPushPolicy.NODE + ":" + Neo4jPolicy.NODE;
            }
            case Neo4jInboundPolicy.NODE -> {
                return Neo4jInboundPolicy.NODE + ":" + Neo4jPolicy.NODE;
            }
            case Neo4jTicketExpirationPolicy.NODE -> {
                return Neo4jTicketExpirationPolicy.NODE + ":" + Neo4jPolicy.NODE;
            }

            case Neo4jPushTask.NODE -> {
                return Neo4jPushTask.NODE + ":" + Neo4jProvisioningTask.NODE + ":" + Neo4jSchedTask.NODE;
            }
            case Neo4jPullTask.NODE -> {
                return Neo4jPullTask.NODE + ":" + Neo4jInboundTask.NODE + ":"
                        + Neo4jProvisioningTask.NODE + ":" + Neo4jSchedTask.NODE;
            }
            case Neo4jLiveSyncTask.NODE -> {
                return Neo4jLiveSyncTask.NODE + ":" + Neo4jInboundTask.NODE + ":"
                        + Neo4jProvisioningTask.NODE + ":" + Neo4jSchedTask.NODE;
            }
            case Neo4jMacroTask.NODE -> {
                return Neo4jMacroTask.NODE + ":" + Neo4jSchedTask.NODE;
            }

            default -> {
                return primaryLabel;
            }
        }
    }

    protected static String escape(final String k) {
        return k.startsWith("plainAttrs.") ? k.replace('.', '_') : k;
    }

    protected final Driver driver;

    protected final Neo4jMappingContext mappingContext;

    public ContentLoaderHandler(
            final Driver driver,
            final Neo4jMappingContext mappingContext,
            final String rootElement,
            final boolean continueOnError,
            final Environment env) {

        super(rootElement, continueOnError, env);
        this.driver = driver;
        this.mappingContext = mappingContext;
    }

    @Override
    protected void fetch(final Attributes atts) {
        try (Session session = driver.session()) {
            String value = session.run(atts.getValue("query")).single().get(0).asString();
            String key = atts.getValue("key");
            fetches.put(key, value);
        } catch (Exception e) {
            LOG.error("While running '{}'", atts.getValue("query"), e);
        }
    }

    protected Optional<Node> parseNode(final NodeDescription<?> nodeDesc, final Attributes atts) {
        String id = null;
        Map<String, Object> props = new HashMap<>();
        for (int i = 0; i < atts.getLength(); i++) {
            String originalName = atts.getQName(i);
            String originalValue = atts.getValue(i);

            if ("id".equalsIgnoreCase(originalName)) {
                id = originalValue;
                props.put("id", originalValue);
            } else {
                String name = nodeDesc.getGraphProperties().stream().
                    map(GraphPropertyDescription::getPropertyName).
                    filter(propertyName -> propertyName.equalsIgnoreCase(originalName)).
                        findFirst().orElseGet(() -> originalName.startsWith("plainAttrs.") ? originalName : null);
                if (name == null) {
                    LOG.error("Property {} not matching for {}", originalName, nodeDesc.getPrimaryLabel());
                    continue;
                }

                Class<?> type = nodeDesc.getGraphProperties().stream().
                        filter(prop -> prop.getPropertyName().equalsIgnoreCase(name)).
                        findFirst().map(GraphPropertyDescription::getActualType).
                        orElseGet(() -> {
                            if (!name.startsWith("plainAttrs.")) {
                                LOG.warn("No type found for property {}#{}", nodeDesc.getPrimaryLabel(), name);
                            }
                            return String.class;
                        });

                String value = paramSubstitutor.replace(atts.getValue(i));
                if (value == null) {
                    LOG.warn("Variable ${} could not be resolved", atts.getValue(i));
                    value = atts.getValue(i);
                }
                value = StringEscapeUtils.unescapeXml(value);

                if (int.class.isAssignableFrom(type) || Integer.class.isAssignableFrom(type)) {
                    try {
                        props.put(name, Integer.valueOf(value));
                    } catch (NumberFormatException e) {
                        LOG.error("Unparsable Integer '{}'", value);
                    }
                } else if (long.class.isAssignableFrom(type) || Long.class.isAssignableFrom(type)) {
                    try {
                        props.put(name, Long.valueOf(value));
                    } catch (NumberFormatException e) {
                        LOG.error("Unparsable Long '{}'", value);
                    }
                } else if (float.class.isAssignableFrom(type) || Float.class.isAssignableFrom(type)) {
                    try {
                        props.put(name, Float.valueOf(value));
                    } catch (NumberFormatException e) {
                        LOG.error("Unparsable Float '{}'", value);
                    }
                } else if (double.class.isAssignableFrom(type) || Double.class.isAssignableFrom(type)) {
                    try {
                        props.put(name, Double.valueOf(value));
                    } catch (NumberFormatException e) {
                        LOG.error("Unparsable Double '{}'", value);
                    }
                } else if (Date.class.isAssignableFrom(type) || OffsetDateTime.class.isAssignableFrom(type)) {
                    try {
                        props.put(name, FormatUtils.parseDate(value));
                    } catch (DateTimeParseException e) {
                        LOG.error("Unparsable Date '{}'", value);
                    }
                } else if (boolean.class.isAssignableFrom(type) || Boolean.class.isAssignableFrom(type)) {
                    props.put(name, "1".equals(value) ? Boolean.TRUE : Boolean.FALSE);
                } else if (byte[].class.isAssignableFrom(type)) {
                    try {
                        props.put(name, DatatypeConverter.parseHexBinary(value));
                    } catch (IllegalArgumentException e) {
                        LOG.warn("Error decoding hex string to specify a blob parameter", e);
                    }
                }
                if (!props.containsKey(name)) {
                    props.put(name, value);
                }
            }
        }

        return id == null ? Optional.empty() : Optional.of(new Node(id, props));
    }

    protected Optional<Relationship> parseRelationship(
            final NodeDescription<?> nodeDesc, final String rightNode, final Attributes atts) {

        String left = null;
        String right = null;
        String type = null;
        String index = null;
        for (int i = 0; i < atts.getLength(); i++) {
            if ("left".equalsIgnoreCase(atts.getQName(i))) {
                left = atts.getValue(i);
            } else if ("right".equalsIgnoreCase(atts.getQName(i))) {
                right = atts.getValue(i);
            } else if ("type".equalsIgnoreCase(atts.getQName(i))) {
                type = atts.getValue(i);
            } else if ("index".equalsIgnoreCase(atts.getQName(i))) {
                index = atts.getValue(i);
            }
        }
        if (left == null || right == null) {
            LOG.warn("Could not find left and/or right attribute in {}_{}", nodeDesc.getPrimaryLabel(), rightNode);
            return Optional.empty();
        }

        String leftId = left;
        String rightId = right;
        String relType = type;
        String indexValue = index;
        return nodeDesc.getRelationships().stream().
                filter(rel -> rightNode.equals(rel.getTarget().getPrimaryLabel())
                && (relType == null || relType.equals(rel.getType()))).
                findFirst().map(rel -> new Relationship(
                leftId,
                rightId,
                rel.getType(),
                Optional.ofNullable(rel.getRelationshipPropertiesEntity()).
                        filter(e -> Neo4jImplementationRelationship.class.getSimpleName().equals(e.getPrimaryLabel())
                        || Neo4jMacroTaskCommandRelationship.class.getSimpleName().equals(e.getPrimaryLabel())).
                        map(e -> indexValue).orElse(null)));
    }

    @Override
    protected void create(final String qName, final Attributes atts) {
        Optional<Query> query;
        if (qName.contains("_")) {
            String[] split = qName.split("_");
            query = parseRelationship(mappingContext.getNodeDescription(split[0]), split[1], atts).
                    map(rel -> new Query(
                    "MATCH (a:" + split[0] + " {id: '" + rel.leftId() + "'}), "
                    + "(b:" + split[1] + " {id: '" + rel.rightId() + "'}) "
                    + "CREATE (a)-"
                    + "[:" + rel.type() + (rel.index() == null ? "" : " {index: " + rel.index() + "}") + "]->(b)",
                    Map.of()));
        } else {
            query = parseNode(mappingContext.getNodeDescription(qName), atts).map(node -> {
                StringBuilder q = new StringBuilder("CREATE (n:").append(nodelabels(qName)).append(" {");
                q.append(node.props().keySet().stream().
                        map(o -> "`" + o + "`" + ": $" + escape(o)).
                        collect(Collectors.joining(", ")));
                q.append("})");
                return new Query(q.toString(), node.props().entrySet().stream().
                        collect(Collectors.toMap(e -> escape(e.getKey()), Map.Entry::getValue)));
            });
        }

        query.ifPresent(q -> {
            LOG.debug("About to run: {}", q);

            try (Session session = driver.session()) {
                session.run(q.statement(), q.props());
            } catch (Exception e) {
                LOG.error("While processing {}", qName, e);
                if (!continueOnError) {
                    throw e;
                }
            }
        });
    }
}
