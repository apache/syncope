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
package org.apache.syncope.common.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.syncope.common.to.EntitlementTO;
import org.apache.syncope.common.to.JobClassTO;
import org.apache.syncope.common.to.LoggerTO;
import org.apache.syncope.common.to.MailTemplateTO;
import org.apache.syncope.common.to.PropagationActionClassTO;
import org.apache.syncope.common.to.SyncActionClassTO;
import org.apache.syncope.common.to.ValidatorTO;
import org.apache.syncope.common.types.AuditLoggerName;
import org.apache.syncope.common.types.SyncopeLoggerLevel;
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

    public static List<LoggerTO> unwrapLogger(List<AuditLoggerName> auditNames) {
        List<LoggerTO> respons = new ArrayList<LoggerTO>();
        for (AuditLoggerName l : auditNames) {
            LoggerTO loggerTO = new LoggerTO();
            loggerTO.setName(l.toLoggerName());
            loggerTO.setLevel(SyncopeLoggerLevel.DEBUG);
            respons.add(loggerTO);
        }
        return respons;
    }

    public static Set<JobClassTO> wrapJobClasses(Set<String> classes) {
        Set<JobClassTO> respons = new HashSet<JobClassTO>();
        for (String cl : classes) {
            respons.add(JobClassTO.instance(cl));
        }
        return respons;
    }

    public static Set<SyncActionClassTO> wrapSyncActionClasses(Set<String> classes) {
        Set<SyncActionClassTO> respons = new HashSet<SyncActionClassTO>();
        for (String cl : classes) {
            respons.add(SyncActionClassTO.instance(cl));
        }
        return respons;
    }

    public static List<AuditLoggerName> wrapLogger(List<LoggerTO> logger) {
        List<AuditLoggerName> respons = new ArrayList<AuditLoggerName>();
        for (LoggerTO l : logger) {
            try {
                respons.add(AuditLoggerName.fromLoggerName(l.getName()));
            } catch (Exception e) {
                //TODO log event
            }
        }
        return respons;
    }

    public static List<String> unwrapJobClasses(List<JobClassTO> jobClasses) {
        List<String> respons = new ArrayList<String>();
        for (JobClassTO e : jobClasses) {
            respons.add(e.getName());
        }
        return respons;
    }

    public static List<String> unwrapSyncActionClasses(List<SyncActionClassTO> actions) {
        List<String> respons = new ArrayList<String>();
        for (SyncActionClassTO e : actions) {
            respons.add(e.getName());
        }
        return respons;
    }

    public static Set<PropagationActionClassTO> wrapPropagationActionClasses(Set<String> classes) {
        Set<PropagationActionClassTO> respons = new HashSet<PropagationActionClassTO>();
        for (String cl : classes) {
            respons.add(PropagationActionClassTO.instance(cl));
        }
        return respons;
    }

    public static List<String> unwrapPropagationActionClasses(Set<PropagationActionClassTO> actions) {
        List<String> respons = new ArrayList<String>();
        for (PropagationActionClassTO e : actions) {
            respons.add(e.getName());
        }
        return respons;
    }
}
