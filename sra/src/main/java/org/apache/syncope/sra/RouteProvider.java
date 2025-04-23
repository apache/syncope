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
package org.apache.syncope.sra;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.syncope.client.lib.AnonymousAuthenticationHandler;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.lib.to.SRARouteTO;
import org.apache.syncope.common.lib.types.SRARouteFilter;
import org.apache.syncope.common.lib.types.SRARoutePredicate;
import org.apache.syncope.common.rest.api.service.SRARouteService;
import org.apache.syncope.sra.filters.ClientCertsToRequestHeaderFilterFactory;
import org.apache.syncope.sra.filters.CustomGatewayFilterFactory;
import org.apache.syncope.sra.filters.LinkRewriteGatewayFilterFactory;
import org.apache.syncope.sra.filters.PrincipalToRequestHeaderFilterFactory;
import org.apache.syncope.sra.filters.QueryParamToRequestHeaderFilterFactory;
import org.apache.syncope.sra.predicates.CustomRoutePredicateFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AddRequestHeaderGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.AddRequestParameterGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.AddResponseHeaderGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.DedupeResponseHeaderGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.FallbackHeadersGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.MapRequestHeaderGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.PrefixPathGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.PreserveHostHeaderGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.RedirectToGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.RemoveRequestHeaderGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.RemoveResponseHeaderGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.RequestHeaderToRequestUriGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.RequestRateLimiterGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.RequestSizeGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.RewriteLocationResponseHeaderGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.RewritePathGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.RewriteResponseHeaderGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.SaveSessionGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.SecureHeadersGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.SetPathGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.SetRequestHeaderGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.SetRequestHostHeaderGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.SetResponseHeaderGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.SetStatusGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.StripPrefixGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.handler.AsyncPredicate;
import org.springframework.cloud.gateway.handler.predicate.AfterRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.BeforeRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.BetweenRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.CookieRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.HeaderRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.HostRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.MethodRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.PathRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.QueryRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.RemoteAddrRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.WeightRoutePredicateFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.unit.DataSize;
import org.springframework.web.server.ServerWebExchange;

public class RouteProvider {

    protected static final Logger LOG = LoggerFactory.getLogger(RouteProvider.class);

    protected final ServiceOps serviceOps;

    protected final ConfigurableApplicationContext ctx;

    protected final String anonymousUser;

    protected final String anonymousKey;

    protected final boolean useGZIPCompression;

    protected SyncopeClient client;

    protected final List<SRARouteTO> routeTOs = new ArrayList<>();

    public RouteProvider(
            final ServiceOps serviceOps,
            final ConfigurableApplicationContext ctx,
            final String anonymousUser,
            final String anonymousKey,
            final boolean useGZIPCompression) {

        this.serviceOps = serviceOps;
        this.ctx = ctx;
        this.anonymousUser = anonymousUser;
        this.anonymousKey = anonymousKey;
        this.useGZIPCompression = useGZIPCompression;
    }

