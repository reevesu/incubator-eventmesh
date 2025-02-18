/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.eventmesh.runtime.admin.handler;

import org.apache.eventmesh.common.Constants;
import org.apache.eventmesh.common.utils.NetUtils;
import org.apache.eventmesh.runtime.admin.controller.HttpHandlerManager;
import org.apache.eventmesh.runtime.boot.EventMeshTCPServer;
import org.apache.eventmesh.runtime.common.EventHttpHandler;
import org.apache.eventmesh.runtime.constants.EventMeshConstants;
import org.apache.eventmesh.runtime.core.protocol.tcp.client.recommend.EventMeshRecommendImpl;
import org.apache.eventmesh.runtime.core.protocol.tcp.client.recommend.EventMeshRecommendStrategy;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;

/**
 * query recommend eventmesh
 */
@EventHttpHandler(path = "/eventMesh/recommend")
public class QueryRecommendEventMeshHandler extends AbstractHttpHandler {

    private final Logger logger = LoggerFactory.getLogger(QueryRecommendEventMeshHandler.class);

    private final EventMeshTCPServer eventMeshTCPServer;

    public QueryRecommendEventMeshHandler(EventMeshTCPServer eventMeshTCPServer, HttpHandlerManager httpHandlerManager) {
        super(httpHandlerManager);
        this.eventMeshTCPServer = eventMeshTCPServer;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        String result = "";
        try (OutputStream out = httpExchange.getResponseBody()) {
            if (!eventMeshTCPServer.getEventMeshTCPConfiguration().eventMeshServerRegistryEnable) {
                throw new Exception("registry enable config is false, not support");
            }
            String queryString = httpExchange.getRequestURI().getQuery();
            Map<String, String> queryStringInfo = NetUtils.formData2Dic(queryString);
            String group = queryStringInfo.get(EventMeshConstants.MANAGE_GROUP);
            String purpose = queryStringInfo.get(EventMeshConstants.MANAGE_PURPOSE);
            if (StringUtils.isBlank(group) || StringUtils.isBlank(purpose)) {
                NetUtils.sendSuccessResponseHeaders(httpExchange);
                result = "params illegal!";
                out.write(result.getBytes(Constants.DEFAULT_CHARSET));
                return;
            }

            EventMeshRecommendStrategy eventMeshRecommendStrategy = new EventMeshRecommendImpl(eventMeshTCPServer);
            String recommendEventMeshResult = eventMeshRecommendStrategy.calculateRecommendEventMesh(group, purpose);
            result = (recommendEventMeshResult == null) ? "null" : recommendEventMeshResult;
            logger.info("recommend eventmesh:{},group:{},purpose:{}", result, group, purpose);
            NetUtils.sendSuccessResponseHeaders(httpExchange);
            out.write(result.getBytes(Constants.DEFAULT_CHARSET));
        } catch (Exception e) {
            logger.error("QueryRecommendEventMeshHandler fail...", e);
        }
    }
}
