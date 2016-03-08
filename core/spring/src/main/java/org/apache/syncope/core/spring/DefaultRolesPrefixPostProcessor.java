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
package org.apache.syncope.core.spring;

import javax.servlet.ServletException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.PriorityOrdered;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter;

/**
 * Removes the limitation of having Spring security roles to be prefixed with 'ROLE_'.
 */
public class DefaultRolesPrefixPostProcessor implements BeanPostProcessor, PriorityOrdered {

    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName) {
        if (bean instanceof DefaultMethodSecurityExpressionHandler) {
            ((DefaultMethodSecurityExpressionHandler) bean).setDefaultRolePrefix(null);
        }
        if (bean instanceof DefaultWebSecurityExpressionHandler) {
            ((DefaultWebSecurityExpressionHandler) bean).setDefaultRolePrefix(null);
        }
        if (bean instanceof SecurityContextHolderAwareRequestFilter) {
            SecurityContextHolderAwareRequestFilter filter = (SecurityContextHolderAwareRequestFilter) bean;
            filter.setRolePrefix(StringUtils.EMPTY);
            try {
                filter.afterPropertiesSet();
            } catch (ServletException e) {
                throw new FatalBeanException(e.getMessage(), e);
            }
        }

        return bean;
    }

    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName) {
        return bean;
    }

    @Override
    public int getOrder() {
        return PriorityOrdered.HIGHEST_PRECEDENCE;
    }
}