    @SuppressWarnings("unchecked")
    protected GatewayFilter toFilter(final SRARouteTO route, final SRARouteFilter gwfilter)
            throws ClassNotFoundException, NumberFormatException {

        GatewayFilter filter;

        switch (gwfilter.getFactory()) {
            case ADD_REQUEST_HEADER:
                String[] addRequestHeaderArgs = gwfilter.getArgs().split(",");
                filter = ctx.getBean(AddRequestHeaderGatewayFilterFactory.class).
                        apply(c -> c.setName(addRequestHeaderArgs[0].trim()).
                        setValue(addRequestHeaderArgs[1].trim()));
                break;

            case ADD_REQUEST_PARAMETER:
                String[] addRequestParameterArgs = gwfilter.getArgs().split(",");
                filter = ctx.getBean(AddRequestParameterGatewayFilterFactory.class).
                        apply(c -> c.setName(addRequestParameterArgs[0].trim()).
                        setValue(addRequestParameterArgs[1].trim()));
                break;

            case ADD_RESPONSE_HEADER:
                String[] addResponseHeaderArgs = gwfilter.getArgs().split(",");
                filter = ctx.getBean(AddResponseHeaderGatewayFilterFactory.class).
                        apply(c -> c.setName(addResponseHeaderArgs[0].trim()).
                        setValue(addResponseHeaderArgs[1].trim()));
                break;

            case DEDUPE_RESPONSE_HEADER:
                String[] dedupeResponseHeaderArgs = gwfilter.getArgs().split(",");
                filter = ctx.getBean(DedupeResponseHeaderGatewayFilterFactory.class).
                        apply(c -> {
                            c.setName(dedupeResponseHeaderArgs[0].trim());
                            if (dedupeResponseHeaderArgs.length > 1) {
                                c.setStrategy(DedupeResponseHeaderGatewayFilterFactory.Strategy.
                                        valueOf(dedupeResponseHeaderArgs[1].trim()));
                            }
                        });
                break;

            case FALLBACK_HEADERS:
                String[] fallbackHeadersArgs = gwfilter.getArgs().split(",");
                filter = ctx.getBean(FallbackHeadersGatewayFilterFactory.class).
                        apply(c -> {
                            if (StringUtils.isNotBlank(fallbackHeadersArgs[0])) {
                                c.setExecutionExceptionTypeHeaderName(fallbackHeadersArgs[0].trim());
                            }
                            if (StringUtils.isNotBlank(fallbackHeadersArgs[1])) {
                                c.setExecutionExceptionMessageHeaderName(fallbackHeadersArgs[1].trim());
                            }
                            if (StringUtils.isNotBlank(fallbackHeadersArgs[2])) {
                                c.setRootCauseExceptionTypeHeaderName(fallbackHeadersArgs[2].trim());
                            }
                            if (StringUtils.isNotBlank(fallbackHeadersArgs[3])) {
                                c.setRootCauseExceptionMessageHeaderName(fallbackHeadersArgs[3].trim());
                            }
                        });
                break;

            case MAP_REQUEST_HEADER:
                String[] mapRequestHeaderArgs = gwfilter.getArgs().split(",");
                filter = ctx.getBean(MapRequestHeaderGatewayFilterFactory.class).
                        apply(c -> c.setFromHeader(mapRequestHeaderArgs[0].trim()).
                        setToHeader(mapRequestHeaderArgs[1].trim()));
                break;

            case PREFIX_PATH:
                filter = ctx.getBean(PrefixPathGatewayFilterFactory.class).
                        apply(c -> c.setPrefix(gwfilter.getArgs().trim()));
                break;

            case PRESERVE_HOST_HEADER:
                filter = ctx.getBean(PreserveHostHeaderGatewayFilterFactory.class).apply();
                break;

            case REDIRECT_TO:
                String[] redirectArgs = gwfilter.getArgs().split(",");
                filter = ctx.getBean(RedirectToGatewayFilterFactory.class).
                        apply(redirectArgs[0].trim(), redirectArgs[1].trim());
                break;

            case REMOVE_REQUEST_HEADER:
                filter = ctx.getBean(RemoveRequestHeaderGatewayFilterFactory.class).
                        apply(c -> c.setName(gwfilter.getArgs().trim()));
                break;

            case REMOVE_RESPONSE_HEADER:
                filter = ctx.getBean(RemoveResponseHeaderGatewayFilterFactory.class).
                        apply(c -> c.setName(gwfilter.getArgs().trim()));
                break;

            case REQUEST_RATE_LIMITER:
                String[] requestRateLimiterArgs = gwfilter.getArgs().split(",");
                filter = ctx.getBean(RequestRateLimiterGatewayFilterFactory.class).
                        apply(c -> {
                            if (StringUtils.isNotBlank(requestRateLimiterArgs[0])) {
                                c.setDenyEmptyKey(BooleanUtils.toBoolean(requestRateLimiterArgs[0].trim()));
                            }
                            if (StringUtils.isNotBlank(requestRateLimiterArgs[1])) {
                                c.setEmptyKeyStatus(requestRateLimiterArgs[1].trim());
                            }
                            if (StringUtils.isNotBlank(requestRateLimiterArgs[2])) {
                                c.setKeyResolver(ctx.getBean(requestRateLimiterArgs[2].trim(), KeyResolver.class));
                            }
                            if (StringUtils.isNotBlank(requestRateLimiterArgs[3])) {
                                c.setRateLimiter(ctx.getBean(requestRateLimiterArgs[3].trim(), RateLimiter.class));
                            }
                            if (StringUtils.isNotBlank(requestRateLimiterArgs[4])) {
                                c.setStatusCode(HttpStatus.valueOf(requestRateLimiterArgs[4].trim()));
                            }
                        });
                break;

            case REWRITE_PATH:
                String[] rewritePathArgs = gwfilter.getArgs().split(",");
                filter = ctx.getBean(RewritePathGatewayFilterFactory.class).
                        apply(c -> c.setRegexp(rewritePathArgs[0].trim()).
                        setReplacement(rewritePathArgs[1].trim()));
                break;

            case REWRITE_LOCATION:
                String[] rewriteLocationArgs = gwfilter.getArgs().split(",");
                filter = ctx.getBean(RewriteLocationResponseHeaderGatewayFilterFactory.class).
                        apply(c -> {
                            c.setStripVersion(RewriteLocationResponseHeaderGatewayFilterFactory.StripVersion.
                                    valueOf(rewriteLocationArgs[0].trim()));
                            if (rewriteLocationArgs.length > 1) {
                                c.setLocationHeaderName(rewriteLocationArgs[1].trim());
                            }
                            if (rewriteLocationArgs.length > 2) {
                                c.setHostValue(rewriteLocationArgs[2].trim());
                            }
                            if (rewriteLocationArgs.length > 3) {
                                c.setProtocols(rewriteLocationArgs[3].trim());
                            }
                        });
                break;

            case REWRITE_RESPONSE_HEADER:
                String[] rewriteResponseHeaderArgs = gwfilter.getArgs().split(",");
                filter = ctx.getBean(RewriteResponseHeaderGatewayFilterFactory.class).
                        apply(c -> c.setReplacement(rewriteResponseHeaderArgs[2].trim()).
                        setRegexp(rewriteResponseHeaderArgs[1].trim()).
                        setName(rewriteResponseHeaderArgs[0].trim()));
                break;

            case RETRY:
                Mutable<Integer> retries = new MutableObject<>();
                try {
                    retries.setValue(Integer.valueOf(gwfilter.getArgs().trim()));
                } catch (NumberFormatException e) {
                    LOG.error("Unexpected argument value: {}", gwfilter.getArgs().trim(), e);
                    retries.setValue(0);
                }
                filter = ctx.getBean(RetryGatewayFilterFactory.class).
                        apply(c -> c.setRetries(retries.getValue()));
                break;

            case SAVE_SESSION:
                filter = ctx.getBean(SaveSessionGatewayFilterFactory.class).apply(c -> {
                });
                break;

            case SECURE_HEADERS:
                filter = ctx.getBean(SecureHeadersGatewayFilterFactory.class).apply(c -> {
                });
                break;

            case SET_PATH:
                filter = ctx.getBean(SetPathGatewayFilterFactory.class).
                        apply(c -> c.setTemplate(gwfilter.getArgs().trim()));
                break;

            case SET_REQUEST_HEADER:
                String[] setRequestHeaderArgs = gwfilter.getArgs().split(",");
                filter = ctx.getBean(SetRequestHeaderGatewayFilterFactory.class).
                        apply(c -> c.setName(setRequestHeaderArgs[0].trim()).
                        setValue(setRequestHeaderArgs[1].trim()));
                break;

            case SET_RESPONSE_HEADER:
                String[] setResponseHeaderArgs = gwfilter.getArgs().split(",");
                filter = ctx.getBean(SetResponseHeaderGatewayFilterFactory.class).
                        apply(c -> c.setName(setResponseHeaderArgs[0].trim()).
                        setValue(setResponseHeaderArgs[1].trim()));
                break;

            case SET_STATUS:
                filter = ctx.getBean(SetStatusGatewayFilterFactory.class).
                        apply(c -> c.setStatus(gwfilter.getArgs().trim()));
                break;

            case STRIP_PREFIX:
                Mutable<Integer> parts = new MutableObject<>();
                try {
                    parts.setValue(Integer.valueOf(gwfilter.getArgs().trim()));
                } catch (NumberFormatException e) {
                    LOG.error("Unexpected argument value: {}", gwfilter.getArgs().trim(), e);
                    parts.setValue(0);
                }
                filter = ctx.getBean(StripPrefixGatewayFilterFactory.class).
                        apply(c -> c.setParts(parts.getValue()));
                break;

            case REQUEST_HEADER_TO_REQUEST_URI:
                filter = ctx.getBean(RequestHeaderToRequestUriGatewayFilterFactory.class).
                        apply(c -> c.setName(gwfilter.getArgs().trim()));
                break;

            case SET_REQUEST_SIZE:
                filter = ctx.getBean(RequestSizeGatewayFilterFactory.class).
                        apply(c -> c.setMaxSize(DataSize.ofBytes(Long.parseLong(gwfilter.getArgs().trim()))));
                break;

            case SET_REQUEST_HOST:
                filter = ctx.getBean(SetRequestHostHeaderGatewayFilterFactory.class).
                        apply(c -> c.setHost(gwfilter.getArgs().trim()));
                break;

            case LINK_REWRITE:
                filter = ApplicationContextUtils.getOrCreateBean(
                        ctx,
                        LinkRewriteGatewayFilterFactory.class.getName(),
                        LinkRewriteGatewayFilterFactory.class).
                        apply(c -> c.setData(route.getTarget().toASCIIString() + "," + gwfilter.getArgs().trim()));
                break;

            case CLIENT_CERTS_TO_REQUEST_HEADER:
                String header = StringUtils.isBlank(gwfilter.getArgs()) ? "X-Client-Certificate" : gwfilter.getArgs();
                filter = ApplicationContextUtils.getOrCreateBean(
                        ctx,
                        ClientCertsToRequestHeaderFilterFactory.class.getName(),
                        ClientCertsToRequestHeaderFilterFactory.class).
                        apply(c -> c.setName(header.trim()));
                break;

            case QUERY_PARAM_TO_REQUEST_HEADER:
                filter = ApplicationContextUtils.getOrCreateBean(
                        ctx,
                        QueryParamToRequestHeaderFilterFactory.class.getName(),
                        QueryParamToRequestHeaderFilterFactory.class).
                        apply(c -> c.setName(gwfilter.getArgs().trim()));
                break;

            case PRINCIPAL_TO_REQUEST_HEADER:
                filter = ApplicationContextUtils.getOrCreateBean(
                        ctx,
                        PrincipalToRequestHeaderFilterFactory.class.getName(),
                        PrincipalToRequestHeaderFilterFactory.class).
                        apply(c -> c.setName(gwfilter.getArgs().trim()));
                break;

            case CUSTOM:
                String[] customArgs = gwfilter.getArgs().split(";");
                Consumer<CustomGatewayFilterFactory.Config> customConsumer = customArgs.length > 1
                        ? c -> c.setData(customArgs[1])
                        : c -> c.setData(null);
                CustomGatewayFilterFactory factory = ApplicationContextUtils.getOrCreateBean(
                        ctx,
                        customArgs[0],
                        CustomGatewayFilterFactory.class);
                filter = factory.getOrder().
                        map(order -> (GatewayFilter) new OrderedGatewayFilter(factory.apply(customConsumer), order)).
                        orElseGet(() -> factory.apply(customConsumer));
                break;

            default:
                filter = null;
        }

        if (filter == null) {
            throw new IllegalArgumentException("Could not translate " + gwfilter);
        }

        return filter instanceof Ordered ? filter : new OrderedGatewayFilter(filter, 0);
    }

