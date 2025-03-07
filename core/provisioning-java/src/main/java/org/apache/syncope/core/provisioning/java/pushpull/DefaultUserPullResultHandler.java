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
package org.apache.syncope.core.provisioning.java.pushpull;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.request.AnyCR;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.request.LinkedAccountUR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.LinkedAccountTO;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.MatchType;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.ProvisioningManager;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.apache.syncope.core.provisioning.api.propagation.PropagationException;
import org.apache.syncope.core.provisioning.api.pushpull.InboundActions;
import org.apache.syncope.core.provisioning.api.pushpull.UserPullResultHandler;
import org.apache.syncope.core.provisioning.api.rules.InboundMatch;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.springframework.beans.factory.annotation.Autowired;

public class DefaultUserPullResultHandler extends AbstractPullResultHandler implements UserPullResultHandler {

    @Autowired
    private UserProvisioningManager userProvisioningManager;

    @Override
    protected AnyUtils getAnyUtils() {
        return anyUtilsFactory.getInstance(AnyTypeKind.USER);
    }

    @Override
    protected String getName(final AnyTO anyTO) {
        return UserTO.class.cast(anyTO).getUsername();
    }

    @Override
    protected String getName(final AnyCR anyCR) {
        return UserCR.class.cast(anyCR).getUsername();
    }

    @Override
    protected ProvisioningManager<?, ?> getProvisioningManager() {
        return userProvisioningManager;
    }

    @Override
    protected AnyTO getAnyTO(final Any any) {
        return userDataBinder.getUserTO((User) any, true);
    }

    @Override
    protected WorkflowResult<? extends AnyUR> update(final AnyUR req) {
        WorkflowResult<Pair<UserUR, Boolean>> update =
                uwfAdapter.update((UserUR) req, null, profile.getExecutor(), profile.getContext());
        return new WorkflowResult<>(update.getResult().getLeft(), update.getPropByRes(), update.getPerformedTasks());
    }

    protected Boolean enabled(final SyncDelta delta) {
        return profile.getTask().isSyncStatus() ? AttributeUtil.isEnabled(delta.getObject()) : null;
    }

    @Override
    protected AnyTO doCreate(final AnyCR anyCR, final SyncDelta delta) {
        Map.Entry<String, List<PropagationStatus>> created = userProvisioningManager.create(
                UserCR.class.cast(anyCR),
                true,
                enabled(delta),
                Set.of(profile.getTask().getResource().getKey()),
                true,
                profile.getExecutor(),
                profile.getContext());

        return userDataBinder.getUserTO(created.getKey());
    }

    @Override
    protected AnyUR doUpdate(
            final AnyTO before,
            final AnyUR req,
            final SyncDelta delta,
            final ProvisioningReport result) {

        Pair<UserUR, List<PropagationStatus>> updated = userProvisioningManager.update(
                UserUR.class.cast(req),
                result,
                enabled(delta),
                Set.of(profile.getTask().getResource().getKey()),
                true,
                profile.getExecutor(),
                profile.getContext());

        createRemediationIfNeeded(req, delta, result);

        return updated.getLeft();
    }

    @Override
    protected OpEvent.Outcome handleLinkedAccounts(
            final SyncDelta delta,
            final List<InboundMatch> matches,
            final Provision provision) throws JobExecutionException {

        OpEvent.Outcome global = OpEvent.Outcome.SUCCESS;
        for (InboundMatch match : matches) {
            if (match.getAny() == null) {
                LOG.error("Could not find linking user, cannot process match {}", match);
                return OpEvent.Outcome.FAILURE;
            }
            User user = (User) match.getAny();

            Optional<? extends LinkedAccount> found =
                    user.getLinkedAccount(profile.getTask().getResource().getKey(), delta.getUid().getUidValue());
            if (found.isPresent()) {
                LinkedAccount account = found.get();

                switch (delta.getDeltaType()) {
                    case CREATE:
                    case UPDATE:
                    case CREATE_OR_UPDATE:
                        switch (profile.getTask().getMatchingRule()) {
                            case UPDATE:
                                global = and(global, update(delta, account, provision));
                                break;

                            case DEPROVISION:
                            case UNASSIGN:
                                global = and(global, deprovision(profile.getTask().getMatchingRule(), delta, account));
                                break;

                            case LINK:
                            case UNLINK:
                                LOG.warn("{} not applicable to linked accounts, ignoring",
                                        profile.getTask().getMatchingRule());
                                break;

                            case IGNORE:
                                global = and(global, ignore(delta, account, true));
                                break;

                            default:
                            // do nothing
                        }
                        break;

                    case DELETE:
                        global = and(global, delete(delta, account, provision));
                        break;

                    default:
                }
            } else {
                switch (delta.getDeltaType()) {
                    case CREATE:
                    case UPDATE:
                    case CREATE_OR_UPDATE:
                        LinkedAccountTO accountTO = new LinkedAccountTO();
                        accountTO.setConnObjectKeyValue(delta.getUid().getUidValue());
                        accountTO.setResource(profile.getTask().getResource().getKey());

                        switch (profile.getTask().getUnmatchingRule()) {
                            case ASSIGN:
                            case PROVISION:
                                global = and(global, provision(
                                        profile.getTask().getUnmatchingRule(), delta, user, accountTO, provision));
                                break;

                            case IGNORE:
                                global = and(global, ignore(delta, null, false));
                                break;

                            default:
                            // do nothing
                        }
                        break;

                    case DELETE:
                        end(AnyTypeKind.USER.name(),
                                ResourceOperation.DELETE.name().toLowerCase(),
                                OpEvent.Outcome.SUCCESS,
                                null,
                                null,
                                delta);
                        LOG.debug("No match found for deletion");
                        break;

                    default:
                }
            }
        }

        return global;
    }

