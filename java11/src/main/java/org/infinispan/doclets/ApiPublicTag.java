package org.infinispan.doclets;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Element;

import com.sun.source.doctree.DocTree;

import jdk.javadoc.doclet.Taglet;

/**
 * @since 14.0
 **/
public class ApiPublicTag implements Taglet {
   public static final String PUBLIC_TAG = "@api.public";
   @Override
   public Set<Location> getAllowedLocations() {
      return EnumSet.allOf(Location.class);
   }

   @Override
   public boolean isInlineTag() {
      return false;
   }

   @Override
   public String getName() {
      return PUBLIC_TAG.substring(1);
   }

   @Override
   public String toString(List<? extends DocTree> tags, Element element) {
      return "";
   }

   public static void register(Map tagletMap) {
      ApiPublicTag tag = new ApiPublicTag();
      Taglet t = (Taglet) tagletMap.get(tag.getName());
      if (t != null) {
         tagletMap.remove(tag.getName());
      }
      tagletMap.put(tag.getName(), tag);
   }
}
