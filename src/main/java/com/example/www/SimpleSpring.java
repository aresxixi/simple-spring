package com.example.www;

import com.example.www.component.*;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class SimpleSpring {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
//        context.register(Vehicle.class, Engine.class, GearBox.class, Wheel.class, Rim.class, Tire.class);
        context.scan("com.example.www.component");
        context.refresh();
        Vehicle vehicle = context.getBean(Vehicle.class);
        System.out.println(vehicle);
    }
}
