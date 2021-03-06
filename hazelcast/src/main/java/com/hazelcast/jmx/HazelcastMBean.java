/*
 * Copyright (c) 2008-2013, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.jmx;

import javax.management.*;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.regex.Pattern;

/**
 * @author ali 1/30/13
 */
public abstract class HazelcastMBean<T> implements DynamicMBean, MBeanRegistration {

    public static final String DOMAIN = "com.hazelcast";

    protected HashMap<String, BeanInfo> attributeMap = new HashMap<String, BeanInfo>();
    protected HashMap<String, BeanInfo> operationMap = new HashMap<String, BeanInfo>();

    final T managedObject;

    final ManagementService service;

    String description;

    ObjectName objectName;

    protected HazelcastMBean(T managedObject, ManagementService service) {
        this.managedObject = managedObject;
        this.service = service;
    }

    private void scan() throws Exception {
        ManagedDescription descAnn = getClass().getAnnotation(ManagedDescription.class);
        if (descAnn != null){
            description = descAnn.value();
        }

        for (Method method: getClass().getMethods()){

            if (method.isAnnotationPresent(ManagedAnnotation.class)){
                ManagedAnnotation ann = method.getAnnotation(ManagedAnnotation.class);
                String name = ann.value();
                if (name.isEmpty()){
                    throw new IllegalArgumentException("Name cannot be empty!");
                }
                boolean operation = ann.operation();
                HashMap<String, BeanInfo> map = operation ? operationMap : attributeMap;
                if (map.containsKey(name)){
                    throw new IllegalArgumentException("Duplicate name: " + name);
                }
                descAnn = method.getAnnotation(ManagedDescription.class);
                String desc = null;
                if (descAnn != null){
                    desc = descAnn.value();
                }
                map.put(name, new BeanInfo(name, desc, method));
            }
        }
    }

    public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
        if (attribute == null || attribute.length() == 0)
            throw new NullPointerException("Invalid null attribute requested");
        BeanInfo info = attributeMap.get(attribute);
        try {
            return info.method.invoke(this);
        } catch (Exception e) {
            throw new ReflectionException(e);
        }
    }

    public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        throw new UnsupportedOperationException();
    }

    public AttributeList getAttributes(String[] attributes) {
        AttributeList list = new AttributeList(attributes.length);
        try {
            for (String attribute : attributes) {
                list.add(new Attribute(attribute, getAttribute(attribute)));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }
        return list;
    }

    public AttributeList setAttributes(AttributeList attributes) {
        throw new UnsupportedOperationException();
    }

    public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException {
        if (actionName == null || actionName.isEmpty()){
            throw new IllegalArgumentException("Empty actionName");
        }
        BeanInfo info = operationMap.get(actionName);
        if (info == null){
            throw new UnsupportedOperationException("Operation: " + actionName + " not registered");
        }
        try {
            return info.method.invoke(this, params);
        } catch (Exception e) {
            throw new ReflectionException(e);
        }
    }

    public MBeanInfo getMBeanInfo() {
        return new MBeanInfo(managedObject.getClass().getName(), description, attributeInfos(), null, operationInfos(), null);
    }

    private MBeanAttributeInfo[] attributeInfos(){
        MBeanAttributeInfo[] array = new MBeanAttributeInfo[attributeMap.size()];
        int i = 0;
        for (BeanInfo beanInfo: attributeMap.values()){
            array[i++] = beanInfo.getAttributeInfo();
        }
        return array;
    }

    private MBeanOperationInfo[] operationInfos(){
        MBeanOperationInfo[] array = new MBeanOperationInfo[operationMap.size()];
        int i = 0;
        for (BeanInfo beanInfo: operationMap.values()){
            array[i++] = beanInfo.getOperationInfo();
        }
        return array;
    }

    ObjectName createObjectName(String type, String name){
        Hashtable<String, String> properties = new Hashtable<String, String>(3);
        properties.put("instance", quote(service.instance.getName()));
        if (type != null){
            properties.put("type", quote(type));
        }
        if (name != null){
            properties.put("name", quote(name));
        }
        try {
            return new ObjectName(DOMAIN, properties);
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException();
        }
    }

    private String quote(String text){
        return Pattern.compile("[:\",=*?]")
                .matcher(text)
                .find() ? ObjectName.quote(text) : text;
    }

    private class BeanInfo {

        final String name;
        final String description;
        transient Method method;

        public BeanInfo(String name, String description, Method method){
            this.name = name;
            this.description = description;
            this.method = method;
        }

        public MBeanAttributeInfo getAttributeInfo() {
            try {
                return new MBeanAttributeInfo(name, description, method, null);
            } catch (IntrospectionException e) {
                throw new IllegalArgumentException();
            }
        }

        public MBeanOperationInfo getOperationInfo(){
            return new MBeanOperationInfo(description, method);
        }
    }

    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
        try {
            scan();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
        return objectName;
    }

    public void postRegister(Boolean registrationDone) {
    }

    public void preDeregister() throws Exception {
    }

    public void postDeregister() {
    }
}
