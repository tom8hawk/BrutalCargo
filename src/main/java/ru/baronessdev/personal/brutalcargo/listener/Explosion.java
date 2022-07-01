package ru.baronessdev.personal.brutalcargo.listener;

import lombok.Getter;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TNTPrimed;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class Explosion {
    private static final List<Explosion> explosions = new CopyOnWriteArrayList<>();
    @Getter private final List<Block> explodedBlocks = new CopyOnWriteArrayList<>();
    private final TNTPrimed tnt;

    public Explosion(TNTPrimed tnt) {
        this.tnt = tnt;
        explosions.add(this);
    }

    public static Optional<Explosion> getByTnt(Entity entity) {
        return explosions.stream()
                .filter(ex -> ex.tnt.equals(entity))
                .findFirst();
    }
}
