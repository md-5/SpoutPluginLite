package org.getspout.spout.player;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.getspout.spoutapi.Spout;
import org.getspout.spoutapi.player.AppearanceManager;
import org.getspout.spoutapi.player.EntitySkinType;
import org.getspout.spoutapi.player.SpoutPlayer;

@SuppressWarnings("deprecation")
public class SimpleAppearanceManager implements AppearanceManager {

    @Override
    public void setGlobalSkin(HumanEntity target, String Url) {
        ((SpoutPlayer) target).setSkin(Url);
    }

    @Override
    public void setPlayerSkin(SpoutPlayer viewingPlayer, HumanEntity target, String Url) {
        ((SpoutPlayer) target).setSkinFor(viewingPlayer, Url);
    }

    @Override
    public void setGlobalCloak(HumanEntity target, String Url) {
        ((SpoutPlayer) target).setCape(Url);
    }

    @Override
    public void setPlayerCloak(SpoutPlayer viewingPlayer, HumanEntity target, String Url) {
        ((SpoutPlayer) target).setCapeFor(viewingPlayer, Url);
    }

    @Override
    public void setPlayerTitle(SpoutPlayer viewingPlayer, LivingEntity target, String title) {
        if (target instanceof SpoutPlayer) {
            ((SpoutPlayer) target).setTitleFor(viewingPlayer, title);
        }
    }

    @Override
    public void setGlobalTitle(LivingEntity target, String title) {
        Spout.getServer().setTitle(target, title);
    }

    @Override
    public void hidePlayerTitle(SpoutPlayer viewingPlayer, LivingEntity target) {
        if (target instanceof SpoutPlayer) {
            ((SpoutPlayer) target).hideTitleFrom(viewingPlayer);
        }
    }

    @Override
    public void hideGlobalTitle(LivingEntity target) {
        if (target instanceof SpoutPlayer) {
            ((SpoutPlayer) target).hideTitle();
        }
    }

    @Override
    public String getSkinUrl(SpoutPlayer viewingPlayer, HumanEntity target) {
        return ((SpoutPlayer) target).getSkin(viewingPlayer);
    }

    @Override
    public void resetGlobalSkin(HumanEntity target) {
        // TODO Auto-generated method stub
    }

    @Override
    public void resetPlayerSkin(SpoutPlayer viewingPlayer, HumanEntity target) {
        // TODO Auto-generated method stub
    }

    @Override
    public void resetGlobalCloak(HumanEntity target) {
        // TODO Auto-generated method stub
    }

    @Override
    public void resetPlayerCloak(SpoutPlayer viewingPlayer, HumanEntity target) {
        // TODO Auto-generated method stub
    }

    @Override
    public void resetPlayerTitle(SpoutPlayer viewingPlayer, LivingEntity target) {
        // TODO Auto-generated method stub
    }

    @Override
    public void resetGlobalTitle(LivingEntity target) {
        // TODO Auto-generated method stub
    }

    @Override
    public String getCloakUrl(SpoutPlayer viewingPlayer, HumanEntity target) {
        return ((SpoutPlayer) target).getCape(viewingPlayer);
    }

    @Override
    public String getTitle(SpoutPlayer viewingPlayer, LivingEntity target) {
        if (target instanceof SpoutPlayer) {
            return ((SpoutPlayer) target).getTitleFor(viewingPlayer);
        }
        return null;
    }

    @Override
    public void resetAllSkins() {
    }

    @Override
    public void resetAllCloaks() {
    }

    @Override
    public void resetAllTitles() {
    }

    @Override
    public void resetAll() {
    }

    @Override
    public void setEntitySkin(SpoutPlayer viewingPlayer, LivingEntity target, String url, EntitySkinType type) {
        viewingPlayer.setEntitySkin(target, url, type);
    }

    @Override
    public void setGlobalEntitySkin(LivingEntity entity, String url, EntitySkinType type) {
        Spout.getServer().setEntitySkin(entity, url, type);
    }

    @Override
    public void resetEntitySkin(LivingEntity entity) {
    }
}
