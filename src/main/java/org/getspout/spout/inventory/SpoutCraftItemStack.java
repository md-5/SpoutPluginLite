/*
 * This file is part of SpoutPlugin (http://www.spout.org/).
 *
 * SpoutPlugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SpoutPlugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.getspout.spout.inventory;

import net.minecraft.server.NBTTagCompound;

import org.bukkit.craftbukkit.inventory.CraftItemStack;

public class SpoutCraftItemStack extends CraftItemStack {
	public SpoutCraftItemStack(net.minecraft.server.ItemStack item) {
		super(item);
	}

	public SpoutCraftItemStack(int type, int amount, short damage) {
		super(type, amount, damage);
	}

	public net.minecraft.server.ItemStack getHandle() {
		return this.item;
	}

	public static SpoutCraftItemStack fromItemStack(net.minecraft.server.ItemStack item) {
		if (item == null) {
			return null;
		}

		return new SpoutCraftItemStack(item);
	}

	public static SpoutCraftItemStack getCraftItemStack(org.bukkit.inventory.ItemStack item) {
		if (item == null) {
			return null;
		}
		if (item instanceof SpoutCraftItemStack) {
			return (SpoutCraftItemStack)item;
		}
		if (item instanceof CraftItemStack) {
			CraftItemStack cis = (CraftItemStack)item;
			if (cis.getHandle() != null) {
				return new SpoutCraftItemStack(cis.getHandle());
			}
		}

		SpoutCraftItemStack scis = new SpoutCraftItemStack(item.getTypeId(), item.getAmount(), item.getDurability());
		scis.addUnsafeEnchantments(item.getEnchantments());
		return scis;
	}

	public NBTTagCompound getTag() {
		if (item.tag == null) {
			item.tag = new NBTTagCompound();
		}
		return item.tag;
	}
}
