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

package org.apache.skywalking.apm.agent.core.plugin.match;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.matcher.ElementMatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Match the class by the given super class or interfaces.
 */
public class HierarchyMatch implements IndirectMatch {
    private final String[] parentTypes;
    private final List<String> methods;

    private HierarchyMatch(List<String> methods, String[] parentTypes) {
        if (parentTypes == null || parentTypes.length == 0) {
            throw new IllegalArgumentException("parentTypes is null");
        }
        this.parentTypes = parentTypes;
        this.methods = methods;
    }

    public static IndirectMatch byHierarchyMatch(String... parentTypes) {
        return new HierarchyMatch(null, parentTypes);
    }

    public static IndirectMatch byHierarchyMatch(List<String> methods, String... parentTypes) {
        return new HierarchyMatch(methods, parentTypes);
    }

    @Override
    public ElementMatcher.Junction buildJunction() {
        ElementMatcher.Junction junction = null;
        for (String superTypeName : parentTypes) {
            if (junction == null) {
                junction = buildSuperClassMatcher(superTypeName);
            } else {
                junction = junction.and(buildSuperClassMatcher(superTypeName));
            }
        }
        junction = junction.and(not(isInterface()));
        return junction;
    }

    private ElementMatcher.Junction buildSuperClassMatcher(String superTypeName) {
        return hasSuperType(named(superTypeName));
    }

    @Override
    public boolean isMatch(TypeDescription typeDescription) {
        List<String> parentTypes = new ArrayList<String>(Arrays.asList(this.parentTypes));

        TypeList.Generic implInterfaces = typeDescription.getInterfaces();
        for (TypeDescription.Generic implInterface : implInterfaces) {
            matchHierarchyClass(implInterface, parentTypes);
        }

        if (typeDescription.getSuperClass() != null) {
            matchHierarchyClass(typeDescription.getSuperClass(), parentTypes);
        }

        boolean flag = parentTypes.size() == 0;
        if (flag && methods != null && methods.size() > 0) {
            flag = false;
            MethodList<MethodDescription.InDefinedShape> data = typeDescription.getDeclaredMethods();
            for (MethodDescription.InDefinedShape t : data) {
                if (t.getActualName() != null && methods.contains(t.getActualName())) {
                    flag = true;
                    break;
                }
            }
            return flag;
        }
        return flag;

    }

    private void matchHierarchyClass(TypeDescription.Generic clazz, List<String> parentTypes) {
        parentTypes.remove(clazz.asRawType().getTypeName());
        if (parentTypes.size() == 0) {
            return;
        }

        for (TypeDescription.Generic generic : clazz.getInterfaces()) {
            matchHierarchyClass(generic, parentTypes);
        }

        TypeDescription.Generic superClazz = clazz.getSuperClass();
        if (superClazz != null && !clazz.getTypeName().equals("java.lang.Object")) {
            matchHierarchyClass(superClazz, parentTypes);
        }

    }
}
