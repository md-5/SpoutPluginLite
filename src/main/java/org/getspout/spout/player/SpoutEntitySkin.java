package org.getspout.spout.player;

import java.util.EnumMap;
import java.util.Map;
import org.getspout.spoutapi.player.EntitySkinType;

public class SpoutEntitySkin {

    private Map<EntitySkinType, String> textures = new EnumMap<EntitySkinType, String>(EntitySkinType.class);

    public void setSkin(EntitySkinType type, String url) {
        textures.put(type, url);
    }

    public String getSkin(EntitySkinType type) {
        return textures.get(type);
    }

    public void reset() {
        textures.clear();
    }
}
