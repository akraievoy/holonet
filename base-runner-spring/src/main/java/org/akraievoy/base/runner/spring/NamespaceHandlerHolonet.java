package org.akraievoy.base.runner.spring;

import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.w3c.dom.Element;

/**
 * Register a simple handler to return a dummy object for any given ConfigIndex element.
 */
public class NamespaceHandlerHolonet extends NamespaceHandlerSupport {
  public void init() {
    registerBeanDefinitionParser("Experiment", new SingleBeanDefinitionParserContextIndex());
  }

  protected static class SingleBeanDefinitionParserContextIndex extends AbstractSingleBeanDefinitionParser {
    @Override
    protected String getBeanClassName(Element element) {
      return "java.lang.Object";
    }
  }
}
