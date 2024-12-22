package com.skamaniak.ugfs.simulation;

import com.badlogic.gdx.Gdx;
import com.skamaniak.ugfs.game.entity.ConduitEntity;
import com.skamaniak.ugfs.game.entity.GeneratorEntity;
import com.skamaniak.ugfs.game.entity.PowerStorageEntity;
import com.skamaniak.ugfs.game.entity.TowerEntity;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class PowerGrid {
    //TODO simulate losses on the grid
    //TODO do not try to fill power path one by one, try to split the power among more paths during simulation

    private final Set<GeneratorEntity> sources = new HashSet<>();
    private final Set<PowerStorageEntity> storages = new HashSet<>();
    private final Set<TowerEntity> sinks = new HashSet<>();
    private final Set<Link> links = new HashSet<>();

    public boolean addSource(GeneratorEntity source) {
        return sources.add(source);
    }

    public boolean removeSource(GeneratorEntity source) {
        for (Link link : links) {
            if (link.from == source) {
                source.removeTo(link.to);
                removeLink(link);
            }
        }
        return sources.remove(source);
    }

    public Set<GeneratorEntity> getSources() {
        return sources;
    }

    public boolean addStorage(PowerStorageEntity storage) {
        for (Link link : links) {
            if (link.from == storage) {
                storage.removeTo(link.to);
                removeLink(link);
            }
            if (link.to == storage) {
                link.from.removeTo(storage);
                removeLink(link);
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
        for (Link link : links) {
            if (link.to == sink) {
                link.from.removeTo(sink);
                removeLink(link);
            }
        }
        return sinks.remove(sink);
    }

    public Set<TowerEntity> getSinks() {
        return sinks;
    }

    public boolean addLink(ConduitEntity conduit, PowerSource source, PowerConsumer destination) {
        return addLink(new Link(conduit, source, destination));
    }

    public boolean addLink(Link link) {
        if (links.add(link)) {
            link.from.addTo(link.to);
            return true;
        }
        return false;
    }

    public boolean removeLink(ConduitEntity conduit) {
        for(Link link : links) {
            if (link.conduit == conduit) {
                return removeLink(link);
            }
        }
        return false;
    }

    public boolean removeLink(Link link) {
        if (links.remove(link)) {
            link.from.removeTo(link.to);
            return true;
        }
        return false;
    }

    public Set<Link> getLinks() {
        return links;
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

    public static class Link {
        public ConduitEntity conduit;
        public PowerSource from;
        public PowerConsumer to;

        public Link(ConduitEntity conduit, PowerSource from, PowerConsumer to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Link link = (Link) o;
            return from.equals(link.from) && to.equals(link.to);
        }

        @Override
        public int hashCode() {
            return Objects.hash(from, to);
        }
    }
}
