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
package org.apache.syncope.wa.starter.events;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

public class WACasConfigurationEventListenerTest {

    @Test
    public void refreshDoesNotInitializeDispatcherServlet() {
        final ConfigurableApplicationContext applicationContext = mock(ConfigurableApplicationContext.class);
        final DispatcherServlet dispatcherServlet = new DispatcherServlet();
        when(applicationContext.getBeanDefinitionNames()).thenReturn(new String[] { "dispatcherServlet" });
        when(applicationContext.getBean("dispatcherServlet")).thenReturn(dispatcherServlet);
        when(applicationContext.containsBean("dispatcherServlet")).thenReturn(true);
        when(applicationContext.getBean(DispatcherServlet.class)).thenReturn(dispatcherServlet);

        final WACasConfigurationEventListener listener =
                new WACasConfigurationEventListener(null, null, null, applicationContext);
        assertDoesNotThrow(() -> listener.onRefreshScopeRefreshed(new RefreshScopeRefreshedEvent()));

        verify(applicationContext).getBean("dispatcherServlet");
        verify(applicationContext, never()).containsBean("dispatcherServlet");
        verify(applicationContext, never()).getBean(DispatcherServlet.class);
    }
}
