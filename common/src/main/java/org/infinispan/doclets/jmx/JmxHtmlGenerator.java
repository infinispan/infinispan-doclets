package org.infinispan.doclets.jmx;

import java.io.PrintWriter;
import java.util.List;

import org.infinispan.doclets.html.HtmlGenerator;

public class JmxHtmlGenerator extends HtmlGenerator {
   private final String title;
   List<MBeanComponent> components;

   public JmxHtmlGenerator(String title, String description, String keywords, List<MBeanComponent> components) {
      super(title, description, keywords);
      this.title = title;
      this.components = components;
   }

   @Override
   protected void generateContents(PrintWriter w) {
      // index of components
      w.println("<div class=\"header\">");
      w.printf("<h1>%s</h1>", title);
      w.println("</div>");
      w.println("<div class=\"contentContainer\">");
      w.println("<table class=\"typeSummary\">");
      w.println("<caption><span>MBean Summary</span><span class=\"tabEnd\">&nbsp;</span></caption>");
      w.println("<tr>\n" +
            "<th class=\"colFirst\" scope=\"col\">Name</th>\n" +
            "<th class=\"colLast\" scope=\"col\">Description</th>\n" +
            "</tr>");
      w.println("<tbody>");
      int row = 0;
      for (MBeanComponent mbean : components) {
         w.printf("<tr class=\"%s\">", rowClass(row++));
         w.printf("<th class=\"colFirst\" scope=\"row\"><a href=\"#%s\">%s</a></th><td>%s</td></tr>", mbean.name, mbean.name, mbean.desc);
      }
      w.println("</tbody></table>");

      w.printf("<div class=\"details\"><ul class=\"blockList\"><li class=\"blockList\">");
      for (MBeanComponent mbean : components) {
         w.printf("<a name=\"%s\" />", mbean.name);
         w.printf("<section role=\"region\"><ul class=\"blockList\"><li class=\"blockList\">");
         w.printf("<h3><a href=\"%s\">%s</a></h3>", toURL(mbean.className), mbean.name);
         w.printf("<p>%s</p>", mbean.desc);

         if (!mbean.attributes.isEmpty()) {
            // Attributes
            w.println("<table class=\"typeSummary\">");
            w.printf("<caption><span>Attributes</span><span class=\"tabEnd\">&nbsp;</span></caption>");
            w.println("<tr><th class=\"colFirst\" scope=\"col\">Name</th><th class=\"colSecond\" scope=\"col\">Description</th><th class=\"colSecond\" scope=\"col\">Type</th><th class=\"colLast\" scope=\"col\">Writable</th></tr>");
            w.println("<tbody>");
            row = 0;
            for (MBeanAttribute attr : mbean.attributes) {
               w.printf("<tr class=\"%s\">", rowClass(row++));
               w.printf("<td><tt>%s</tt></td>", attr.name);
               w.printf("<td>%s</td>", attr.desc);
               w.printf("<td><tt>%s</tt></td>", attr.type);
               w.printf("<td>%s</td>", attr.writable);
               w.println("</tr>");
            }
            w.println("</tbody></table>");
         }

         if (!mbean.operations.isEmpty()) {
            // Operations
            w.println("<table class=\"typeSummary\">");
            w.printf("<caption><span>Operations</span><span class=\"tabEnd\">&nbsp;</span></caption>");
            w.println("<tr><th class=\"colFirst\" scope=\"col\">Name</th><th  class=\"colSecond\" scope=\"col\">Description</th><th class=\"colLast\" scope=\"col\">Signature</th></tr>");
            w.println("<tbody>");
            row = 0;
            for (MBeanOperation operation : mbean.operations) {
               w.printf("<tr class=\"%s\">", rowClass(row++));
               w.printf("<td><tt>%s</tt></td>", operation.name);
               w.printf("<td>%s</td>", operation.desc);
               w.printf("<td><tt>%s</tt></td>", generateSignature(operation));
               w.println("</tr>");
            }
            w.println("</tbody></table>");
         }

         w.println("</li></ul></section>");
      }
      w.println("</li></ul></div>");
      w.println("</div>");
   }

   private String rowClass(int i) {
      return (i % 2 == 0) ? "altColor" : "rowColor";
   }

   private String toURL(String fqcn) {
      return fqcn.replace(".", "/") + ".html";
   }

   private String generateSignature(MBeanOperation op) {
      // <retType> <name>(<args>)
      StringBuilder sb = new StringBuilder();
      if (isValid(op.returnType))
         sb.append(escapeHTML(op.returnType));
      else
         sb.append("void");

      sb.append(" ").append(op.name);
      if (isValid(op.signature))
         sb.append("(").append(escapeHTML(op.signature)).append(")");
      else
         sb.append("()");
      return sb.toString();
   }

   public static String escapeHTML(String s) {
      StringBuilder out = new StringBuilder(Math.max(16, s.length()));
      for (int i = 0; i < s.length(); i++) {
         char c = s.charAt(i);
         if (c > 127 || c == '"' || c == '<' || c == '>' || c == '&') {
            out.append("&#");
            out.append((int) c);
            out.append(';');
         } else {
            out.append(c);
         }
      }
      return out.toString();
   }
}
