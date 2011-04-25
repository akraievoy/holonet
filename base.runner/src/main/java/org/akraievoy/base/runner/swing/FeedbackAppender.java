/*
 Copyright 2011 Anton Kraievoy akraievoy@gmail.com
 This file is part of Holonet.

 Holonet is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Holonet is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Holonet. If not, see <http://www.gnu.org/licenses/>.
 */

package org.akraievoy.base.runner.swing;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class FeedbackAppender extends AppenderSkeleton {
  protected static final String PATTERN_DEFAULT = "%d{HH:mm:ss.SSS} %c{1} %m";

  protected final JTextPane outputPane;
  protected Map<Level, Style> styles = new HashMap<Level, Style>();

  //	please access this value only from Swing worker thread...
  protected int traceStart = -1;

  public FeedbackAppender(JTextPane outputPane) {
    this(outputPane, PATTERN_DEFAULT);
  }

  public FeedbackAppender(JTextPane outputPane, final String patternDefault) {
    this.outputPane = outputPane;

    setLayout(new PatternLayout(patternDefault));

    init();
  }

  protected void init() {
    final Style fatal = outputPane.addStyle("fatal", null);
    StyleConstants.setForeground(fatal, Color.RED.brighter());
    StyleConstants.setBold(fatal, true);
    styles.put(Level.FATAL, fatal);

    final Style error = outputPane.addStyle("error", null);
    StyleConstants.setForeground(error, Color.RED);
    StyleConstants.setBold(fatal, true);
    styles.put(Level.ERROR, error);

    final Style warn = outputPane.addStyle("warn", null);
    StyleConstants.setForeground(warn, Color.ORANGE);
    StyleConstants.setBold(warn, true);
    styles.put(Level.WARN, warn);

    final Style info = outputPane.addStyle("info", null);
    StyleConstants.setForeground(info, Color.GRAY);
    styles.put(Level.INFO, info);

    final Style debug = outputPane.addStyle("debug", null);
    StyleConstants.setForeground(debug, Color.DARK_GRAY);
    styles.put(Level.DEBUG, debug);

    final Style trace = outputPane.addStyle("trace", null);
    StyleConstants.setForeground(trace, Color.DARK_GRAY.darker());
    styles.put(Level.TRACE, trace);
  }

  protected void append(final LoggingEvent event) {
    final Style style = styles.get(event.getLevel());
    final String message = getLayout().format(event);

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        final Document document = outputPane.getDocument();

        try {
          final int lastTraceStart = traceStart;
          if (lastTraceStart > 0) {
            document.remove(lastTraceStart, document.getLength() - lastTraceStart);
            traceStart = -1;
          }
        } catch (BadLocationException e) {
          //	ignoring: appender should not use logging itself
        }

        try {
          final int prevLength = document.getLength();

          document.insertString(
              prevLength,
              message + "\n",
              style.copyAttributes()
          );

          if (Level.DEBUG.isGreaterOrEqual(event.getLevel())) {
            traceStart = prevLength;
          }
        } catch (BadLocationException e) {
          //	ignoring: appender should not use logging itself
        }
      }
    });
  }

  public boolean requiresLayout() {
    return true;
  }

  public void close() {
    //	nothing to do
  }
}
