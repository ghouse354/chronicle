package com.ghouse354.chronicle;

import java.util.Optional;

public class SubscribedTopic extends Topic {
    private String d_name;
    private String d_unit;
    private DataInferenceMode d_mode;
    private String[] d_attrs;

    private String d_value = Chronicle.DEFAULT_DATA;

    public SubscribedTopic(String name, String unit, DataInferenceMode mode, String... attrs) {
        d_name = name;
        d_unit = unit;
        d_mode = mode;
        d_attrs = attrs;
    }

    public String getUnit() {
        return d_unit;
    }

    public String[] getAttributes() {
        return d_attrs;
    }

    public String getValue() {
        return d_value;
    }

    public String getName() {
        return d_name;
    }

    public void handlePublishedData(Optional<String> data) {
        switch(d_mode) {
            case DEFAULT:
                d_value = data.orElse(Chronicle.DEFAULT_DATA);
                break;
            case LAST_VALUE:
                if (data.isPresent()) {
                    d_value = data.get();
                }
                break;
        }
    }
}