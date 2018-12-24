package com.ghouse354.chronicle;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;

import com.ghouse354.chronicle.COptions.TimeGranularity;

public class ChronicleRunner {
    public static void main(String args[]) {

        Writer stdout = new BufferedWriter(new OutputStreamWriter(System.out));
        COptions opts = new COptions();
        opts.timeGranularity = TimeGranularity.Milliseconds;
        Chronicle chronicle = Chronicle.create("temp", stdout, opts);
        chronicle.addTopicString("Test Topic 1", "ul", () -> "Value 1, with commas");
        chronicle.addTopicDouble("Test Topic Double", "ul", () -> 1.0);
        chronicle.addStaticValue("Static Value 1", "Some Value");
        chronicle.addTopicSubscription("Subscribed Topic", "ul", DataInferenceMode.DEFAULT);
        chronicle.finalize();

        chronicle.update();
        chronicle.log();

        for (int i = 0; i < 10; i++) {
            chronicle.publish("Subscribed Topic", Integer.toString(i));

            chronicle.update();
            chronicle.log();

        }
    }
}