package com.elmakers.mine.bukkit.action.builtin;

import com.elmakers.mine.bukkit.action.BaseTeleportAction;
import com.elmakers.mine.bukkit.api.action.CastContext;
import com.elmakers.mine.bukkit.api.action.GUIAction;
import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.api.magic.MageController;
import com.elmakers.mine.bukkit.api.spell.SpellResult;
import com.elmakers.mine.bukkit.api.wand.LostWand;
import com.elmakers.mine.bukkit.block.MaterialAndData;
import com.elmakers.mine.bukkit.utility.CompatibilityUtils;
import com.elmakers.mine.bukkit.utility.ConfigurationUtils;
import com.elmakers.mine.bukkit.utility.InventoryUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class RecallAction extends BaseTeleportAction implements GUIAction
{
    private final static Material DefaultWaypointMaterial = Material.BEACON;
    private final static String MARKER_KEY = "recall_marker";
    private final static String UNLOCKED_WARPS = "recall_warps";

    private boolean allowCrossWorld = true;
    private Map<String, ConfigurationSection> warps = new HashMap<String, ConfigurationSection>();
    private Map<String, ConfigurationSection> commands = new HashMap<String, ConfigurationSection>();
    private List<RecallType> enabledTypes = new ArrayList<RecallType>();
    private Map<Integer, Waypoint> options = new HashMap<Integer, Waypoint>();
    private CastContext context;
    private ConfigurationSection parameters;

    private class UndoMarkerMove implements Runnable
    {
        private final Location location;
        private final Mage mage;

        public UndoMarkerMove(Mage mage, Location currentLocation)
        {
            this.location = currentLocation;
            this.mage = mage;
        }

        @Override
        public void run()
        {
            mage.getData().set(MARKER_KEY, ConfigurationUtils.fromLocation(location));
        }
    }

    private enum RecallType
    {
        COMMAND,
        WARP,
        DEATH,
        SPAWN,
        HOME,
        WAND,
        MARKER
    }

    private static MaterialAndData defaultMaterial = new MaterialAndData(DefaultWaypointMaterial);

    private class Waypoint implements Comparable<Waypoint>
    {
        public final RecallType type;
        public final String name;
        public final String description;
        public final Location location;
        public final String message;
        public final String failMessage;
        public final MaterialAndData icon;
        public final String iconURL;
        public final String command;
        public final boolean opPlayer;
        public final boolean maintainDirection;

        public Waypoint(RecallType type, Location location, String name, String message, String failMessage, String description, MaterialAndData icon, boolean maintainDirection) {
            this.name = name;
            this.type = type;
            this.location = location;
            this.message = message;
            this.description = description;
            this.failMessage = failMessage;
            this.icon = icon == null ? defaultMaterial : icon;
            this.iconURL = null;
            this.command = null;
            this.opPlayer = false;
            this.maintainDirection = maintainDirection;
        }

        public Waypoint(RecallType type, Location location, String name, String message, String failMessage, String description, MaterialAndData icon, String iconURL) {
            this.name = name;
            this.type = type;
            this.location = location;
            this.message = message;
            this.description = description;
            this.failMessage = failMessage;
            this.icon = icon == null ? defaultMaterial : icon;
            this.iconURL = iconURL;
            this.command = null;
            this.opPlayer = false;
            this.maintainDirection = false;
        }

        public Waypoint(RecallType type, String command, boolean opPlayer, String name, String message, String failMessage, String description, MaterialAndData icon, String iconURL) {
            this.name = name;
            this.type = type;
            this.location = null;
            this.message = message;
            this.description = description;
            this.failMessage = failMessage;
            this.icon = icon == null ? defaultMaterial : icon;
            this.iconURL = iconURL;
            this.command = command;
            this.opPlayer = opPlayer;
            this.maintainDirection = false;
        }

        @Override
        public int compareTo(Waypoint o) {
            if (type != o.type)
            {
                if (type == RecallType.COMMAND) return -1;
                if (o.type == RecallType.COMMAND) return 1;
                if (type == RecallType.WARP) return -1;
                if (o.type == RecallType.WARP) return 1;
            }
            return name.compareTo(o.name);
        }

        public boolean isValid(boolean crossWorld, Location source)
        {
            if (isCommand())
            {
                return true;
            }
            if (location == null || location.getWorld() == null)
            {
                return false;
            }
            return crossWorld || source.getWorld().equals(location.getWorld());
        }

        public boolean isCommand() {
            return command != null;
        }
    };

    @Override
    public boolean isUndoable() {
        return true;
    }

    @Override
    public void deactivated() {
// Check for shop items glitched into the player's inventory
        if (context != null) {
            context.getMage().removeItemsWithTag("waypoint");
        }
    }

    @Override
    public void dragged(InventoryDragEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void clicked(InventoryClickEvent event)
    {
        event.setCancelled(true);
        if (context == null) {
            event.getWhoClicked().closeInventory();
            return;
        }
        int slot = event.getSlot();
        if (event.getSlotType() == InventoryType.SlotType.CONTAINER)
        {
            Waypoint waypoint = options.get(slot);
            if (waypoint != null)
            {
                Mage mage = context.getMage();
                Player player = mage.getPlayer();
                mage.deactivateGUI();
                tryTeleport(player, waypoint);
            }
        }
    }

    @Override
    public void prepare(CastContext context, ConfigurationSection parameters) {
        super.prepare(context, parameters);
        this.parameters = parameters;
        this.context = context;

        allowCrossWorld = parameters.getBoolean("allow_cross_world", true);
    }

    @Override
    public SpellResult perform(CastContext context) {
        this.context = context;
		enabledTypes.clear();
        warps.clear();
        commands.clear();

        Mage mage = context.getMage();
        MageController controller = context.getController();
		Player player = mage.getPlayer();
		if (player == null) {
            return SpellResult.PLAYER_REQUIRED;
        }

        Set<String> unlockedWarps = new HashSet<String>();
        ConfigurationSection mageData = mage.getData();

        String unlockedString = mageData.getString(UNLOCKED_WARPS);
        if (unlockedString != null && !unlockedString.isEmpty())
        {
            unlockedWarps.addAll(Arrays.asList(StringUtils.split(unlockedString, ",")));
        }

        ConfigurationSection warpConfig = null;
        if (parameters.contains("warps"))
        {
            warpConfig = parameters.getConfigurationSection("warps");
        }

        ConfigurationSection commandConfig = null;
        if (parameters.contains("commands"))
        {
            commandConfig = parameters.getConfigurationSection("commands");
        }

        if (parameters.contains("unlock"))
        {
            String unlockWarp = parameters.getString("unlock");
            if (unlockWarp == null || unlockWarp.isEmpty() || unlockedWarps.contains(unlockWarp))
            {
                return SpellResult.NO_ACTION;
            }

            if (warpConfig == null)
            {
                return SpellResult.FAIL;
            }

            unlockedWarps.add(unlockWarp);
            unlockedString = StringUtils.join(unlockedWarps, ",");
            mageData.set(UNLOCKED_WARPS, unlockedString);

            String warpName = unlockWarp;
            ConfigurationSection config = warpConfig.getConfigurationSection(unlockWarp);
            if (config != null)
            {
                warpName = config.getString("name", warpName);
            }
            String unlockMessage = context.getMessage("unlock_warp").replace("$name", warpName);
            context.sendMessage(unlockMessage);

            // This is a little hacky :(
            return SpellResult.DEACTIVATE;
        }

        if (parameters.contains("lock"))
        {
            String lockWarp = parameters.getString("lock");
            if (!unlockedWarps.contains(lockWarp))
            {
                return SpellResult.NO_ACTION;
            }

            unlockedWarps.remove(lockWarp);
            unlockedString = StringUtils.join(unlockedWarps, ",");
            mageData.set(UNLOCKED_WARPS, unlockedString);

            return SpellResult.DEACTIVATE;
        }

        Location playerLocation = mage.getLocation();
		for (RecallType testType : RecallType.values())
        {
			// Special-case for warps
			if (testType == RecallType.WARP)
            {
				if (warpConfig != null)
                {
                    Collection<String> warpKeys = warpConfig.getKeys(false);
                    for (String warpKey : warpKeys)
                    {
                        ConfigurationSection config = warpConfig.getConfigurationSection(warpKey);
                        boolean isLocked = config.getBoolean("locked", false);
                        if (!isLocked || unlockedWarps.contains(warpKey))
                        {
                            warps.put(warpKey, config);
                        }
                    }
				}
			}
            // Special-case for warps
            else if (testType == RecallType.COMMAND)
            {
                if (commandConfig != null)
                {
                    Collection<String> commandKeys = commandConfig.getKeys(false);
                    for (String commandKey : commandKeys)
                    {
                        ConfigurationSection config = commandConfig.getConfigurationSection(commandKey);
                        boolean isLocked = config.getBoolean("locked", false);
                        if (!isLocked || unlockedWarps.contains(commandKey))
                        {
                            commands.put(commandKey, config);
                        }
                    }
                }
            }
            else
            {
				if (parameters.getBoolean("allow_" + testType.name().toLowerCase(), true))
                {
					enabledTypes.add(testType);
				}
			}
		}

        if (warps.size() > 0)
        {
            enabledTypes.add(RecallType.WARP);
        }

        if (commands.size() > 0)
        {
            enabledTypes.add(RecallType.COMMAND);
        }

        if (parameters.contains("warp"))
        {
            String warpName = parameters.getString("warp");
            Waypoint waypoint = getWarp(warpName);
            if (tryTeleport(player, waypoint)) {
                return SpellResult.CAST;
            }
            return SpellResult.FAIL;
        }
        else
        if (parameters.contains("command"))
        {
            String commandName = parameters.getString("command");
            Waypoint waypoint = getCommand(commandName);
            if (tryTeleport(player, waypoint)) {
                return SpellResult.CAST;
            }
            return SpellResult.FAIL;
        }
        else if (parameters.contains("type"))
        {
			String typeString = parameters.getString("type", "");
			if (typeString.equalsIgnoreCase("remove"))
            {
				if (removeMarker())
                {
                    return SpellResult.TARGET_SELECTED;
                }
                return SpellResult.FAIL;
			}

            if (typeString.equalsIgnoreCase("place"))
            {
                if (placeMarker(context.getLocation().getBlock()))
                {
                    return SpellResult.TARGET_SELECTED;
                }

                return SpellResult.FAIL;
            }

			RecallType recallType = RecallType.valueOf(typeString.toUpperCase());
			if (recallType == null) {
				controller.getLogger().warning("Unknown recall type " + typeString);
				return SpellResult.FAIL;
			}
			
			Waypoint location = getWaypoint(player, recallType, 0, parameters);
			if (tryTeleport(player, location)) {
				return SpellResult.CAST;
			}
			return SpellResult.FAIL;
		}

        List<Waypoint> allWaypoints = new LinkedList<Waypoint>();
        for (RecallType selectedType : enabledTypes) {
            if (selectedType == RecallType.WARP) {
                for (String warpKey : warps.keySet()) {
                    Waypoint targetLocation = getWarp(warpKey);
                    if (targetLocation != null && targetLocation.isValid(allowCrossWorld, playerLocation)) {
                        allWaypoints.add(targetLocation);
                    }
                }
            } else if (selectedType == RecallType.COMMAND) {
                for (String commandKey : commands.keySet()) {
                    Waypoint targetLocation = getCommand(commandKey);
                    if (targetLocation != null && targetLocation.isValid(allowCrossWorld, playerLocation)) {
                        allWaypoints.add(targetLocation);
                    }
                }
            } else if (selectedType == RecallType.WAND) {
                List<LostWand> lostWands = mage.getLostWands();
                for (int i = 0; i < lostWands.size(); i++) {
                    Waypoint targetLocation = getWaypoint(player, selectedType, i, parameters);
                    if (targetLocation != null && targetLocation.isValid(allowCrossWorld, playerLocation)) {
                        allWaypoints.add(targetLocation);
                    }
                }
            } else {
                Waypoint targetLocation = getWaypoint(player, selectedType, 0, parameters);
                if (targetLocation != null && targetLocation.isValid(allowCrossWorld, playerLocation)) {
                    allWaypoints.add(targetLocation);
                }
            }
        }
        if (allWaypoints.size() == 0) {
            return SpellResult.NO_TARGET;
        }

        options.clear();
        Collections.sort(allWaypoints);
        String inventoryTitle = context.getMessage("title", "Recall");
        int invSize = (int)Math.ceil((float)allWaypoints.size() / 9.0f) * 9;
        Inventory displayInventory = CompatibilityUtils.createInventory(null, invSize, inventoryTitle);
        int index = 0;
        for (Waypoint waypoint : allWaypoints)
        {
            ItemStack waypointItem;
            if (controller.isUrlIconsEnabled() && waypoint.iconURL != null && !waypoint.iconURL.isEmpty()) {
                waypointItem = InventoryUtils.getURLSkull(waypoint.iconURL);
            } else {
                waypointItem = waypoint.icon.getItemStack(1);
            }
            ItemMeta meta = waypointItem == null ? null : waypointItem.getItemMeta();
            if (meta == null) {
                waypointItem = new ItemStack(DefaultWaypointMaterial);
                meta = waypointItem.getItemMeta();
                controller.getLogger().warning("Invalid waypoint icon for " + waypoint.name);
            }
            meta.setDisplayName(waypoint.name);
            if (waypoint.description != null && waypoint.description.length() > 0)
            {
                List<String> lore = new ArrayList<String>();
                lore.add(waypoint.description);
                meta.setLore(lore);
            }
            waypointItem.setItemMeta(meta);
            waypointItem = InventoryUtils.makeReal(waypointItem);
            InventoryUtils.setMeta(waypointItem, "waypoint", "true");
            displayInventory.setItem(index, waypointItem);
            options.put(index, waypoint);
            index++;
        }
        mage.activateGUI(this);
        mage.getPlayer().openInventory(displayInventory);

        return SpellResult.CAST;
	}

    protected Waypoint getWarp(String warpKey)
    {
        if (warps == null) return null;
        ConfigurationSection config = warps.get(warpKey);
        if (config == null) return null;

        MageController controller = context.getController();
        String warpName = config.getString("name", warpKey);
        String castMessage = context.getMessage("cast_warp").replace("$name", warpName);
        String failMessage = context.getMessage("no_target_warp").replace("$name", warpName);
        String title = context.getMessage("title_warp").replace("$name", warpName);
        String description = config.getString("description");
        String iconURL = config.getString("icon_url");
        MaterialAndData icon = ConfigurationUtils.getMaterialAndData(config, "icon");
        return new Waypoint(RecallType.WARP, controller.getWarp(warpKey), title, castMessage, failMessage, description, icon, iconURL);
    }

    protected Waypoint getCommand(String commandKey)
    {
        if (commands == null) return null;
        ConfigurationSection config = commands.get(commandKey);
        if (config == null) return null;

        String commandName = config.getString("name", commandKey);
        String castMessage = context.getMessage("cast_warp").replace("$name", commandName);
        String failMessage = context.getMessage("no_target_warp").replace("$name", commandName);
        String title = context.getMessage("title_warp").replace("$name", commandName);
        String description = config.getString("description");
        String iconURL = config.getString("icon_url");
        String command = config.getString("command");
        boolean op = config.getBoolean("op", false);
        MaterialAndData icon = ConfigurationUtils.getMaterialAndData(config, "icon");
        return new Waypoint(RecallType.COMMAND, command, op, title, castMessage, failMessage, description, icon, iconURL);
    }

	protected Waypoint getWaypoint(Player player, RecallType type, int index, ConfigurationSection parameters) {
		Mage mage = context.getMage();
		switch (type) {
		case MARKER:
            Location location = ConfigurationUtils.getLocation(mage.getData(), MARKER_KEY);
			return new Waypoint(type, location, context.getMessage("title_marker"), context.getMessage("cast_marker"), context.getMessage("no_target_marker"), context.getMessage("description_marker", ""), ConfigurationUtils.getMaterialAndData(parameters, "icon_marker"), true);
		case DEATH:
            return new Waypoint(type, mage.getLastDeathLocation(), "Last Death", context.getMessage("cast_death"), context.getMessage("no_target_death"), context.getMessage("description_death", ""), ConfigurationUtils.getMaterialAndData(parameters, "icon_death"), true);
		case SPAWN:
			return new Waypoint(type, context.getWorld().getSpawnLocation(), context.getMessage("title_spawn"), context.getMessage("cast_spawn"), context.getMessage("no_target_spawn"), context.getMessage("description_spawn", ""), ConfigurationUtils.getMaterialAndData(parameters, "icon_spawn"), false);
        case HOME:
            Location bedLocation = player == null ? null : player.getBedSpawnLocation();
            if (bedLocation != null) {
                bedLocation.setX(bedLocation.getX() + 0.5);
                bedLocation.setZ(bedLocation.getZ() + 0.5);
            }
            return new Waypoint(type, bedLocation, context.getMessage("title_home"), context.getMessage("cast_home"), context.getMessage("no_target_home"), context.getMessage("description_home", ""), ConfigurationUtils.getMaterialAndData(parameters, "icon_home"), false);
		case WAND:
            List<LostWand> lostWands = mage.getLostWands();
			if (lostWands == null || index < 0 || index >= lostWands.size()) return null;
			return new Waypoint(type, lostWands.get(index).getLocation(), context.getMessage("title_wand"), context.getMessage("cast_wand"), context.getMessage("no_target_wand"), context.getMessage("description_wand", ""), ConfigurationUtils.getMaterialAndData(parameters, "icon_wand"), true);
		}
		
		return null;
	}

	protected boolean removeMarker()
	{
        Mage mage = context.getMage();
        ConfigurationSection mageData = mage.getData();
        Location location = ConfigurationUtils.getLocation(mageData, MARKER_KEY);
		if (location == null) return false;
        mageData.set(MARKER_KEY, null);
		return true;
	}
	
	protected boolean tryTeleport(final Player player, final Waypoint waypoint) {
        Mage mage = context.getMage();
        if (waypoint.isCommand()) {
            CommandSender sender = mage.getCommandSender();
            boolean isOp = sender.isOp();
            if (waypoint.opPlayer && !isOp) {
                sender.setOp(true);
            }
            try {
                player.getServer().dispatchCommand(sender, waypoint.command);
            } catch (Exception ex) {
                context.getLogger().log(Level.WARNING, "Error running command " + waypoint.command, ex);
            }
            if (waypoint.opPlayer && !isOp) {
                sender.setOp(false);
            }

            return true;
        }

        Location targetLocation = waypoint == null ? null : waypoint.location;
		if (targetLocation == null) {
            if (waypoint != null) {
                context.sendMessage(waypoint.failMessage);
            }
			return false;
		}
		if (!allowCrossWorld && !mage.getLocation().getWorld().equals(targetLocation.getWorld())) {
            context.sendMessage("cross_world_disallowed");
			return false;
		}

        if (waypoint.maintainDirection)
        {
            Location playerLocation = player.getLocation();
            targetLocation.setYaw(playerLocation.getYaw());
            targetLocation.setPitch(playerLocation.getPitch());
        }
        context.teleport(player, targetLocation, verticalSearchDistance);
        context.castMessage(waypoint.message);
		return true;
	}

	protected boolean placeMarker(Block target)
	{
		if (target == null)
		{
			return false;
		}

        Mage mage = context.getMage();
        ConfigurationSection mageData = mage.getData();
        Location location = ConfigurationUtils.getLocation(mageData, MARKER_KEY);

        context.registerForUndo(new UndoMarkerMove(mage, location));
		if (location != null) 
		{
            context.sendMessage("cast_marker_move");
		}
		else
		{
            context.sendMessage("cast_marker_place");
		}

		location = context.getLocation();
		location.setX(target.getX());
		location.setY(target.getY());
		location.setZ(target.getZ());

        mageData.set(MARKER_KEY, ConfigurationUtils.fromLocation(location));
        return true;
	}
	
	@Override
	public void load(Mage mage, ConfigurationSection node)
	{
        // This is here for backwards-compatibility with RecallSpell
		Location location = ConfigurationUtils.getLocation(node, "location");
        if (location != null)
        {
            mage.getData().set(MARKER_KEY, ConfigurationUtils.fromLocation(location));
        }
	}
}