    protected AsyncPredicate<ServerWebExchange> toPredicate(final SRARoutePredicate gwpredicate, final boolean negate)
            throws ClassNotFoundException, NumberFormatException {

        AsyncPredicate<ServerWebExchange> predicate;
        switch (gwpredicate.getFactory()) {
            case AFTER:
                predicate = ctx.getBean(AfterRoutePredicateFactory.class).
                        applyAsync(c -> c.setDatetime(ZonedDateTime.parse(gwpredicate.getArgs().trim())));
                break;

            case BEFORE:
                predicate = ctx.getBean(BeforeRoutePredicateFactory.class).
                        applyAsync(c -> c.setDatetime(ZonedDateTime.parse(gwpredicate.getArgs().trim())));
                break;

            case BETWEEN:
                String[] betweenArgs = gwpredicate.getArgs().split(",");
                predicate = ctx.getBean(BetweenRoutePredicateFactory.class).
                        applyAsync(c -> c.setDatetime1(ZonedDateTime.parse(betweenArgs[0].trim())).
                        setDatetime2(ZonedDateTime.parse(betweenArgs[1].trim())));
                break;

            case COOKIE:
                String[] cookieArgs = gwpredicate.getArgs().split(",");
                predicate = ctx.getBean(CookieRoutePredicateFactory.class).
                        applyAsync(c -> c.setName(cookieArgs[0].trim()).
                        setRegexp(cookieArgs[1].trim()));
                break;

            case HEADER:
                String[] headerArgs = gwpredicate.getArgs().split(",");
                predicate = ctx.getBean(HeaderRoutePredicateFactory.class).
                        applyAsync(c -> c.setHeader(headerArgs[0].trim()).
                        setRegexp(headerArgs[1].trim()));
                break;

            case HOST:
                String[] hostArgs = gwpredicate.getArgs().split(",");
                predicate = ctx.getBean(HostRoutePredicateFactory.class).
                        applyAsync(c -> c.setPatterns(List.of(hostArgs)));
                break;

            case METHOD:
                String[] methodArgs = gwpredicate.getArgs().split(",");
                predicate = ctx.getBean(MethodRoutePredicateFactory.class).
                        applyAsync(c -> c.setMethods(
                        Stream.of(methodArgs).map(arg -> HttpMethod.valueOf(arg.trim())).toArray(HttpMethod[]::new)));
                break;

            case PATH:
                String[] pathArgs = gwpredicate.getArgs().split(",");
                predicate = ctx.getBean(PathRoutePredicateFactory.class).
                        applyAsync(c -> c.setPatterns(List.of(pathArgs)));
                break;

            case QUERY:
                String[] queryArgs = gwpredicate.getArgs().split(",");
                Consumer<QueryRoutePredicateFactory.Config> queryConsumer =
                        queryArgs.length > 1
                                ? c -> c.setParam(queryArgs[0].trim()).setRegexp(queryArgs[1].trim())
                                : c -> c.setParam(queryArgs[0].trim());
                predicate = ctx.getBean(QueryRoutePredicateFactory.class).
                        applyAsync(queryConsumer);
                break;

            case REMOTE_ADDR:
                String[] remoteAddrArgs = gwpredicate.getArgs().split(",");
                predicate = ctx.getBean(RemoteAddrRoutePredicateFactory.class).
                        applyAsync(c -> c.setSources(List.of(remoteAddrArgs)));
                break;

            case WEIGHT:
                String[] weigthArgs = gwpredicate.getArgs().split(",");
                Mutable<Integer> weight = new MutableObject<Integer>();
                try {
                    weight.setValue(Integer.valueOf(weigthArgs[1].trim()));
                } catch (NumberFormatException e) {
                    LOG.error("Unexpected argument value: {}", weigthArgs[1].trim(), e);
                    weight.setValue(0);
                }
                predicate = ctx.getBean(WeightRoutePredicateFactory.class).
                        applyAsync(c -> c.setGroup(weigthArgs[0].trim()).
                        setWeight(weight.getValue()));
                break;

            case CUSTOM:
                String[] customArgs = gwpredicate.getArgs().split(";");
                predicate = ApplicationContextUtils.getOrCreateBean(
                        ctx,
                        customArgs[0],
                        CustomRoutePredicateFactory.class).
                        applyAsync(c -> c.setData(customArgs[1]));
                break;

            default:
                predicate = null;
        }

        if (predicate == null) {
            throw new IllegalArgumentException("Could not translate predicate " + gwpredicate);
        }

        return negate ? predicate.negate() : predicate;
    }

