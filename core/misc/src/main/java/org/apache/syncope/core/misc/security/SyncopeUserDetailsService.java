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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Resource;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.api.GroupEntitlementUtil;
import org.apache.syncope.core.persistence.api.dao.EntitlementDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.Entitlement;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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

    @Autowired
    protected EntitlementDAO entitlementDAO;

    @Resource(name = "adminUser")
    protected String adminUser;

    @Resource(name = "anonymousUser")
    protected String anonymousUser;

    @Override
    public UserDetails loadUserByUsername(final String username) {
        final Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        if (anonymousUser.equals(username)) {
            authorities.add(new SimpleGrantedAuthority(SyncopeConstants.ANONYMOUS_ENTITLEMENT));
        } else if (adminUser.equals(username)) {
            for (Entitlement entitlement : entitlementDAO.findAll()) {
                authorities.add(new SimpleGrantedAuthority(entitlement.getKey()));
            }
        } else {
            org.apache.syncope.core.persistence.api.entity.user.User user = userDAO.find(username);

            if (user == null) {
                throw new UsernameNotFoundException("Could not find any user with id " + username);
            }

            // Give entitlements based on groups assigned to user (and their ancestors)
            final Set<Group> groups = new HashSet<>(user.getGroups());
            for (Group group : user.getGroups()) {
                groups.addAll(groupDAO.findAncestors(group));
            }
            for (Group group : groups) {
                for (Entitlement entitlement : group.getEntitlements()) {
                    authorities.add(new SimpleGrantedAuthority(entitlement.getKey()));
                }
            }
            // Give group operational entitlements for owned groups
            List<Group> ownedGroups = groupDAO.findOwnedByUser(user.getKey());
            if (!ownedGroups.isEmpty()) {
                authorities.add(new SimpleGrantedAuthority("GROUP_CREATE"));
                authorities.add(new SimpleGrantedAuthority("GROUP_READ"));
                authorities.add(new SimpleGrantedAuthority("GROUP_UPDATE"));
                authorities.add(new SimpleGrantedAuthority("GROUP_DELETE"));

                for (Group group : ownedGroups) {
                    authorities.add(new SimpleGrantedAuthority(
                            GroupEntitlementUtil.getEntitlementNameFromGroupKey(group.getKey())));
                }
            }
        }

        return new User(username, "<PASSWORD_PLACEHOLDER>", true, true, true, true, authorities);
    }
}
