package com.ghouse354.chronicle;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.Supplier;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Chronicle {
    public static final String DEFAULT_DATA = Double.toString(-1.0);

    private static Optional<Chronicle> s_instance = Optional.empty();

    private boolean d_isInitialized = false;

    private List<NamespaceEntity> d_namespace;
    private List<Topic> d_topics;
    private Map<String, Optional<String>> d_publishedData;

    private FileWriter d_fileWriter;

    private Function<Double, String> d_doubleStringFunction = (d) -> String.format("%.5g", d);

    private Chronicle(String path) {
        d_isInitialized = false;

        d_namespace = new ArrayList<>();
        d_topics = new ArrayList<>();
        d_publishedData = new HashMap<>();

        try {
            d_fileWriter = new FileWriter(path);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes Chronicle
     * @param path path to save log file to
     * @return instance of Chronicle
     * @throws RuntimeException if already initialized
     */
    public static Chronicle initialize(String path) {
        if (s_instance.isPresent()) {
            throw new RuntimeException("Chronicle already initialized");
        }

        Chronicle chronicle = new Chronicle(path);
        s_instance = Optional.of(chronicle);

        return chronicle;
    }

    public static void createTopicString(String name, String unit, Supplier<String> supplier, String... attrs) {
        if (s_instance.get().d_isInitialized) {
            throw new RuntimeException("Chronicle is already locked");
        }

        if (isRegistered(name)) {
            throw new RuntimeException("Topic '" + name + "' is already registered");
        }

        QueriedTopic topic = new QueriedTopic(name, unit, supplier, attrs);
        s_instance.get().d_namespace.add(topic);
        s_instance.get().d_topics.add(topic);
    }

    public static void createTopic(String name, String unit, Supplier<Double> supplier, String... attrs) {
        createTopicString(name, unit, () -> s_instance.get().d_doubleStringFunction.apply(supplier.get()), attrs);
    }

    public static void createTopicSubscriber(String name, String unit, DataInferenceMode mode, String... attrs) {
        Chronicle instance = s_instance.get();
        if (instance.d_isInitialized) {
            throw new RuntimeException("Chronicle is already locked");
        }
        if (isRegistered(name)) {
            throw new RuntimeException("Topic '" + name + "' is already registered");
        }

        instance.d_publishedData.put(name, Optional.empty());
        SubscribedTopic topic = new SubscribedTopic(name, unit, mode, attrs);
        instance.d_namespace.add(topic);
        instance.d_topics.add(topic);
    }

    public static void createValue(String name, String value) {
        Chronicle instance = s_instance.get();
        if (instance.d_isInitialized) {
            throw new RuntimeException("Chronicle is already locked");
        }
        if (isRegistered(name)) {
            throw new RuntimeException("Topic '" + name + "' is already registered");
        }

        instance.d_namespace.add(new Value(name, value));
    }

    public static void publish(String name, String value) {
        Chronicle instance = s_instance.get();
        if (!instance.d_isInitialized) {
            throw new RuntimeException("Chronicle not yet initialized");
        }
        instance.handlePublishedData(name, value);
    }

    public static void publish(String name, double value) {
        publish(name, s_instance.get().d_doubleStringFunction.apply(value));
    }

    public void finalizeInitialization() {
        if (d_isInitialized) {
            throw new RuntimeException("Chronicle already initialized");
        }
        d_isInitialized = true;

        String jsonHeader = generateJsonHeader();

        // Write the CSV Header
        StringJoiner joiner = new StringJoiner(",");
        d_topics.stream().map(Topic::getName).forEach((n) -> joiner.add(n));
        String header = joiner.toString();

        writeLine(jsonHeader);
        writeLine(header);

        try {
            d_fileWriter.flush();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateTopics() {
        if (!d_isInitialized) {
            throw new RuntimeException("Chronicle is not initialized");
        }

        d_topics.stream()
            .filter((o) -> o instanceof QueriedTopic)
            .map((o) -> (QueriedTopic) o)
            .forEach(QueriedTopic::refreshValue);

        d_topics.stream()
            .filter((o) -> o instanceof SubscribedTopic)
            .map((o) -> (SubscribedTopic) o)
            .forEach((t) -> t.handlePublishedData(d_publishedData.get(t.getName())));

        d_publishedData.replaceAll((k, v) -> Optional.empty());
    }

    public void log() {
        if (!d_isInitialized) {
            throw new RuntimeException("Chronicle is not initialized");
        }

        StringJoiner joiner = new StringJoiner(",");
        d_topics.stream()
            .map(Topic::getValue)
            .map(Chronicle::escapeCommas)
            .forEach((v) -> joiner.add(v));

        String line = joiner.toString();

        writeLine(line);
    }

    @SuppressWarnings("unchecked")
    private String generateJsonHeader() {
        JSONObject jsonRoot = new JSONObject();

        JSONArray jsonTopics = new JSONArray();
        for (Topic t : d_topics) {
            JSONObject topic = new JSONObject();
            topic.put("name", t.getName());
            topic.put("unit", t.getUnit());
            JSONArray attrs = new JSONArray();
            Arrays.stream(t.getAttributes()).forEach((a) -> attrs.add(a));
            topic.put("attrs", attrs);
            jsonTopics.add(topic);
        }

        jsonRoot.put("topics", jsonTopics);

        JSONArray jsonValues = new JSONArray();
        d_namespace.stream().filter((o) -> o instanceof Value).map((v) -> (Value) v).forEach((v) -> {
            JSONObject value = new JSONObject();
            value.put("name", v.getName());
            value.put("value", v.getValue());
            jsonValues.add(value);
        });

        jsonRoot.put("values", jsonValues);

        return jsonRoot.toJSONString();
    }

    private static boolean isRegistered(String name) {
        return s_instance.get().d_namespace.stream().anyMatch((o) -> o.getName().equals(name));
    }

    private static String escapeCommas(String in) {
        if (in.contains(",")) {
            return "\"" + in + "\"";
        }
        return in;
    }

    private void handlePublishedData(String name, String value) {
        if (d_publishedData.get(name) == null) {
            throw new NullPointerException();
        }

        d_publishedData.put(name, Optional.of(value));
    }

    private void writeLine(String line) {
        try {
            d_fileWriter.write(line + System.lineSeparator());
            d_fileWriter.flush();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}