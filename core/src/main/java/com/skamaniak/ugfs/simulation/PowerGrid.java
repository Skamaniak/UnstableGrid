package com.skamaniak.ugfs.simulation;

import com.badlogic.gdx.Gdx;
import com.skamaniak.ugfs.asset.model.Conduit;
import com.skamaniak.ugfs.game.entity.ConduitEntity;
import com.skamaniak.ugfs.game.entity.GeneratorEntity;
import com.skamaniak.ugfs.game.entity.PowerStorageEntity;
import com.skamaniak.ugfs.game.entity.TowerEntity;

import java.util.HashSet;
import java.util.Set;

public class PowerGrid {
    //TODO simulate losses on the grid
    //TODO do not try to fill power path one by one, try to split the power among more paths during simulation

    private final Set<GeneratorEntity> sources = new HashSet<>();
    private final Set<PowerStorageEntity> storages = new HashSet<>();
    private final Set<TowerEntity> sinks = new HashSet<>();
    private final Set<ConduitEntity> conduits = new HashSet<>();

    public boolean addSource(GeneratorEntity source) {
        return sources.add(source);
    }

    public boolean removeSource(GeneratorEntity source) {
        for (ConduitEntity conduit : conduits) {
            if (conduit.from == source) {
                source.removeTo(conduit.to);
                removeConduit(conduit);
            }
        }
        return sources.remove(source);
    }

    public Set<GeneratorEntity> getSources() {
        return sources;
    }

    public boolean addStorage(PowerStorageEntity storage) {
        for (ConduitEntity conduit : conduits) {
            if (conduit.from == storage) {
                storage.removeTo(conduit.to);
                removeConduit(conduit);
            }
            if (conduit.to == storage) {
                conduit.from.removeTo(storage);
                removeConduit(conduit);
            }
        }
        return storages.remove(storage);
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
        for (ConduitEntity conduit : conduits) {
            if (conduit.to == sink) {
                conduit.from.removeTo(sink);
                removeConduit(conduit);
            }
        }
        return sinks.remove(sink);
    }

    public Set<TowerEntity> getSinks() {
        return sinks;
    }

    public boolean addConduit(ConduitEntity conduitToAdd) {
        if (conduits.add(conduitToAdd)) {
            conduitToAdd.from.addTo(conduitToAdd.to);
            return true;
        }
        return false;
    }

    public boolean removeConduit(ConduitEntity conduit) {
        if (conduits.remove(conduit)) {
            conduit.from.removeTo(conduit.to);
            return true;
        }
        return false;
    }

    public Set<ConduitEntity> getConduits() {
        return conduits;
    }

    private void resetPropagation() {
        for (GeneratorEntity producer : sources) {
            producer.resetPropagation();
        }
        for (PowerStorageEntity storage : storages) {
            storage.resetPropagation();
        }
    }

    public void simulatePropagation(float delta) {
        if (delta > 1f) {
            Gdx.app.error(PowerGrid.class.getName(), "Delta between frames is more than 1 second (" + delta + "). The game seems to be lagging.");
        }

        while (delta != 0f) {
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
