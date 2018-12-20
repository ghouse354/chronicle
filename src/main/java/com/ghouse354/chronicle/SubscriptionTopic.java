package com.ghouse354.chronicle;

import java.util.Optional;

public class SubscriptionTopic extends CTopic {

    private DataInferenceMode d_mode;

    public SubscriptionTopic(String name, String unit, DataInferenceMode mode, String... attrs) {
        d_name = name;
        d_attrs = attrs;
        d_unit = unit;
        d_mode = mode;
    }


    public void onPublishedData(Optional<String> data) {
        switch (d_mode) {
            case DEFAULT:
                d_value = Optional.of(data.orElse(Chronicle.DEFAULT_DATA));
                break;
            case LAST_VALUE:
                if (data.isPresent()) {
                    d_value = data;
                }
                break;
        }
    }
}