    protected Route.AsyncBuilder toRoute(final SRARouteTO gwroute) {
        Route.AsyncBuilder builder = new Route.AsyncBuilder().
                id(gwroute.getKey()).order(gwroute.getOrder()).uri(gwroute.getTarget());

        if (gwroute.getPredicates().isEmpty()) {
            builder.predicate(exchange -> true);
        } else {
            gwroute.getPredicates().forEach(gwpredicate -> {
                if (builder.getPredicate() == null) {
                    try {
                        builder.asyncPredicate(toPredicate(gwpredicate, gwpredicate.isNegate()));
                    } catch (Exception e) {
                        LOG.error("Could not translate {}, skipping", gwpredicate, e);
                    }
                } else {
                    try {
                        switch (gwpredicate.getCond()) {
                            case OR:
                                builder.or(toPredicate(gwpredicate, gwpredicate.isNegate()));
                                break;

                            case AND:
                            default:
                                builder.and(toPredicate(gwpredicate, gwpredicate.isNegate()));
                        }
                    } catch (Exception e) {
                        LOG.error("Could not translate {}, skipping", gwpredicate, e);
                    }
                }
            });
        }

        if (!gwroute.getFilters().isEmpty()) {
            builder.filters(gwroute.getFilters().stream().
                    map(gwfilter -> {
                        try {
                            return toFilter(gwroute, gwfilter);
                        } catch (Exception e) {
                            LOG.error("Could not translate {}, skipping", gwfilter, e);
                            return null;
                        }
                    }).
                    filter(Objects::nonNull).
                    toList());
        }

        return builder;
    }

    public List<Route.AsyncBuilder> fetch() {
        synchronized (this) {
            if (client == null) {
                try {
                    client = new SyncopeClientFactoryBean().
                            setAddress(serviceOps.get(NetworkService.Type.CORE).getAddress()).
                            setUseCompression(useGZIPCompression).
                            create(new AnonymousAuthenticationHandler(anonymousUser, anonymousKey));
                } catch (Exception e) {
                    LOG.error("Could not init SyncopeClient", e);
                    return List.of();
                }
            }
        }

        synchronized (routeTOs) {
            routeTOs.clear();
            routeTOs.addAll(client.getService(SRARouteService.class).list());
        }

        return routeTOs.stream().map(this::toRoute).toList();
    }

    public List<SRARouteTO> getRouteTOs() {
        return routeTOs;
    }
}
