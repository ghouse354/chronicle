package com.ghouse354.chronicle;

import org.junit.Test;
import static org.junit.Assert.*;

public class ChronicleTest {
    @Test public void breathingTest() {
        Chronicle chronicle = Chronicle.initialize("test.chronicle");
        Chronicle.createValue("OS Version", System.getProperty("os.version"));
        Chronicle.createTopic("Example Topic", "Bytes", () -> (double) Runtime.getRuntime().freeMemory());
        Chronicle.createTopicSubscriber("Subscribed Topic", "s", DataInferenceMode.DEFAULT);
        chronicle.finalizeInitialization();

        for (int i = 0; i < 10; i++) {
            Chronicle.publish("Subscribed Topic", (double) System.nanoTime());

            chronicle.updateTopics();
            chronicle.log();
        }
    }
}