    protected OpEvent.Outcome deprovision(
            final MatchingRule matchingRule,
            final SyncDelta delta,
            final LinkedAccount account) throws JobExecutionException {

        if (!profile.getTask().isPerformUpdate()) {
            LOG.debug("PullTask not configured for update");
            end(AnyTypeKind.USER.name(),
                    MatchingRule.toOp(MatchingRule.UPDATE), OpEvent.Outcome.SUCCESS, null, null, delta);
            return OpEvent.Outcome.SUCCESS;
        }

        LOG.debug("About to deprovision {}", account);

        ProvisioningReport report = new ProvisioningReport();
        report.setOperation(ResourceOperation.DELETE);
        report.setAnyType(MatchType.LINKED_ACCOUNT.name());
        report.setStatus(ProvisioningReport.Status.SUCCESS);
        report.setKey(account.getKey());
        report.setUidValue(account.getConnObjectKeyValue());

        LinkedAccountTO before = userDataBinder.getLinkedAccountTO(account);

        OpEvent.Outcome resultStatus = OpEvent.Outcome.SUCCESS;
        if (!profile.isDryRun()) {
            Object output = before;

            try {
                if (matchingRule == MatchingRule.UNASSIGN) {
                    for (InboundActions action : profile.getActions()) {
                        action.beforeUnassign(profile, delta, before);
                    }
                } else if (matchingRule == MatchingRule.DEPROVISION) {
                    for (InboundActions action : profile.getActions()) {
                        action.beforeDeprovision(profile, delta, before);
                    }
                }

                PropagationByResource<Pair<String, String>> propByLinkedAccount = new PropagationByResource<>();
                propByLinkedAccount.add(
                        ResourceOperation.DELETE,
                        Pair.of(account.getResource().getKey(), account.getConnObjectKeyValue()));
                taskExecutor.execute(propagationManager.getDeleteTasks(
                        AnyTypeKind.USER,
                        account.getOwner().getKey(),
                        null,
                        propByLinkedAccount,
                        null),
                        false,
                        profile.getExecutor());

                for (InboundActions action : profile.getActions()) {
                    action.after(profile, delta, before, report);
                }

                resultStatus = OpEvent.Outcome.SUCCESS;

                LOG.debug("Linked account {} successfully updated", account.getConnObjectKeyValue());
            } catch (PropagationException e) {
                // A propagation failure doesn't imply a pull failure.
                // The propagation exception status will be reported into the propagation task execution.
                LOG.error("Could not propagate linked account {}", account.getConnObjectKeyValue());
                output = e;
                resultStatus = OpEvent.Outcome.FAILURE;
            } catch (Exception e) {
                throwIgnoreProvisionException(delta, e);

                report.setStatus(ProvisioningReport.Status.FAILURE);
                report.setMessage(ExceptionUtils.getRootCauseMessage(e));
                LOG.error("Could not update linked account {}", account, e);
                output = e;
                resultStatus = OpEvent.Outcome.FAILURE;
            }

            end(AnyTypeKind.USER.name(), MatchingRule.toOp(matchingRule), resultStatus, before, output, delta);
            profile.getResults().add(report);
        }

        return resultStatus;
    }

