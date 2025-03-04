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
 *
 */

package org.apache.skywalking.apm.plugin.jdk.threading.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.StaticMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;
import org.apache.skywalking.apm.agent.core.plugin.match.IndirectMatch;
import org.apache.skywalking.apm.agent.core.plugin.match.logical.LogicalMatchOperation;
import org.apache.skywalking.apm.plugin.jdk.threading.ThreadingConfig;

import java.util.Arrays;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static org.apache.skywalking.apm.agent.core.plugin.match.HierarchyMatch.byHierarchyMatch;

public class CallableInstrumentation extends ClassEnhancePluginDefine {
    private static final String CALLABLE_CLASS = "java.util.concurrent.Callable";
    private static final String CALLABLE_CLASS_INTERCEPTOR = "org.apache.skywalking.apm.plugin.jdk.threading.ThreadingConstructorInterceptor";

    private static final String CALLABLE_CALL_METHOD = "call";
    private static final String CALLABLE_CALL_METHOD_INTERCEPTOR = "org.apache.skywalking.apm.plugin.jdk.threading.ThreadingMethodInterceptor";

    @Override
    protected ClassMatch enhanceClass() {
        final IndirectMatch prefixMatches = ThreadingConfig.prefixesMatchesForJdkThreading();

        if (prefixMatches == null) {
            return null;
        }

        return LogicalMatchOperation.and(prefixMatches, byHierarchyMatch(Arrays.asList(CALLABLE_CALL_METHOD), CALLABLE_CLASS));
    }

    @Override
    public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[]{
                new ConstructorInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getConstructorMatcher() {
                        return any();
                    }

                    @Override
                    public String getConstructorInterceptor() {
                        return CALLABLE_CLASS_INTERCEPTOR;
                    }
                }
        };
    }

    @Override
    public InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        return new InstanceMethodsInterceptPoint[]{
                new InstanceMethodsInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getMethodsMatcher() {
                        return named(CALLABLE_CALL_METHOD).and(takesArguments(0));
                    }

                    @Override
                    public String getMethodsInterceptor() {
                        return CALLABLE_CALL_METHOD_INTERCEPTOR;
                    }

                    @Override
                    public boolean isOverrideArgs() {
                        return false;
                    }
                }
        };
    }

    @Override
    public StaticMethodsInterceptPoint[] getStaticMethodsInterceptPoints() {
        return new StaticMethodsInterceptPoint[0];
    }

    @Override
    public boolean isBootstrapInstrumentation() {
        return true;
    }
}