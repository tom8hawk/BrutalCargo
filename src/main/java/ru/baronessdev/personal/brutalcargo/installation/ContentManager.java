package ru.baronessdev.personal.brutalcargo.installation;

import lombok.Getter;
import org.bukkit.Location;

public class ContentManager {
    @Getter private final Location location;

    public ContentManager(Location location) {
        this.location = location;
    }
}
