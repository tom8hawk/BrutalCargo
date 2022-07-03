package ru.baronessdev.personal.brutalcargo.listener;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class Explosion {
    private static final List<Explosion> explosions = new CopyOnWriteArrayList<>();
    @Getter private final List<Block> explodedBlocks = new CopyOnWriteArrayList<>();
    private final Location tnt;

    public Explosion(Location tnt) {
        this.tnt = tnt;
        explosions.add(this);
    }

    public static Optional<Explosion> getByTnt(Location exploded) {
        return explosions.stream()
                .filter(ex -> ex.tnt.equals(exploded))
                .findFirst();
    }

    public void delete() {
        explosions.remove(this);
    }
}
