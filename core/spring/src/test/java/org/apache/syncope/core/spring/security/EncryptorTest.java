package org.apache.syncope.core.spring.security;

import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.*;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


@RunWith(Parameterized.class)
public class EncryptorTest{
    private static Encryptor ENCRYPTOR;
    private static MockedStatic<ApplicationContextProvider> UTIL;

    // Parameters constructed by category partition
    private final PlainTextValue value;
    private final CipherAlgorithm cipherAlgorithm;

    @BeforeClass
    public static void setUp(){
        DefaultListableBeanFactory factory=new DefaultListableBeanFactory();

        factory.registerSingleton("securityProperties", new SecurityProperties());

        UTIL = Mockito.mockStatic(ApplicationContextProvider.class);
        UTIL.when(ApplicationContextProvider::getBeanFactory).thenReturn(factory);
        UTIL.when(ApplicationContextProvider::getApplicationContext).thenReturn(new DummyConfigurableApplicationContext(factory));
        ENCRYPTOR = Encryptor.getInstance();
    }


    public EncryptorTest(PlainTextValue value, CipherAlgorithm cipherAlgorithm) {
        this.value = value;
        this.cipherAlgorithm = cipherAlgorithm;
    }

    private enum PlainTextValue {
        VALID_STRING("secret_password"),
        EMPTY_STRING(""),
        NULL_STRING(null);

        private String value;

        PlainTextValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }


    @Parameterized.Parameters
    public static Collection<Object[]> getParameters() {
        List<Object[]> parameters = new ArrayList<>();

        for(PlainTextValue value: PlainTextValue.values()) {
            for (CipherAlgorithm cipher : CipherAlgorithm.values()) {
                Object[] parameterSet = {value, cipher};
                parameters.add(parameterSet);
            }
        }
        return parameters;
    }

    @Test
    public void testEncryptWithoutVerify() throws Exception {

        String encryptedValue = ENCRYPTOR.encode(value.getValue(), cipherAlgorithm);

        switch (value) {
            case NULL_STRING:
                Assert.assertEquals("A null value must return a null ciphertext", encryptedValue, null);
                break;

            default:
                Assert.assertNotEquals("The encrypted value must not be null", encryptedValue, null);
                Assert.assertNotEquals("The encrypted value must be different then the original one",
                    encryptedValue, value.getValue());
        }

    }


    @Test
    public void testEncryptWithVerify() throws Exception {

        String encryptedValue = ENCRYPTOR.encode(value.getValue(), cipherAlgorithm);

        switch (value) {
            case NULL_STRING:
                Assert.assertFalse("A null value cannot be verified",
                    ENCRYPTOR.verify(value.getValue(), cipherAlgorithm, encryptedValue));
                break;

            default:
                Assert.assertTrue("The verification of the ciphertext and plaintext has failed",
                    ENCRYPTOR.verify(value.getValue(), cipherAlgorithm, encryptedValue));

        }

    }

}