    protected OpEvent.Outcome provision(
            final UnmatchingRule rule,
            final SyncDelta delta,
            final User user,
            final LinkedAccountTO accountTO,
            final Provision provision)
            throws JobExecutionException {

        if (!profile.getTask().isPerformCreate()) {
            LOG.debug("PullTask not configured for create");
            end(AnyTypeKind.USER.name(), UnmatchingRule.toOp(rule), OpEvent.Outcome.SUCCESS, null, null, delta);
            return OpEvent.Outcome.SUCCESS;
        }

        LOG.debug("About to create {}", accountTO);

        ProvisioningReport report = new ProvisioningReport();
        report.setOperation(ResourceOperation.CREATE);
        report.setName(accountTO.getConnObjectKeyValue());
        report.setUidValue(accountTO.getConnObjectKeyValue());
        report.setAnyType(MatchType.LINKED_ACCOUNT.name());
        report.setStatus(ProvisioningReport.Status.SUCCESS);

        if (profile.isDryRun()) {
            report.setKey(null);
            end(AnyTypeKind.USER.name(), UnmatchingRule.toOp(rule), OpEvent.Outcome.SUCCESS, null, null, delta);
            return OpEvent.Outcome.SUCCESS;
        }

        UserTO owner = userDataBinder.getUserTO(user, false);
        UserCR connObject = connObjectUtils.getAnyCR(
                delta.getObject(), profile.getTask(), AnyTypeKind.USER, provision, false);

        if (connObject.getUsername().equals(owner.getUsername())) {
            accountTO.setUsername(null);
        } else if (!connObject.getUsername().equals(accountTO.getUsername())) {
            accountTO.setUsername(connObject.getUsername());
        }

        if (connObject.getPassword() != null) {
            accountTO.setPassword(connObject.getPassword());
        }

        accountTO.setSuspended(BooleanUtils.isTrue(BooleanUtils.negate(enabled(delta))));

        connObject.getPlainAttrs().forEach(connObjectAttr -> {
            Optional<Attr> ownerAttr = owner.getPlainAttr(connObjectAttr.getSchema());
            if (ownerAttr.isPresent() && ownerAttr.get().getValues().equals(connObjectAttr.getValues())) {
                accountTO.getPlainAttrs().removeIf(attr -> connObjectAttr.getSchema().equals(attr.getSchema()));
            } else {
                accountTO.getPlainAttrs().add(connObjectAttr);
            }
        });

        for (InboundActions action : profile.getActions()) {
            if (rule == UnmatchingRule.ASSIGN) {
                action.beforeAssign(profile, delta, accountTO);
            } else if (rule == UnmatchingRule.PROVISION) {
                action.beforeProvision(profile, delta, accountTO);
            }
        }
        report.setName(accountTO.getConnObjectKeyValue());

        UserUR req = new UserUR();
        req.setKey(user.getKey());
        req.getLinkedAccounts().add(new LinkedAccountUR.Builder().
                operation(PatchOperation.ADD_REPLACE).linkedAccountTO(accountTO).build());

        OpEvent.Outcome resultStatus;
        Object output;

        try {
            userProvisioningManager.update(
                    req,
                    report,
                    null,
                    Set.of(profile.getTask().getResource().getKey()),
                    true,
                    profile.getExecutor(),
                    profile.getContext());

            LinkedAccountTO created = userDAO.findById(req.getKey()).
                    orElseThrow(() -> new IllegalStateException("Could not find the User just updated")).
                    getLinkedAccount(accountTO.getResource(), accountTO.getConnObjectKeyValue()).
                    map(acct -> userDataBinder.getLinkedAccountTO(acct)).
                    orElse(null);

            output = created;
            resultStatus = OpEvent.Outcome.SUCCESS;

            for (InboundActions action : profile.getActions()) {
                action.after(profile, delta, created, report);
            }

            LOG.debug("Linked account {} successfully created", accountTO.getConnObjectKeyValue());
        } catch (PropagationException e) {
            // A propagation failure doesn't imply a pull failure.
            // The propagation exception status will be reported into the propagation task execution.
            LOG.error("Could not propagate linked account {}", accountTO.getConnObjectKeyValue());
            output = e;
            resultStatus = OpEvent.Outcome.FAILURE;
        } catch (Exception e) {
            throwIgnoreProvisionException(delta, e);

            report.setStatus(ProvisioningReport.Status.FAILURE);
            report.setMessage(ExceptionUtils.getRootCauseMessage(e));
            LOG.error("Could not create linked account {} ", accountTO.getConnObjectKeyValue(), e);
            output = e;
            resultStatus = OpEvent.Outcome.FAILURE;

            if (profile.getTask().isRemediation()) {
                createRemediation(provision.getAnyType(), null, null, req, report, delta);
            }
        }

        end(AnyTypeKind.USER.name(), UnmatchingRule.toOp(rule), resultStatus, null, output, delta);
        profile.getResults().add(report);

        return resultStatus;
    }

