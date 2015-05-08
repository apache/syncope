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
package org.apache.syncope.core.misc.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Resource;
import org.apache.commons.collections4.Closure;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.PredicateUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.syncope.common.lib.CollectionUtils2;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.Entitlement;
import org.apache.syncope.core.misc.RealmUtils;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Configurable
public class SyncopeUserDetailsService implements UserDetailsService {

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected GroupDAO groupDAO;

    @Resource(name = "adminUser")
    protected String adminUser;

    @Resource(name = "anonymousUser")
    protected String anonymousUser;

    @Override
    public UserDetails loadUserByUsername(final String username) {
        final Set<SyncopeGrantedAuthority> authorities = new HashSet<>();
        if (anonymousUser.equals(username)) {
            authorities.add(new SyncopeGrantedAuthority(Entitlement.ANONYMOUS));
        } else if (adminUser.equals(username)) {
            CollectionUtils2.collect(Entitlement.values(),
                    new Transformer<String, SyncopeGrantedAuthority>() {

                        @Override
                        public SyncopeGrantedAuthority transform(final String entitlement) {
                            return new SyncopeGrantedAuthority(entitlement, SyncopeConstants.ROOT_REALM);
                        }
                    },
                    PredicateUtils.notPredicate(PredicateUtils.equalPredicate(Entitlement.ANONYMOUS)),
                    authorities);
        } else {
            org.apache.syncope.core.persistence.api.entity.user.User user = userDAO.find(username);
            if (user == null) {
                throw new UsernameNotFoundException("Could not find any user with id " + username);
            }

            // Give entitlements as assigned by roles (with realms, where applicable) - assigned either
            // statically and dynamically
            for (final Role role : userDAO.findAllRoles(user)) {
                CollectionUtils.forAllDo(role.getEntitlements(), new Closure<String>() {

                    @Override
                    public void execute(final String entitlement) {
                        SyncopeGrantedAuthority authority = new SyncopeGrantedAuthority(entitlement);
                        authorities.add(authority);

                        List<String> realmFullPahs = new ArrayList<>();
                        CollectionUtils.collect(role.getRealms(), new Transformer<Realm, String>() {

                            @Override
                            public String transform(final Realm realm) {
                                return realm.getFullPath();
                            }
                        }, realmFullPahs);
                        authority.addRealms(realmFullPahs);
                    }
                });
            }

            // Give group entitlements for owned groups
            for (Group group : groupDAO.findOwnedByUser(user.getKey())) {
                for (String entitlement : Arrays.asList(
                        Entitlement.GROUP_READ, Entitlement.GROUP_UPDATE, Entitlement.GROUP_DELETE)) {

                    SyncopeGrantedAuthority authority = new SyncopeGrantedAuthority(entitlement);
                    authority.addRealm(RealmUtils.getGroupOwnerRealm(group.getRealm().getFullPath(), group.getKey()));
                    authorities.add(authority);
                }
            }
        }

        return new User(username, "<PASSWORD_PLACEHOLDER>", authorities);
    }
}
