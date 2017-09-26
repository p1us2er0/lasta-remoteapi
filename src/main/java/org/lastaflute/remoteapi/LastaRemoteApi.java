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
package org.lastaflute.remoteapi;

import java.lang.reflect.Type;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.optional.OptionalThing;
import org.dbflute.remoteapi.FlutyRemoteApi;
import org.dbflute.remoteapi.FlutyRemoteApiRule;
import org.dbflute.remoteapi.exception.RemoteApiRequestValidationErrorException;
import org.dbflute.remoteapi.exception.RemoteApiResponseValidationErrorException;
import org.lastaflute.core.message.UserMessages;
import org.lastaflute.core.message.supplier.UserMessagesCreator;
import org.lastaflute.core.util.Lato;
import org.lastaflute.web.response.ApiResponse;
import org.lastaflute.web.response.JsonResponse;
import org.lastaflute.web.ruts.process.exception.ResponseBeanValidationErrorException;
import org.lastaflute.web.ruts.process.validatebean.ResponseSimpleBeanValidator;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.validation.ActionValidator;
import org.lastaflute.web.validation.exception.ValidationStoppedException;

/**
 * @author jflute
 * @author awaawa
 * @author inoue
 */
public class LastaRemoteApi extends FlutyRemoteApi {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected RequestManager requestManager; // not null after set, for validation and various purpose

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public LastaRemoteApi(Consumer<FlutyRemoteApiRule> defaultOpLambda, Object callerExp) {
        super(defaultOpLambda, callerExp);
    }

    public void acceptRequestManager(RequestManager requestManager) {
        this.requestManager = requestManager;
    }

    // ===================================================================================
    //                                                                          Validation
    //                                                                          ==========
    @Override
    protected void validateParam(Type beanType, String urlBase, String actionPath, Object[] pathVariables, Object param) {
        try {
            createTransferredBeanValidator().validate(param);
        } catch (ResponseBeanValidationErrorException e) {
            throwRemoteApiRequestValidationErrorException(beanType, urlBase, actionPath, pathVariables, param, e);
        }
    }

    @Override
    protected void validateReturn(Type beanType, String url, OptionalThing<Object> form, int httpStatus, OptionalThing<String> body,
            Object ret, FlutyRemoteApiRule rule) {
        try {
            createTransferredBeanValidator().validate(ret);
        } catch (ResponseBeanValidationErrorException | ValidationStoppedException e) {
            throwRemoteApiResponseValidationErrorException(beanType, url, form, httpStatus, body, ret, e);
        }
    }

    protected ResponseSimpleBeanValidator createTransferredBeanValidator() {
        // use ActionValidator #for_now (with suppressing request process) by jflute
        return new ResponseSimpleBeanValidator(requestManager, callerExp, isTransferredBeanValidationAsWarning()) {
            @Override
            protected ActionValidator<UserMessages> createActionValidator() {
                final Class<?>[] groups = getValidatorGroups().orElse(ActionValidator.DEFAULT_GROUPS);
                return newActionValidator(() -> new UserMessages(), groups);
            }
        };
    }

    protected boolean isTransferredBeanValidationAsWarning() {
        return false;
    }

    protected ActionValidator<UserMessages> newActionValidator(UserMessagesCreator<UserMessages> messagesCreator, Class<?>[] groups) {
        return new ActionValidator<UserMessages>(requestManager, messagesCreator, groups) {
            @Override
            protected Locale provideUserLocale() { // not to use request
                return Locale.ENGLISH; // fixedly English because of non-user validation
            }

            @Override
            protected ApiResponse processApiValidationError() {
                return JsonResponse.asEmptyBody(); // first, may not be called
            }
        };
    }

    protected void throwRemoteApiRequestValidationErrorException(Type beanType, String urlBase, String actionPath, Object[] pathVariables,
            Object param, ResponseBeanValidationErrorException e) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Validation Error as HTTP Request from the remote API.");
        final StringBuilder sb = new StringBuilder();
        sb.append(urlBase).append(actionPath).append(actionPath.endsWith("/") ? "" : "/");
        if (pathVariables != null && pathVariables.length > 0) {
            sb.append(Stream.of(pathVariables).map(el -> el.toString()).collect(Collectors.joining("/"))); // simple for debug message
        }
        final String url = sb.toString();
        setupRequestInfo(br, beanType, url, OptionalThing.of(param));
        final String msg = br.buildExceptionMessage();
        throw new RemoteApiRequestValidationErrorException(msg, e);
    }

    protected void throwRemoteApiResponseValidationErrorException(Type beanType, String url, OptionalThing<Object> param, int httpStatus,
            OptionalThing<String> body, Object ret, RuntimeException e) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Validation Error as HTTP Response from the remote API.");
        setupRequestInfo(br, beanType, url, param);
        setupResponseInfo(br, httpStatus, body);
        setupReturnInfo(br, ret);
        final String msg = br.buildExceptionMessage();
        throw new RemoteApiResponseValidationErrorException(msg, e);
    }

    // ===================================================================================
    //                                                                      RemoteApi Rule
    //                                                                      ==============
    @Override
    protected FlutyRemoteApiRule newRemoteApiRule() {
        return new LastaRemoteApiRule();
    }

    // ===================================================================================
    //                                                                      Error Handling
    //                                                                      ==============
    @Override
    protected String convertBeanToDebugString(Object bean) {
        return Lato.string(bean); // because its toString() may not be overridden
    }
}
