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

import net.minecraft.server.IInventory;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.getspout.spoutapi.inventory.DoubleChestInventory;

public class SpoutDoubleChestInventory extends SpoutCraftInventory implements DoubleChestInventory {
	protected Block top;
	protected Block bottom;
	public SpoutDoubleChestInventory(IInventory inventory, Block top, Block bottom) {
		super(inventory);
		this.top = top;
		this.bottom = bottom;
	}

	@Override
	public Block getTopHalf() {
		return top;
	}

	@Override
	public Block getBottomHalf() {
		return bottom;
	}

	@Override
	public Block getLeftSide() {
		if ((this.getDirection() == BlockFace.WEST) || (this.getDirection() == BlockFace.NORTH)) {
			return top;
		} else {
			return bottom;
		}
	}

	@Override
	public Block getRightSide() {
		if (this.getLeftSide().equals(top)) {
			return bottom;
		} else {
			return top;
		}
	}

	@Override
	public BlockFace getDirection() {
		if (top.getLocation().getBlockX() == bottom.getLocation().getBlockX()) {
			return this.isReversed(BlockFace.SOUTH) ? BlockFace.NORTH : BlockFace.SOUTH;
		} else {
			return this.isReversed(BlockFace.WEST) ? BlockFace.EAST : BlockFace.WEST;
		}
	}

	private boolean isReversed(BlockFace primary) {
		BlockFace secondary = primary.getOppositeFace();
		if (isSolid(top.getRelative(secondary)) || isSolid(bottom.getRelative(secondary))) {
			return false;
		} else {
			return isSolid(top.getRelative(primary)) || isSolid(bottom.getRelative(primary));
		}
	}

	private static boolean isSolid(Block block) {
		// o[]: If block type is completely solid.
		// This should really be part of Spout or Bukkit, but for now it's here.
		return net.minecraft.server.Block.n[block.getTypeId()];
	}
}
