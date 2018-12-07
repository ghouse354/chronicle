package com.ghouse354.chronicle;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
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

    private List<NamespaceEntity> d_registeredEntities;
    private List<Topic> d_topics;
    private Map<String, Optional<String>> d_publishedData;

    private Writer d_writer;

    private Function<Double, String> d_doubleStringFunction = (d) -> String.format("%.5g", d);

    private Chronicle(Writer writer) {
        d_isInitialized = false;

        d_registeredEntities = new ArrayList<>();
        d_topics = new ArrayList<>();
        d_publishedData = new HashMap<>();

        d_writer = writer;
    }

    private Chronicle(String path) throws IOException {
        this(new FileWriter(path));
    }

    /**
     * Initializes Chronicle
     * @param path path to save log file to
     * @return instance of Chronicle
     * @throws RuntimeException if already initialized
     */
    public static Chronicle initialize(String path) {
        try {
            FileWriter fw = new FileWriter(path);
            return initialize(fw);
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Chronicle initialize(Writer writer) {
        if (s_instance.isPresent()) {
            throw new RuntimeException("Chronicle already initialized");
        }

        Chronicle chronicle = new Chronicle(writer);
        s_instance = Optional.of(chronicle);

        return chronicle;
    }

    public static void createTopicString(String name, String unit, Supplier<String> supplier, String... attrs) {
        Chronicle instance = s_instance.get();
        if (instance.isInitialized()) {
            throw new RuntimeException("Chronicle is already locked");
        }

        if (isRegistered(name)) {
            throw new RuntimeException("Topic '" + name + "' is already registered");
        }

        QueriedTopic topic = new QueriedTopic(name, unit, supplier, attrs);
        instance.getRegisteredEntities().add(topic);
        instance.getTopics().add(topic);
    }

    public static void createTopic(String name, String unit, Supplier<Double> supplier, String... attrs) {
        createTopicString(name, unit, () -> s_instance.get().d_doubleStringFunction.apply(supplier.get()), attrs);
    }

    public static void createTopicSubscriber(String name, String unit, DataInferenceMode mode, String... attrs) {
        Chronicle instance = s_instance.get();
        if (instance.isInitialized()) {
            throw new RuntimeException("Chronicle is already locked");
        }
        if (isRegistered(name)) {
            throw new RuntimeException("Topic '" + name + "' is already registered");
        }

        instance.getPublishedData().put(name, Optional.empty());
        SubscribedTopic topic = new SubscribedTopic(name, unit, mode, attrs);
        instance.getRegisteredEntities().add(topic);
        instance.getTopics().add(topic);

    }

    public static void createValue(String name, String value) {
        Chronicle instance = s_instance.get();
        if (instance.d_isInitialized) {
            throw new RuntimeException("Chronicle is already locked");
        }
        if (isRegistered(name)) {
            throw new RuntimeException("Topic '" + name + "' is already registered");
        }

        instance.getRegisteredEntities().add(new Value(name, value));
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

    public static void finalizeInitialization() {
        Chronicle instance = s_instance.get();
        if (instance.isInitialized()) {
            throw new RuntimeException("Chronicle already initialized");
        }

        instance.setInitialized(true);

        String jsonHeader = instance.generateJsonHeader();

        StringJoiner joiner = new StringJoiner(",");
        instance.d_topics.stream()
            .map(Topic::getName)
            .forEach((n) -> joiner.add(n));
            String header = joiner.toString();

        instance.writeLine(jsonHeader);
        instance.writeLine(header);

        try {
            instance.getWriter().flush();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ===== Instance Methods =====
    public boolean isInitialized() {
        return d_isInitialized;
    }

    public void setInitialized(boolean val) {
        d_isInitialized = val;
    }

    public List<NamespaceEntity> getRegisteredEntities() {
        return d_registeredEntities;
    }

    public List<Topic> getTopics() {
        return d_topics;
    }

    public Map<String, Optional<String>> getPublishedData() {
        return d_publishedData;
    }

    private Writer getWriter() {
        return d_writer;
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
        d_registeredEntities.stream().filter((o) -> o instanceof Value).map((v) -> (Value) v).forEach((v) -> {
            JSONObject value = new JSONObject();
            value.put("name", v.getName());
            value.put("value", v.getValue());
            jsonValues.add(value);
        });

        jsonRoot.put("values", jsonValues);

        return jsonRoot.toJSONString();
    }

    private static boolean isRegistered(String name) {
        return s_instance.get().d_registeredEntities.stream().anyMatch((o) -> o.getName().equals(name));
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
            d_writer.write(line + System.lineSeparator());
            d_writer.flush();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}