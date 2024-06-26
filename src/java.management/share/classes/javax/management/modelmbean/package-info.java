/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */


/**
 * <p>Provides the definition of the ModelMBean classes.  A Model
 * MBean is an MBean that acts as a bridge between the management
 * interface and the underlying managed resource.  Both the
 * management interface and the managed resource are specified as
 * Java objects.  The same Model MBean implementation can be
 * reused many times with different management interfaces and
 * managed resources, and it can provide common functionality
 * such as persistence and caching.</p>
 *
 * <p>A Model MBean implements the {@link
 * javax.management.modelmbean.ModelMBean ModelMBean} interface.
 * It is a {@link javax.management.DynamicMBean DynamicMBean}
 * whose {@link javax.management.DynamicMBean#getMBeanInfo()
 * getMBeanInfo} method returns an object implementing {@link
 * javax.management.modelmbean.ModelMBeanInfo
 * ModelMBeanInfo}.</p>
 *
 * <p>Every MBean has an {@link javax.management.MBeanInfo
 * MBeanInfo} with information about the MBean itself, and its
 * attributes, operations, constructors, and notifications.  A
 * Model MBean augments this <code>MBeanInfo</code> with {@link
 * javax.management.Descriptor Descriptor}s that encode
 * additional information in the form of (key,value) pairs.
 * Usually, <code>Descriptor</code>s are instances of {@link
 * javax.management.modelmbean.DescriptorSupport
 * DescriptorSupport}.</p>
 *
 * <p>The class {@link
 * javax.management.modelmbean.RequiredModelMBean
 * RequiredModelMBean} provides a standard Model MBean
 * implementation.</p>
 *
 * <p>The following example shows a Model MBean being used to make
 * the <code>get</code> method of a <code>HashMap</code>
 * available for management through an MBean server.  No other
 * methods are available through the MBean server.  There is
 * nothing special about <code>HashMap</code> here.  Public
 * methods from any public class can be exposed for management in
 * the same way.</p>
 *
 * <pre>
 * import java.lang.reflect.Method;
 * import java.util.HashMap;
 * import javax.management.*;
 * import javax.management.modelmbean.*;
 *
 * // ...
 *
 * MBeanServer mbs = MBeanServerFactory.createMBeanServer();
 * // The MBean Server
 *
 * HashMap map = new HashMap();
 * // The resource that will be managed
 *
 * // Construct the management interface for the Model MBean
 * Method getMethod = HashMap.class.getMethod("get", new Class[] {Object.class});
 * ModelMBeanOperationInfo getInfo =
 *     new ModelMBeanOperationInfo("Get value for key", getMethod);
 * ModelMBeanInfo mmbi =
 *     new ModelMBeanInfoSupport(HashMap.class.getName(),
 *                   "Map of keys and values",
 *                   null,  // no attributes
 *                   null,  // no constructors
 *                   new ModelMBeanOperationInfo[] {getInfo},
 *                   null); // no notifications
 *
 * // Make the Model MBean and link it to the resource
 * ModelMBean mmb = new RequiredModelMBean(mmbi);
 * mmb.setManagedResource(map, "ObjectReference");
 *
 * // Register the Model MBean in the MBean Server
 * ObjectName mapName = new ObjectName(":type=Map,name=whatever");
 * mbs.registerMBean(mmb, mapName);
 *
 * // Resource can evolve independently of the MBean
 * map.put("key", "value");
 *
 * // Can access the "get" method through the MBean Server
 * mbs.invoke(mapName, "get", new Object[] {"key"}, new String[] {Object.class.getName()});
 * // returns "value"
 *     </pre>
 *
 * <h2><a id="spec">Package Specification</a></h2>
 *
 * <ul>
 *   <li>See the <i>JMX 1.4 Specification</i>
 *      <a href="https://jcp.org/aboutJava/communityprocess/mrel/jsr160/index2.html">
 *          JMX Specification, version 1.4</a>
 * </ul>
 *
 * @since 1.5
 */
package javax.management.modelmbean;
