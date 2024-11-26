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
package org.apache.syncope.common.lib;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.request.AnyCR;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.LinkedAccountTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.RelationshipTO;
import org.apache.syncope.common.lib.to.UserTO;

public final class EntityTOUtils {

    public static Map<String, Attr> buildAttrMap(final Collection<Attr> attrs) {
        return attrs.stream().collect(Collectors.toUnmodifiableMap(
                Attr::getSchema, Function.identity(), (exist, repl) -> repl));
    }

    public static Map<Pair<String, String>, RelationshipTO> buildRelationshipMap(
            final Collection<RelationshipTO> relationships) {

        return relationships.stream().collect(Collectors.toUnmodifiableMap(
                rel -> Pair.of(rel.getType(), rel.getOtherEndKey()), Function.identity(), (exist, repl) -> repl));
    }

    public static Map<String, MembershipTO> buildMembershipMap(final Collection<MembershipTO> memberships) {
        return memberships.stream().collect(Collectors.toUnmodifiableMap(
                MembershipTO::getGroupKey, Function.identity(), (exist, repl) -> repl));
    }

    public static Map<Pair<String, String>, LinkedAccountTO> buildLinkedAccountMap(
            final Collection<LinkedAccountTO> accounts) {

        return accounts.stream().collect(Collectors.toUnmodifiableMap(
                account -> Pair.of(account.getResource(), account.getConnObjectKeyValue()),
                Function.identity(),
                (exist, repl) -> repl));
    }

    public static <A extends AnyTO, C extends AnyCR> void toAnyCR(final A anyTO, final C anyCR) {
        anyCR.setRealm(anyTO.getRealm());
        anyCR.getAuxClasses().addAll(anyTO.getAuxClasses());
        anyCR.getPlainAttrs().addAll(anyTO.getPlainAttrs());
        anyCR.getVirAttrs().addAll(anyTO.getVirAttrs());
        anyCR.getResources().addAll(anyTO.getResources());

        if (anyCR instanceof final UserCR userCR && anyTO instanceof final UserTO userTO) {

            userCR.setUsername(userTO.getUsername());
            userCR.setPassword(userTO.getPassword());
            userCR.setSecurityQuestion(userTO.getSecurityQuestion());
            userCR.setSecurityAnswer(userTO.getSecurityAnswer());
            userCR.setMustChangePassword(userTO.isMustChangePassword());
            userCR.getRelationships().addAll(userTO.getRelationships());
            userCR.getMemberships().addAll(userTO.getMemberships());
            userCR.getRoles().addAll(userTO.getRoles());
        } else if (anyCR instanceof final GroupCR groupCR && anyTO instanceof final GroupTO groupTO) {

            groupCR.setName(groupTO.getName());
            groupCR.setUserOwner(groupTO.getUserOwner());
            groupCR.setGroupOwner(groupTO.getGroupOwner());
            groupCR.setUDynMembershipCond(groupTO.getUDynMembershipCond());
            groupCR.getADynMembershipConds().putAll(groupTO.getADynMembershipConds());
            groupCR.getTypeExtensions().addAll(groupTO.getTypeExtensions());
        } else if (anyCR instanceof final AnyObjectCR anyObjectCR && anyTO instanceof final AnyObjectTO anyObjectTO) {

            anyObjectCR.setType(anyObjectTO.getType());
            anyObjectCR.setName(anyObjectTO.getName());
            anyObjectCR.getRelationships().addAll(anyObjectTO.getRelationships());
            anyObjectCR.getMemberships().addAll(anyObjectTO.getMemberships());
        }
    }

    public static <C extends AnyCR, A extends AnyTO> void toAnyTO(final C anyCR, final A anyTO) {
        anyTO.setRealm(anyCR.getRealm());
        anyTO.getAuxClasses().addAll(anyCR.getAuxClasses());
        anyTO.getPlainAttrs().addAll(anyCR.getPlainAttrs());
        anyTO.getVirAttrs().addAll(anyCR.getVirAttrs());
        anyTO.getResources().addAll(anyCR.getResources());

        if (anyTO instanceof final UserTO userTO && anyCR instanceof final UserCR userCR) {

            userTO.setUsername(userCR.getUsername());
            userTO.setPassword(userCR.getPassword());
            userTO.setSecurityQuestion(userCR.getSecurityQuestion());
            userTO.setSecurityAnswer(userCR.getSecurityAnswer());
            userTO.setMustChangePassword(userCR.isMustChangePassword());
            userTO.getRelationships().addAll(userCR.getRelationships());
            userTO.getMemberships().addAll(userCR.getMemberships());
            userTO.getRoles().addAll(userCR.getRoles());
        } else if (anyTO instanceof final GroupTO groupTO && anyCR instanceof final GroupCR groupCR) {

            groupTO.setName(groupCR.getName());
            groupTO.setUserOwner(groupCR.getUserOwner());
            groupTO.setGroupOwner(groupCR.getGroupOwner());
            groupTO.setUDynMembershipCond(groupCR.getUDynMembershipCond());
            groupTO.getADynMembershipConds().putAll(groupCR.getADynMembershipConds());
            groupTO.getTypeExtensions().addAll(groupCR.getTypeExtensions());
        } else if (anyTO instanceof final AnyObjectTO anyObjectTO && anyCR instanceof final AnyObjectCR anyObjectCR) {

            anyObjectTO.setType(anyObjectCR.getType());
            anyObjectTO.setName(anyObjectCR.getName());
            anyObjectTO.getRelationships().addAll(anyObjectCR.getRelationships());
            anyObjectTO.getMemberships().addAll(anyObjectCR.getMemberships());
        }
    }

    /**
     * Private default constructor, for static-only classes.
     */
    private EntityTOUtils() {
    }
}
