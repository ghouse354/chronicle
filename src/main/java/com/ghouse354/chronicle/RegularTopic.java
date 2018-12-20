package com.ghouse354.chronicle;

import java.util.Optional;
import java.util.function.Supplier;

public class RegularTopic extends CTopic {
    private Supplier<String> d_supplier;

    public RegularTopic(String name, String unit, Supplier<String> supplier, boolean refreshOnInit, String... attrs) {
        d_name = name;
        d_unit = unit;
        d_supplier = supplier;
        d_attrs = attrs;

        if (refreshOnInit) {
            refreshValue();
        }
    }

    public RegularTopic(String name, String unit, Supplier<String> supplier, String... attrs) {
        this(name, unit, supplier, false, attrs);
    }

    public void refreshValue() {
        d_value = Optional.of(d_supplier.get());
    }
}