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

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
    name = "Apache Sling Clam Mail Sending Scan Result Handler",
    description = "Sends JCR property scan results via mail"
)
@SuppressWarnings("java:S100")
@interface MailSendingScanResultHandlerConfiguration {

    @AttributeDefinition(
        name = "Mail From",
        description = "Mail From address",
        required = false
    )
    String mail_from();

    @AttributeDefinition(
        name = "Mail To",
        description = "Mail To addresses"
    )
    String[] mail_to() default {};

    @AttributeDefinition(
        name = "Mail CC",
        description = "Mail CC addresses"
    )
    String[] mail_cc() default {};

    @AttributeDefinition(
        name = "Mail BCC",
        description = "Mail BCC addresses"
    )
    String[] mail_bcc() default {};

    @AttributeDefinition(
        name = "Mail Reply-To",
        description = "Mail Reply-To addresses"
    )
    String[] mail_replyTo() default {};

    @AttributeDefinition(
        name = "Mail Subject",
        description = "Mail Subject template (available variables: path, index, message, status, userId, started, size, timestamp)",
        required = false
    )
    String mail_subject() default "Clam scan result: [(${status})] for [(${path})][# th:if=\"${index}\"] [(${index})][/]";

    @AttributeDefinition(
        name = "Mail Text",
        description = "Mail Text template (available variables: path, index, message, status, userId, started, size, timestamp)",
        required = false
    ) // newlines get lost in scr and metatype XML
    String mail_text() default "status: [(${status})]\n" +
        "message: [(${message})]\n" +
        "path: [(${path})]\n" +
        "[# th:if=\"${index}\"]index: [(${index})][/]\n" +
        "size: [(${size})]\n" +
        "[# th:if=\"${userId}\"]userId: [(${userId})][/]\n" +
        "started: [(${#dates.formatISO(new java.util.Date(started))})]\n" +
        "timestamp: [(${#dates.formatISO(new java.util.Date(timestamp))})]\n";

    @AttributeDefinition(
        name = "Mail HTML",
        description = "Mail HTML template (available variables: path, index, message, status, userId, started, size, timestamp)",
        required = false
    )
    String mail_html();

    @AttributeDefinition(
        name = "send status ok",
        description = "Send scan results with status OK also"
    )
    boolean result_status_ok_send() default false;

    @AttributeDefinition(
        name = "Mail Service target",
        description = "Filter expression to target a Mail Service",
        required = false
    )
    String mailService_target();

    String webconsole_configurationFactory_nameHint() default "{mail.to}:{result.status.ok.send}:{mail.subject}";

}
