package org.infinispan.doclets.sample;

import java.util.Collection;
import java.util.Collections;

import org.infinispan.jmx.annotations.MBean;
import org.infinispan.jmx.annotations.ManagedAttribute;
import org.infinispan.jmx.annotations.ManagedOperation;

/**
 * SampleMBeanA
 */
@SuppressWarnings("unused")
@MBean(description = "Sample MBean A", objectName = "MBeanA")
public class SampleMBeanA {

   private String stuff = "stuff";

   /**
    * Get some stuff
    * @return stuff
    */
   @ManagedAttribute(description = "Gets some stuff", writable = true)
   public String getSomeStuff() {
      return stuff;
   }

   /**
    * Set some stuff
    * @param stuff some stuff
    */
   public void setSomeStuff(String stuff) {
      this.stuff = stuff;
   }

   /**
    * Do stuff
    * @param x a value
    * @param i another value
    */
   @ManagedOperation(description = "Do some stuff", displayName = "Do it", name = "Do stuff")
   public void doStuff(String x, int i) {
      // Do stuff !
   }

   /**
    * Do more stuff
    * @param x a value
    * @param i another value
    * @return more stuff
    */
   @ManagedOperation(description = "Do more stuff", displayName = "Do it", name = "Do more stuff")
   public int doMoreStuff(String x, int i) {
      // Do stuff !
      return 0;
   }

   /**
    * Do long stuff
    * @param x a value
    * @param i another value
    * @return long stuff
    */
   @ManagedOperation(description = "Do long stuff", displayName = "Do it", name = "Do long stuff")
   public Collection<String> doLongStuff(String x, int i) {
      return Collections.emptyList();
   }
}
