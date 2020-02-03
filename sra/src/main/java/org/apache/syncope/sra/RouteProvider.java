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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.lib.AnonymousAuthenticationHandler;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.lib.to.GatewayRouteTO;
import org.apache.syncope.common.lib.types.GatewayRouteFilter;
import org.apache.syncope.common.lib.types.GatewayRoutePredicate;
import org.apache.syncope.common.lib.types.GatewayRouteStatus;
import org.apache.syncope.common.rest.api.service.GatewayRouteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AddRequestHeaderGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.AddRequestParameterGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.AddResponseHeaderGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.FallbackHeadersGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.HystrixGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.PrefixPathGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.PreserveHostHeaderGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.RedirectToGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.RemoveRequestHeaderGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.RemoveResponseHeaderGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.RequestHeaderToRequestUriGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.RequestRateLimiterGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.RequestSizeGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.RewritePathGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.RewriteResponseHeaderGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.SaveSessionGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.SecureHeadersGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.SetPathGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.SetRequestHeaderGatewayFilterFactory;
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
import org.springframework.cloud.gateway.route.Route;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.server.ServerWebExchange;

@Component
public class RouteProvider {

    private static final Logger LOG = LoggerFactory.getLogger(RouteProvider.class);

    @Autowired
    private ServiceOps serviceOps;

    @Autowired
    private ConfigurableApplicationContext ctx;

    @Value("${anonymousUser}")
    private String anonymousUser;

    @Value("${anonymousKey}")
    private String anonymousKey;

    @Value("${useGZIPCompression}")
    private boolean useGZIPCompression;

    private SyncopeClient client;

    @SuppressWarnings("unchecked")
    private GatewayFilter toFilter(final String routeId, final GatewayRouteFilter gwfilter)
            throws ClassNotFoundException {

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

            case HYSTRIX:
                String[] hystrixArgs = gwfilter.getArgs().split(",");
                filter = ctx.getBean(HystrixGatewayFilterFactory.class).
                        apply(routeId, c -> {
                            if (StringUtils.isNotBlank(hystrixArgs[0])) {
                                c.setName(hystrixArgs[0].trim());
                            }
                            if (StringUtils.isNotBlank(hystrixArgs[1])) {
                                c.setFallbackUri(hystrixArgs[1].trim());
                            }
                        });
                break;

            case FALLBACK_HEADERS:
                String[] fallbackHeadersArgs = gwfilter.getArgs().split(",");
                filter = ctx.getBean(FallbackHeadersGatewayFilterFactory.class).
                        apply(c -> {
                            if (StringUtils.isNotBlank(fallbackHeadersArgs[0])) {
                                c.setCauseExceptionMessageHeaderName(fallbackHeadersArgs[0].trim());
                            }
                            if (StringUtils.isNotBlank(fallbackHeadersArgs[1])) {
                                c.setExecutionExceptionMessageHeaderName(fallbackHeadersArgs[1].trim());
                            }
                            if (StringUtils.isNotBlank(fallbackHeadersArgs[2])) {
                                c.setExecutionExceptionTypeHeaderName(fallbackHeadersArgs[2].trim());
                            }
                            if (StringUtils.isNotBlank(fallbackHeadersArgs[3])) {
                                c.setRootCauseExceptionTypeHeaderName(fallbackHeadersArgs[3].trim());
                            }
                        });
                break;

            case PREFIX_PATH:
                filter = ctx.getBean(PrefixPathGatewayFilterFactory.class).
                        apply(c -> c.setPrefix(gwfilter.getArgs().trim()));
                break;

            case PRESERVE_HOST_HEADER:
                filter = ctx.getBean(PreserveHostHeaderGatewayFilterFactory.class).apply();
                break;

            case REDIRECT:
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

            case RETRY:
                filter = ctx.getBean(RetryGatewayFilterFactory.class).
                        apply(c -> c.setRetries(Integer.valueOf(gwfilter.getArgs().trim())));
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

            case REWRITE_RESPONSE_HEADER:
                String[] rewriteResponseHeaderArgs = gwfilter.getArgs().split(",");
                filter = ctx.getBean(RewriteResponseHeaderGatewayFilterFactory.class).
                        apply(c -> c.setReplacement(rewriteResponseHeaderArgs[2].trim()).
                        setRegexp(rewriteResponseHeaderArgs[1].trim()).
                        setName(rewriteResponseHeaderArgs[0].trim()));
                break;

            case SET_STATUS:
                filter = ctx.getBean(SetStatusGatewayFilterFactory.class).
                        apply(c -> c.setStatus(gwfilter.getArgs().trim()));
                break;

            case SAVE_SESSION:
                filter = ctx.getBean(SaveSessionGatewayFilterFactory.class).apply(c -> {
                });
                break;

            case STRIP_PREFIX:
                filter = ctx.getBean(StripPrefixGatewayFilterFactory.class).
                        apply(c -> c.setParts(Integer.valueOf(gwfilter.getArgs().trim())));
                break;

            case REQUEST_HEADER_TO_REQUEST_URI:
                filter = ctx.getBean(RequestHeaderToRequestUriGatewayFilterFactory.class).
                        apply(c -> c.setName(gwfilter.getArgs().trim()));
                break;

            case SET_REQUEST_SIZE:
                filter = ctx.getBean(RequestSizeGatewayFilterFactory.class).
                        apply(c -> c.setMaxSize(DataSize.ofBytes(Long.valueOf(gwfilter.getArgs().trim()))));
                break;

            case CUSTOM:
                String[] customArgs = gwfilter.getArgs().split(";");
                CustomGatewayFilterFactory factory;
                if (ctx.getBeanFactory().containsSingleton(customArgs[0])) {
                    factory = (CustomGatewayFilterFactory) ctx.getBeanFactory().getSingleton(customArgs[0]);
                } else {
                    factory = (CustomGatewayFilterFactory) ctx.getBeanFactory().
                            createBean(Class.forName(customArgs[0]), AbstractBeanDefinition.AUTOWIRE_BY_TYPE, false);
                    ctx.getBeanFactory().registerSingleton(customArgs[0], factory);
                }
                filter = factory.apply(c -> c.setData(customArgs[1]));
                break;

            default:
                filter = null;
        }

