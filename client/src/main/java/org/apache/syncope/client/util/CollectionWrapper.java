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
package org.apache.syncope.client.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.syncope.client.to.EntitlementTO;
import org.apache.syncope.client.to.MailTemplateTO;
import org.apache.syncope.client.to.ValidatorTO;
import org.springframework.web.servlet.ModelAndView;

public final class CollectionWrapper {

    private CollectionWrapper() {
    }

    public static Set<EntitlementTO> wrap(final Set<String> collection) {
        Set<EntitlementTO> respons = new HashSet<EntitlementTO>();
        for (String e : collection) {
            respons.add(EntitlementTO.instance(e));
        }
        return respons;
    }

    public static List<EntitlementTO> wrap(final List<String> collection) {
        List<EntitlementTO> respons = new ArrayList<EntitlementTO>();
        for (String e : collection) {
            respons.add(EntitlementTO.instance(e));
        }
        return respons;
    }

    public static Set<String> unwrap(final Set<EntitlementTO> collection) {
        Set<String> respons = new HashSet<String>();
        for (EntitlementTO e : collection) {
            respons.add(e.getName());
        }
        return respons;
    }

    public static List<String> unwrap(final List<EntitlementTO> collection) {
        List<String> respons = new ArrayList<String>();
        for (EntitlementTO e : collection) {
            respons.add(e.getName());
        }
        return respons;
    }

    public static Set<MailTemplateTO> wrapMailTemplates(final ModelAndView mailTemplates) {
        @SuppressWarnings("unchecked")
        Set<String> collection = (Set<String>) mailTemplates.getModel().values().iterator().next();
        return wrapMailTemplates(collection);
    }

    public static Set<MailTemplateTO> wrapMailTemplates(final Set<String> collection) {
        Set<MailTemplateTO> respons = new HashSet<MailTemplateTO>();
        for (String e : collection) {
            respons.add(MailTemplateTO.instance(e));
        }
        return respons;
    }

    public static Set<ValidatorTO> wrapValidator(final ModelAndView validators) {
        @SuppressWarnings("unchecked")
        Set<String> collection = (Set<String>) validators.getModel().values().iterator().next();
        return wrapValidator(collection);
    }

    public static List<String> unwrapValidator(final List<ValidatorTO> collection) {
        List<String> respons = new ArrayList<String>();
        for (ValidatorTO e : collection) {
            respons.add(e.getName());
        }
        return respons;
    }

    public static List<String> unwrapMailTemplates(final List<MailTemplateTO> collection) {
        List<String> respons = new ArrayList<String>();
        for (MailTemplateTO e : collection) {
            respons.add(e.getName());
        }
        return respons;
    }

    public static Set<ValidatorTO> wrapValidator(final Set<String> collection) {
        Set<ValidatorTO> respons = new HashSet<ValidatorTO>();
        for (String e : collection) {
            respons.add(ValidatorTO.instance(e));
        }
        return respons;
    }
}
