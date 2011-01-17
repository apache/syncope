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
package org.syncope.core.workflow;

import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.FunctionProvider;
import com.opensymphony.workflow.WorkflowException;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.MissingConfKeyException;
import org.syncope.core.persistence.dao.ConfDAO;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import org.identityconnectors.common.Base64;

public class GenerateToken extends OSWorkflowComponent
        implements FunctionProvider {

    private ConfDAO confDAO;

    public GenerateToken() {
        super();

        confDAO = (ConfDAO) context.getBean("confDAOImpl");
    }

    @Override
    @Transactional
    public void execute(Map transientVars, Map args, PropertySet ps)
            throws WorkflowException {

        SyncopeUser user = (SyncopeUser) transientVars.get(
                Constants.SYNCOPE_USER);

        final String token = (String) transientVars.get(
                Constants.TOKEN);

        LOG.debug("Received token {}", token);

        try {
            user.generateToken(
                    Integer.parseInt(confDAO.find(
                    "token.length").getConfValue()),
                    Integer.parseInt(confDAO.find(
                    "token.expireTime").getConfValue()), token);
        } catch (MissingConfKeyException e) {
            throw new WorkflowException(e);
        }

        transientVars.put(Constants.SYNCOPE_USER, user);
    }

    public final String encrypt(final String toBeCrypted) {

        String res = null;

        try {

            final DESKeySpec keySpec =
                    new DESKeySpec(confDAO.find(
                    "token.encryption.key").getConfValue().getBytes("UTF8"));

            final SecretKeyFactory keyFactory =
                    SecretKeyFactory.getInstance("DES");

            final SecretKey key = keyFactory.generateSecret(keySpec);

            final byte[] cleartext = toBeCrypted.getBytes("UTF8");

            final Cipher cipher = Cipher.getInstance("DES");

            if (LOG.isDebugEnabled()) {
                LOG.debug("To Be Encrypted: " + toBeCrypted);
            }

            cipher.init(Cipher.ENCRYPT_MODE, key);
            res = Base64.encode(cipher.doFinal(cleartext));

            if (LOG.isDebugEnabled()) {
                LOG.debug("Encrypted: " + res);
            }

        } catch (Exception e) {
            LOG.error("Encrypt operation failed", e);
        }

        return res;
    }

    public final String decrypt(final String toBeDecrypted) {
        String res = null;

        try {

            final DESKeySpec keySpec =
                    new DESKeySpec(confDAO.find(
                    "token.encryption.key").getConfValue().getBytes("UTF8"));

            final SecretKeyFactory keyFactory =
                    SecretKeyFactory.getInstance("DES");

            final SecretKey key = keyFactory.generateSecret(keySpec);

            final byte[] encrypedPwdBytes =
                    Base64.decode(toBeDecrypted);

            final Cipher cipher = Cipher.getInstance("DES");

            if (LOG.isDebugEnabled()) {
                LOG.debug("To Be Dencrypted: " + toBeDecrypted);
            }

            cipher.init(Cipher.DECRYPT_MODE, key);
            res = new String(cipher.doFinal(encrypedPwdBytes));

            if (LOG.isDebugEnabled()) {
                LOG.debug("Dencrypted: " + res);
            }

        } catch (Exception e) {
            LOG.error("Decrypt operation failed", e);
        }

        return res;
    }
}
