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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.syncope.common.to.CorrelationRuleClassTO;
import org.apache.syncope.common.to.EntitlementTO;
import org.apache.syncope.common.to.JobClassTO;
import org.apache.syncope.common.to.LoggerTO;
import org.apache.syncope.common.to.MailTemplateTO;
import org.apache.syncope.common.to.PropagationActionClassTO;
import org.apache.syncope.common.to.SyncActionClassTO;
import org.apache.syncope.common.to.ValidatorTO;
import org.apache.syncope.common.types.AuditLoggerName;
import org.apache.syncope.common.types.SyncopeLoggerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;

public final class CollectionWrapper {

    /**
     * Logger.
     */
    private final static Logger LOG = LoggerFactory.getLogger(CollectionWrapper.class);

    private CollectionWrapper() {
        // empty constructor for static utility class
    }

    @SuppressWarnings("unchecked")
    public static List<String> wrapStrings(final ModelAndView modelAndView) {
        return (List<String>) modelAndView.getModel().values().iterator().next();
    }

    public static Set<EntitlementTO> wrap(final Collection<String> collection) {
        Set<EntitlementTO> respons = new HashSet<EntitlementTO>();
        for (String e : collection) {
            respons.add(EntitlementTO.instance(e));
        }
        return respons;
    }

    public static Set<String> unwrap(final Collection<EntitlementTO> collection) {
        Set<String> respons = new HashSet<String>();
        for (EntitlementTO e : collection) {
            respons.add(e.getName());
        }
        return respons;
    }

    public static Set<MailTemplateTO> wrapMailTemplates(final Collection<String> collection) {
        Set<MailTemplateTO> respons = new HashSet<MailTemplateTO>();
        for (String e : collection) {
            respons.add(MailTemplateTO.instance(e));
        }
        return respons;
    }

    public static List<String> unwrapMailTemplates(final Collection<MailTemplateTO> collection) {
        List<String> respons = new ArrayList<String>();
        for (MailTemplateTO e : collection) {
            respons.add(e.getName());
        }
        return respons;
    }

    public static Set<ValidatorTO> wrapValidators(final Collection<String> validators) {
        Set<ValidatorTO> respons = new HashSet<ValidatorTO>();
        for (String validator : validators) {
            respons.add(ValidatorTO.instance(validator));
        }
        return respons;
    }

    public static List<String> unwrapValidator(final Collection<ValidatorTO> collection) {
        List<String> respons = new ArrayList<String>();
        for (ValidatorTO e : collection) {
            respons.add(e.getName());
        }
        return respons;
    }

    public static List<AuditLoggerName> wrapLogger(final Collection<LoggerTO> logger) {
        List<AuditLoggerName> respons = new ArrayList<AuditLoggerName>();
        for (LoggerTO l : logger) {
            try {
                respons.add(AuditLoggerName.fromLoggerName(l.getName()));
            } catch (Exception e) {
                LOG.error("Error wrapping logger", e);
            }
        }
        return respons;
    }

    public static List<LoggerTO> unwrapLogger(final Collection<AuditLoggerName> auditNames) {
        List<LoggerTO> respons = new ArrayList<LoggerTO>();
        for (AuditLoggerName l : auditNames) {
            LoggerTO loggerTO = new LoggerTO();
            loggerTO.setName(l.toLoggerName());
            loggerTO.setLevel(SyncopeLoggerLevel.DEBUG);
            respons.add(loggerTO);
        }
        return respons;
    }

    public static Set<JobClassTO> wrapJobClasses(final Collection<String> classes) {
        Set<JobClassTO> respons = new HashSet<JobClassTO>();
        for (String cl : classes) {
            respons.add(JobClassTO.instance(cl));
        }
        return respons;
    }

    public static List<String> unwrapJobClasses(final Collection<JobClassTO> jobClasses) {
        List<String> respons = new ArrayList<String>();
        for (JobClassTO e : jobClasses) {
            respons.add(e.getName());
        }
        return respons;
    }

    public static Set<SyncActionClassTO> wrapSyncActionClasses(final Collection<String> classes) {
        Set<SyncActionClassTO> respons = new HashSet<SyncActionClassTO>();
        for (String cl : classes) {
            respons.add(SyncActionClassTO.instance(cl));
        }
        return respons;
    }

    public static List<String> unwrapSyncActionClasses(final Collection<SyncActionClassTO> actions) {
        List<String> respons = new ArrayList<String>();
        for (SyncActionClassTO e : actions) {
            respons.add(e.getName());
        }
        return respons;
    }

    public static Set<PropagationActionClassTO> wrapPropagationActionClasses(final Collection<String> classes) {
        Set<PropagationActionClassTO> respons = new HashSet<PropagationActionClassTO>();
        for (String cl : classes) {
            respons.add(PropagationActionClassTO.instance(cl));
        }
        return respons;
    }

    public static List<String> unwrapPropagationActionClasses(final Collection<PropagationActionClassTO> actions) {
        List<String> respons = new ArrayList<String>();
        for (PropagationActionClassTO e : actions) {
            respons.add(e.getName());
        }
        return respons;
    }

    public static Set<CorrelationRuleClassTO> wrapSyncCorrelationRuleClasses(final Collection<String> classes) {
        Set<CorrelationRuleClassTO> respons = new HashSet<CorrelationRuleClassTO>();
        for (String cl : classes) {
            respons.add(CorrelationRuleClassTO.instance(cl));
        }
        return respons;
    }

    public static List<String> unwrapSyncCorrelationRuleClasses(final Collection<CorrelationRuleClassTO> actions) {
        List<String> respons = new ArrayList<String>();
        for (CorrelationRuleClassTO e : actions) {
            respons.add(e.getName());
        }
        return respons;
    }
}
