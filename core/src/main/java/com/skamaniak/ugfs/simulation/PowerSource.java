package com.skamaniak.ugfs.simulation;

public interface PowerSource extends GridComponent {

    boolean addTo(PowerConsumer consumer);

    boolean removeTo(PowerConsumer consumer);
}
