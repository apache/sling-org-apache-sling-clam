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

import javax.inject.Inject;

import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.ops4j.pax.exam.CoreOptions.options;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class JcrPropertyScanJobConsumerIT extends ClamTestSupport {

    @Inject
    @Filter("(job.topics=org/apache/sling/clam/scan/jcr/property/*)")
    private JobConsumer jobConsumer;

    @Configuration
    public Option[] configuration() {
        return options(
            baseConfiguration()
        );
    }

    @Test
    public void testJobConsumer() {
        assertThat(jobConsumer, notNullValue());
    }

}
