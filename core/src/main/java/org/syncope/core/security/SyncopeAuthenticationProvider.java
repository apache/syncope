/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.SyncopeUserDAO;

@Configurable
public class SyncopeAuthenticationProvider implements AuthenticationProvider {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            SyncopeAuthenticationProvider.class);

    @Autowired
    private SyncopeUserDAO syncopeUserDAO;

    private SyncopeUserDetailsService syncopeUserDetailsService;

    private String adminUser;

    private String adminMD5Password;

    public String getAdminMD5Password() {
        return adminMD5Password;
    }

    public void setAdminMD5Password(String adminMD5Password) {
        this.adminMD5Password = adminMD5Password;
    }

    public String getAdminUser() {
        return adminUser;
    }

    public void setAdminUser(String adminUser) {
        this.adminUser = adminUser;
    }

    public SyncopeUserDetailsService getSyncopeUserDetailsService() {
        return syncopeUserDetailsService;
    }

    public void setSyncopeUserDetailsService(
            SyncopeUserDetailsService syncopeUserDetailsService) {

        this.syncopeUserDetailsService = syncopeUserDetailsService;
    }

    @Override
    public Authentication authenticate(final Authentication authentication)
            throws AuthenticationException {

        boolean authenticated;
        SyncopeUser passwordUser = new SyncopeUser();
        passwordUser.setPassword(
                authentication.getCredentials().toString());
        if (adminUser.equals(authentication.getPrincipal())) {
            authenticated = adminMD5Password.equalsIgnoreCase(
                    passwordUser.getPassword());
        } else {
            Long id;
            try {
                id = Long.valueOf(authentication.getPrincipal().toString());
            } catch (NumberFormatException e) {
                throw new UsernameNotFoundException(
                        "Invalid user id: " + authentication.getName(), e);
            }

            SyncopeUser user = syncopeUserDAO.find(id);
            if (user == null) {
                throw new UsernameNotFoundException(
                        "Could not find any user with id " + id);
            }

            authenticated = user.getPassword().equalsIgnoreCase(
                    passwordUser.getPassword());
        }

        Authentication result;
        if (authenticated) {
            UsernamePasswordAuthenticationToken token =
                    new UsernamePasswordAuthenticationToken(
                    authentication.getPrincipal(),
                    null,
                    syncopeUserDetailsService.loadUserByUsername(
                    authentication.getPrincipal().toString()).getAuthorities());
            token.setDetails(authentication.getDetails());

            result = token;

            LOG.debug("User {} authenticated with roles {}",
                    authentication.getPrincipal(), token.getAuthorities());
        } else {
            result = authentication;

            LOG.debug("User {} not authenticated",
                    authentication.getPrincipal());
        }

        return result;
    }

    @Override
    public boolean supports(final Class<? extends Object> type) {
        return type.equals(UsernamePasswordAuthenticationToken.class);
    }
}
