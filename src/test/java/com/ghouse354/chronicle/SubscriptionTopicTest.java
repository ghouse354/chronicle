package com.ghouse354.chronicle;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Optional;

public class SubscriptionTopicTest {
    @Test public void breathingTest() {
        SubscriptionTopic topic = new SubscriptionTopic("Test Topic", "ul", DataInferenceMode.DEFAULT);

        assertEquals("Test Topic", topic.getName());
        assertEquals("ul", topic.getUnit());
        assertEquals(Chronicle.DEFAULT_DATA, topic.getValue());
    }

    @Test public void handleUpdate() {
        SubscriptionTopic topic = new SubscriptionTopic("Test Topic", "ul", DataInferenceMode.DEFAULT);

        topic.onPublishedData(Optional.of("new data"));
        assertEquals("new data", topic.getValue());
    }
}