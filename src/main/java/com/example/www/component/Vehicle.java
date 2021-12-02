package com.example.www.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Vehicle {

    @Autowired
    private Engine engine;

    @Autowired
    private GearBox gearBox;

    @Autowired
    private List<Wheel> wheels;

    @Override
    public String toString() {
        return "Vehicle{" +
                "engine=" + engine +
                ", gearBox=" + gearBox +
                ", wheels=" + wheels +
                '}';
    }
}
