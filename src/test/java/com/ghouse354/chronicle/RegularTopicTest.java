package com.ghouse354.chronicle;

import org.junit.Test;
import static org.junit.Assert.*;

public class RegularTopicTest {
    @Test public void breathingTest() {
        RegularTopic topic = new RegularTopic("Test Topic", "ul", () -> "static value");

        assertEquals("Test Topic", topic.getName());
        assertEquals("ul", topic.getUnit());
        assertEquals(Chronicle.DEFAULT_DATA, topic.getValue());
    }

    @Test public void refreshOnInit() {
        RegularTopic topic = new RegularTopic("Test Topic", "ul", () -> "static value", true);

        assertEquals("static value", topic.getValue());
    }

    @Test public void refreshValue() {
        int counter = 10;

        RegularTopic topic = new RegularTopic("Test Topic", "ul", () -> Integer.toString(counter));

        topic.refreshValue();

        assertEquals("10", topic.getValue());
    }
}