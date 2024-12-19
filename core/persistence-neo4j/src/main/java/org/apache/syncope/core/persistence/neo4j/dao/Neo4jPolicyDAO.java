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
package org.apache.syncope.core.persistence.neo4j.dao;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.dao.CASSPClientAppDAO;
import org.apache.syncope.core.persistence.api.dao.EntityCacheDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.OIDCRPClientAppDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.SAML2SPClientAppDAO;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.policy.AccessPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AttrReleasePolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AuthPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.InboundPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.Policy;
import org.apache.syncope.core.persistence.api.entity.policy.PropagationPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PushPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.TicketExpirationPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jExternalResource;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jImplementation;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jAccessPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jAccountPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jAttrReleasePolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jAuthPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jInboundCorrelationRuleEntity;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jInboundPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jPasswordPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jPropagationPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jPushCorrelationRuleEntity;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jPushPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jTicketExpirationPolicy;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;

public class Neo4jPolicyDAO extends AbstractDAO implements PolicyDAO {

    protected static final Logger LOG = LoggerFactory.getLogger(PolicyDAO.class);

    protected static <T extends Policy> Class<? extends Neo4jPolicy> getNodeReference(final Class<T> reference) {
        return AccountPolicy.class.isAssignableFrom(reference)
                ? Neo4jAccountPolicy.class
                : PasswordPolicy.class.isAssignableFrom(reference)
                ? Neo4jPasswordPolicy.class
                : PropagationPolicy.class.isAssignableFrom(reference)
                ? Neo4jPropagationPolicy.class
                : InboundPolicy.class.isAssignableFrom(reference)
                ? Neo4jInboundPolicy.class
                : PushPolicy.class.isAssignableFrom(reference)
                ? Neo4jPushPolicy.class
                : AuthPolicy.class.isAssignableFrom(reference)
                ? Neo4jAuthPolicy.class
                : AccessPolicy.class.isAssignableFrom(reference)
                ? Neo4jAccessPolicy.class
                : AttrReleasePolicy.class.isAssignableFrom(reference)
                ? Neo4jAttrReleasePolicy.class
                : TicketExpirationPolicy.class.isAssignableFrom(reference)
                ? Neo4jTicketExpirationPolicy.class
                : null;
    }

    protected final RealmDAO realmDAO;

    protected final ExternalResourceDAO resourceDAO;

    protected final CASSPClientAppDAO casSPClientAppDAO;

    protected final OIDCRPClientAppDAO oidcRPClientAppDAO;

    protected final SAML2SPClientAppDAO saml2SPClientAppDAO;

    protected final EntityCacheDAO entityCacheDAO;

    protected final NodeValidator nodeValidator;

    public Neo4jPolicyDAO(
            final RealmDAO realmDAO,
            final ExternalResourceDAO resourceDAO,
            final CASSPClientAppDAO casSPClientAppDAO,
            final OIDCRPClientAppDAO oidcRPClientAppDAO,
            final SAML2SPClientAppDAO saml2SPClientAppDAO,
            final EntityCacheDAO entityCacheDAO,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator) {

        super(neo4jTemplate, neo4jClient);
        this.realmDAO = realmDAO;
        this.resourceDAO = resourceDAO;
        this.casSPClientAppDAO = casSPClientAppDAO;
        this.oidcRPClientAppDAO = oidcRPClientAppDAO;
        this.saml2SPClientAppDAO = saml2SPClientAppDAO;
        this.entityCacheDAO = entityCacheDAO;
        this.nodeValidator = nodeValidator;
    }

    @Override
    public boolean existsById(final String key) {
        return neo4jTemplate.count(
                "MATCH (n:" + Neo4jPolicy.NODE + ") WHERE n.id = $key RETURN count(n)", Map.of("key", key)) > 0;
    }

