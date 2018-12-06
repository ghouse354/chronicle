package com.ghouse354.chronicle;

import java.util.Optional;
import java.util.function.Supplier;

public class QueriedTopic extends Topic {
    private String d_name;
    private String d_unit;
    private Supplier<String> d_supplier;
    private String[] d_attrs;

    private Optional<String> d_value;

    public QueriedTopic(String name, String unit, Supplier<String> supplier, String... attrs) {
        d_name = name;
        d_unit = unit;
        d_supplier = supplier;
        d_attrs = attrs;
    }

    public String getName() {
        return d_name;
    }

    public String getUnit() {
        return d_unit;
    }

    public String[] getAttributes() {
        return d_attrs;
    }

    public void refreshValue() {
        d_value = Optional.of(d_supplier.get());
    }

    public String getValue() {
        return d_value.get();
    }
}