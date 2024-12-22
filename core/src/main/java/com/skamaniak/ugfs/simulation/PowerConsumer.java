package com.skamaniak.ugfs.simulation;

public interface PowerConsumer extends GridComponent {

    float consume(float power, float delta);

}
