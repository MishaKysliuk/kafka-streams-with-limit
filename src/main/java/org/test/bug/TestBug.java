package org.test.bug;

import java.util.concurrent.atomic.AtomicBoolean;

public class TestBug {

  public static void main(String[] args) {
    AtomicBoolean isAppActive = new AtomicBoolean(true);

    // attach shutdown handler to catch control-c
    Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
      public void run() {
        isAppActive.set(false);
      }
    });
    try {
      QuickStreamRunner.initializeFs();

      while (isAppActive.get()) {
        QuickStreamRunner.runStream();
      }

    } catch (Throwable e) {
      System.out.println("Error " + e.toString());
      System.exit(1);
    }
    System.exit(0);
  }

}
