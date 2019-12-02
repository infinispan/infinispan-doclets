package org.infinispan.doclets.sample;

import org.infinispan.jmx.annotations.MBean;
import org.infinispan.jmx.annotations.ManagedAttribute;

@SuppressWarnings("unused")
@MBean(description = "Sample MBean A", objectName = "MBeanB")
public class SampleMBeanB {
   @ManagedAttribute(writable = true, description = "A field", displayName = "Field A")
   Integer fieldA;
}
