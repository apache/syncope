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
package org.apache.syncope.core.flowable.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.syncope.core.flowable.impl.FlowableWorkflowUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.idm.spring.SpringIdmEngineConfiguration;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
class DomainProcessEngineFactoryBeanTest {

    @Mock
    private ApplicationContext ctx;

    @Mock
    private DataSource dataSource;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private SpringProcessEngineConfiguration conf;

    @Mock
    private SpringIdmEngineConfiguration idmConfig;

    @Mock
    private ProcessEngine processEngine;

    @Mock
    private JavaDelegate javaDelegateBean;

    private DomainProcessEngineFactoryBean factoryBean;

    @BeforeEach
    void setUp() {
        factoryBean = new DomainProcessEngineFactoryBean(ctx);

        doReturn(transactionManager).when(ctx).getBean(
                eq("domainTransactionManager"), eq(PlatformTransactionManager.class));
        doReturn(conf).when(ctx).getBean(eq(SpringProcessEngineConfiguration.class));
        doReturn(null).when(ctx).getBean(eq("entityManagerFactory"));
        doReturn(idmConfig).when(ctx).getBean(eq(SpringIdmEngineConfiguration.class));

        when(idmConfig.getEngineCfgKey()).thenReturn("key");
        when(idmConfig.getEngineScopeType()).thenReturn("scope");

        when(conf.buildProcessEngine()).thenReturn(processEngine);
    }

    private ExpressionManager captureExpressionManager() {
        ArgumentCaptor<ExpressionManager> captor = ArgumentCaptor.forClass(ExpressionManager.class);
        verify(conf).setExpressionManager(captor.capture());
        return captor.getValue();
    }

    @Test
    void buildSetsSandboxedExpressionManager() {
        when(ctx.containsBean("myJavaDelegate")).thenReturn(true);
        doReturn(JavaDelegate.class).when(ctx).getType("myJavaDelegate");
        when(ctx.getBean("myJavaDelegate")).thenReturn(javaDelegateBean);

        factoryBean.build(dataSource);

        ExpressionManager expressionManager = captureExpressionManager();
        assertInstanceOf(SandboxedProcessExpressionManager.class, expressionManager);
        assertTrue(expressionManager.getBeans().containsKey("myJavaDelegate"));
        assertSame(javaDelegateBean, expressionManager.getBeans().get("myJavaDelegate"));

        FlowableException ex = assertThrows(FlowableException.class, () -> expressionManager.createExpression(
                "${''['getClass']()['forName']('java.lang.Runtime')['getMethod']('getRuntime')"
                + "['invoke'](null)['exec']('touch /tmp/syncope-fix-B1') != null}").
                getValue(VariableContainer.empty()));
        assertTrue(ex.getMessage().contains("Unknown method used in expression"), ex.getMessage());
    }

    @Test
    void buildWhenNoAllowedBeans() {
        factoryBean.build(dataSource);

        ExpressionManager expressionManager = captureExpressionManager();
        assertInstanceOf(SandboxedProcessExpressionManager.class, expressionManager);
        assertFalse(expressionManager.getBeans().containsKey("myJavaDelegate"));
    }

    @Test
    void buildLeavesExistingBeansUntouched() {
        Map<Object, Object> existingBeans = new HashMap<>();
        existingBeans.put("someBean", new Object());
        doReturn(existingBeans).when(conf).getBeans();

        factoryBean.build(dataSource);

        verify(conf, never()).setBeans(any());
        assertFalse(captureExpressionManager().getBeans().containsKey("someBean"));
    }

    @Test
    void resolvesFlowableUtilsLazily() {
        FlowableWorkflowUtils flowableUtils = new FlowableWorkflowUtils(mock(DomainProcessEngine.class));
        when(ctx.containsBean("flowableUtils")).thenReturn(true);
        doReturn(FlowableWorkflowUtils.class).when(ctx).getType("flowableUtils");
        when(ctx.getBean("flowableUtils")).thenReturn(flowableUtils);

        factoryBean.build(dataSource);

        assertSame(flowableUtils, captureExpressionManager().getBeans().get("flowableUtils"));
    }

    @Test
    void doesNotExposeOtherBeans() {
        when(ctx.containsBean("dataSource")).thenReturn(true);
        doReturn(DataSource.class).when(ctx).getType("dataSource");

        factoryBean.build(dataSource);

        assertFalse(captureExpressionManager().getBeans().containsKey("dataSource"));
    }
}
