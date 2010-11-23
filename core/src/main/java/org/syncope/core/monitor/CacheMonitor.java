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
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Aspect
public class CacheMonitor {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            CacheMonitor.class);
    private static final NumberFormat NF = new DecimalFormat("0.0###");
    @Autowired
    private EntityManager entityManager;

    @Around("execution(* org.syncope.core.persistence.dao..*.*(..))")
    public final Object log(final ProceedingJoinPoint pjp)
            throws Throwable {

        Object result;
        if (!LOG.isDebugEnabled()) {
            result = pjp.proceed();
        } else {
            Statistics statistics =
                    ((Session) entityManager.getDelegate()).getSessionFactory().
                    getStatistics();
            //statistics.logSummary();

            long hit0 = statistics.getQueryCacheHitCount();
            long miss0 = statistics.getQueryCacheMissCount();

            result = pjp.proceed();

            long hit1 = statistics.getQueryCacheHitCount();
            long miss1 = statistics.getQueryCacheMissCount();

            String ratio;
            synchronized (NF) {
                ratio = NF.format((double) hit1 / (hit1 + miss1));
            }

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
        }

        return result;
    }
}