        if (filter == null) {
            throw new IllegalArgumentException("Could not translate " + gwfilter);
        }

        return filter instanceof Ordered ? filter : new OrderedGatewayFilter(filter, 0);
    }

    private AsyncPredicate<ServerWebExchange> toPredicate(final GatewayRoutePredicate gwpredicate, final boolean negate)
            throws ClassNotFoundException {

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
                predicate = ctx.getBean(HeaderRoutePredicateFactory.class).
                        applyAsync(c -> c.setHeader(gwpredicate.getArgs().trim()));
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
                        Stream.of(methodArgs).map(arg -> HttpMethod.resolve(arg.trim())).toArray(HttpMethod[]::new)));
                break;

            case PATH:
                String[] pathArgs = gwpredicate.getArgs().split(",");
                predicate = ctx.getBean(PathRoutePredicateFactory.class).
                        applyAsync(c -> c.setPatterns(List.of(pathArgs)));
                break;

            case QUERY:
                String[] queryArgs = gwpredicate.getArgs().split(",");
                predicate = ctx.getBean(QueryRoutePredicateFactory.class).
                        applyAsync(c -> c.setParam(queryArgs[0].trim()).
                        setRegexp(queryArgs[1].trim()));
                break;

            case REMOTE_ADDR:
                String[] remoteAddrArgs = gwpredicate.getArgs().split(",");
                predicate = ctx.getBean(RemoteAddrRoutePredicateFactory.class).
                        applyAsync(c -> c.setSources(List.of(remoteAddrArgs)));
                break;

            case CUSTOM:
                String[] customArgs = gwpredicate.getArgs().split(";");
                CustomRoutePredicateFactory factory;
                if (ctx.getBeanFactory().containsSingleton(customArgs[0])) {
                    factory = (CustomRoutePredicateFactory) ctx.getBeanFactory().getSingleton(customArgs[0]);
                } else {
                    factory = (CustomRoutePredicateFactory) ctx.getBeanFactory().
                            createBean(Class.forName(customArgs[0]), AbstractBeanDefinition.AUTOWIRE_BY_TYPE, false);
                    ctx.getBeanFactory().registerSingleton(customArgs[0], factory);
                }
                predicate = factory.applyAsync(c -> c.setData(customArgs[1]));
                break;

            default:
                predicate = null;
        }

        if (predicate == null) {
            throw new IllegalArgumentException("Could not translate " + gwpredicate);
        }

        if (negate) {
            predicate.negate();
        }
        return predicate;
    }

    private Route.AsyncBuilder toRoute(final GatewayRouteTO gwroute) {
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
                            return toFilter(gwroute.getKey(), gwfilter);
                        } catch (Exception e) {
                            LOG.error("Could not translate {}, skipping", gwfilter, e);
                            return null;
                        }
                    }).
                    filter(Objects::nonNull).
                    collect(Collectors.toList()));
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

        return client.getService(GatewayRouteService.class).list().stream().
                filter(gwroute -> gwroute.getStatus() == GatewayRouteStatus.PUBLISHED).
                map(this::toRoute).
                collect(Collectors.toList());
    }
}
