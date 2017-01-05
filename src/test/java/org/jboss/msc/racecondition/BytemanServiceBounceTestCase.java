package org.jboss.msc.racecondition;

import org.jboss.byteman.contrib.bmunit.BMScript;
import org.jboss.byteman.contrib.bmunit.BMUnitConfig;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Ignore;
import org.junit.runner.RunWith;

/**
 * @author Stuart Douglas
 */
@RunWith(BMUnitRunner.class)
@BMScript(dir = "src/test/resources")
@Ignore("This tests needs org.jboss.msc.directionalExecutor set to pass")
public class BytemanServiceBounceTestCase extends ServiceBounceTestCase {

    @Override
    protected int getIterationCount() {
        return 1;
    }
}
