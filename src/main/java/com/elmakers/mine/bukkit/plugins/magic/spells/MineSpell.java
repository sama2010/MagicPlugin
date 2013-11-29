package com.elmakers.mine.bukkit.plugins.magic.spells;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

import com.elmakers.mine.bukkit.dao.BlockList;
import com.elmakers.mine.bukkit.plugins.magic.Spell;
import com.elmakers.mine.bukkit.plugins.magic.SpellResult;
import com.elmakers.mine.bukkit.utilities.borrowed.ConfigurationNode;

public class MineSpell extends Spell
{
	static final String		DEFAULT_MINEABLE	= "14,15,16, 56, 73, 74, 21 ,129,153";
	static final String		DEFAULT_MINED		= "14,15,263,264,331,331,351,388,406";
	static final String		DEFAULT_DATA		= "0 ,0 ,0  ,0  ,0  ,0  ,4  ,0  ,1";

	private List<Material>	mineableMaterials	= new ArrayList<Material>();
	private List<Material>	minedMaterials	= new ArrayList<Material>();
	private List<Integer>	minedData	= new ArrayList<Integer>();
	private int maxRecursion = 16;

	@SuppressWarnings("deprecation")
	@Override
	public SpellResult onCast(ConfigurationNode parameters) 
	{
		Block target = getTargetBlock();
		if (target == null)
		{
			castMessage("No target");
			return SpellResult.NO_TARGET;
		}
		if (!isMineable(target))
		{
			sendMessage("Can't mine " + target.getType().name().toLowerCase());
			return SpellResult.NO_TARGET;
		}
		if (!hasBuildPermission(target)) {
			castMessage("You don't have permission to build here.");
			return SpellResult.INSUFFICIENT_PERMISSION;
		}

		BlockList minedBlocks = new BlockList();
		Material mineMaterial = target.getType();
		mine(target, mineMaterial, minedBlocks);

		World world = player.getWorld();

		int index = mineableMaterials.indexOf(mineMaterial);
		mineMaterial = minedMaterials.get(index);
		byte data = (byte)(int)minedData.get(index);

		Location itemDrop = new Location(world, target.getX(), target.getY(), target.getZ(), 0, 0);
		ItemStack items = new ItemStack(mineMaterial, (int)minedBlocks.size(), (short)0 , data);
		player.getWorld().dropItemNaturally(itemDrop, items);

		// This isn't undoable, since we can't pick the items back up!
		// So, don't add it to the undo queue.
		castMessage("Mined " + minedBlocks.size() + " blocks of " + mineMaterial.name().toLowerCase());

		return SpellResult.SUCCESS;
	}

	protected void mine(Block block, Material fillMaterial, BlockList minedBlocks)
	{		
		mine(block, fillMaterial, minedBlocks, 0);
	}

	protected void mine(Block block, Material fillMaterial, BlockList minedBlocks, int rDepth)
	{
		minedBlocks.add(block);
		block.setType(Material.AIR);

		if (rDepth < maxRecursion)
		{
			tryMine(block.getRelative(BlockFace.NORTH), fillMaterial, minedBlocks, rDepth + 1);
			tryMine(block.getRelative(BlockFace.WEST), fillMaterial, minedBlocks, rDepth + 1);
			tryMine(block.getRelative(BlockFace.SOUTH), fillMaterial, minedBlocks, rDepth + 1);
			tryMine(block.getRelative(BlockFace.EAST), fillMaterial, minedBlocks, rDepth + 1);
			tryMine(block.getRelative(BlockFace.UP), fillMaterial, minedBlocks, rDepth + 1);
			tryMine(block.getRelative(BlockFace.DOWN), fillMaterial, minedBlocks, rDepth + 1);
		}
	}

	protected void tryMine(Block target, Material fillMaterial, BlockList minedBlocks, int rDepth)
	{
		if (target.getType() != fillMaterial || minedBlocks.contains(target))
		{
			return;
		}

		mine(target, fillMaterial, minedBlocks, rDepth);
	}

	public boolean isMineable(Block block)
	{
		if (block.getType() == Material.AIR)
			return false;

		return mineableMaterials.contains(block.getType());
	}

	@Override
	public void onLoad(ConfigurationNode properties)  
	{
		mineableMaterials = csv.parseMaterials(DEFAULT_MINEABLE);
		minedMaterials = csv.parseMaterials(DEFAULT_MINED);
		minedData = csv.parseIntegers(DEFAULT_DATA);

		maxRecursion = properties.getInteger("recursion_depth", maxRecursion);
	}
}
