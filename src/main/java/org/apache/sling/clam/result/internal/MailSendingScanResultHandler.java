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
package org.apache.sling.clam.result.internal;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import jakarta.mail.internet.MimeMessage;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.clam.result.JcrPropertyScanResultHandler;
import org.apache.sling.commons.clam.ScanResult;
import org.apache.sling.commons.messaging.mail.MailService;
import org.apache.sling.commons.messaging.mail.MessageBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.TemplateSpec;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IContext;
import org.thymeleaf.templatemode.TemplateMode;

import static org.apache.sling.clam.internal.ClamUtil.properties;

/**
 * Service to send scan results as mail.
 */
@Component(
    configurationPolicy = ConfigurationPolicy.REQUIRE,
    property = {
        Constants.SERVICE_DESCRIPTION + "=Apache Sling Clam Mail Sending Scan Result Handler",
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
    }
)
@Designate(
    ocd = MailSendingScanResultHandlerConfiguration.class,
    factory = true
)
@SuppressWarnings({"java:S112", "java:S1117", "java:S3077", "checkstyle:ClassFanOutComplexity"})
public final class MailSendingScanResultHandler implements JcrPropertyScanResultHandler {

    @Reference(
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY
    )
    private volatile MailService mailService;

    private MailSendingScanResultHandlerConfiguration configuration;

    private final ITemplateEngine templateEngine = new TemplateEngine();

    private final Logger logger = LoggerFactory.getLogger(MailSendingScanResultHandler.class);

    public MailSendingScanResultHandler() { //
    }

    @Activate
    @SuppressWarnings("unused")
    private void activate(final MailSendingScanResultHandlerConfiguration configuration) {
        logger.debug("activating");
        this.configuration = configuration;
    }

    @Modified
    @SuppressWarnings("unused")
    private void modified(final MailSendingScanResultHandlerConfiguration configuration) {
        logger.debug("modifying");
        this.configuration = configuration;
    }

    @Deactivate
    @SuppressWarnings("unused")
    private void deactivate() {
        logger.debug("deactivating");
        this.configuration = null;
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void handleJcrPropertyScanResult(@NotNull final ScanResult scanResult, @NotNull final String path, final int propertyType, @Nullable final String userId) {
        if (checkPublish(scanResult)) {
            final Map<String, Object> properties = properties(path, userId, scanResult);
            try {
                sendMail(properties);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void handleJcrPropertyScanResult(@NotNull final ScanResult scanResult, @NotNull final String path, final int index, final int propertyType, @Nullable final String userId) {
        if (checkPublish(scanResult)) {
            final Map<String, Object> properties = properties(path, index, userId, scanResult);
            try {
                sendMail(properties);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private boolean checkPublish(final ScanResult scanResult) {
        return !scanResult.isOk() || configuration.result_status_ok_send();
    }

    @SuppressWarnings({"java:S3776", "checkstyle:CyclomaticComplexity", "checkstyle:ExecutableStatementCount", "checkstyle:NPathComplexity"})
    private void sendMail(final Map<String, Object> properties) throws Exception {
        final MailService mailService = this.mailService;
        final MessageBuilder builder = mailService.getMessageBuilder();

        final String from = configuration.mail_from();
        final String[] to = configuration.mail_to();
        final String[] cc = configuration.mail_cc();
        final String[] bcc = configuration.mail_bcc();
        final String[] replyTo = configuration.mail_replyTo();
        final String subject = configuration.mail_subject();
        final String text = configuration.mail_text();
        final String html = configuration.mail_html();

        if (StringUtils.isNotBlank(from)) {
            builder.from(from);
        }
        for (final String address : to) {
            if (StringUtils.isNotBlank(address)) {
                builder.to(address);
            }
        }
        for (final String address : cc) {
            if (StringUtils.isNotBlank(address)) {
                builder.cc(address);
            }
        }
        for (final String address : bcc) {
            if (StringUtils.isNotBlank(address)) {
                builder.bcc(address);
            }
        }
        for (final String address : replyTo) {
            if (StringUtils.isNotBlank(address)) {
                builder.replyTo(address);
            }
        }
        if (StringUtils.isNotBlank(subject)) {
            final IContext context = new Context(Locale.ENGLISH, properties);
            final TemplateSpec templateSpec = new TemplateSpec(subject, TemplateMode.TEXT);
            final String s = templateEngine.process(templateSpec, context);
            builder.subject(s);
        }
        if (StringUtils.isNotBlank(text)) {
            final IContext context = new Context(Locale.ENGLISH, properties);
            final TemplateSpec templateSpec = new TemplateSpec(text, TemplateMode.TEXT);
            final String t = templateEngine.process(templateSpec, context);
            builder.text(t);
        }
        if (StringUtils.isNotBlank(html)) {
            final IContext context = new Context(Locale.ENGLISH, properties);
            final TemplateSpec templateSpec = new TemplateSpec(html, TemplateMode.HTML);
            final String h = templateEngine.process(templateSpec, context);
            builder.html(h);
        }
        final MimeMessage message = builder.build();
        logger.debug("sending scan result mail: {}", properties);
        final CompletableFuture<Void> future = mailService.sendMessage(message);
        future.whenComplete((v, e) -> {
            if (Objects.nonNull(e)) {
                logger.error("sending scan result mail failed", e);
            } else {
                logger.debug("sending scan result mail succeeded");
            }
        });
    }

}
