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
package org.syncope.core.monitor;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import javax.persistence.EntityManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
//import org.hibernate.Session;
//import org.hibernate.stat.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Log cache hits and misses.
 */
@Aspect
public class CacheMonitor {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            CacheMonitor.class);

    /**
     * Number formatter for hit / miss percentage.
     */
    private static final ThreadLocal<NumberFormat> PERCENT_FORMAT =
            new ThreadLocal<NumberFormat>() {

                @Override
                protected NumberFormat initialValue() {
                    return new DecimalFormat("0.0###");
                }
            };

    /**
     * EntityManager.
     */
    @Autowired
    private EntityManager entityManager;

    /**
     * Intercept any DAO method call and use Hibernate's statistics features
     * to log cache access.
     *
     * @param pjp Aspect's ProceedingJoinPoint
     * @return DAO method's return value
     * @throws Throwable if anything goes wrong
     */
    @Around("execution(* org.syncope.core.persistence.dao..*.*(..))")
    public final Object log(final ProceedingJoinPoint pjp)
            throws Throwable {

        Object result;
        /*if (!LOG.isDebugEnabled()) {*/
            result = pjp.proceed();
        /*} else {
            Statistics statistics =
                    ((Session) entityManager.getDelegate()).getSessionFactory().
                    getStatistics();
            //statistics.logSummary();

            final long hit0 = statistics.getQueryCacheHitCount();
            final long miss0 = statistics.getQueryCacheMissCount();

            result = pjp.proceed();

            final long hit1 = statistics.getQueryCacheHitCount();
            final long miss1 = statistics.getQueryCacheMissCount();

            final String ratio = PERCENT_FORMAT.get().
                    format((double) hit1 / (hit1 + miss1));

            if (hit1 > hit0) {
                LOG.debug(
                        String.format("CACHE HIT; Ratio=%s; Signature=%s#%s()",
                        ratio, pjp.getTarget().getClass().getName(),
                        pjp.getSignature().toShortString()));
            } else if (miss1 > miss0) {
                LOG.debug(
                        String.format("CACHE MISS; Ratio=%s; Signature=%s#%s()",
                        ratio, pjp.getTarget().getClass().getName(),
                        pjp.getSignature().toShortString()));
            }
        }*/

        return result;
    }
}
