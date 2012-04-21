package org.kohsuke;

import org.apache.commons.jexl.JexlContext;

/**
 * @author Kohsuke Kawaguchi
 */
public interface FastExpression {
    Object evaluate(JexlContext context);
}