    protected OpEvent.Outcome update(
            final SyncDelta delta,
            final LinkedAccount account,
            final Provision provision)
            throws JobExecutionException {

        if (!profile.getTask().isPerformUpdate()) {
            LOG.debug("PullTask not configured for update");
            end(AnyTypeKind.USER.name(),
                    MatchingRule.toOp(MatchingRule.UPDATE), OpEvent.Outcome.SUCCESS, null, null, delta);
            return OpEvent.Outcome.SUCCESS;
        }

        LOG.debug("About to update {}", account);

        ProvisioningReport report = new ProvisioningReport();
        report.setOperation(ResourceOperation.UPDATE);
        report.setKey(account.getKey());
        report.setUidValue(account.getConnObjectKeyValue());
        report.setName(account.getConnObjectKeyValue());
        report.setAnyType(MatchType.LINKED_ACCOUNT.name());
        report.setStatus(ProvisioningReport.Status.SUCCESS);

        OpEvent.Outcome resultStatus = OpEvent.Outcome.SUCCESS;
        if (!profile.isDryRun()) {
            LinkedAccountTO before = userDataBinder.getLinkedAccountTO(account);

            UserTO owner = userDataBinder.getUserTO(account.getOwner(), false);
            UserCR connObject = connObjectUtils.getAnyCR(
                    delta.getObject(), profile.getTask(), AnyTypeKind.USER, provision, false);

            LinkedAccountTO update = userDataBinder.getLinkedAccountTO(account);

            if (connObject.getUsername().equals(owner.getUsername())) {
                update.setUsername(null);
            } else if (!connObject.getUsername().equals(update.getUsername())) {
                update.setUsername(connObject.getUsername());
            }

            if (connObject.getPassword() != null) {
                update.setPassword(connObject.getPassword());
            }

            update.setSuspended(BooleanUtils.isTrue(BooleanUtils.negate(enabled(delta))));

            Set<String> attrsToRemove = new HashSet<>();
            connObject.getPlainAttrs().forEach(connObjectAttr -> {
                Optional<Attr> ownerAttr = owner.getPlainAttr(connObjectAttr.getSchema());
                if (ownerAttr.isPresent() && ownerAttr.get().getValues().equals(connObjectAttr.getValues())) {
                    attrsToRemove.add(connObjectAttr.getSchema());
                } else {
                    Optional<Attr> updateAttr = update.getPlainAttr(connObjectAttr.getSchema());
                    if (updateAttr.isEmpty() || !updateAttr.get().getValues().equals(connObjectAttr.getValues())) {
                        attrsToRemove.add(connObjectAttr.getSchema());
                        update.getPlainAttrs().add(connObjectAttr);
                    }
                }
            });
            update.getPlainAttrs().removeIf(attr -> attrsToRemove.contains(attr.getSchema()));

            UserUR userUR = new UserUR();
            userUR.setKey(account.getOwner().getKey());
            userUR.getLinkedAccounts().add(new LinkedAccountUR.Builder().
                    operation(PatchOperation.ADD_REPLACE).linkedAccountTO(update).build());

            for (InboundActions action : profile.getActions()) {
                action.beforeUpdate(profile, delta, before, userUR);
            }

            Object output;
            try {
                userProvisioningManager.update(
                        userUR,
                        report,
                        null,
                        Set.of(profile.getTask().getResource().getKey()),
                        true,
                        profile.getExecutor(),
                        profile.getContext());
                resultStatus = OpEvent.Outcome.SUCCESS;

                LinkedAccountTO updated = userDAO.findById(userUR.getKey()).
                        orElseThrow(() -> new IllegalStateException("Could not find the User just updated")).
                        getLinkedAccount(account.getResource().getKey(), account.getConnObjectKeyValue()).
                        map(acct -> userDataBinder.getLinkedAccountTO(acct)).
                        orElse(null);
                output = updated;

                for (InboundActions action : profile.getActions()) {
                    action.after(profile, delta, updated, report);
                }

                LOG.debug("Linked account {} successfully updated", account.getConnObjectKeyValue());
            } catch (PropagationException e) {
                // A propagation failure doesn't imply a pull failure.
                // The propagation exception status will be reported into the propagation task execution.
                LOG.error("Could not propagate linked account {}", account.getConnObjectKeyValue());
                output = e;
                resultStatus = OpEvent.Outcome.FAILURE;
            } catch (Exception e) {
                throwIgnoreProvisionException(delta, e);

                report.setStatus(ProvisioningReport.Status.FAILURE);
                report.setMessage(ExceptionUtils.getRootCauseMessage(e));
                LOG.error("Could not update linked account {}", account, e);
                output = e;
                resultStatus = OpEvent.Outcome.FAILURE;

                if (profile.getTask().isRemediation()) {
                    createRemediation(provision.getAnyType(), null, null, userUR, report, delta);
                }
            }

            end(AnyTypeKind.USER.name(),
                    MatchingRule.toOp(MatchingRule.UPDATE),
                    resultStatus, before, output, delta);
            profile.getResults().add(report);
        }

        return resultStatus;
    }

