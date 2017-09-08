/*
 * Copyright 2015-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.lastaflute.remoteapi.converter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.dbflute.optional.OptionalThing;
import org.dbflute.remoteapi.converter.FlutyResponseConverter;
import org.lastaflute.core.json.JsonManager;
import org.lastaflute.core.json.JsonMappingOption;
import org.lastaflute.core.json.engine.RealJsonEngine;
import org.lastaflute.web.servlet.request.RequestManager;

/**
 * @author inoue
 * @author jflute
 */
public class LaJsonResponseConverter implements FlutyResponseConverter {

    protected final RealJsonEngine jsonEngine; // to parse JSON response and request as JsonBody

    public LaJsonResponseConverter(RequestManager requestManager, JsonMappingOption jsonMappingOption) {
        this.jsonEngine = createJsonEngine(requestManager.getJsonManager(), jsonMappingOption);
    }

    protected RealJsonEngine createJsonEngine(JsonManager jsonManager, JsonMappingOption jsonMappingOption) {
        return jsonManager.newAnotherEngine(OptionalThing.ofNullable(jsonMappingOption, () -> {
            throw new IllegalStateException("Not found the json mapping option: " + jsonMappingOption);
        }));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <CONTENT extends Object> CONTENT toResult(String json, Type type) {
        if (type instanceof Class) {
            return (CONTENT) jsonEngine.fromJson(json, (Class<?>) type);
        }
        return (CONTENT) jsonEngine.fromJsonParameteried(json, (ParameterizedType) type);
    }
}
