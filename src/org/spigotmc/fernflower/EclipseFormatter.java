package org.spigotmc.fernflower;

import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.internal.core.util.SimpleDocument;
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatter;
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatterOptions;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;

public class EclipseFormatter {

  private static final CodeFormatter formatter;

  static {
    DefaultCodeFormatterOptions options = new DefaultCodeFormatterOptions(null);
    options.setJavaConventionsSettings();

    options.tab_char = DefaultCodeFormatterOptions.SPACE;
    options.page_width = 0;

    options.insert_new_line_after_label = true;
    options.insert_new_line_in_empty_method_body = false;
    options.insert_new_line_in_empty_type_declaration = false;

    options.insert_space_before_closing_brace_in_array_initializer = false; // Compatability
    // options.blank_lines_before_first_class_body_declaration = 1; // Needed later

    formatter = new DefaultCodeFormatter(options);
  }

  public static String format(String contents) throws BadLocationException {
    TextEdit formatted = formatter.format(CodeFormatter.K_COMPILATION_UNIT, contents, 0, contents.length(), 0, "\n");

    IDocument doc = new SimpleDocument(contents);
    formatted.apply(doc);

    return doc.get();
  }
}
