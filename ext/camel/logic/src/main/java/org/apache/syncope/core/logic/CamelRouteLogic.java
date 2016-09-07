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
package org.apache.syncope.core.logic;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.camel.component.metrics.routepolicy.MetricsRegistryService;
import java.util.Map;
import org.apache.commons.collections4.ComparatorUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.to.CamelMetrics;
import org.apache.syncope.common.lib.to.CamelRouteTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.CamelEntitlement;
import org.apache.syncope.core.persistence.api.dao.CamelRouteDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.CamelRoute;
import org.apache.syncope.core.provisioning.api.data.CamelRouteDataBinder;
import org.apache.syncope.core.provisioning.camel.CamelException;
import org.apache.syncope.core.provisioning.camel.SyncopeCamelContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CamelRouteLogic extends AbstractTransactionalLogic<CamelRouteTO> {

    @Autowired
    private CamelRouteDAO routeDAO;

    @Autowired
    private CamelRouteDataBinder binder;

    @Autowired
    private SyncopeCamelContext context;

    @PreAuthorize("hasRole('" + CamelEntitlement.ROUTE_LIST + "')")
    @Transactional(readOnly = true)
    public List<CamelRouteTO> list(final AnyTypeKind anyTypeKind) {
        List<CamelRouteTO> routes = new ArrayList<>();

        for (CamelRoute route : routeDAO.find(anyTypeKind)) {
            routes.add(binder.getRouteTO(route));
        }
        return routes;
    }

    @PreAuthorize("hasRole('" + CamelEntitlement.ROUTE_READ + "')")
    @Transactional(readOnly = true)
    public CamelRouteTO read(final String key) {
        CamelRoute route = routeDAO.find(key);
        if (route == null) {
            throw new NotFoundException("CamelRoute with key=" + key);
        }

        return binder.getRouteTO(route);
    }

    @PreAuthorize("hasRole('" + CamelEntitlement.ROUTE_UPDATE + "')")
    public void update(final CamelRouteTO routeTO) {
        CamelRoute route = routeDAO.find(routeTO.getKey());
        if (route == null) {
            throw new NotFoundException("CamelRoute with key=" + routeTO.getKey());
        }
        String originalContent = route.getContent();

        LOG.debug("Updating route {} with content {}", routeTO.getKey(), routeTO.getContent());
        binder.update(route, routeTO);

        try {
            context.updateContext(routeTO.getKey());
        } catch (CamelException e) {
            // if an exception was thrown while updating the context, restore the former route definition
            LOG.debug("Update of route {} failed, reverting", routeTO.getKey());
            context.restoreRoute(routeTO.getKey(), originalContent);

            throw e;
        }
    }

    @PreAuthorize("hasRole('" + CamelEntitlement.ROUTE_UPDATE + "')")
    public void restartContext() {
        context.restartContext();
    }

    @PreAuthorize("isAuthenticated()")
    public CamelMetrics metrics() {
        CamelMetrics metrics = new CamelMetrics();

        MetricsRegistryService registryService = context.getContext().hasService(MetricsRegistryService.class);
        if (registryService == null) {
            LOG.warn("Camel metrics not available");
        } else {
            MetricRegistry registry = registryService.getMetricsRegistry();
            for (Map.Entry<String, Timer> entry : registry.getTimers().entrySet()) {
                CamelMetrics.MeanRate meanRate = new CamelMetrics.MeanRate();
                meanRate.setRouteId(StringUtils.substringBetween(entry.getKey(), ".", "."));
                meanRate.setValue(entry.getValue().getMeanRate());
                metrics.getResponseMeanRates().add(meanRate);
            }

            Collections.sort(metrics.getResponseMeanRates(), new Comparator<CamelMetrics.MeanRate>() {

                @Override
                public int compare(final CamelMetrics.MeanRate o1, final CamelMetrics.MeanRate o2) {
                    return ComparatorUtils.reversedComparator(ComparatorUtils.<Double>naturalComparator()).
                            compare(o1.getValue(), o2.getValue());
                }
            });
        }

        return metrics;
    }

    @Override
    protected CamelRouteTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    key = (String) args[i];
                } else if (args[i] instanceof CamelRouteTO) {
                    key = ((CamelRouteTO) args[i]).getKey();
                }
            }
        }

        if (key != null) {
            try {
                return binder.getRouteTO(routeDAO.find(key));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }

}
