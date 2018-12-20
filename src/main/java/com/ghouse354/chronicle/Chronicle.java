package com.ghouse354.chronicle;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import com.opencsv.CSVWriter;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Chronicle {
    // === CLASS LEVEL PROPERTIES ===
    private static Map<String, Chronicle> s_instances = new HashMap<>();

    public static final String DEFAULT_DATA = "-1.0";

    private static Function<Double, String> s_doubleToString = (d) -> String.format("%.5g", d);
    private static Function<Long, String> s_longToString = (l) -> Long.toString(l);

    // === CLASS LEVEL METHODS ===
    /**
     * Create a new Chronicle instance with an identifier and an output writer
     * @param ident Unique identifier for this Chronicle instance
     * @param writer Output writer to use for Chronicle output
     * @return Instance of Chronicle
     */
    public static Chronicle create(String ident, Writer writer) {
        // Check if this identifier already exists
        if (s_instances.containsKey(ident)) {
            throw new DuplicateIdentifierException();
        }

        Chronicle chronicle = new Chronicle(writer);
        s_instances.put(ident, chronicle);

        return chronicle;
    }

    /**
     * Create a new Chronicle instance that writes to a provided file path
     * @param ident Unique identifier for this Chronicle instance
     * @param path File name to save output to
     * @return Instance of Chronicle
     */
    public static Chronicle create(String ident, String path) {
        try {
            FileWriter fw = new FileWriter(path);
            return create(ident, fw);
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Retrieve a registered Chronicle instance
     * @param ident Identifier
     * @return Chronicle instance, or null if nothing was registered with that ID
     */
    public static Chronicle getInstance(String ident) {
        return s_instances.get(ident);
    }

    @SuppressWarnings("unchecked")
    private static String generateJsonHeader(List<CEntity> entities) {
        JSONObject jsonRoot = new JSONObject();
        JSONArray jsonTopics = new JSONArray();
        JSONArray jsonValues = new JSONArray();

        for (CEntity entity : entities) {
            if (entity instanceof CTopic) {
                CTopic t = (CTopic)entity;
                JSONObject topic = new JSONObject();
                topic.put("name", t.getName());
                topic.put("unit", t.getUnit());
                JSONArray attrs = new JSONArray();
                Arrays.stream(t.getAttributes())
                    .forEach((a) -> attrs.add(a));
                topic.put("attrs", attrs);
                jsonTopics.add(topic);
            }
            else if (entity instanceof CValue) {
                CValue v = (CValue)entity;
                JSONObject value = new JSONObject();
                value.put("name", v.getName());
                value.put("value", v.getValue());
                jsonValues.add(value);
            }
        }

        jsonRoot.put("topics", jsonTopics);
        jsonRoot.put("values", jsonValues);

        return jsonRoot.toJSONString();
    }

    // === INSTANCE LEVEL PROPERTIES ===
    private boolean d_initialized = false;
    private Writer d_writer = null;

    private CSVWriter d_csvWriter = null;

    /**
     * List of all registered entities
     */
    private List<CEntity> d_entities;

    /**
     * Convenience data structure for quick lookup of registered entities
     */
    private Set<String> d_entitySet;

    private List<CTopic> d_topics;
    private Map<String, Optional<String>> d_publishedData;


    // === INSTANCE LEVEL METHODS ===
    private Chronicle(Writer writer) {
        d_initialized = false;
        d_writer = writer;
        d_csvWriter = new CSVWriter(d_writer);

        d_entities = new ArrayList<>();
        d_entitySet = new HashSet<>();

        d_topics = new ArrayList<>();
        d_publishedData = new HashMap<>();

        // Automatically add Time as a RegularTopic
        addTopicLong("Time", "ms", () -> System.currentTimeMillis(), "xaxis");
    }

    /**
     * Complete initialization/set up of the Chronicle instance.
     *
     * Calling this method will freeze the field definitions for this Chronicle
     * instance, and allow the instance to actually begin logging
     */
    public void finalize() {
        // Throw an error if we have already been initialized
        if (d_initialized) {
            throw new InitializationException("Chronicle instance already finalized");
        }

        d_initialized = true;

        String jsonHeaderBlock = generateJsonHeader(d_entities);
        writeRawLine(jsonHeaderBlock);

        String[] topicNames = d_topics.stream()
                                .map(CTopic::getName)
                                .toArray(String[]::new);

        d_csvWriter.writeNext(topicNames);
        try {
            d_csvWriter.flush();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Add a registered topic that provides Strings to the Chronicle instance
     * @param name
     * @param unit
     * @param supplier
     * @param attrs
     */
    public void addTopicString(String name, String unit, Supplier<String> supplier, String... attrs) {
        if (d_initialized) {
            throw new InitializationException("Chronicle instance already initialized");
        }

        if (d_entitySet.contains(name)) {
            throw new DuplicateIdentifierException();
        }

        RegularTopic topic = new RegularTopic(name, unit, supplier, attrs);
        d_entities.add(topic);
        d_entitySet.add(name);
        d_topics.add(topic);
    }

    /**
     * Add a registered topic that provides Doubles to the Chronicle instance
     * @param name
     * @param unit
     * @param supplier
     * @param attrs
     */
    public void addTopicDouble(String name, String unit, Supplier<Double> supplier, String... attrs) {
        addTopicString(name, unit, () -> s_doubleToString.apply(supplier.get()), attrs);
    }

    /**
     * Add a registered topic that provides Longs to the Chronicle instance
     * @param name
     * @param unit
     * @param supplier
     * @param attrs
     */
    public void addTopicLong(String name, String unit, Supplier<Long> supplier, String... attrs) {
        addTopicString(name, unit, () -> s_longToString.apply(supplier.get()), attrs);
    }

    /**
     * Add a subscription to a topic
     * @param name
     * @param unit
     * @param mode
     * @param attrs
     */
    public void addTopicSubscription(String name, String unit, DataInferenceMode mode, String... attrs) {
        if (d_initialized) {
            throw new InitializationException("Chronicle instance already initialized");
        }

        if (d_entitySet.contains(name)) {
            throw new DuplicateIdentifierException();
        }

        d_publishedData.put(name, Optional.empty());
        SubscriptionTopic topic = new SubscriptionTopic(name, unit, mode, attrs);
        d_entities.add(topic);
        d_entitySet.add(name);
        d_topics.add(topic);
    }

    /**
     * Register a static value
     * @param name
     * @param value
     */
    public void addStaticValue(String name, String value) {
        if (d_initialized) {
            throw new InitializationException("Chronicle instance already initialized");
        }

        if (d_entitySet.contains(name)) {
            throw new DuplicateIdentifierException();
        }

        d_entities.add(new CValue(name, value));
        d_entitySet.add(name);
    }

    /**
     * Run through all registered topics and update their values
     */
    public void update() {
        if (!d_initialized) {
            throw new InitializationException("Chronicle instance not fully initialized");
        }

        d_topics.stream()
            .filter((o) -> o instanceof RegularTopic)
            .map((o) -> (RegularTopic) o)
            .forEach(RegularTopic::refreshValue);

        d_topics.stream()
            .filter((o) -> o instanceof SubscriptionTopic)
            .map((o) -> (SubscriptionTopic) o)
            .forEach((t) -> t.onPublishedData(d_publishedData.get(t.getName())));

        // Erase the used published data
        d_publishedData.replaceAll((k, v) -> Optional.empty());
    }

    /**
     * Write a new line of data to the output
     */
    public void log() {
        if (!d_initialized) {
            throw new InitializationException("Chronicle instance not fully initialized");
        }

        String[] dataLine = d_topics.stream()
                                .map(CTopic::getValue)
                                .toArray(String[]::new);

        writeDataLine(dataLine);
    }

    /**
     * Publish a new value for a specified topic
     * @param name
     * @param value
     */
    public void publish(String name, String value) {
        if (!d_initialized) {
            throw new InitializationException("Chronicle instance not fully initialized");
        }

        if (d_publishedData.get(name) == null) {
            throw new NullPointerException("Topic '" + name + "' not registered");
        }


        d_publishedData.put(name, Optional.of(value));
    }

    private void writeRawLine(String line) {
        try {
            d_writer.write(line + System.lineSeparator());
            d_writer.flush();
        }
        catch (IOException e) {
            e.printStackTrace();;
        }
    }

    private void writeDataLine(String[] data) {
        d_csvWriter.writeNext(data);
        try {
            d_csvWriter.flush();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}