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
package org.syncope.core.rest.controller;

import java.util.HashSet;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/auth")
public class AuthenticationController extends AbstractController {

    @RequestMapping(method = RequestMethod.GET,
    value = "/entitlements")
    public Set<String> getEntitlements() {
        Set<String> result = new HashSet<String>(
                SecurityContextHolder.getContext().
                getAuthentication().getAuthorities().size());
        for (GrantedAuthority authority :
                SecurityContextHolder.getContext().
                getAuthentication().getAuthorities()) {

            result.add(authority.getAuthority());
        }

        return result;
    }
}
