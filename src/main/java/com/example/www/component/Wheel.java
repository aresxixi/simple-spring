package com.example.www.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component()
public class Wheel {

    @Autowired
    private Rim rim;

    @Autowired
    private Tire tire;

}