    protected OpEvent.Outcome delete(
            final SyncDelta delta,
            final LinkedAccount account,
            final Provision provision) {

        if (!profile.getTask().isPerformDelete()) {
            LOG.debug("PullTask not configured for delete");
            end(AnyTypeKind.USER.name(),
                    ResourceOperation.DELETE.name().toLowerCase(), OpEvent.Outcome.SUCCESS, null, null, delta);
            return OpEvent.Outcome.SUCCESS;
        }

        LOG.debug("About to delete {}", account);

        Object output;
        OpEvent.Outcome resultStatus = OpEvent.Outcome.FAILURE;

        ProvisioningReport report = new ProvisioningReport();

        try {
            report.setKey(account.getKey());
            report.setName(account.getConnObjectKeyValue());
            report.setUidValue(account.getConnObjectKeyValue());
            report.setOperation(ResourceOperation.DELETE);
            report.setAnyType(MatchType.LINKED_ACCOUNT.name());
            report.setStatus(ProvisioningReport.Status.SUCCESS);

            if (!profile.isDryRun()) {
                LinkedAccountTO before = userDataBinder.getLinkedAccountTO(account);

                for (InboundActions action : profile.getActions()) {
                    action.beforeDelete(profile, delta, before);
                }

                UserUR req = new UserUR();
                req.setKey(account.getOwner().getKey());
                req.getLinkedAccounts().add(new LinkedAccountUR.Builder().
                        operation(PatchOperation.DELETE).linkedAccountTO(before).build());

                try {
                    userProvisioningManager.update(
                            req,
                            report,
                            null,
                            Set.of(profile.getTask().getResource().getKey()),
                            true,
                            profile.getExecutor(),
                            profile.getContext());
                    resultStatus = OpEvent.Outcome.SUCCESS;

                    output = null;

                    for (InboundActions action : profile.getActions()) {
                        action.after(profile, delta, before, report);
                    }
                } catch (Exception e) {
                    throwIgnoreProvisionException(delta, e);

                    report.setStatus(ProvisioningReport.Status.FAILURE);
                    report.setMessage(ExceptionUtils.getRootCauseMessage(e));
                    LOG.error("Could not delete linked account {}", account, e);
                    output = e;

                    if (profile.getTask().isRemediation()) {
                        createRemediation(provision.getAnyType(), null, null, req, report, delta);
                    }
                }

                end(AnyTypeKind.USER.name(),
                        ResourceOperation.DELETE.name().toLowerCase(),
                        resultStatus, before, output, delta);
                profile.getResults().add(report);
            }
        } catch (Exception e) {
            LOG.error("Could not delete linked account {}", account, e);
        }

        return resultStatus;
    }

    protected OpEvent.Outcome ignore(
            final SyncDelta delta,
            final LinkedAccount account,
            final boolean matching,
            final String... message) {

        LOG.debug("Linked account to ignore {}", delta.getObject().getUid().getUidValue());

        ProvisioningReport report = new ProvisioningReport();
        report.setName(delta.getUid().getUidValue());
        report.setUidValue(delta.getUid().getUidValue());
        report.setOperation(ResourceOperation.NONE);
        report.setAnyType(MatchType.LINKED_ACCOUNT.name());
        report.setStatus(ProvisioningReport.Status.SUCCESS);
        if (message != null && message.length >= 1) {
            report.setMessage(message[0]);
        }
        if (account != null) {
            report.setKey(account.getKey());
        }

        end(AnyTypeKind.USER.name(),
                matching
                        ? MatchingRule.toOp(MatchingRule.IGNORE)
                        : UnmatchingRule.toOp(UnmatchingRule.IGNORE),
                OpEvent.Outcome.SUCCESS, null, null, delta);

        profile.getResults().add(report);
        return OpEvent.Outcome.SUCCESS;
    }
}
