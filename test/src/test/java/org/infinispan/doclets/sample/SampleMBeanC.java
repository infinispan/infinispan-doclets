package org.infinispan.doclets.sample;

import java.util.Collection;
import java.util.Collections;

import org.infinispan.jmx.annotations.MBean;
import org.infinispan.jmx.annotations.ManagedAttribute;
import org.infinispan.jmx.annotations.ManagedOperation;

/**
 * @private
 */
@SuppressWarnings("unused")
@MBean(description = "SampleMBeanC has the same objectName as MBeanA, just to confuse you", objectName = "MBeanA")
public class SampleMBeanC {
   private String stuff = "stuff";

   @ManagedAttribute(description = "Gets some stuff", writable = true)
   public String getSomeStuff() {
      return stuff;
   }

   public void setSomeStuff(String stuff) {
      this.stuff = stuff;
   }

   @ManagedOperation(description = "Do some stuff", displayName = "Do it", name = "Do stuff")
   public void doStuff(String x, int i) {
      // Do stuff !
   }

   @ManagedOperation(description = "Do more stuff", displayName = "Do it", name = "Do more stuff")
   public int doMoreStuff(String x, int i) {
      // Do stuff !
      return 0;
   }

   @ManagedOperation(description = "Do long stuff", displayName = "Do it", name = "Do long stuff")
   public Collection<String> doLongStuff(String x, int i) {
      return Collections.emptyList();
   }
}