    @Override
    public Optional<? extends Policy> findById(final String key) {
        return neo4jClient.query(
                "MATCH (n:" + Neo4jPolicy.NODE + ") WHERE n.id = $key RETURN n.id").
                bindAll(Map.of("key", key)).fetch().one().
                flatMap(toOptional("n.id", Neo4jPolicy.class, null));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Policy> Optional<T> findById(final String key, final Class<T> reference) {
        return neo4jTemplate.findById(key, getNodeReference(reference)).map(reference::cast);
    }

    @Override
    public long count() {
        return neo4jTemplate.count("MATCH (n:" + Neo4jPolicy.NODE + ") RETURN count(n)");
    }

    @Override
    public List<? extends Policy> findAll() {
        return toList(neo4jClient.query(
                "MATCH (n:" + Neo4jPolicy.NODE + ") RETURN n.id").
                fetch().all(), "n.id", Neo4jPolicy.class, null);
    }

    @Override
    public <T extends Policy> List<T> findAll(final Class<T> reference) {
        return neo4jTemplate.findAll(getNodeReference(reference)).stream().map(reference::cast).toList();
    }

    @Override
    public List<AccountPolicy> findByAccountRule(final Implementation accountRule) {
        return findByRelationship(
                Neo4jAccountPolicy.NODE,
                Neo4jImplementation.NODE,
                accountRule.getKey(),
                Neo4jAccountPolicy.class,
                null);
    }

    @Override
    public List<PasswordPolicy> findByPasswordRule(final Implementation passwordRule) {
        return findByRelationship(
                Neo4jPasswordPolicy.NODE,
                Neo4jImplementation.NODE,
                passwordRule.getKey(),
                Neo4jPasswordPolicy.class,
                null);
    }

    @Override
    public List<InboundPolicy> findByInboundCorrelationRule(final Implementation correlationRule) {
        return findByRelationship(
                Neo4jInboundPolicy.NODE,
                Neo4jImplementation.NODE,
                correlationRule.getKey(),
                Neo4jInboundPolicy.class,
                null);
    }

    @Override
    public List<PushPolicy> findByPushCorrelationRule(final Implementation correlationRule) {
        return findByRelationship(
                Neo4jPushPolicy.NODE,
                Neo4jImplementation.NODE,
                correlationRule.getKey(),
                Neo4jPushPolicy.class,
                null);
    }

    @Override
    public List<AccountPolicy> findByResource(final ExternalResource resource) {
        return findByRelationship(
                Neo4jAccountPolicy.NODE,
                Neo4jExternalResource.NODE,
                resource.getKey(),
                Neo4jAccountPolicy.class,
                null);
    }

    @Override
    public <P extends Policy> P save(final P policy) {
        P saved = neo4jTemplate.save(nodeValidator.validate(policy));

        switch (saved) {
            case Neo4jAccountPolicy accountPolicy ->
                neo4jTemplate.findById(accountPolicy.getKey(), Neo4jAccountPolicy.class).
                        ifPresent(t -> t.getRules().stream().filter(rule -> !accountPolicy.getRules().contains(rule)).
                        forEach(impl -> deleteRelationship(
                        Neo4jAccountPolicy.NODE,
                        Neo4jImplementation.NODE,
                        accountPolicy.getKey(),
                        impl.getKey(),
                        Neo4jAccountPolicy.ACCOUNT_POLICY_RULE_REL)));

            case Neo4jPasswordPolicy passwordPolicy ->
                neo4jTemplate.findById(passwordPolicy.getKey(), Neo4jPasswordPolicy.class).
                        ifPresent(t -> t.getRules().stream().filter(rule -> !passwordPolicy.getRules().contains(rule)).
                        forEach(impl -> deleteRelationship(
                        Neo4jPasswordPolicy.NODE,
                        Neo4jImplementation.NODE,
                        passwordPolicy.getKey(),
                        impl.getKey(),
                        Neo4jPasswordPolicy.PASSWORD_POLICY_RULE_REL)));

            default -> {
            }
        }

        if (policy instanceof AccountPolicy
                || policy instanceof PasswordPolicy
                || policy instanceof PropagationPolicy
                || policy instanceof InboundPolicy
                || policy instanceof PushPolicy) {

            resourceDAO.findByPolicy(policy).
                    forEach(resource -> entityCacheDAO.evict(Neo4jExternalResource.class, resource.getKey()));
        }

        return saved;
    }

    @Override
    public void delete(final Policy policy) {
        if (policy instanceof AccountPolicy) {
            realmDAO.findByPolicy(policy).forEach(realm -> realm.setAccountPolicy(null));
            resourceDAO.findByPolicy(policy).forEach(resource -> resource.setAccountPolicy(null));
        } else if (policy instanceof PasswordPolicy) {
            realmDAO.findByPolicy(policy).forEach(realm -> realm.setPasswordPolicy(null));
            resourceDAO.findByPolicy(policy).forEach(resource -> resource.setPasswordPolicy(null));
        } else if (policy instanceof PropagationPolicy) {
            resourceDAO.findByPolicy(policy).forEach(resource -> resource.setPropagationPolicy(null));
        } else if (policy instanceof InboundPolicy) {
            resourceDAO.findByPolicy(policy).forEach(resource -> resource.setInboundPolicy(null));

            cascadeDelete(Neo4jInboundCorrelationRuleEntity.NODE,
                    Neo4jInboundPolicy.NODE,
                    policy.getKey());
        } else if (policy instanceof PushPolicy) {
            resourceDAO.findByPolicy(policy).forEach(resource -> resource.setPushPolicy(null));

            cascadeDelete(
                    Neo4jPushCorrelationRuleEntity.NODE,
                    Neo4jPushPolicy.NODE,
                    policy.getKey());
        } else if (policy instanceof AuthPolicy) {
            realmDAO.findByPolicy(policy).forEach(realm -> realm.setAuthPolicy(null));
            casSPClientAppDAO.findAllByPolicy(policy).forEach(clientApp -> clientApp.setAuthPolicy(null));
            oidcRPClientAppDAO.findAllByPolicy(policy).forEach(clientApp -> clientApp.setAuthPolicy(null));
            saml2SPClientAppDAO.findAllByPolicy(policy).forEach(clientApp -> clientApp.setAuthPolicy(null));
        } else if (policy instanceof AccessPolicy) {
            realmDAO.findByPolicy(policy).forEach(realm -> realm.setAccessPolicy(null));
            casSPClientAppDAO.findAllByPolicy(policy).forEach(clientApp -> clientApp.setAccessPolicy(null));
            oidcRPClientAppDAO.findAllByPolicy(policy).forEach(clientApp -> clientApp.setAccessPolicy(null));
            saml2SPClientAppDAO.findAllByPolicy(policy).forEach(clientApp -> clientApp.setAccessPolicy(null));
        } else if (policy instanceof AttrReleasePolicy) {
            realmDAO.findByPolicy(policy).forEach(realm -> realm.setAttrReleasePolicy(null));
            casSPClientAppDAO.findAllByPolicy(policy).forEach(clientApp -> clientApp.setAttrReleasePolicy(null));
            oidcRPClientAppDAO.findAllByPolicy(policy).forEach(clientApp -> clientApp.setAttrReleasePolicy(null));
            saml2SPClientAppDAO.findAllByPolicy(policy).forEach(clientApp -> clientApp.setAttrReleasePolicy(null));
        } else if (policy instanceof TicketExpirationPolicy) {
            realmDAO.findByPolicy(policy).forEach(realm -> realm.setTicketExpirationPolicy(null));
            casSPClientAppDAO.findAllByPolicy(policy).forEach(clientApp -> clientApp.setTicketExpirationPolicy(null));
            oidcRPClientAppDAO.findAllByPolicy(policy).forEach(clientApp -> clientApp.setTicketExpirationPolicy(null));
            saml2SPClientAppDAO.findAllByPolicy(policy).forEach(clientApp -> clientApp.setTicketExpirationPolicy(null));
        }

        neo4jClient.query("MATCH (n:" + Neo4jPolicy.NODE + " {id: $id}) DETACH DELETE n").
                bindAll(Map.of("id", policy.getKey())).run();
    }

    @Override
    public void deleteById(final String key) {
        findById(key).ifPresent(this::delete);
    }
}
