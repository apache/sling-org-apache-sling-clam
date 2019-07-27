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
package org.apache.sling.clam.http.internal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.clam.result.JcrPropertyScanResultHandler;
import org.apache.sling.commons.clam.ScanResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.sling.clam.http.internal.ResponseUtil.json;

@Component(
    service = {
        Servlet.class,
        JcrPropertyScanResultHandler.class
    },
    property = {
        Constants.SERVICE_DESCRIPTION + "=Apache Sling Clam Events Servlet",
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation",
        HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT + "=(osgi.http.whiteboard.context.name=org.osgi.service.http)",
        HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED + "=true"
    }
)
@Designate(
    ocd = ClamEventsServletConfiguration.class
)
public class ClamEventsServlet extends HttpServlet implements JcrPropertyScanResultHandler {

    private List<Client> clients = Collections.synchronizedList(new ArrayList<>());

    private final AtomicLong counter = new AtomicLong(0);

    private static final String JCR_RESULT_EVENT_TYPE = "sling/clam/jcr/result";

    private final Logger logger = LoggerFactory.getLogger(ClamEventsServlet.class);

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/event-stream");
        response.addHeader("Connection", "close");
        final AsyncContext context = request.startAsync();
        context.setTimeout(0);
        final Client client = new Client(context);
        context.getResponse().getOutputStream().setWriteListener(client);
        clients.add(client);
    }

    @Override
    public void handleJcrPropertyScanResult(final @NotNull ScanResult scanResult, final @NotNull String path, final int propertyType, final @Nullable String userId) {
        final String data = json(scanResult, path, null, propertyType, userId);
        addEvent(JCR_RESULT_EVENT_TYPE, data);
    }

    @Override
    public void handleJcrPropertyScanResult(final @NotNull ScanResult scanResult, final @NotNull String path, final int index, final int propertyType, final @Nullable String userId) {
        final String data = json(scanResult, path, index, propertyType, userId);
        addEvent(JCR_RESULT_EVENT_TYPE, data);
    }

    private void addEvent(final String type, final String data) {
        final Event event = new Event(type, data);
        clients.iterator().forEachRemaining(client -> client.addEvent(event));
    }

    private class Event {

        final String type;

        final String data;

        Event(final String type, final String data) {
            this.type = type;
            this.data = data;
        }

    }

    private class Client implements AsyncListener, WriteListener {

        private final AsyncContext context;

        private final Queue<Event> events = new ConcurrentLinkedQueue<>();

        private Client(final AsyncContext context) {
            this.context = context;
            context.addListener(this);
        }

        @Override
        public void onComplete(final AsyncEvent event) throws IOException {
            logger.debug("on complete: {}", event.getAsyncContext());
            clients.remove(this);
        }

        @Override
        public void onTimeout(final AsyncEvent event) throws IOException {
            logger.debug("on timeout: {}", event.getAsyncContext());
            clients.remove(this);
        }

        @Override
        public void onError(final AsyncEvent event) throws IOException {
            logger.debug("on error: {}", event.getAsyncContext());
            clients.remove(this);
        }

        @Override
        public void onStartAsync(final AsyncEvent event) throws IOException {
            logger.debug("on start async: {}", event.getAsyncContext());
        }

        @Override
        public void onWritePossible() throws IOException {
            final ServletOutputStream outputStream = context.getResponse().getOutputStream();
            while (outputStream.isReady() && events.peek() != null) {
                final Event event = events.poll();
                final String data = String.format("event: %s\ndata: %s\n\n", event.type, event.data);
                outputStream.write(data.getBytes(StandardCharsets.UTF_8));
                flushIfReady(outputStream);
            }
            flushIfReady(outputStream);
        }

        @Override
        public void onError(final Throwable t) {
            logger.error("on error: {}", t.getMessage(), t);
            clients.remove(this);
            context.complete();
        }

        private void flushIfReady(final ServletOutputStream outputStream) throws IOException {
            if (outputStream.isReady()) {
                outputStream.flush();
            }
        }

        private void addEvent(final Event event) {
            final long count = counter.incrementAndGet();
            logger.info("adding event: {}", count);
            events.add(event);
            try {
                onWritePossible();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

    }

}
