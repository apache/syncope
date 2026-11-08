/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the License.  You may not use this file except in compliance
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
package org.apache.syncope.core.persistence.api;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;

class ApplicationContextProviderTest {

    @BeforeEach
    void setUp() {
        ApplicationContextProvider.setApplicationContext(null);
        ApplicationContextProvider.setBeanFactory(null);
    }

    @AfterEach
    void tearDown() {
        ApplicationContextProvider.setApplicationContext(null);
        ApplicationContextProvider.setBeanFactory(null);
    }

    @Test
    void getApplicationContextReturnsNullWhenUnset() {
        assertNull(ApplicationContextProvider.getApplicationContext());
    }

    @Test
    void getBeanFactoryReturnsNullWhenUnset() {
        assertNull(ApplicationContextProvider.getBeanFactory());
    }

    @Test
    void setApplicationContextSetsContext() {
        final ConfigurableApplicationContext ctx = Mockito.mock(ConfigurableApplicationContext.class);

        ApplicationContextProvider.setApplicationContext(ctx);

        assertSame(ctx, ApplicationContextProvider.getApplicationContext());
    }

    @Test
    void getBeanFactoryReturnsBeanFactoryFromContext() {
        final ConfigurableApplicationContext ctx = Mockito.mock(ConfigurableApplicationContext.class);
        final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        Mockito.when(ctx.getBeanFactory()).thenReturn(beanFactory);

        ApplicationContextProvider.setApplicationContext(ctx);

        assertSame(beanFactory, ApplicationContextProvider.getBeanFactory());
    }

    @Test
    void setBeanFactorySetsBeanFactory() {
        final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

        ApplicationContextProvider.setBeanFactory(beanFactory);

        assertSame(beanFactory, ApplicationContextProvider.getBeanFactory());
    }

    @Test
    void setApplicationContextViaAware() {
        final ConfigurableApplicationContext ctx = Mockito.mock(ConfigurableApplicationContext.class);
        final ApplicationContextProvider provider = new ApplicationContextProvider();

        provider.setApplicationContext(ctx);

        assertSame(ctx, ApplicationContextProvider.getApplicationContext());
    }
}
