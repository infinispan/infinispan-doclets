package org.infinispan.doclets.sample;

import org.infinispan.jmx.annotations.MBean;
import org.infinispan.jmx.annotations.ManagedAttribute;

@SuppressWarnings("unused")
@MBean(description = "Sample MBean A", objectName = "MBeanB")
public class SampleMBeanB {

   private Integer fieldA;

   @ManagedAttribute(writable = true, description = "A field", displayName = "Field A")
   public Integer getFieldA() {
      return fieldA;
   }

   public void setFieldA(Integer fieldA) {
      this.fieldA = fieldA;
   }
}
