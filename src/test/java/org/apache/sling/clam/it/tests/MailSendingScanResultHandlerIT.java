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
package org.apache.sling.clam.it.tests;

import java.security.Security;
import java.util.Objects;

import javax.inject.Inject;

import jakarta.mail.internet.MimeMessage;

import com.icegreen.greenmail.util.DummySSLSocketFactory;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import org.apache.commons.mail.util.MimeMessageParser;
import org.apache.sling.clam.jcr.NodeDescendingJcrPropertyDigger;
import org.apache.sling.clam.result.JcrPropertyScanResultHandler;
import org.apache.sling.resource.presence.ResourcePresence;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;
import org.ops4j.pax.exam.util.PathUtils;

import static com.google.common.truth.Truth.assertThat;
import static org.apache.sling.testing.paxexam.SlingOptions.greenmail;
import static org.apache.sling.testing.paxexam.SlingOptions.slingCommonsMessagingMail;
import static org.apache.sling.testing.paxexam.SlingOptions.slingResourcePresence;
import static org.apache.sling.testing.paxexam.SlingOptions.slingStarterContent;
import static org.apache.sling.testing.paxexam.SlingOptions.thymeleaf;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.factoryConfiguration;
import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.newConfiguration;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class MailSendingScanResultHandlerIT extends ClamTestSupport {

    @Inject
    @Filter(value = "(service.pid=org.apache.sling.clam.result.internal.MailSendingScanResultHandler)", timeout = 300000)
    private JcrPropertyScanResultHandler jcrPropertyScanResultHandler;

    @Inject
    @Filter(value = "(path=/content/starter)", timeout = 300000)
    private ResourcePresence resourcePresence;

    @Inject
    private NodeDescendingJcrPropertyDigger nodeDescendingJcrPropertyDigger;

    private GreenMail greenMail;

    @Configuration
    public Option[] configuration() {
        final int port = findFreePort();
        final String path = String.format("%s/src/test/resources/password", PathUtils.getBaseDir());
        return options(
            baseConfiguration(),
            clamdConfiguration(),
            slingResourcePresence(),
            factoryConfiguration("org.apache.sling.resource.presence.internal.ResourcePresenter")
                .put("path", "/content/starter")
                .asOption(),
            newConfiguration("org.apache.sling.clam.result.internal.MailSendingScanResultHandler")
                .put("mail.from", "from@example.org")
                .put("mail.to", "to@example.org")
                .put("mail.cc", "cc@example.org")
                .put("mail.bcc", "bcc@example.org")
                .put("mail.replyTo", "reply-to@example.org")
                .put("result.status.ok.send", true)
                .asOption(),
            slingStarterContent(),
            // Commons Messaging Mail
            factoryConfiguration("org.apache.sling.commons.messaging.mail.internal.SimpleMessageIdProvider")
                .put("host", "localhost")
                .asOption(),
            factoryConfiguration("org.apache.sling.commons.messaging.mail.internal.SimpleMailService")
                .put("mail.smtps.ssl.checkserveridentity", false)
                .put("mail.smtps.from", "envelope-from@example.org")
                .put("mail.smtps.host", "localhost")
                .put("mail.smtps.port", port)
                .put("username", "username")
                .put("password", "OEKPFL5cVJRqVjh4QaDZhvBiqv8wgWBMJ8PGbYHTqev046oV6888mna9w1mIGCXK")
                .asOption(),
            slingCommonsMessagingMail(),
            // Commons Crypto
            factoryConfiguration("org.apache.sling.commons.crypto.jasypt.internal.JasyptStandardPbeStringCryptoService")
                .put("algorithm", "PBEWITHHMACSHA512ANDAES_256")
                .asOption(),
            factoryConfiguration("org.apache.sling.commons.crypto.jasypt.internal.JasyptRandomIvGeneratorRegistrar")
                .put("algorithm", "SHA1PRNG")
                .asOption(),
            factoryConfiguration("org.apache.sling.commons.crypto.internal.FilePasswordProvider")
                .put("path", path)
                .asOption(),
            // Thymeleaf
            thymeleaf(),
            // testing – mail
            greenmail()
        );
    }

    @Before
    public void setUp() throws Exception {
        if (Objects.isNull(greenMail)) {
            // set up GreenMail server
            Security.setProperty("ssl.SocketFactory.provider", DummySSLSocketFactory.class.getName());
            final org.osgi.service.cm.Configuration[] configurations = configurationAdmin.listConfigurations("(service.factoryPid=org.apache.sling.commons.messaging.mail.internal.SimpleMailService)");
            final org.osgi.service.cm.Configuration configuration = configurations[0];
            final int port = (int) configuration.getProperties().get("mail.smtps.port");
            final ServerSetup serverSetup = new ServerSetup(port, "127.0.0.1", "smtps");
            greenMail = new GreenMail(serverSetup);
            greenMail.setUser("username", "password");
            greenMail.start();
        }
    }

    @After
    public void tearDown() {
        if (Objects.nonNull(greenMail)) {
            greenMail.stop();
            greenMail = null;
        }
    }

    @Test
    public void testJcrPropertyScanResultHandler() {
        assertThat(jcrPropertyScanResultHandler).isNotNull();
    }

    @Test
    public void testSentResults() throws Exception {
        digBinaries(nodeDescendingJcrPropertyDigger, "/content/starter");
        greenMail.waitForIncomingEmail(360000, 24);
        final MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages.length).isEqualTo(24);
        for (final MimeMessage message : messages) {
            assertThat(message.getSubject()).startsWith("Clam scan result: OK for /content/starter/");
            final MimeMessageParser parser = new MimeMessageParser(message).parse();
            final String text = parser.getPlainContent();
            assertThat(text).contains("status: OK");
            assertThat(text).contains("message: ");
            assertThat(text).contains("path: /content/starter/");
            assertThat(text).contains("started: ");
            assertThat(text).contains("timestamp: ");
        }
    }

}
