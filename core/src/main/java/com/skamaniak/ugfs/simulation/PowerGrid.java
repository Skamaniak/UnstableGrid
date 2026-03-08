package com.skamaniak.ugfs.simulation;

import com.badlogic.gdx.Gdx;
import com.skamaniak.ugfs.game.entity.ConduitEntity;
import com.skamaniak.ugfs.game.entity.GeneratorEntity;
import com.skamaniak.ugfs.game.entity.PowerStorageEntity;
import com.skamaniak.ugfs.game.entity.TowerEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PowerGrid {
    //TODO do not try to fill power path one by one, try to split the power among more paths during simulation

    private final Set<GeneratorEntity> sources = new HashSet<>();
    private final Set<PowerStorageEntity> storages = new HashSet<>();
    private final Set<TowerEntity> sinks = new HashSet<>();
    private final Set<ConduitEntity> conduits = new HashSet<>();

    public boolean addSource(GeneratorEntity source) {
        return sources.add(source);
    }

    public boolean removeSource(GeneratorEntity source) {
        List<ConduitEntity> toRemove = new ArrayList<>();
        for (ConduitEntity conduit : conduits) {
            if (conduit.from == source) toRemove.add(conduit);
        }
        for (ConduitEntity conduit : toRemove) {
            source.removeTo(conduit.to);
            removeConduit(conduit);
        }
        return sources.remove(source);
    }

    public Set<GeneratorEntity> getSources() {
        return sources;
    }

    public boolean addStorage(PowerStorageEntity storage) {
        return storages.add(storage);
    }

    public boolean removeStorage(PowerStorageEntity storage) {
        return storages.remove(storage);
    }

    public Set<PowerStorageEntity> getStorages() {
        return storages;
    }

    public boolean addSink(TowerEntity sink) {
        return sinks.add(sink);
    }

    public boolean removeSink(TowerEntity sink) {
        List<ConduitEntity> toRemove = new ArrayList<>();
        for (ConduitEntity conduit : conduits) {
            if (conduit.to == sink) toRemove.add(conduit);
        }
        for (ConduitEntity conduit : toRemove) {
            conduit.from.removeTo(sink);
            removeConduit(conduit);
        }
        return sinks.remove(sink);
    }

    public Set<TowerEntity> getSinks() {
        return sinks;
    }

    public boolean addConduit(ConduitEntity conduitToAdd) {
        conduitToAdd.register();
        return conduits.add(conduitToAdd);
    }

    public boolean removeConduit(ConduitEntity conduit) {
        conduit.unregister();
        return conduits.remove(conduit);
    }

    public Set<ConduitEntity> getConduits() {
        return conduits;
    }

    private void resetPropagation() {
        for (PowerStorageEntity storage : storages) {
            storage.resetPropagation();
        }
        for (TowerEntity sink : sinks) {
            sink.resetPropagation();
        }
        for (ConduitEntity conduit : conduits) {
            conduit.resetPropagation();
        }
    }

    public void simulatePropagation(float delta) {
        while (delta > 0f) {
            if (delta > 1f) {
                Gdx.app.error(PowerGrid.class.getName(), "Delta between frames is more than 1 second (" + delta + "). The game seems to be lagging.");
            }
            float partialDelta = Math.min(1f, delta);
            delta -= partialDelta;
            resetPropagation();

            for (GeneratorEntity producer : sources) {
                producer.produce(partialDelta);
            }
            for (PowerStorageEntity storage : storages) {
                storage.produce(partialDelta);
            }
        }
    }
}
