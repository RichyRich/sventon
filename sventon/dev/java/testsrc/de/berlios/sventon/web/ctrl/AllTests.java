package de.berlios.sventon.web.ctrl;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

  public static void main(String[] args) {
    junit.textui.TestRunner.run(AllTests.suite());
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(AllTests.class.getPackage().getName());
    suite.addTestSuite(GetControllerTest.class);
    suite.addTestSuite(ShowFileControllerTest.class);
    suite.addTestSuite(ShowThumbnailsControllerTest.class);
    return suite;
  }

}