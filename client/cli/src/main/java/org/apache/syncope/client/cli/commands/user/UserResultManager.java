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
package org.apache.syncope.client.cli.commands.user;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.syncope.client.cli.commands.CommonsResultManager;
import org.apache.syncope.client.cli.view.Table;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.to.RelationshipTO;
import org.apache.syncope.common.lib.to.UserTO;

public class UserResultManager extends CommonsResultManager {

    public void printUsers(final List<UserTO> userTOs) {
        System.out.println("");
        userTOs.forEach(userTO -> {
            printUser(userTO);
        });
    }

    private void printUser(final UserTO userTO) {
        System.out.println(" > USER KEY: " + userTO.getKey());
        System.out.println("    username: " + userTO.getUsername());
        System.out.println("    realm: " + userTO.getRealm());
        System.out.println("    status: " + userTO.getStatus());
        System.out.println("    RESOURCES: ");
        printResource(userTO.getResources());
        System.out.println("    ROLES: ");
        printRole(userTO.getRoles());
        System.out.println("    creation date: " + userTO.getCreationDate());
        System.out.println("    change password date: " + userTO.getChangePwdDate());
        System.out.println("    PLAIN ATTRIBUTES: ");
        printAttributes(userTO.getPlainAttrs());
        System.out.println("    DERIVED ATTRIBUTES: ");
        printAttributes(userTO.getDerAttrs());
        System.out.println("    VIRTUAL ATTRIBUTES: ");
        printAttributes(userTO.getVirAttrs());
        System.out.println("    creator: " + userTO.getCreator());
        System.out.println("    last modifier: " + userTO.getLastModifier());
        System.out.println("    token: " + userTO.getToken());
        System.out.println("    token expiration time: " + userTO.getTokenExpireTime());
        System.out.println("    last change: " + userTO.getLastChangeDate());
        System.out.println("    last login: " + userTO.getLastLoginDate());
        System.out.println("    failed logins: " + userTO.getFailedLogins());
        System.out.println("RELATIONSHIPS:");
        printRelationships(userTO.getRelationships());
        System.out.println("    security question key: " + userTO.getSecurityQuestion());
        System.out.println("    security question answer key: " + userTO.getSecurityAnswer());
        System.out.println("");
    }

    private void printResource(final Set<String> resources) {
        resources.forEach(resource -> {
            System.out.println("       - " + resource);
        });
    }

    private void printRole(final List<String> roles) {
        roles.forEach((role) -> {
            System.out.println("       - " + role);
        });
    }

    private void printAttributes(final Set<Attr> derAttrs) {
        derAttrs.forEach(attrTO -> {
            final StringBuilder attributeSentence = new StringBuilder();
            attributeSentence.append("       ")
                    .append(attrTO.getSchema())
                    .append(": ")
                    .append(attrTO.getValues());
            System.out.println(attributeSentence);
        });
    }

    private void printRelationships(final List<RelationshipTO> relationshipTOs) {
        relationshipTOs.forEach(relationshipTO -> {
            System.out.println("       type: " + relationshipTO.getType());
        });
    }

    public void printFailedUsers(final Map<String, String> users) {
        Table.TableBuilder tableBuilder =
                new Table.TableBuilder("Users not deleted").header("user key").header("cause");
        users.forEach((key, value) -> {
            tableBuilder.rowValues(Arrays.asList(key, value));
        });
        tableBuilder.build().print();
    }

    public void printDetails(final Map<String, String> details) {
        printDetails("users details", details);
    }
}
