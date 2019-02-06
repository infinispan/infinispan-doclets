package org.infinispan.doclets.html;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates HTML documents
 *
 * @author Tristan Tarrant
 */
public abstract class HtmlGenerator {
   Map<String, String> subs;

   public HtmlGenerator(String title, String description, String keywords) {
      subs = new HashMap<>();
      subs.put("title", title);
      subs.put("description", description);
      subs.put("keywords", keywords);
   }

   public void generateHtml(String fileName) throws IOException {

      try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fileName), StandardCharsets.UTF_8))) {
         copyToWriter(pw, "header.html");
         generateContents(pw);
         copyToWriter(pw, "footer.html");
      }
   }

   protected abstract void generateContents(PrintWriter writer);

   protected boolean isValid(String s) {
      return s != null && s.trim().length() != 0;
   }

   private void copyToWriter(PrintWriter pw, String resourceName) throws IOException {
      Pattern pattern = Pattern.compile("%%(\\w+)%%");
      try (BufferedReader r = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(resourceName)))) {
         for(String line = r.readLine(); line != null; line = r.readLine()) {
            Matcher matcher = pattern.matcher(line);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
               matcher.appendReplacement(sb, subs.get(matcher.group(1)));
            }
            matcher.appendTail(sb);
            pw.println(sb);
         }
      }
   }
}
