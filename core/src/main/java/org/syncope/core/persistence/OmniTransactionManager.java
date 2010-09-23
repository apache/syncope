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
package org.syncope.core.persistence;

import java.io.Serializable;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.jta.TransactionFactory;

public class OmniTransactionManager implements PlatformTransactionManager,
        TransactionFactory, InitializingBean, Serializable {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            OmniTransactionManager.class);
    private JpaTransactionManager jpaTransactionManager;
    private JtaTransactionManager jtaTransactionManager;

    public OmniTransactionManager() {
        jtaTransactionManager = new JtaTransactionManager();
    }

    public OmniTransactionManager(JpaTransactionManager jpaTransactionManager) {
        super();
        setJpaTransactionManager(jpaTransactionManager);
    }

    public JpaTransactionManager getJpaTransactionManager() {
        return jpaTransactionManager;
    }

    public void setJpaTransactionManager(
            JpaTransactionManager jpaTransactionManager) {

        this.jpaTransactionManager = jpaTransactionManager;
    }

    private TransactionFactory getTransactionFactory() {
        return (TransactionFactory) (jtaTransactionManager != null
                ? jtaTransactionManager : jpaTransactionManager);
    }

    private PlatformTransactionManager getPlatformTransactionManager() {
        return jtaTransactionManager != null
                ? jtaTransactionManager : jpaTransactionManager;
    }

    @Override
    public TransactionStatus getTransaction(TransactionDefinition definition)
            throws TransactionException {

        return getPlatformTransactionManager().getTransaction(definition);
    }

    @Override
    public void commit(TransactionStatus status) throws TransactionException {
        getPlatformTransactionManager().commit(status);
    }

    @Override
    public void rollback(TransactionStatus status) throws TransactionException {
        getPlatformTransactionManager().rollback(status);
    }

    @Override
    public Transaction createTransaction(String name, int timeout)
            throws NotSupportedException, SystemException {

        return getTransactionFactory().createTransaction(name, timeout);
    }

    @Override
    public boolean supportsResourceAdapterManagedTransactions() {
        return getTransactionFactory().
                supportsResourceAdapterManagedTransactions();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            jtaTransactionManager.afterPropertiesSet();
        } catch (Throwable t) {
            jtaTransactionManager = null;

            LOG.error("Could not instantiate JtaTransactionManager, "
                    + "reverting to JpaTransactionManager");
            if (LOG.isDebugEnabled()) {
                LOG.debug("Here's why", t);
            }
        }
    }
}
