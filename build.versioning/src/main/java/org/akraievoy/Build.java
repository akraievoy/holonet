package org.akraievoy;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class Build {
  protected static final String STAMP_PATTERN = "yyMMddHHmmssSSSZ";
  protected static final DateFormat STAMP_FORMAT = new SimpleDateFormat(STAMP_PATTERN);

  protected static final Date loadStamp = new Date();

  protected static final Properties props = new Properties();
  protected static boolean propsLoaded = false;

  public synchronized static Properties getProps() {
    if (propsLoaded) {
      return props;
    }

    try {
      final InputStream propsStream = Build.class.getResourceAsStream("build.properties");
      if (propsStream != null) {
        props.load(propsStream);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    propsLoaded = true;
    return props;
  }

  public static String getBuild() {
    return getProps().getProperty("org.akraievoy.build", "unknown");
  }

  public static Date getStamp() {
    final String stampString = getProps().getProperty("org.akraievoy.timestamp");
    if (stampString == null) {
      return loadStamp;
    } else {
      try {
        return STAMP_FORMAT.parse(stampString);
      } catch (ParseException e) {
        return loadStamp;
      }
    }
  }
}
