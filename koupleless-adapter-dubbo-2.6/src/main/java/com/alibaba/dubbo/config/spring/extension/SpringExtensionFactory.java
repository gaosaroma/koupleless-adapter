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
package com.alibaba.dubbo.config.spring.extension;

import com.alibaba.dubbo.common.extension.ExtensionFactory;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConcurrentHashSet;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SpringExtensionFactory
 */
public class SpringExtensionFactory implements ExtensionFactory {
    private static final Logger                                    logger                  = LoggerFactory
        .getLogger(SpringExtensionFactory.class);

    private static final Map<ClassLoader, Set<ApplicationContext>> contextsWithClassLoader = new ConcurrentHashMap<>();

    public static void addApplicationContext(ApplicationContext context) {
        getContexts().add(context);
    }

    public static void removeApplicationContext(ApplicationContext context) {
        getContexts().remove(context);
    }

    // currently for test purpose
    public static void clearContexts() {

        getContexts().clear();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getExtension(Class<T> type, String name) {
        for (ApplicationContext context : getContexts()) {
            if (context.containsBean(name)) {
                Object bean = context.getBean(name);
                if (type.isInstance(bean)) {
                    return (T) bean;
                }
            }
        }

        logger.warn("No spring extension(bean) named:" + name
                    + ", try to find an extension(bean) of type " + type.getName());

        for (ApplicationContext context : getContexts()) {
            try {
                return context.getBean(type);
            } catch (NoUniqueBeanDefinitionException multiBeanExe) {
                throw multiBeanExe;
            } catch (NoSuchBeanDefinitionException noBeanExe) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Error when get spring extension(bean) for type:" + type.getName(),
                        noBeanExe);
                }
            }
        }

        logger.warn("No spring extension(bean) named:" + name + ", type:" + type.getName()
                    + " found, stop get bean.");

        return null;
    }

    private static ClassLoader findClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader != null)
            return classLoader;
        return SpringExtensionFactory.class.getClassLoader();
    }

    private static Set<ApplicationContext> getContexts() {
        ClassLoader classLoader = findClassLoader();
        Set<ApplicationContext> contexts = null;
        if ((contexts = contextsWithClassLoader.get(classLoader)) == null) {
            contextsWithClassLoader.putIfAbsent(classLoader, new ConcurrentHashSet<>());
            contexts = contextsWithClassLoader.get(classLoader);
        }
        ;
        return contexts;
    }

}
