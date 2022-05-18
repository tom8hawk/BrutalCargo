package ru.baronessdev.personal.brutalcargo.utils;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Hemisphere {

    private Hemisphere() {
        throw new IllegalStateException("Utility class");
    }

    /**
     *
     * @param centerBlock Define the center of the sphere
     * @param radius Radius of your sphere
     * @param hollow If your sphere should be hollow (you only get the blocks outside) just put in "true" here
     * @return Returns the locations of the blocks in the sphere
     *
     */
    public static List<Location> generate(Location centerBlock, int radius, boolean hollow) {
        List<Location> circleBlocks = new ArrayList<>();
        World w = centerBlock.getWorld();

        int bx = centerBlock.getBlockX();
        int by = centerBlock.getBlockY();
        int bz = centerBlock.getBlockZ();

        for (int x = bx - radius; x <= bx + radius; x++) {
            for (int y = by - radius; y <= by + radius; y++) {
                for (int z = bz - radius; z <= bz + radius; z++) {
                    double distance = ((bx-x) * (bx-x) + ((bz-z) * (bz-z)) + ((by-y) * (by-y)));

                    if (distance < radius * radius && !(hollow && distance < ((radius - 1) * (radius - 1))))
                        circleBlocks.add(new Location(w, x, y, z));
                }
            }
        }

        int half = by + radius;
        return circleBlocks.parallelStream()
                .filter(l -> l.getY() <= half)
                .collect(Collectors.toList());
    }
}
