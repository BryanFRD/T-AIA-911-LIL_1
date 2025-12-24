package com.epitech.travelorder.nlu;

import ai.djl.Device;
import ai.djl.engine.Engine;

public class Gpu {

    public void check() {
        Engine engine = Engine.getInstance();
        String engineName = engine.getEngineName();
        Device device = engine.defaultDevice();

        System.out.println("Motor used : " + engineName);
        System.out.println("Default device : " + device);
    }
}
