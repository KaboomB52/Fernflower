package org.jetbrains.java.decompiler.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class VarHelper {

  private static final Map<String, String[]> switches = new HashMap<String, String[]>();

  static {
    switches.put("byte", new String[]{
      "b"
    });
    switches.put("char", new String[]{
      "c"
    });
    switches.put("short", new String[]{
      "short"
    });
    switches.put("int", new String[]{
      "i", "j", "k", "l"
    });
    switches.put("long", new String[]{
      "i", "j", "k", "l"
    });
    switches.put("boolean", new String[]{
      "flag"
    });
    switches.put("double", new String[]{
      "d"
    });
    switches.put("float", new String[]{
      "f", "f" // Add twice because the original script is inconsistent
    });
    switches.put("String", new String[]{
      "s", "s" // Add twice because the original script is inconsistent
    });
    switches.put("Class", new String[]{
      "oclass"
    });
    switches.put("Long", new String[]{
      "olong"
    });
    switches.put("Byte", new String[]{
      "obyte"
    });
    switches.put("Short", new String[]{
      "oshort"
    });
    switches.put("Boolean", new String[]{
      "obool"
    });
    switches.put("Double", new String[]{
      "odouble"
    });
    switches.put("Float", new String[]{
      "ofloat"
    });
    switches.put("Long", new String[]{
      "olong"
    });
    switches.put("Enum", new String[]{
      "oenum"
    });
  }
  private final Set<String> used = new HashSet<String>();

  public String help(String name, String type, boolean varArgs) {
    if (type == null || !name.startsWith("var")) {
      return name;
    }

    while (type.contains( "<" )) {
      type = type.substring(0, type.indexOf('<')) + type.substring(type.lastIndexOf('>') + 1);
    }
    type = type.replace( '.', '_' );

    if (type.endsWith("]")) {
      type = "a" + type.substring(0, type.indexOf('['));
    } else if (varArgs) {
      type = "a" + type;
    }

    String[] remap = switches.get(type);
    if (remap == null) {
      remap = new String[]{
        type.toLowerCase(Locale.ENGLISH)
      };
    }

    for (int counter = 0;; counter++) {
      for (String subMap : remap) {
        String attempt = subMap + ((counter == 0 && !subMap.equals("short") && (remap.length > 1 || subMap.length() > 1)) ? "" : counter);
        if (!used.contains(attempt)) {
          used.add(attempt);
          return attempt;
        }
      }
    }
  }
}
