package com.elmakers.mine.bukkit.wand;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.logging.Level;

import com.elmakers.mine.bukkit.api.block.BrushMode;
import com.elmakers.mine.bukkit.api.magic.MageController;
import com.elmakers.mine.bukkit.api.magic.Messages;
import com.elmakers.mine.bukkit.api.spell.CostReducer;
import com.elmakers.mine.bukkit.api.spell.MageSpell;
import com.elmakers.mine.bukkit.api.spell.Spell;
import com.elmakers.mine.bukkit.api.spell.SpellKey;
import com.elmakers.mine.bukkit.api.spell.SpellTemplate;
import com.elmakers.mine.bukkit.block.MaterialAndData;
import com.elmakers.mine.bukkit.block.MaterialBrush;
import com.elmakers.mine.bukkit.effect.builtin.EffectRing;
import com.elmakers.mine.bukkit.heroes.HeroesManager;
import com.elmakers.mine.bukkit.magic.Mage;
import com.elmakers.mine.bukkit.magic.MagicController;
import com.elmakers.mine.bukkit.utility.ColorHD;
import com.elmakers.mine.bukkit.utility.CompatibilityUtils;
import com.elmakers.mine.bukkit.utility.ConfigurationUtils;
import com.elmakers.mine.bukkit.utility.InventoryUtils;
import com.elmakers.mine.bukkit.utility.NMSUtils;
import com.elmakers.mine.bukkit.utility.SoundEffect;
import de.slikey.effectlib.util.ParticleEffect;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class Wand implements CostReducer, com.elmakers.mine.bukkit.api.wand.Wand {
	public final static int INVENTORY_SIZE = 27;
	public final static int HOTBAR_SIZE = 9;
	public final static int HOTBAR_INVENTORY_SIZE = HOTBAR_SIZE - 1;
	public final static float DEFAULT_SPELL_COLOR_MIX_WEIGHT = 0.0001f;
	public final static float DEFAULT_WAND_COLOR_MIX_WEIGHT = 1.0f;

    public final static String[] EMPTY_PARAMETERS = new String[0];
	
	// REMEMBER! Each of these MUST have a corresponding class in .traders, else traders will
	// destroy the corresponding data.
	public final static String[] PROPERTY_KEYS = {
		"active_spell", "active_material",
        "path", "passive",
		"xp", "xp_regeneration", "xp_max", "xp_max_boost", "xp_regeneration_boost",
		"bound", "has_uses", "uses", "upgrade", "indestructible", "undroppable",
		"cost_reduction", "cooldown_reduction", "effect_bubbles", "effect_color", 
		"effect_particle", "effect_particle_count", "effect_particle_data", "effect_particle_interval",
        "effect_particle_min_velocity",
        "effect_particle_radius", "effect_particle_offset",
        "effect_sound", "effect_sound_interval", "effect_sound_pitch", "effect_sound_volume",
        "cast_spell", "cast_parameters", "cast_interval", "cast_min_velocity", "cast_velocity_direction",
		"hotbar_count", "hotbar",
		"icon", "mode", "brush_mode", "keep", "locked", "quiet", "force", "randomize", "rename",
		"power", "overrides",
		"protection", "protection_physical", "protection_projectiles", 
		"protection_falling", "protection_fire", "protection_explosions",
        "potion_effects",
		"materials", "spells", "powered", "protected", "heroes"
	};

	public final static String[] HIDDEN_PROPERTY_KEYS = {
		"id", "owner", "owner_id", "name", "description", "template",
		"organize", "alphabetize", "fill", "stored", "upgrade_icon", "xp_timestamp",
        // For legacy wands
        "haste",
        "health_regeneration", "hunger_regeneration",
    };
	public final static String[] ALL_PROPERTY_KEYS = (String[])ArrayUtils.addAll(PROPERTY_KEYS, HIDDEN_PROPERTY_KEYS);
	
	protected ItemStack item;
	protected MagicController controller;
	protected Mage mage;
	
	// Cached state
	private String id = "";
	private List<Inventory> hotbars;
	private List<Inventory> inventories;
    private Map<String, Integer> spells = new HashMap<String, Integer>();
    private Map<String, Integer> spellLevels = new HashMap<String, Integer>();
    private Map<String, Integer> brushes = new HashMap<String, Integer>();
	
	private String activeSpell = "";
	private String activeMaterial = "";
	protected String wandName = "";
	protected String description = "";
	private String owner = "";
    private String ownerId = "";
	private String template = "";
    private String path = "";
    private boolean superProtected = false;
    private boolean superPowered = false;
    private boolean glow = false;
	private boolean bound = false;
	private boolean indestructible = false;
    private boolean undroppable = false;
	private boolean keep = false;
    private boolean passive = false;
	private boolean autoOrganize = false;
    private boolean autoAlphabetize = false;
	private boolean autoFill = false;
	private boolean isUpgrade = false;
    private boolean randomize = false;
    private boolean rename = false;
	
	private MaterialAndData icon = null;
    private MaterialAndData upgradeIcon = null;
	
	protected float costReduction = 0;
    protected float cooldownReduction = 0;
    protected float damageReduction = 0;
    protected float damageReductionPhysical = 0;
    protected float damageReductionProjectiles = 0;
    protected float damageReductionFalling = 0;
    protected float damageReductionFire = 0;
    protected float damageReductionExplosions = 0;
    private float power = 0;

	private boolean hasInventory = false;
	private boolean locked = false;
    private boolean forceUpgrade = false;
    private boolean isHeroes = false;
	private int uses = 0;
    private boolean hasUses = false;
	private float xp = 0;

    private float xpMaxBoost = 0;
    private float xpRegenerationBoost = 0;
	private int xpRegeneration = 0;
	private int xpMax = 0;
    private long lastXpRegeneration = 0;
    private int effectiveXpMax = 0;
    private int effectiveXpRegeneration = 0;
	
	private ColorHD effectColor = null;
	private float effectColorSpellMixWeight = DEFAULT_SPELL_COLOR_MIX_WEIGHT;
	private float effectColorMixWeight = DEFAULT_WAND_COLOR_MIX_WEIGHT;	
	private ParticleEffect effectParticle = null;
	private float effectParticleData = 0;
	private int effectParticleCount = 0;
	private int effectParticleInterval = 0;
    private double effectParticleMinVelocity = 0;
    private double effectParticleRadius = 0.2;
    private double effectParticleOffset = 0.3;
	private boolean effectBubbles = false;
	private EffectRing effectPlayer = null;

    private int castInterval = 0;
    private double castMinVelocity = 0;
    private Vector castVelocityDirection = null;
    private String castSpell = null;
    private String[] castParameters = null;

    private Map<PotionEffectType, Integer> potionEffects = new HashMap<PotionEffectType, Integer>();

    private SoundEffect effectSound = null;
	private int effectSoundInterval = 0;

    private int quietLevel = 0;
    private Map<String, String> castOverrides = null;

    // Transient state

	private int storedXpLevel = 0;
	private float storedXpProgress = 0;

    private long lastLocationTime;
    private Vector lastLocation;

    private long lastSoundEffect;
    private long lastParticleEffect;
    private long lastSpellCast;
	
	// Inventory functionality
	
	private WandMode mode = null;
    private WandMode brushMode = null;
	private int openInventoryPage = 0;
	private boolean inventoryIsOpen = false;
	private Inventory displayInventory = null;
	private int currentHotbar = 0;
	
	// Kinda of a hacky initialization optimization :\
	private boolean suspendSave = false;

	// Wand configurations
	protected static Map<String, ConfigurationSection> wandTemplates = new HashMap<String, ConfigurationSection>();
	
	public static boolean displayManaAsBar = true;
    public static boolean displayManaAsDurability = true;
    public static boolean displayManaAsGlow = true;
	public static boolean retainLevelDisplay = true;
    public static boolean regenWhileInactive = true;
	public static Material DefaultUpgradeMaterial = Material.NETHER_STAR;
	public static Material DefaultWandMaterial = Material.BLAZE_ROD;
	public static Material EnchantableWandMaterial = null;
    public static boolean SpellGlow = false;
    public static boolean BrushGlow = false;
    public static boolean BrushItemGlow = true;
    public static boolean LiveHotbar = true;
    public static SoundEffect inventoryOpenSound = null;
    public static SoundEffect inventoryCloseSound = null;
    public static SoundEffect inventoryCycleSound = null;
    public static String WAND_KEY = "wand";
    public static byte HIDE_FLAGS = 60;

    private Inventory storedInventory = null;
    private Integer playerInventorySlot = null;

    private static final ItemStack[] itemTemplate = new ItemStack[0];

	public Wand(MagicController controller, ItemStack itemStack) {
		this.controller = controller;
        wandName = controller.getMessages().get("wand.default_name");
        hotbars = new ArrayList<Inventory>();
		setHotbarCount(1);
		this.icon = new MaterialAndData(itemStack);
		inventories = new ArrayList<Inventory>();
        item = itemStack;
		loadState();
        updateName();
        updateLore();
	}

	protected void setHotbarCount(int count) {
		hotbars.clear();
		while (hotbars.size() < count) {
			hotbars.add(CompatibilityUtils.createInventory(null, HOTBAR_INVENTORY_SIZE, "Wand"));
		}
		while (hotbars.size() > count) {
			hotbars.remove(0);
		}
	}
	
	public Wand(MagicController controller) {
		this(controller, DefaultWandMaterial, (short)0);
	}

    public Wand(MagicController controller, ConfigurationSection config) {
        this(controller, DefaultWandMaterial, (short)0);
        loadProperties(config);
        updateName();
        updateLore();
        saveState();
    }
	
	protected Wand(MagicController controller, String templateName) throws UnknownWandException {
		this(controller);
		suspendSave = true;
		String wandName = controller.getMessages().get("wand.default_name");
		String wandDescription = "";

		// Check for default wand
		if ((templateName == null || templateName.length() == 0) && wandTemplates.containsKey("default"))
		{
			templateName = "default";
		}
		
		// See if there is a template with this key
		if (templateName != null && templateName.length() > 0) {
            // Check for randomized/pre-enchanted wands
            int level = 0;
            if (templateName.contains("(")) {
                String levelString = templateName.substring(templateName.indexOf('(') + 1, templateName.length() - 1);
                try {
                    level = Integer.parseInt(levelString);
                } catch (Exception ex) {
                    throw new IllegalArgumentException(ex);
                }
                templateName = templateName.substring(0, templateName.indexOf('('));
            }

			if (!wandTemplates.containsKey(templateName)) {
				throw new UnknownWandException(templateName);
			}
			ConfigurationSection wandConfig = wandTemplates.get(templateName);

			// Default to template names, override with localizations
            wandName = wandConfig.getString("name", wandName);
			wandName = controller.getMessages().get("wands." + templateName + ".name", wandName);
            wandDescription = wandConfig.getString("description", wandDescription);
			wandDescription = controller.getMessages().get("wands." + templateName + ".description", wandDescription);

			// Load all properties
			loadProperties(wandConfig);

            // Add vanilla enchantments
            if (wandConfig.contains("enchantments") && item != null)
            {
                ConfigurationSection enchantConfig = wandConfig.getConfigurationSection("enchantments");
                Collection<String> enchantKeys = enchantConfig.getKeys(false);
                for (String enchantKey : enchantKeys)
                {
                    try {
                        Enchantment enchantment = Enchantment.getByName(enchantKey.toUpperCase());
                        item.addUnsafeEnchantment(enchantment, enchantConfig.getInt(enchantKey));
                    } catch (Exception ex) {
                        controller.getLogger().warning("Invalid enchantment: " + enchantKey);
                    }
                }
            }

            // Enchant, if an enchanting level was provided
            if (level > 0) {
                // Account for randomized locked wands
                boolean wasLocked = locked;
                locked = false;
                randomize(level, false, null);
                locked = wasLocked;
            }
		}
		setDescription(wandDescription);
		setName(wandName);

        // Don't randomize now if set to randomize later
        // Otherwise, do this here so the description updates
        if (!randomize) {
            randomize();
        }

        setTemplate(templateName);
		suspendSave = false;
		saveState();
	}
	
	public Wand(MagicController controller, Material icon, short iconData) {
		// This will make the Bukkit ItemStack into a real ItemStack with NBT data.
		this(controller, InventoryUtils.makeReal(new ItemStack(icon, 1, iconData)));
        saveState();
		updateName();
	}
	
	public void unenchant() {
		item = new ItemStack(item.getType(), 1, (short)item.getDurability());
	}
	
	public void setIcon(Material material, byte data) {
		setIcon(material == null ? null : new MaterialAndData(material, data));
	}
	
	public void setIcon(MaterialAndData materialData) {
        if (materialData == null || !materialData.isValid()) return;
		icon = materialData;
        if (icon != null) {
            icon.applyToItem(item);
        }
	}
	
	public void makeUpgrade() {
        if (!isUpgrade) {
            isUpgrade = true;
            String oldName = wandName;
            wandName = controller.getMessages().get("wand.upgrade_name");
            wandName = wandName.replace("$name", oldName);
            description = controller.getMessages().get("wand.upgrade_default_description");
            if (template != null && template.length() > 0) {
                description = controller.getMessages().get("wands." + template + ".upgrade_description", description);
            }
            setIcon(DefaultUpgradeMaterial, (byte) 0);
            saveState();
            updateName(true);
            updateLore();
        }
	}

    public String getLostId() { return id; }
    public void clearLostId() {
        if (id != null) {
            id = null;
            saveState();
        }
    }

    public float getXpRegenerationBoost() {
        return xpRegenerationBoost;
    }

    public float getXpMaxBoost() {
        return xpMaxBoost;
    }
	
	public int getXpRegeneration() {
		return xpRegeneration;
	}

	public int getXpMax() {
		return xpMax;
	}
	
	public int getMana() {
		return (int)xp;
	}

	public void removeMana(int amount) {
        if (isHeroes && mage != null) {
            HeroesManager heroes = controller.getHeroes();
            if (heroes != null) {
                heroes.removeMana(mage.getPlayer(), amount);
            }
        }
		xp = Math.max(0,  xp - amount);
		updateMana();
	}
	
	public boolean isModifiable() {
		return !locked;
	}
	
	public boolean isIndestructible() {
		return indestructible;
	}

    public boolean isUndroppable() {
		// Don't allow dropping wands while the inventory is open.
        return undroppable || isInventoryOpen();
    }
	
	public boolean isUpgrade() {
		return isUpgrade;
	}
	
	public boolean usesMana() {
		return (xpMax > 0 && xpRegeneration > 0 && !isCostFree()) || isHeroes;
	}

	public float getCooldownReduction() {
		return controller.getCooldownReduction() + cooldownReduction * controller.getMaxCooldownReduction();
	}

	public float getCostReduction() {
		if (isCostFree()) return 1.0f;
		return controller.getCostReduction() + costReduction * controller.getMaxCostReduction();
	}
	
	public void setCooldownReduction(float reduction) {
		cooldownReduction = reduction;
	}
	
	public boolean getHasInventory() {
		return hasInventory;
	}

	public float getPower() {
		return power;
	}
	
	public boolean isSuperProtected() {
		return superProtected;
	}
	
	public boolean isSuperPowered() {
		return superPowered;
	}
	
	public boolean isCostFree() {
		return costReduction > 1;
	}
	
	public boolean isCooldownFree() {
		return cooldownReduction > 1;
	}

	public float getDamageReduction() {
		return damageReduction;
	}

	public float getDamageReductionPhysical() {
		return damageReductionPhysical;
	}
	
	public float getDamageReductionProjectiles() {
		return damageReductionProjectiles;
	}

	public float getDamageReductionFalling() {
		return damageReductionFalling;
	}

	public float getDamageReductionFire() {
		return damageReductionFire;
	}

	public float getDamageReductionExplosions() {
		return damageReductionExplosions;
	}
	
	public String getName() {
		return wandName;
	}
	
	public String getDescription() {
		return description;
	}
	
	public String getOwner() {
		return owner;
	}

    public String getOwnerId() {
        return ownerId;
    }

    public long getWorth() {
        long worth = 0;
        // TODO: Item properties, brushes, etc
        Set<String> spells = getSpells();
        for (String spellKey : spells) {
            SpellTemplate spell = controller.getSpellTemplate(spellKey);
            if (spell != null) {
                worth += spell.getWorth();
            }
        }
        return worth;
    }

	public void setName(String name) {
		wandName = ChatColor.stripColor(name);
		updateName();
	}
	
	public void setTemplate(String templateName) {
		this.template = templateName;
	}

    @Override
	public String getTemplate() {
		return this.template;
	}

    public com.elmakers.mine.bukkit.api.wand.WandUpgradePath getPath() {
        String pathKey = path;
        if (pathKey == null || pathKey.length() == 0) {
            pathKey = controller.getDefaultWandPath();
        }
        return WandUpgradePath.getPath(pathKey);
    }

    public boolean hasPath() {
        return path != null && path.length() > 0;
    }
	
	public void setDescription(String description) {
		this.description = description;
		updateLore();
	}
	
	public void tryToOwn(Player player) {
        if (ownerId == null || ownerId.length() == 0) {
            takeOwnership(player);
        }
	}

    protected void takeOwnership(Player player) {
        takeOwnership(player, controller != null && controller.bindWands(), controller != null && controller.keepWands());
    }

     public void takeOwnership(Player player, boolean setBound, boolean setKeep) {
        if (mage != null && (ownerId == null || ownerId.length() == 0))
        {
            mage.sendMessage(controller.getMessages().get("wand.bound_instructions", "").replace("$wand", getName()));
            Spell spell = getActiveSpell();
            if (spell != null)
            {
                String message = controller.getMessages().get("wand.spell_instructions", "").replace("$wand", getName());
                mage.sendMessage(message.replace("$spell", spell.getName()));
            }
            if (spells.size() > 1)
            {
                mage.sendMessage(controller.getMessages().get("wand.inventory_instructions", "").replace("$wand", getName()));
            }
            com.elmakers.mine.bukkit.api.wand.WandUpgradePath path = getPath();
            if (path != null)
            {
                String message = controller.getMessages().get("wand.enchant_instructions", "").replace("$wand", getName());
                mage.sendMessage(message);
            }
        }
        owner = ChatColor.stripColor(player.getDisplayName());
        ownerId = player.getUniqueId().toString();
		if (setBound) {
			bound = true;
		}
		if (setKeep) {
			keep = true;
		}
		updateLore();
	}
	
	public ItemStack getItem() {
		return item;
	}
	
	protected List<Inventory> getAllInventories() {
        WandMode mode = getMode();
        int hotbarCount = mode == WandMode.INVENTORY ? hotbars.size() : 0;
		List<Inventory> allInventories = new ArrayList<Inventory>(inventories.size() + hotbarCount);
        if (mode == WandMode.INVENTORY) {
            allInventories.addAll(hotbars);
        }
		allInventories.addAll(inventories);
		return allInventories;
	}
	
	public Set<String> getSpells() {
		return spells.keySet();
	}

    protected String getSpellString() {
		Set<String> spellNames = new TreeSet<String>();
        for (Map.Entry<String, Integer> spellSlot : spells.entrySet()) {
            Integer slot = spellSlot.getValue();
            String spellKey = spellSlot.getKey();
            if (slot != null) {
                spellKey += "@" + slot;
            }
            spellNames.add(spellKey);
        }
		return StringUtils.join(spellNames, ",");
	}

	public Set<String> getBrushes() {
		return brushes.keySet();
	}

    protected String getMaterialString() {
		Set<String> materialNames = new TreeSet<String>();
        for (Map.Entry<String, Integer> brushSlot : brushes.entrySet()) {
            Integer slot = brushSlot.getValue();
            String materialKey = brushSlot.getKey();
            if (slot != null) {
                materialKey += "@" + slot;
            }
            materialNames.add(materialKey);
        }
		return StringUtils.join(materialNames, ",");
	}
	
	protected Integer parseSlot(String[] pieces) {
		Integer slot = null;
		if (pieces.length > 1) {
			try {
				slot = Integer.parseInt(pieces[1]);
			} catch (Exception ex) {
				slot = null;
			}
			if (slot != null && slot < 0) {
				slot = null;
			}
		}
		return slot;
	}
	
	protected void addToInventory(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return;
        }
        if (getBrushMode() != WandMode.INVENTORY && isBrush(itemStack)) {
            String brushKey = getBrush(itemStack);
            if (!MaterialBrush.isSpecialMaterialKey(brushKey) || MaterialBrush.isSchematic(brushKey))
            {
                return;
            }
        }
		List<Inventory> checkInventories = getAllInventories();
		boolean added = false;

		for (Inventory inventory : checkInventories) {
			HashMap<Integer, ItemStack> returned = inventory.addItem(itemStack);

			if (returned.size() == 0) {
				added = true;
				break;
			}
		}
		if (!added) {
			Inventory newInventory = CompatibilityUtils.createInventory(null, INVENTORY_SIZE, "Wand");
			newInventory.addItem(itemStack);
			inventories.add(newInventory);
		}
	}
	
	protected Inventory getDisplayInventory() {
		if (displayInventory == null) {
            int inventorySize = INVENTORY_SIZE;
            if (getMode() == WandMode.INVENTORY) {
                inventorySize += HOTBAR_SIZE;
            }
			displayInventory = CompatibilityUtils.createInventory(null, inventorySize, "Wand");
		}
		
		return displayInventory;
	}
	
	protected Inventory getInventoryByIndex(int inventoryIndex) {
		while (inventoryIndex >= inventories.size()) {
			inventories.add(CompatibilityUtils.createInventory(null, INVENTORY_SIZE, "Wand"));
		}
		return inventories.get(inventoryIndex);
	}

	protected int getHotbarSize() {
        if (getMode() != WandMode.INVENTORY) return 0;
		return hotbars.size() * HOTBAR_INVENTORY_SIZE;
	}
	
	protected Inventory getInventory(Integer slot) {
		Inventory inventory = null;
		int hotbarSize = getHotbarSize();
		if (slot >= hotbarSize) {
			int inventoryIndex = (slot - hotbarSize) / INVENTORY_SIZE;
			inventory = getInventoryByIndex(inventoryIndex);
		} else {
			inventory = hotbars.get(slot / HOTBAR_INVENTORY_SIZE);
		}
		
		return inventory;
	}
	
	protected int getInventorySlot(Integer slot) {
		int hotbarSize = getHotbarSize();
		if (slot < hotbarSize) {
			return slot % HOTBAR_INVENTORY_SIZE;
		}
		
		return ((slot - hotbarSize) % INVENTORY_SIZE);
	}
	
	protected void addToInventory(ItemStack itemStack, Integer slot) {
		if (slot == null) {
			addToInventory(itemStack);
			return;
		}
		Inventory inventory = getInventory(slot);
		slot = getInventorySlot(slot);
		
		ItemStack existing = inventory.getItem(slot);
		inventory.setItem(slot, itemStack);
		
		if (existing != null && existing.getType() != Material.AIR) {
			addToInventory(existing);
		}
	}
	
	protected void parseInventoryStrings(String spellString, String materialString) {
        // Force an update of the display inventory since chest mode is a different size
        displayInventory = null;

		for (Inventory hotbar : hotbars) {
			hotbar.clear();
		}
		inventories.clear();
        spells.clear();
        spellLevels.clear();
        brushes.clear();

		// Support YML-List-As-String format
		spellString = spellString.replaceAll("[\\]\\[]", "");
		String[] spellNames = StringUtils.split(spellString, ",");
		for (String spellName : spellNames)
        {
			String[] pieces = spellName.split("@");
			Integer slot = parseSlot(pieces);

            // Handle aliases and upgrades smoothly
            String loadedKey = pieces[0].trim();
            SpellTemplate spell = controller.getSpellTemplate(loadedKey);
            if (spell != null)
            {
                SpellKey spellKey = new SpellKey(spell.getKey());
                Integer currentLevel = spellLevels.get(spellKey.getBaseKey());
                if (currentLevel == null || currentLevel < spellKey.getLevel()) {
                    spellLevels.put(spellKey.getBaseKey(), spellKey.getLevel());
                    spells.put(spellKey.getKey(), slot);
                    if (currentLevel != null)
                    {
                        SpellKey oldKey = new SpellKey(spellKey.getBaseKey(), currentLevel);
                        spells.remove(oldKey.getKey());
                    }
                }
                if (activeSpell == null || activeSpell.length() == 0)
                {
                    activeSpell = spellKey.getKey();
                }
            }
            ItemStack itemStack = createSpellItem(spell, controller, getActivePlayer(), this, false);
			if (itemStack == null)
            {
				controller.getPlugin().getLogger().warning("Unable to create spell icon for key " + loadedKey + " - someone has a dead spell");
				itemStack = new ItemStack(item.getType(), 1);
                CompatibilityUtils.setDisplayName(itemStack, loadedKey);
                CompatibilityUtils.setMeta(itemStack, "spell", loadedKey);
            }
			addToInventory(itemStack, slot);
		}
		materialString = materialString.replaceAll("[\\]\\[]", "");
		String[] materialNames = StringUtils.split(materialString, ",");
        WandMode brushMode = getBrushMode();
		for (String materialName : materialNames) {
			String[] pieces = materialName.split("@");
			Integer slot = parseSlot(pieces);
			String materialKey = pieces[0].trim();
            brushes.put(materialKey, slot);
            boolean addToInventory = brushMode == WandMode.INVENTORY || (MaterialBrush.isSpecialMaterialKey(materialKey) && !MaterialBrush.isSchematic(materialKey));
            if (addToInventory)
            {
                ItemStack itemStack = createBrushIcon(materialKey);
                if (itemStack == null) {
                    controller.getPlugin().getLogger().warning("Unable to create material icon for key " + materialKey);
                    continue;
                }
                if (activeMaterial == null || activeMaterial.length() == 0) activeMaterial = materialKey;
                addToInventory(itemStack, slot);
            }
		}
		hasInventory = spellNames.length + materialNames.length > 1;
        if (openInventoryPage >= inventories.size()) {
            openInventoryPage = 0;
        }
	}

    protected ItemStack createSpellIcon(SpellTemplate spell) {
        return createSpellItem(spell, controller, getActivePlayer(), this, false);
    }

    public static ItemStack createSpellItem(String spellKey, MagicController controller, Wand wand, boolean isItem) {
        return createSpellItem(controller.getSpellTemplate(spellKey), controller, wand == null ? null : wand.getActivePlayer(), wand, isItem);
    }

    public static ItemStack createSpellItem(String spellKey, MagicController controller, com.elmakers.mine.bukkit.api.magic.Mage mage, Wand wand, boolean isItem) {
        return createSpellItem(controller.getSpellTemplate(spellKey), controller, mage, wand, isItem);
    }

    @SuppressWarnings("deprecation")
	public static ItemStack createSpellItem(SpellTemplate spell, MagicController controller, com.elmakers.mine.bukkit.api.magic.Mage mage, Wand wand, boolean isItem) {
		if (spell == null) return null;
        String iconURL = spell.getIconURL();

        ItemStack itemStack = null;
        if (iconURL != null && controller.isUrlIconsEnabled())
        {
            itemStack = InventoryUtils.getURLSkull(iconURL);
        }

        if (itemStack == null)
        {
            ItemStack originalItemStack = null;
            com.elmakers.mine.bukkit.api.block.MaterialAndData icon = spell.getIcon();
            if (icon == null) {
                controller.getPlugin().getLogger().warning("Unable to create spell icon for " + spell.getName() + ", missing material");
                return null;
            }
            try {
                originalItemStack = new ItemStack(icon.getMaterial(), 1, icon.getData());
                itemStack = InventoryUtils.makeReal(originalItemStack);
            } catch (Exception ex) {
                itemStack = null;
            }

            if (itemStack == null) {
                String iconName = icon == null ? "Unknown" : icon.getName();
                controller.getPlugin().getLogger().warning("Unable to create spell icon for " + spell.getKey() + " with material " + iconName);
                return originalItemStack;
            }
        }
		updateSpellItem(controller.getMessages(), itemStack, spell, mage, wand, wand == null ? null : wand.activeMaterial, isItem);
		return itemStack;
	}
	
	protected ItemStack createBrushIcon(String materialKey) {
		return createBrushItem(materialKey, controller, this, false);
	}
	
	@SuppressWarnings("deprecation")
	public static ItemStack createBrushItem(String materialKey, com.elmakers.mine.bukkit.api.magic.MageController controller, Wand wand, boolean isItem) {
		MaterialBrush brushData = MaterialBrush.parseMaterialKey(materialKey);
		if (brushData == null) return null;

        ItemStack itemStack = brushData.getItem(controller, isItem);
        if (BrushGlow || (isItem && BrushItemGlow))
        {
            CompatibilityUtils.addGlow(itemStack);
        }
        updateBrushItem(controller.getMessages(), itemStack, brushData, wand);
		return itemStack;
	}

	protected void saveState() {
		if (suspendSave) return;
        if (checkWandItem()) {
            updateName();
            updateLore();
            if (displayManaAsDurability && xpMax > 0 && xpRegeneration > 0) {
                updateDurability();
            }
        }

        ConfigurationSection stateNode = new MemoryConfiguration();
        saveProperties(stateNode);

		Object wandNode = InventoryUtils.createNode(item, WAND_KEY);
		if (wandNode == null) {
			controller.getLogger().warning("Failed to save wand state for wand to : " + item + " in slot " + playerInventorySlot);
		} else {
            InventoryUtils.saveTagsToNBT(stateNode, wandNode, ALL_PROPERTY_KEYS);
        }
	}
	
	protected void loadState() {
		if (item == null) return;

        Object wandNode = InventoryUtils.getNode(item, WAND_KEY);
        if (wandNode == null) {
            return;
        }

        ConfigurationSection stateNode = new MemoryConfiguration();
        InventoryUtils.loadTagsFromNBT(stateNode, wandNode, ALL_PROPERTY_KEYS);

        loadProperties(stateNode);
	}

    protected String getPotionEffectString() {
        if (potionEffects.size() == 0) return null;
        Collection<String> effectStrings = new ArrayList<String>();
        for (Map.Entry<PotionEffectType, Integer> entry : potionEffects.entrySet()) {
            String effectString = entry.getKey().getName();
            if (entry.getValue() > 1) {
                effectString += ":" + entry.getValue();
            }
            effectStrings.add(effectString);
        }
        return StringUtils.join(effectStrings, ",");
    }

	public void saveProperties(ConfigurationSection node) {
		node.set("id", id);
		node.set("materials", getMaterialString());
		node.set("spells", getSpellString());
        node.set("potion_effects", getPotionEffectString());
		node.set("hotbar_count", hotbars.size());
		node.set("hotbar", currentHotbar);
		node.set("active_spell", activeSpell);
		node.set("active_material", activeMaterial);
		node.set("name", wandName);
		node.set("description", description);
		node.set("owner", owner);
        node.set("owner_id", ownerId);

		node.set("cost_reduction", costReduction);
		node.set("cooldown_reduction", cooldownReduction);
		node.set("power", power);
		node.set("protection", damageReduction);
		node.set("protection_physical", damageReductionPhysical);
		node.set("protection_projectiles", damageReductionProjectiles);
		node.set("protection_falling", damageReductionFalling);
		node.set("protection_fire", damageReductionFire);
		node.set("protection_explosions", damageReductionExplosions);
		node.set("xp", xp);
		node.set("xp_regeneration", xpRegeneration);
		node.set("xp_max", xpMax);
        node.set("xp_max_boost", xpMaxBoost);
        node.set("xp_regeneration_boost", xpRegenerationBoost);
        node.set("xp_timestamp", lastXpRegeneration);
		node.set("uses", uses);
        node.set("has_uses", hasUses);
		node.set("locked", locked);
		node.set("effect_color", effectColor == null ? "none" : effectColor.toString());
		node.set("effect_bubbles", effectBubbles);
		node.set("effect_particle_data", Float.toString(effectParticleData));
		node.set("effect_particle_count", effectParticleCount);
        node.set("effect_particle_min_velocity", effectParticleMinVelocity);
		node.set("effect_particle_interval", effectParticleInterval);
        node.set("effect_particle_radius", effectParticleRadius);
        node.set("effect_particle_offset", effectParticleOffset);
		node.set("effect_sound_interval", effectSoundInterval);

        node.set("cast_interval", castInterval);
        node.set("cast_min_velocity", castMinVelocity);
        String directionString = null;
        if (castVelocityDirection != null) {
            directionString = ConfigurationUtils.fromVector(castVelocityDirection);
        }
        node.set("cast_velocity_direction", directionString);
        node.set("cast_spell", castSpell);
        String castParameterString = null;
        if (castParameters != null && castParameters.length > 0) {
            castParameterString = StringUtils.join(castParameters, " ");
        }
        node.set("cast_parameters", castParameterString);

		node.set("quiet", quietLevel);
        node.set("passive", passive);
		node.set("keep", keep);
        node.set("randomize", randomize);
        node.set("rename", rename);
		node.set("bound", bound);
        node.set("force", forceUpgrade);
		node.set("indestructible", indestructible);
        node.set("protected", superProtected);
        node.set("powered", superPowered);
        node.set("glow", glow);
        node.set("undroppable", undroppable);
        node.set("heroes", isHeroes);
		node.set("fill", autoFill);
		node.set("upgrade", isUpgrade);
		node.set("organize", autoOrganize);
        node.set("alphabetize", autoAlphabetize);
        if (castOverrides != null && castOverrides.size() > 0) {
            Collection<String> parameters = new ArrayList<String>();
            for (Map.Entry entry : castOverrides.entrySet()) {
                parameters.add(entry.getKey() + " " + entry.getValue());
            }
            node.set("overrides", StringUtils.join(parameters, ","));
        } else {
            node.set("overrides", null);
        }
		if (effectSound != null) {
			node.set("effect_sound", effectSound.toString());
		} else {
			node.set("effectSound", null);
		}
		if (effectParticle != null) {
			node.set("effect_particle", effectParticle.name());
		} else {
			node.set("effect_particle", null);
		}
		if (mode != null) {
			node.set("mode", mode.name());
		} else {
			node.set("mode", null);
		}
        if (brushMode != null) {
            node.set("brush_mode", brushMode.name());
        } else {
            node.set("brush_mode", null);
        }
		if (icon != null) {;
			String iconKey = icon.getKey();
			if (iconKey != null && iconKey.length() > 0) {
				node.set("icon", iconKey);
			} else {
				node.set("icon", null);
			}
		} else {
			node.set("icon", null);
		}
        if (upgradeIcon != null) {
            String iconKey = upgradeIcon.getKey();
            if (iconKey != null && iconKey.length() > 0) {
                node.set("upgrade_icon", iconKey);
            } else {
                node.set("upgrade_icon", null);
            }
        } else {
            node.set("upgrade_icon", null);
        }
		if (template != null && template.length() > 0) {
			node.set("template", template);
		} else {
			node.set("template", null);
		}
        if (path != null && path.length() > 0) {
            node.set("path", path);
        } else {
            node.set("path", null);
        }

        if (storedInventory != null && controller.isInventoryBackupEnabled()) {
            YamlConfiguration inventoryConfig = new YamlConfiguration();
            ItemStack[] contents = storedInventory.getContents();
            inventoryConfig.set("contents", contents);
            String serialized = inventoryConfig.saveToString();
            node.set("stored", serialized);
        }
	}
	
	public void loadProperties(ConfigurationSection wandConfig) {
		loadProperties(wandConfig, false);
	}
	
	public void setEffectColor(String hexColor) {
        // Annoying config conversion issue :\
        if (hexColor.contains(".")) {
            hexColor = hexColor.substring(0, hexColor.indexOf('.'));
        }

		if (hexColor == null || hexColor.length() == 0 || hexColor.equals("none")) {
			effectColor = null;
			return;
		}
		effectColor = new ColorHD(hexColor);
	}

    public static void addPotionEffects(Map<PotionEffectType, Integer> effects, ItemStack item) {
        addPotionEffects(effects, getWandString(item, "potion_effects"));
    }

    protected static void addPotionEffects(Map<PotionEffectType, Integer> effects, String effectsString) {
        if (effectsString == null || effectsString.isEmpty()) {
            return;
        }
        String[] effectStrings = StringUtils.split(effectsString, ",");
        for (String effectString : effectStrings) {
            try {
                PotionEffectType type;
                int power = 1;
                if (effectString.contains(":")) {
                    String[] pieces = effectString.split(":");
                    type = PotionEffectType.getByName(pieces[0].toUpperCase());
                    power = Integer.parseInt(pieces[1]);
                } else {
                    type = PotionEffectType.getByName(effectString.toUpperCase());
                }
                if (type == null) {
                    throw new Exception("Invalid potion effect");
                }

                Integer existing = effects.get(type);
                if (existing == null || existing < power) {
                    effects.put(type, power);
                }
            } catch (Exception ex) {
                Bukkit.getLogger().log(Level.WARNING, "Invalid potion effect: " + effectString);
            }
        }
    }
	
	public void loadProperties(ConfigurationSection wandConfig, boolean safe) {
		locked = wandConfig.getBoolean("locked", locked);
		float _costReduction = (float)wandConfig.getDouble("cost_reduction", costReduction);
		costReduction = safe ? Math.max(_costReduction, costReduction) : _costReduction;
		float _cooldownReduction = (float)wandConfig.getDouble("cooldown_reduction", cooldownReduction);
		cooldownReduction = safe ? Math.max(_cooldownReduction, cooldownReduction) : _cooldownReduction;
		float _power = (float)wandConfig.getDouble("power", power);
		power = safe ? Math.max(_power, power) : _power;
		float _damageReduction = (float)wandConfig.getDouble("protection", damageReduction);
		damageReduction = safe ? Math.max(_damageReduction, damageReduction) : _damageReduction;
		float _damageReductionPhysical = (float)wandConfig.getDouble("protection_physical", damageReductionPhysical);
		damageReductionPhysical = safe ? Math.max(_damageReductionPhysical, damageReductionPhysical) : _damageReductionPhysical;
		float _damageReductionProjectiles = (float)wandConfig.getDouble("protection_projectiles", damageReductionProjectiles);
		damageReductionProjectiles = safe ? Math.max(_damageReductionProjectiles, damageReductionPhysical) : _damageReductionProjectiles;
		float _damageReductionFalling = (float)wandConfig.getDouble("protection_falling", damageReductionFalling);
		damageReductionFalling = safe ? Math.max(_damageReductionFalling, damageReductionFalling) : _damageReductionFalling;
		float _damageReductionFire = (float)wandConfig.getDouble("protection_fire", damageReductionFire);
		damageReductionFire = safe ? Math.max(_damageReductionFire, damageReductionFire) : _damageReductionFire;
		float _damageReductionExplosions = (float)wandConfig.getDouble("protection_explosions", damageReductionExplosions);
		damageReductionExplosions = safe ? Math.max(_damageReductionExplosions, damageReductionExplosions) : _damageReductionExplosions;
		int _xpRegeneration = wandConfig.getInt("xp_regeneration", xpRegeneration);
		xpRegeneration = safe ? Math.max(_xpRegeneration, xpRegeneration) : _xpRegeneration;
		int _xpMax = wandConfig.getInt("xp_max", xpMax);
		xpMax = safe ? Math.max(_xpMax, xpMax) : _xpMax;
		int _xp = wandConfig.getInt("xp", (int)xp);
		xp = safe ? Math.max(_xp, xp) : _xp;
        float _xpMaxBoost = (float)wandConfig.getDouble("xp_max_boost", xpMaxBoost);
        xpMaxBoost = safe ? Math.max(_xpMaxBoost, xpMaxBoost) : _xpMaxBoost;
        float _xpRegenerationBoost = (float)wandConfig.getDouble("xp_regeneration_boost", xpRegenerationBoost);
        xpRegenerationBoost = safe ? Math.max(_xpRegenerationBoost, xpRegenerationBoost) : _xpRegenerationBoost;
        int _uses = wandConfig.getInt("uses", uses);
        uses = safe ? Math.max(_uses, uses) : _uses;
        hasUses = wandConfig.getBoolean("has_uses", hasUses) || uses > 0;

        // Convert some legacy properties to potion effects
        float healthRegeneration = (float)wandConfig.getDouble("health_regeneration", 0);
		float hungerRegeneration = (float)wandConfig.getDouble("hunger_regeneration", 0);
		float speedIncrease = (float)wandConfig.getDouble("haste", 0);

        if (speedIncrease > 0) {
            potionEffects.put(PotionEffectType.SPEED, 1);
        }
        if (healthRegeneration > 0) {
            potionEffects.put(PotionEffectType.REGENERATION, 1);
        }
        if (hungerRegeneration > 0) {
            potionEffects.put(PotionEffectType.SATURATION, 1);
        }

        if (regenWhileInactive) {
            lastXpRegeneration = wandConfig.getLong("xp_timestamp");
        } else {
            lastXpRegeneration = System.currentTimeMillis();
        }

		if (wandConfig.contains("effect_color") && !safe) {
			setEffectColor(wandConfig.getString("effect_color"));
		}
		
		// Don't change any of this stuff in safe mode
		if (!safe) {
			id = wandConfig.getString("id", id);
            isUpgrade = wandConfig.getBoolean("upgrade", isUpgrade);
            quietLevel = wandConfig.getInt("quiet", quietLevel);
			effectBubbles = wandConfig.getBoolean("effect_bubbles", effectBubbles);
			keep = wandConfig.getBoolean("keep", keep);
            passive = wandConfig.getBoolean("passive", passive);
            indestructible = wandConfig.getBoolean("indestructible", indestructible);
            superPowered = wandConfig.getBoolean("powered", superPowered);
            superProtected = wandConfig.getBoolean("protected", superProtected);
            glow = wandConfig.getBoolean("glow", glow);
            undroppable = wandConfig.getBoolean("undroppable", undroppable);
            isHeroes = wandConfig.getBoolean("heroes", isHeroes);
			bound = wandConfig.getBoolean("bound", bound);
            forceUpgrade = wandConfig.getBoolean("force", forceUpgrade);
            autoOrganize = wandConfig.getBoolean("organize", autoOrganize);
            autoAlphabetize = wandConfig.getBoolean("alphabetize", autoAlphabetize);
			autoFill = wandConfig.getBoolean("fill", autoFill);
            randomize = wandConfig.getBoolean("randomize", randomize);
            rename = wandConfig.getBoolean("rename", rename);

            if (wandConfig.contains("effect_particle")) {
                effectParticle = ConfigurationUtils.toParticleEffect(wandConfig.getString("effect_particle"));
				effectParticleData = 0;
			}
			if (wandConfig.contains("effect_sound")) {
                effectSound = ConfigurationUtils.toSoundEffect(wandConfig.getString("effect_sound"));
			}
			effectParticleData = (float)wandConfig.getDouble("effect_particle_data", effectParticleData);
			effectParticleCount = wandConfig.getInt("effect_particle_count", effectParticleCount);
            effectParticleRadius = wandConfig.getDouble("effect_particle_radius", effectParticleRadius);
            effectParticleOffset = wandConfig.getDouble("effect_particle_offset", effectParticleOffset);
			effectParticleInterval = wandConfig.getInt("effect_particle_interval", effectParticleInterval);
            effectParticleMinVelocity = wandConfig.getDouble("effect_particle_min_velocity", effectParticleMinVelocity);
			effectSoundInterval =  wandConfig.getInt("effect_sound_interval", effectSoundInterval);

            castInterval = wandConfig.getInt("cast_interval", castInterval);
            castMinVelocity = wandConfig.getDouble("cast_min_velocity", castMinVelocity);
            castVelocityDirection = ConfigurationUtils.getVector(wandConfig, "cast_velocity_direction", castVelocityDirection);
            castSpell = wandConfig.getString("cast_spell", castSpell);
            String castParameterString = wandConfig.getString("cast_parameters", null);
            if (castParameterString != null && !castParameterString.isEmpty()) {
                castParameters = StringUtils.split(castParameterString, " ");
            }

            boolean needsInventoryUpdate = false;
            if (wandConfig.contains("mode")) {
                WandMode newMode = parseWandMode(wandConfig.getString("mode"), mode);
                if (newMode != mode) {
                    setMode(newMode);
                    needsInventoryUpdate = true;
                }
            }
            setBrushMode(parseWandMode(wandConfig.getString("brush_mode"), brushMode));

			owner = wandConfig.getString("owner", owner);
            ownerId = wandConfig.getString("owner_id", ownerId);
			wandName = wandConfig.getString("name", wandName);			
			description = wandConfig.getString("description", description);
			template = wandConfig.getString("template", template);
            path = wandConfig.getString("path", path);

			activeSpell = wandConfig.getString("active_spell", activeSpell);
			activeMaterial = wandConfig.getString("active_material", activeMaterial);

            String wandMaterials = "";
            String wandSpells = "";
            if (wandConfig.contains("hotbar_count")) {
                int newCount = Math.max(1, wandConfig.getInt("hotbar_count"));
                if ((!safe && newCount != hotbars.size()) || newCount > hotbars.size()) {
                    if (isInventoryOpen()) {
                        closeInventory();
                    }
                    needsInventoryUpdate = true;
                    setHotbarCount(newCount);
                }
            }

            if (wandConfig.contains("hotbar")) {
                int hotbar = wandConfig.getInt("hotbar");
                currentHotbar = hotbar < 0 || hotbar >= hotbars.size() ? 0 : hotbar;
            }

            if (needsInventoryUpdate) {
                // Force a re-parse of materials and spells
                wandSpells = getSpellString();
                wandMaterials = getMaterialString();
            }

			wandMaterials = wandConfig.getString("materials", wandMaterials);
			wandSpells = wandConfig.getString("spells", wandSpells);

			if (wandMaterials.length() > 0 || wandSpells.length() > 0) {
				wandMaterials = wandMaterials.length() == 0 ? getMaterialString() : wandMaterials;
				wandSpells = wandSpells.length() == 0 ? getSpellString() : wandSpells;
				parseInventoryStrings(wandSpells, wandMaterials);
			}

            if (wandConfig.contains("randomize_icon")) {
                setIcon(new MaterialAndData(wandConfig.getString("randomize_icon")));
                randomize = true;
            } else if (!randomize && wandConfig.contains("icon")) {
                String iconKey = wandConfig.getString("icon");
                if (iconKey.contains(",")) {
                    Random r = new Random();
                    String[] keys = StringUtils.split(iconKey, ',');
                    iconKey = keys[r.nextInt(keys.length)];
                }
                // Port old custom wand icons
                if (template != null && !template.isEmpty() && iconKey.contains("i.imgur.com")) {
                    ConfigurationSection templateConfig = getWandTemplate(template);
                    if (templateConfig != null) {
                        iconKey = templateConfig.getString("icon");
                    }
                }
                setIcon(new MaterialAndData(iconKey));
			} else if (isUpgrade) {
                setIcon(new MaterialAndData(DefaultUpgradeMaterial));
            }

            if (wandConfig.contains("upgrade_icon")) {
                upgradeIcon = new MaterialAndData(wandConfig.getString("upgrade_icon"));
            }

            if (wandConfig.contains("overrides")) {
                castOverrides = null;
                String overrides = wandConfig.getString("overrides", null);
                if (overrides != null && !overrides.isEmpty()) {
                    castOverrides = new HashMap<String, String>();
                    String[] pairs = StringUtils.split(overrides, ',');
                    for (String pair : pairs) {
                        String[] keyValue = StringUtils.split(pair, " ");
                        if (keyValue.length > 0) {
                            String value = keyValue.length > 1 ? keyValue[1] : "";
                            castOverrides.put(keyValue[0], value);
                        }
                    }
                }
            }

            if (wandConfig.contains("potion_effects")) {
                potionEffects.clear();
                addPotionEffects(potionEffects, wandConfig.getString("potion_effects", null));
            }

            if (wandConfig.contains("stored")) {
                try {
                    YamlConfiguration inventoryConfig = new YamlConfiguration();
                    String serialized = wandConfig.getString("stored");
                    if (serialized.isEmpty()) {
                        storedInventory = null;
                    } else {
                        inventoryConfig.loadFromString(serialized);
                        Collection<ItemStack> collection = (Collection<ItemStack>) inventoryConfig.get("contents");
                        ItemStack[] contents = collection.toArray(itemTemplate);
                        storedInventory = CompatibilityUtils.createInventory(null, contents.length, "Stored Inventory");
                        storedInventory.setContents(contents);
                    }
                } catch (Exception ex) {
                    controller.getLogger().warning("Error loading stored wand inventory");
                    ex.printStackTrace();
                }
            }
		}
		
		// Some cleanup and sanity checks. In theory we don't need to store any non-zero value (as it is with the traders)
		// so try to keep defaults as 0/0.0/false.
		if (effectSound == null) {
			effectSoundInterval = 0;
		} else {
			effectSoundInterval = (effectSoundInterval == 0) ? 5 : effectSoundInterval;
		}
		
		if (effectParticle == null) {
			effectParticleInterval = 0;
		}

        updateMaxMana();
		checkActiveMaterial();
	}

	public void describe(CommandSender sender) {
		Object wandNode = InventoryUtils.getNode(item, WAND_KEY);
		if (wandNode == null) {
			sender.sendMessage("Found a wand with missing NBT data. This may be an old wand, or something may have wiped its data");
            return;
		}
		ChatColor wandColor = isModifiable() ? ChatColor.AQUA : ChatColor.RED;
		sender.sendMessage(wandColor + wandName);
		if (description.length() > 0) {
			sender.sendMessage(ChatColor.ITALIC + "" + ChatColor.GREEN + description);
		} else {
			sender.sendMessage(ChatColor.ITALIC + "" + ChatColor.GREEN + "(No Description)");
		}
		if (owner.length() > 0 || ownerId.length() > 0) {
			sender.sendMessage(ChatColor.ITALIC + "" + ChatColor.WHITE + owner + " (" + ChatColor.GRAY + ownerId + ChatColor.WHITE + ")");
		} else {
			sender.sendMessage(ChatColor.ITALIC + "" + ChatColor.WHITE + "(No Owner)");
		}
        if (storedInventory != null) {
            sender.sendMessage(ChatColor.RED + "Has a stored inventory");
        }
		
		for (String key : PROPERTY_KEYS) {
			String value = InventoryUtils.getMeta(wandNode, key);
			if (value != null && value.length() > 0) {
				sender.sendMessage(key + ": " + value);
			}
		}
	}

    private static String getBrushDisplayName(Messages messages, MaterialBrush brush) {
        String materialName = brush.getName(messages);
        if (materialName == null) {
            materialName = "none";
        }
        return ChatColor.GRAY + materialName;
    }

    private static String getSpellDisplayName(Messages messages, SpellTemplate spell, MaterialBrush brush) {
		String name = "";
		if (spell != null) {
			if (brush != null && spell.usesBrush()) {
				name = ChatColor.GOLD + spell.getName() + " " + getBrushDisplayName(messages, brush) + ChatColor.WHITE;
			} else {
				name = ChatColor.GOLD + spell.getName() + ChatColor.WHITE;
			}
		}
		
		return name;
	}

	private String getActiveWandName(SpellTemplate spell, MaterialBrush brush) {
		// Build wand name
        int remaining = getRemainingUses();
		ChatColor wandColor = (hasUses && remaining <= 1) ? ChatColor.DARK_RED : isModifiable()
                ? (bound ? ChatColor.DARK_AQUA : ChatColor.AQUA) :
                  (path != null && path.length() > 0 ? ChatColor.LIGHT_PURPLE : ChatColor.GOLD);
		String name = wandColor + getDisplayName();
        if (randomize) return name;

        Set<String> spells = getSpells();

        // Add active spell to description
        Messages messages = controller.getMessages();
        boolean showSpell = isModifiable() && hasPath();
        if (spell != null && (spells.size() > 1 || showSpell)) {
            name = getSpellDisplayName(messages, spell, brush) + " (" + name + ChatColor.WHITE + ")";
        }

		if (remaining > 1) {
			String message = controller.getMessages().get("wand.uses_remaining_brief");
			name = name + ChatColor.DARK_RED + " (" + ChatColor.RED + message.replace("$count", ((Integer)remaining).toString()) + ChatColor.DARK_RED + ")";
		}
		return name;
	}
	
	private String getActiveWandName(SpellTemplate spell) {
		return getActiveWandName(spell, MaterialBrush.parseMaterialKey(activeMaterial));
	}

    private String getActiveWandName(MaterialBrush brush) {
        SpellTemplate spell = null;
        if (activeSpell != null && activeSpell.length() > 0) {
            spell = controller.getSpellTemplate(activeSpell);
        }
        return getActiveWandName(spell, brush);
    }
	
	private String getActiveWandName() {
		SpellTemplate spell = null;
		if (activeSpell != null && activeSpell.length() > 0) {
			spell = controller.getSpellTemplate(activeSpell);
		}
		return getActiveWandName(spell);
	}

    protected String getDisplayName() {
        return randomize ? controller.getMessages().get("wand.randomized_name") : wandName;
    }

	public void updateName(boolean isActive) {
        CompatibilityUtils.setDisplayName(item, isActive && !isUpgrade ? getActiveWandName() : ChatColor.GOLD + getDisplayName());

		// Reset Enchantment glow
		if (glow) {
            CompatibilityUtils.addGlow(item);
		}

        // Make indestructible
        if (indestructible && !displayManaAsDurability) {
            CompatibilityUtils.makeUnbreakable(item);
        } else {
            CompatibilityUtils.removeUnbreakable(item);
        }
        CompatibilityUtils.hideFlags(item, HIDE_FLAGS);
	}
	
	private void updateName() {
		updateName(true);
	}
	
	protected static String convertToHTML(String line) {
		int tagCount = 1;
		line = "<span style=\"color:white\">" + line;
		for (ChatColor c : ChatColor.values()) {
			tagCount += StringUtils.countMatches(line, c.toString());
			String replaceStyle = "";
			if (c == ChatColor.ITALIC) {
				replaceStyle = "font-style: italic";
			} else if (c == ChatColor.BOLD) {
				replaceStyle = "font-weight: bold";
			} else if (c == ChatColor.UNDERLINE) {
				replaceStyle = "text-decoration: underline";
			} else {
				String color = c.name().toLowerCase().replace("_", "");
				if (c == ChatColor.LIGHT_PURPLE) {
					color = "mediumpurple";
				}
				replaceStyle = "color:" + color;
			}
			line = line.replace(c.toString(), "<span style=\"" + replaceStyle + "\">");
		}
		for (int i = 0; i < tagCount; i++) {
			line += "</span>";
		}
		
		return line;
	}

	public String getHTMLDescription() {
		Collection<String> rawLore = getLore();
		Collection<String> lore = new ArrayList<String>();
		lore.add("<h2>" + convertToHTML(getActiveWandName()) + "</h2>");
 		for (String line : rawLore) {
			lore.add(convertToHTML(line));
		}
		
		return "<div style=\"background-color: black; margin: 8px; padding: 8px\">" + StringUtils.join(lore, "<br/>") + "</div>";
	}

	protected List<String> getLore() {
		return getLore(getSpells().size(), getBrushes().size());
	}
	
	protected void addPropertyLore(List<String> lore)
	{
		if (usesMana()) {
            if (effectiveXpMax != xpMax) {
                String fullMessage = getLevelString(controller.getMessages(), "wand.mana_amount_boosted", xpMax, controller.getMaxMana());
                lore.add(ChatColor.LIGHT_PURPLE + "" + ChatColor.ITALIC + fullMessage.replace("$mana", Integer.toString(effectiveXpMax)));
            } else {
                lore.add(ChatColor.LIGHT_PURPLE + "" + ChatColor.ITALIC + getLevelString(controller.getMessages(), "wand.mana_amount", xpMax, controller.getMaxMana()));
            }
            if (effectiveXpRegeneration != xpRegeneration) {
                String fullMessage = getLevelString(controller.getMessages(), "wand.mana_regeneration_boosted", xpRegeneration, controller.getMaxManaRegeneration());
                lore.add(ChatColor.LIGHT_PURPLE + "" + ChatColor.ITALIC + fullMessage.replace("$mana", Integer.toString(effectiveXpRegeneration)));
            } else {
                lore.add(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE + getLevelString(controller.getMessages(), "wand.mana_regeneration", xpRegeneration, controller.getMaxManaRegeneration()));
            }
		}
        if (superPowered) {
            lore.add(ChatColor.DARK_AQUA + controller.getMessages().get("wand.super_powered"));
        }
        if (xpMaxBoost != 0) {
            lore.add(ChatColor.LIGHT_PURPLE + "" + ChatColor.ITALIC + getPercentageString(controller.getMessages(), "wand.mana_boost", xpMaxBoost));
        }
        if (xpRegenerationBoost != 0) {
            lore.add(ChatColor.LIGHT_PURPLE + "" + ChatColor.ITALIC + getPercentageString(controller.getMessages(), "wand.mana_regeneration_boost", xpRegenerationBoost));
        }
        if (castSpell != null) {
            SpellTemplate spell = controller.getSpellTemplate(castSpell);
            if (spell != null)
            {
                lore.add(ChatColor.AQUA + controller.getMessages().get("wand.spell_aura").replace("$spell", spell.getName()));
            }
        }
        for (Map.Entry<PotionEffectType, Integer> effect : potionEffects.entrySet()) {
            String effectName = effect.getKey().getName();
            String effectFirst = effectName.substring(0, 1);
            effectName = effectName.substring(1).toLowerCase().replace("_", " ");
            effectName = effectFirst + effectName;
            lore.add(ChatColor.AQUA + getLevelString(controller.getMessages(), "wand.potion_effect", effect.getValue(), 5).replace("$effect", effectName));
        }
		if (costReduction > 0) lore.add(ChatColor.AQUA + getLevelString(controller.getMessages(), "wand.cost_reduction", costReduction));
		if (cooldownReduction > 0) lore.add(ChatColor.AQUA + getLevelString(controller.getMessages(), "wand.cooldown_reduction", cooldownReduction));
		if (power > 0) lore.add(ChatColor.AQUA + getLevelString(controller.getMessages(), "wand.power", power));
        if (superProtected) {
            lore.add(ChatColor.DARK_AQUA + controller.getMessages().get("wand.super_protected"));
        } else {
            if (damageReduction > 0) lore.add(ChatColor.AQUA + getLevelString(controller.getMessages(), "wand.protection", damageReduction));
            if (damageReductionPhysical > 0) lore.add(ChatColor.AQUA + getLevelString(controller.getMessages(), "wand.protection_physical", damageReductionPhysical));
            if (damageReductionProjectiles > 0) lore.add(ChatColor.AQUA + getLevelString(controller.getMessages(), "wand.protection_projectile", damageReductionProjectiles));
            if (damageReductionFalling > 0) lore.add(ChatColor.AQUA + getLevelString(controller.getMessages(), "wand.protection_fall", damageReductionFalling));
            if (damageReductionFire > 0) lore.add(ChatColor.AQUA + getLevelString(controller.getMessages(), "wand.protection_fire", damageReductionFire));
            if (damageReductionExplosions > 0) lore.add(ChatColor.AQUA + getLevelString(controller.getMessages(), "wand.protection_blast", damageReductionExplosions));
        }
	}
	
	public static String getLevelString(Messages messages, String templateName, float amount)
	{
		return getLevelString(messages, templateName, amount, 1);
	}
	
	public static String getLevelString(Messages messages, String templateName, float amount, float max)
	{
		String templateString = messages.get(templateName);
		if (templateString.contains("$roman")) {
            if (max != 1) {
                amount = amount / max;
            }
			templateString = templateString.replace("$roman", getRomanString(messages, amount));
		}
		return templateString.replace("$amount", Integer.toString((int) amount));
	}

    public static String getPercentageString(Messages messages, String templateName, float amount)
    {
        String templateString = messages.get(templateName);
        return templateString.replace("$amount", Integer.toString((int)(amount * 100)));
    }

	private static String getRomanString(Messages messages, float amount) {
		String roman = "";

		if (amount > 1) {
			roman = messages.get("wand.enchantment_level_max");
		} else if (amount > 0.8) {
			roman = messages.get("wand.enchantment_level_5");
		} else if (amount > 0.6) {
			roman = messages.get("wand.enchantment_level_4");
		} else if (amount > 0.4) {
			roman = messages.get("wand.enchantment_level_3");
		} else if (amount > 0.2) {
			roman = messages.get("wand.enchantment_level_2");
		} else {
			 roman = messages.get("wand.enchantment_level_1");
		}
		return roman;
	}
	
	protected List<String> getLore(int spellCount, int materialCount) 
	{
		List<String> lore = new ArrayList<String>();

        if (description.length() > 0) {
            if (description.contains("$path")) {
                String pathName = "Unknown";
                com.elmakers.mine.bukkit.api.wand.WandUpgradePath path = getPath();
                if (path != null) {
                    pathName = path.getName();
                }
                String description = this.description;
                description = description.replace("$path", pathName);
                lore.add(ChatColor.ITALIC + "" + ChatColor.GREEN + description);
            }
            else if (description.contains("$")) {
                String randomDescription = controller.getMessages().get("wand.randomized_lore");
                if (randomDescription.length() > 0) {
                    lore.add(ChatColor.ITALIC + "" + ChatColor.DARK_GREEN + randomDescription);
                }
            } else {
                lore.add(ChatColor.ITALIC + "" + ChatColor.GREEN + description);
            }
        }

        if (randomize) {
            return lore;
        }

		SpellTemplate spell = controller.getSpellTemplate(activeSpell);
        Messages messages = controller.getMessages();

        // This is here specifically for a wand that only has
        // one spell now, but may get more later. Since you
        // can't open the inventory in this state, you can not
        // otherwise see the spell lore.
		if (spell != null && spellCount == 1 && !hasInventory && !isUpgrade && hasPath() && !spell.isHidden())
        {
            addSpellLore(messages, spell, lore, getActivePlayer(), this);
		}
        if (materialCount == 1 && activeMaterial != null && activeMaterial.length() > 0)
        {
            lore.add(getBrushDisplayName(messages, MaterialBrush.parseMaterialKey(activeMaterial)));
        }
        if (!isUpgrade) {
            if (owner.length() > 0) {
                if (bound) {
                    String ownerDescription = messages.get("wand.bound_description", "$name").replace("$name", owner);
                    lore.add(ChatColor.ITALIC + "" + ChatColor.DARK_AQUA + ownerDescription);
                } else {
                    String ownerDescription = messages.get("wand.owner_description", "$name").replace("$name", owner);
                    lore.add(ChatColor.ITALIC + "" + ChatColor.DARK_GREEN + ownerDescription);
                }
            }
        }

        if (spellCount > 0) {
            if (isUpgrade) {
                lore.add(messages.get("wand.upgrade_spell_count").replace("$count", ((Integer)spellCount).toString()));
            } else if (spellCount > 1) {
                lore.add(messages.get("wand.spell_count").replace("$count", ((Integer)spellCount).toString()));
            }
        }
        if (materialCount > 0) {
            if (isUpgrade) {
                lore.add(messages.get("wand.upgrade_material_count").replace("$count", ((Integer)materialCount).toString()));
            } else if (materialCount > 1) {
                lore.add(messages.get("wand.material_count").replace("$count", ((Integer)materialCount).toString()));
            }
        }

		int remaining = getRemainingUses();
		if (remaining > 0) {
			if (isUpgrade) {
				String message = (remaining == 1) ? messages.get("wand.upgrade_uses_singular") : messages.get("wand.upgrade_uses");
				lore.add(ChatColor.RED + message.replace("$count", ((Integer)remaining).toString()));
			} else {
				String message = (remaining == 1) ? messages.get("wand.uses_remaining_singular") : messages.get("wand.uses_remaining_brief");
				lore.add(ChatColor.RED + message.replace("$count", ((Integer)remaining).toString()));
			}
		}
		addPropertyLore(lore);
		if (isUpgrade) {
			lore.add(ChatColor.YELLOW + messages.get("wand.upgrade_item_description"));
		}
		return lore;
	}
	
	protected void updateLore() {
        CompatibilityUtils.setLore(item, getLore());

		if (glow) {
			CompatibilityUtils.addGlow(item);
		}
	}

    public void save() {
        saveState();
        updateName();
        updateLore();
    }
	
	public int getRemainingUses() {
		return uses;
	}
	
	public void makeEnchantable(boolean enchantable) {
		if (EnchantableWandMaterial == null) return;
		
		if (!enchantable) {
			item.setType(icon.getMaterial());
			item.setDurability(icon.getData());
		} else {
			Set<Material> enchantableMaterials = controller.getMaterialSet("enchantable");
			if (!enchantableMaterials.contains(item.getType())) {
				item.setType(EnchantableWandMaterial);
				item.setDurability((short)0);
			}
		}
		updateName();
	}
	
	public static boolean hasActiveWand(Player player) {
		if (player == null) return false;
		ItemStack activeItem =  player.getInventory().getItemInHand();
		return isWand(activeItem);
	}
	
	public static Wand getActiveWand(MagicController spells, Player player) {
		ItemStack activeItem =  player.getInventory().getItemInHand();
		if (isWand(activeItem)) {
			return new Wand(spells, activeItem);
		}
		
		return null;
	}

	public static boolean isWand(ItemStack item) {
        return item != null && InventoryUtils.hasMeta(item, WAND_KEY) && !isUpgrade(item);
	}

    public static boolean isUpgrade(ItemStack item) {
        if (item == null) return false;
        Object wandNode = InventoryUtils.getNode(item, WAND_KEY);

        if (wandNode == null) return false;
        String upgradeData = InventoryUtils.getMeta(wandNode, "upgrade");

        return upgradeData != null && upgradeData.equals("true");
    }

	public static boolean isSpell(ItemStack item) {
        return item != null && InventoryUtils.hasMeta(item, "spell");
	}

    public static boolean isSkill(ItemStack item) {
        return item != null && InventoryUtils.hasMeta(item, "skill");
    }

	public static boolean isBrush(ItemStack item) {
        return item != null && InventoryUtils.hasMeta(item, "brush");
	}
	
	public static String getSpell(ItemStack item) {
        Object spellNode = InventoryUtils.getNode(item, "spell");
        if (spellNode == null) return null;
        return InventoryUtils.getMeta(spellNode, "key");
	}

    public static String getBrush(ItemStack item) {
        Object brushNode = InventoryUtils.getNode(item, "brush");
        if (brushNode == null) return null;
        return InventoryUtils.getMeta(brushNode, "key");
    }

	protected void updateInventoryName(ItemStack item, boolean activeName) {
		if (isSpell(item)) {
			Spell spell = mage.getSpell(getSpell(item));
			if (spell != null) {
				updateSpellItem(controller.getMessages(), item, spell, getActivePlayer(), activeName ? this : null, activeMaterial, false);
			}
		} else if (isBrush(item)) {
			updateBrushItem(controller.getMessages(), item, getBrush(item), activeName ? this : null);
		}
	}

    public static void updateSpellItem(Messages messages, ItemStack itemStack, SpellTemplate spell, Wand wand, String activeMaterial, boolean isItem) {
        updateSpellItem(messages, itemStack, spell, wand == null ? null : wand.getActivePlayer(), wand, activeMaterial, isItem);
    }

    public static void updateSpellItem(Messages messages, ItemStack itemStack, SpellTemplate spell, com.elmakers.mine.bukkit.api.magic.Mage mage, Wand wand, String activeMaterial, boolean isItem) {
        String displayName;
		if (wand != null) {
			displayName = wand.getActiveWandName(spell);
		} else {
			displayName = getSpellDisplayName(messages, spell, MaterialBrush.parseMaterialKey(activeMaterial));
		}
        CompatibilityUtils.setDisplayName(itemStack, displayName);
		List<String> lore = new ArrayList<String>();
		addSpellLore(messages, spell, lore, mage, wand);
		if (isItem) {
			lore.add(ChatColor.YELLOW + messages.get("wand.spell_item_description"));
		}
        CompatibilityUtils.setLore(itemStack, lore);
        Object spellNode = CompatibilityUtils.createNode(itemStack, "spell");
		CompatibilityUtils.setMeta(spellNode, "key", spell.getKey());
        if (SpellGlow) {
            CompatibilityUtils.addGlow(itemStack);
        }
	}

    public static void updateBrushItem(Messages messages, ItemStack itemStack, String materialKey, Wand wand) {
        updateBrushItem(messages, itemStack, MaterialBrush.parseMaterialKey(materialKey), wand);
    }
	
	public static void updateBrushItem(Messages messages, ItemStack itemStack, MaterialBrush brush, Wand wand) {
		String displayName;
		if (wand != null) {
            Spell activeSpell = wand.getActiveSpell();
            if (activeSpell != null && activeSpell.usesBrush()) {
                displayName = wand.getActiveWandName(brush);
            } else {
                displayName = ChatColor.RED + brush.getName(messages);
            }
        } else {
            displayName = brush.getName(messages);
        }
        CompatibilityUtils.setDisplayName(itemStack, displayName);
        Object brushNode = CompatibilityUtils.createNode(itemStack, "brush");
        CompatibilityUtils.setMeta(brushNode, "key", brush.getKey());
	}

    public void updateHotbar() {
        if (mage == null) return;
        if (!isInventoryOpen()) return;
        Player player = mage.getPlayer();
        if (player == null) return;
        if (!hasStoredInventory()) return;

        WandMode wandMode = getMode();
        if (wandMode == WandMode.INVENTORY) {
            PlayerInventory inventory = player.getInventory();
            updateHotbar(inventory);
            player.updateInventory();
        }
    }

	@SuppressWarnings("deprecation")
	private void updateInventory() {
		if (mage == null) return;
		if (!isInventoryOpen()) return;
		Player player = mage.getPlayer();
		if (player == null) return;
		
		WandMode wandMode = getMode();
		if (wandMode == WandMode.INVENTORY) {
			if (!hasStoredInventory()) return;
			PlayerInventory inventory = player.getInventory();
            inventory.clear();
			updateHotbar(inventory);
			updateInventory(inventory, HOTBAR_SIZE);
			updateName();
			player.updateInventory();
		} else if (wandMode == WandMode.CHEST) {
			Inventory inventory = getDisplayInventory();
			inventory.clear();
			updateInventory(inventory, 0);
			player.updateInventory();
		}
	}
	
	private void updateHotbar(PlayerInventory playerInventory) {
		Inventory hotbar = getHotbar();
        if (hotbar == null) return;

        // Check for an item already in the player's held slot, which
        // we are about to replace with the wand.
		int currentSlot = playerInventory.getHeldItemSlot();

        // Set hotbar items from remaining list
		int targetOffset = 0;
		for (int hotbarSlot = 0; hotbarSlot < HOTBAR_INVENTORY_SIZE; hotbarSlot++)
		{
			if (hotbarSlot == currentSlot)
			{
				targetOffset = 1;
			}

			ItemStack hotbarItem = hotbar.getItem(hotbarSlot);
			updateInventoryName(hotbarItem, true);
			playerInventory.setItem(hotbarSlot + targetOffset, hotbarItem);
		}

        // Put the wand in the player's active slot.
        playerInventory.setItem(currentSlot, item);
        item = playerInventory.getItem(currentSlot);
    }
	
	private void updateInventory(Inventory targetInventory, int startOffset) {
		// Set inventory from current page
		int currentOffset = startOffset;
		if (openInventoryPage < inventories.size()) {
            Inventory inventory = inventories.get(openInventoryPage);
            ItemStack[] contents = inventory.getContents();
            for (int i = 0; i < contents.length; i++) {
                ItemStack inventoryItem = contents[i];
                updateInventoryName(inventoryItem, false);
                targetInventory.setItem(currentOffset, inventoryItem);
                currentOffset++;
            }
        }
	}
	
	protected static void addSpellLore(Messages messages, SpellTemplate spell, List<String> lore, com.elmakers.mine.bukkit.api.magic.Mage mage, Wand wand) {
        spell.addLore(messages, mage, wand, lore);
	}
	
	protected Inventory getOpenInventory() {
		while (openInventoryPage >= inventories.size()) {
			inventories.add(CompatibilityUtils.createInventory(null, INVENTORY_SIZE, "Wand"));
		}
		return inventories.get(openInventoryPage);
	}
	
	public void saveInventory() {
		if (mage == null) return;
		if (!isInventoryOpen()) return;
		if (mage.getPlayer() == null) return;
		if (getMode() != WandMode.INVENTORY) return;
		if (!hasStoredInventory()) return;

        // Work-around glitches that happen if you're dragging an item on death
        if (mage.isDead()) return;

		// Fill in the hotbar
		Player player = mage.getPlayer();
		PlayerInventory playerInventory = player.getInventory();
		Inventory hotbar = getHotbar();
        if (hotbar != null)
        {
            int saveOffset = 0;
            for (int i = 0; i < HOTBAR_SIZE; i++) {
                ItemStack playerItem = playerInventory.getItem(i);
                if (isWand(playerItem)) {
                    saveOffset = -1;
                    continue;
                }
                int hotbarOffset = i + saveOffset;
                if (hotbarOffset >= hotbar.getSize()) {
                    // This can happen if there is somehow no wand in the wand inventory.
                    break;
                }
                hotbar.setItem(i + saveOffset, playerItem);
                if (!updateSlot(i + saveOffset + currentHotbar * HOTBAR_INVENTORY_SIZE, playerItem)) {
                    hotbar.setItem(i + saveOffset, new ItemStack(Material.AIR));
                }
            }
        }

		// Fill in the active inventory page
		int hotbarOffset = getHotbarSize();
		Inventory openInventory = getOpenInventory();
		for (int i = 0; i < openInventory.getSize(); i++) {
            ItemStack playerItem = playerInventory.getItem(i + HOTBAR_SIZE);
			openInventory.setItem(i, playerItem);
            if (!updateSlot(i + hotbarOffset + openInventoryPage * INVENTORY_SIZE, playerItem)) {
                openInventory.setItem(i, new ItemStack(Material.AIR));
            }
		}
	}

    protected boolean updateSlot(int slot, ItemStack item) {
        String spellKey = getSpell(item);
        if (spellKey != null) {
            spells.put(spellKey, slot);
        } else {
            String brushKey = getBrush(item);
            if (brushKey != null) {
                brushes.put(brushKey, slot);
            } else if (mage != null && item != null && item.getType() != Material.AIR) {
                // Must have been an item inserted directly into player's inventory?
                mage.giveItem(item);
                return false;
            }
        }

        return true;
    }

    public int enchant(int totalLevels, com.elmakers.mine.bukkit.api.magic.Mage mage) {
        return randomize(totalLevels, true, mage);
    }

    public int enchant(int totalLevels) {
        return randomize(totalLevels, true, null);
    }

	protected int randomize(int totalLevels, boolean additive, com.elmakers.mine.bukkit.api.magic.Mage enchanter) {
        if (enchanter == null && mage != null) {
            enchanter = mage;
        }
        WandUpgradePath path = (WandUpgradePath)getPath();
		if (path == null) {
            if (enchanter != null) {
                enchanter.sendMessage(controller.getMessages().get("wand.no_path"));
            }
            return 0;
        }

        path.catchup(this, enchanter);

        int minLevel = path.getMinLevel();
        if (totalLevels < minLevel) {
            if (enchanter != null) {
                String levelMessage = controller.getMessages().get("wand.need_more_levels");
                levelMessage = levelMessage.replace("$levels", Integer.toString(minLevel));
                enchanter.sendMessage(levelMessage);
            }
            return 0;
        }

        // Just a hard-coded sanity check
        int maxLevel = path.getMaxLevel();
        totalLevels = Math.min(totalLevels, maxLevel * 50);

		int addLevels = Math.min(totalLevels, maxLevel);
        int levels = 0;
        boolean modified = true;
		while (addLevels >= minLevel && modified) {
            boolean hasUpgrade = path.hasUpgrade();
            WandLevel level = path.getLevel(addLevels);
            modified = level.randomizeWand(enchanter, this, additive, hasUpgrade);
			totalLevels -= maxLevel;
            if (modified) {
                if (enchanter != null) {
                    path.enchanted(enchanter);
                }
                levels += addLevels;

                // Check for level up
                WandUpgradePath nextPath = path.getUpgrade();
                if (nextPath != null && path.checkUpgradeRequirements(this, null) && !path.canEnchant(this)) {
                    path.upgrade(this, enchanter);
                    path = nextPath;
                }
            } else if (path.canEnchant(this)) {
                if (enchanter != null && levels == 0)
                {
                    String message = controller.getMessages().get("wand.require_more_levels");
                    enchanter.sendMessage(message);
                }
            } else if (hasUpgrade) {
                if (path.checkUpgradeRequirements(this, enchanter)) {
                    path.upgrade(this, enchanter);
                    levels += addLevels;
                }
            } else if (enchanter != null) {
                enchanter.sendMessage(controller.getMessages().get("wand.fully_enchanted").replace("$wand", getName()));
            }
			addLevels = Math.min(totalLevels, maxLevel);
			additive = true;
		}

        saveState();
        updateName();
        updateLore();
        return levels;
	}

    public static ItemStack createItem(MagicController controller, String templateName) {
        ItemStack item = createSpellItem(templateName, controller, null, true);
        if (item == null) {
            item = createBrushItem(templateName, controller, null, true);
            if (item == null) {
                Wand wand = createWand(controller, templateName);
                if (wand != null) {
                    item = wand.getItem();
                }
            }
        }

        return item;
    }
	
	public static Wand createWand(MagicController controller, String templateName) {
		if (controller == null) return null;
		
		Wand wand = null;
		try {
            wand = new Wand(controller, templateName);
        } catch (UnknownWandException ignore) {
            // the Wand constructor throws an exception on an unknown template
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return wand; 
	}

    public static Wand createWand(MagicController controller, ItemStack itemStack) {
        if (controller == null) return null;

        Wand wand = null;
        try {
            wand = new Wand(controller, InventoryUtils.makeReal(itemStack));
            wand.saveState();
            wand.updateName();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return wand;
    }
	
	protected void sendAddMessage(com.elmakers.mine.bukkit.api.magic.Mage mage, String messageKey, String nameParam) {
		if (mage == null) return;
		
		String message = controller.getMessages().get(messageKey).replace("$name", nameParam).replace("$wand", getName());
		mage.sendMessage(message);
	}

    public boolean add(Wand other) {
        return add(other, this.mage);
    }

    @Override
    public boolean add(com.elmakers.mine.bukkit.api.wand.Wand other, com.elmakers.mine.bukkit.api.magic.Mage mage) {
        if (other instanceof Wand) {
            return add((Wand)other, mage);
        }

        return false;
    }

	public boolean add(Wand other, com.elmakers.mine.bukkit.api.magic.Mage mage) {
		if (!isModifiable()) {
            // Only allow upgrading a modifiable wand via an upgrade item
            // and only if the paths match.
            if (!other.isUpgrade() || other.path == null || path == null || other.path.isEmpty() || path.isEmpty() || !other.path.equals(path)) {
                return false;
            }
        }

        // Can't combine limited-use wands
        if (hasUses || other.hasUses)
        {
            return false;
        }

        // Don't allow upgrades from an item on a different path
        if (other.isUpgrade() && other.path != null && !other.path.isEmpty() && (this.path == null || !this.path.equals(other.path))) {
            return false;
        }

        if (isHeroes || other.isHeroes) {
            return false;
        }
		
		boolean modified = false;

        Messages messages = controller.getMessages();
		if (other.isForcedUpgrade() || other.costReduction > costReduction) { costReduction = other.costReduction; modified = true; if (costReduction > 0) sendAddMessage(mage, "wand.upgraded_property", getLevelString(messages, "wand.cost_reduction", costReduction)); }
		if (other.isForcedUpgrade() || other.power > power) { power = other.power; modified = true; if (power > 0) sendAddMessage(mage, "wand.upgraded_property", getLevelString(messages, "wand.power", power)); }
		if (other.isForcedUpgrade() || other.damageReduction > damageReduction) { damageReduction = other.damageReduction; modified = true; if (damageReduction > 0) sendAddMessage(mage, "wand.upgraded_property", getLevelString(messages, "wand.protection", damageReduction)); }
		if (other.isForcedUpgrade() || other.damageReductionPhysical > damageReductionPhysical) { damageReductionPhysical = other.damageReductionPhysical; modified = true; if (damageReductionPhysical > 0) sendAddMessage(mage, "wand.upgraded_property", getLevelString(messages, "wand.protection_physical", damageReductionPhysical)); }
		if (other.isForcedUpgrade() || other.damageReductionProjectiles > damageReductionProjectiles) { damageReductionProjectiles = other.damageReductionProjectiles; modified = true; if (damageReductionProjectiles > 0) sendAddMessage(mage, "wand.upgraded_property", getLevelString(messages, "wand.protection_projectile", damageReductionProjectiles)); }
		if (other.isForcedUpgrade() || other.damageReductionFalling > damageReductionFalling) { damageReductionFalling = other.damageReductionFalling; modified = true; if (damageReductionFalling > 0) sendAddMessage(mage, "wand.upgraded_property", getLevelString(messages, "wand.protection_fall", damageReductionFalling)); }
		if (other.isForcedUpgrade() || other.damageReductionFire > damageReductionFire) { damageReductionFire = other.damageReductionFire; modified = true; if (damageReductionFire > 0) sendAddMessage(mage, "wand.upgraded_property", getLevelString(messages, "wand.protection_fire", damageReductionFire)); }
		if (other.isForcedUpgrade() || other.damageReductionExplosions > damageReductionExplosions) { damageReductionExplosions = other.damageReductionExplosions; modified = true; if (damageReductionExplosions > 0) sendAddMessage(mage, "wand.upgraded_property", getLevelString(messages, "wand.protection_blast", damageReductionExplosions)); }
        if (other.isForcedUpgrade() || other.xpRegenerationBoost > xpRegenerationBoost) { xpRegenerationBoost = other.xpRegenerationBoost; modified = true; if (xpRegenerationBoost > 0) sendAddMessage(mage, "wand.upgraded_property", getLevelString(messages, "wand.mana_regeneration_boost", xpRegenerationBoost)); }
        if (other.isForcedUpgrade() || other.xpMaxBoost > xpMaxBoost) { xpMaxBoost = other.xpMaxBoost; modified = true; if (xpMaxBoost > 0) sendAddMessage(mage, "wand.upgraded_property", getLevelString(messages, "wand.mana_boost", xpMaxBoost)); }

        boolean needsInventoryUpdate = false;
		if (other.isForcedUpgrade() || other.hotbars.size() > hotbars.size()) {
			int newCount = Math.max(1, other.hotbars.size());
			if (newCount != hotbars.size()) {
				if (isInventoryOpen()) {
					closeInventory();
				}
                setHotbarCount(newCount);
                needsInventoryUpdate = true;
				modified = true;
                if (getMode() == WandMode.INVENTORY) {
                    sendAddMessage(mage, "wand.hotbar_added", Integer.toString(newCount));
                }
			}
		}

		// Mix colors
		if (other.effectColor != null) {
			if (this.effectColor == null || (other.isUpgrade() && other.effectColor != null)) {
				this.effectColor = other.effectColor;
			} else {
				this.effectColor = this.effectColor.mixColor(other.effectColor, other.effectColorMixWeight);
			}
			modified = true;
		}

        if (other.rename && other.template != null && other.template.length() > 0) {
            ConfigurationSection template = wandTemplates.get(other.template);

            wandName = template.getString("name", wandName);
            wandName = messages.get("wands." + other.template + ".name", wandName);
            updateName();
        }

        // Kind of a hacky way to allow for quiet level overries
        if (other.quietLevel < 0) {
            int quiet = -other.quietLevel - 1;
            modified = quietLevel != quiet;
            quietLevel = quiet;
        }

        for (Map.Entry<PotionEffectType, Integer> otherEffects : other.potionEffects.entrySet()) {
            Integer current = potionEffects.get(otherEffects.getKey());
            if (current == null || current < otherEffects.getValue()) {
                potionEffects.put(otherEffects.getKey(), otherEffects.getValue());
                modified = true;
            }
        }

		modified = modified | (!keep && other.keep);
		modified = modified | (!bound && other.bound);
		modified = modified | (!effectBubbles && other.effectBubbles);
        modified = modified | (!undroppable && other.undroppable);
        modified = modified | (!indestructible && other.indestructible);
        modified = modified | (!superPowered && other.superPowered);
        modified = modified | (!superProtected && other.superProtected);
        modified = modified | (!glow && other.glow);

		keep = keep || other.keep;
		bound = bound || other.bound;
        indestructible = indestructible || other.indestructible;
        superPowered = superPowered || other.superPowered;
        superProtected = superProtected || other.superProtected;
        glow = glow || other.glow;
        undroppable = undroppable || other.undroppable;
        effectBubbles = effectBubbles || other.effectBubbles;
		if (other.effectParticle != null && (other.isUpgrade || effectParticle == null)) {
			modified = modified | (effectParticle != other.effectParticle);
			effectParticle = other.effectParticle;
			modified = modified | (effectParticleData != other.effectParticleData);
			effectParticleData = other.effectParticleData;
			modified = modified | (effectParticleCount != other.effectParticleCount);
			effectParticleCount = other.effectParticleCount;
			modified = modified | (effectParticleInterval != other.effectParticleInterval);
			effectParticleInterval = other.effectParticleInterval;
            modified = modified | (effectParticleRadius != other.effectParticleRadius);
            effectParticleRadius = other.effectParticleRadius;
            modified = modified | (effectParticleOffset != other.effectParticleOffset);
            effectParticleOffset = other.effectParticleOffset;
            modified = modified | (effectParticleMinVelocity < other.effectParticleOffset);
            effectParticleMinVelocity = Math.max(effectParticleMinVelocity, other.effectParticleMinVelocity);
		}

        if (other.castSpell != null && (other.isUpgrade || castSpell == null || !castSpell.equals(other.castSpell))) {
            modified = true;
            castSpell = other.castSpell;
            castParameters = other.castParameters;
            castInterval = other.castInterval;
            castVelocityDirection = other.castVelocityDirection;
            castMinVelocity = other.castMinVelocity;
        }
		
		if (other.effectSound != null && (other.isUpgrade || effectSound == null)) {
			modified = modified | (effectSound == null || !effectSound.equals(other.effectSound));
            effectSound = other.effectSound;
			modified = modified | (effectSoundInterval != other.effectSoundInterval);
			effectSoundInterval = other.effectSoundInterval;
		}
		
		if ((template == null || template.length() == 0) && (other.template != null && other.template.length() > 0)) {
			modified = true;
			template = other.template;
		}
		
		if (other.isUpgrade && other.mode != null) {
            if (mode != other.mode) {
                if (isInventoryOpen()) {
                    closeInventory();
                }
                needsInventoryUpdate = true;
                modified = true;
                setMode(other.mode);
            }
		}

        if (needsInventoryUpdate) {
            String wandSpells = getSpellString();
            String wandMaterials = getMaterialString();
            if (wandMaterials.length() > 0 || wandSpells.length() > 0) {
                parseInventoryStrings(wandSpells, wandMaterials);
            }
        }

        if (other.isUpgrade && other.brushMode != null) {
            modified = modified | (brushMode != other.brushMode);
            setBrushMode(other.brushMode);
        }

        if (other.upgradeIcon != null && (this.icon == null
               || this.icon.getMaterial() != other.upgradeIcon.getMaterial()
               || this.icon.getData() != other.upgradeIcon.getData())) {
            modified = true;
            this.setIcon(other.upgradeIcon);
        }
		
		// Don't need mana if cost-free
		if (isCostFree()) {
			xpRegeneration = 0;
			xpMax = 0;
			xp = 0;
		} else {
			if (other.isForcedUpgrade() || other.xpRegeneration > xpRegeneration) { xpRegeneration = other.xpRegeneration; modified = true; sendAddMessage(mage, "wand.upgraded_property", getLevelString(messages, "wand.mana_regeneration", xpRegeneration, controller.getMaxManaRegeneration())); }
			if (other.isForcedUpgrade() || other.xpMax > xpMax) { xpMax = other.xpMax; modified = true; sendAddMessage(mage, "wand.upgraded_property", getLevelString(messages, "wand.mana_amount", xpMax, controller.getMaxMana())); }
			if (other.isForcedUpgrade() || other.xp > xp) {
                float previousXP = xp;
                xp = Math.min(xpMax, other.xp);
                if (xp > previousXP) {
                    if (mage != null)
                    {
                        String message = controller.getMessages().get("wand.mana_added").replace("$value", Integer.toString((int)xp)).replace("$wand", getName());
                        mage.sendMessage(message);
                    }
                    modified = true;
                }
            }
		}
		
		// Add spells
		Set<String> spells = other.getSpells();
		for (String spellKey : spells) {
            SpellTemplate currentSpell = getBaseSpell(spellKey);
			if (addSpell(spellKey)) {
				modified = true;
				String spellName = spellKey;
				SpellTemplate spell = controller.getSpellTemplate(spellKey);
				if (spell != null) spellName = spell.getName();

                if (mage != null) {
                    if (currentSpell != null) {
                        String levelDescription = spell.getLevelDescription();
                        if (levelDescription == null || levelDescription.isEmpty()) {
                            levelDescription = spellName;
                        }
                        mage.sendMessage(messages.get("wand.spell_upgraded").replace("$name", currentSpell.getName()).replace("$level", levelDescription).replace("$wand", getName()));
                        mage.sendMessage(spell.getUpgradeDescription().replace("$name", currentSpell.getName()));
                    } else {
                        mage.sendMessage(messages.get("wand.spell_added").replace("$name", spellName).replace("$wand", getName()));
                    }
                }
			}
		}

		// Add materials
		Set<String> materials = other.getBrushes();
		for (String materialKey : materials) {
			if (addBrush(materialKey)) {
				modified = true;
				if (mage != null) mage.sendMessage(messages.get("wand.brush_added").replace("$wand", getName()).replace("$name", MaterialBrush.getMaterialName(messages, materialKey)));
			}
		}

        // Add cast overrides
        if (other.castOverrides != null && other.castOverrides.size() > 0) {
            if (castOverrides == null) {
                castOverrides = new HashMap<String, String>();
            }
            HashSet<String> upgradedSpells = new HashSet<String>();
            for (Map.Entry<String, String> entry : other.castOverrides.entrySet()) {
                String overrideKey = entry.getKey();
                String currentValue = castOverrides.get(overrideKey);
                String value = entry.getValue();
                if (currentValue != null && !other.isForcedUpgrade()) {
                    try {
                        double currentDouble = Double.parseDouble(currentValue);
                        double newDouble = Double.parseDouble(value);
                        if (newDouble < currentDouble) {
                            value = currentValue;
                        }
                    } catch (Exception ex) {
                    }
                }

                boolean addOverride = currentValue == null || !value.equals(currentValue);
                modified = modified || addOverride;
                if (addOverride && mage != null && overrideKey.contains(".")) {
                    String[] pieces = StringUtils.split(overrideKey, '.');
                    String spellKey = pieces[0];
                    String spellName = spellKey;
                    if (!upgradedSpells.contains(spellKey)) {
                        SpellTemplate spell = controller.getSpellTemplate(spellKey);
                        if (spell != null) spellName = spell.getName();
                        mage.sendMessage(messages.get("wand.spell_override_upgraded").replace("$name", spellName));
                        upgradedSpells.add(spellKey);
                    }
                }
                castOverrides.put(entry.getKey(), entry.getValue());
            }
        }
		
		Player player = (mage == null) ? null : mage.getPlayer();
		if (other.autoFill && player != null) {
			this.fill(player, controller.getMaxWandFillLevel());
			modified = true;
			if (mage != null) mage.sendMessage(controller.getMessages().get("wand.filled").replace("$wand", getName()));
		}
		
		if (other.autoOrganize && mage != null) {
			this.organizeInventory(mage);
			modified = true;
			if (mage != null) mage.sendMessage(controller.getMessages().get("wand.reorganized").replace("$wand", getName()));
		}

        if (other.autoAlphabetize) {
            this.alphabetizeInventory();
            modified = true;
            if (mage != null) mage.sendMessage(controller.getMessages().get("wand.alphabetized").replace("$wand", getName()));
        }

		saveState();
		updateName();
		updateLore();
        updateMaxMana();

		return modified;
	}

    public boolean isForcedUpgrade()
    {
        return isUpgrade && forceUpgrade;
    }
	
	public boolean keepOnDeath() {
		return keep;
	}
	
	public static void loadTemplates(ConfigurationSection properties) {
		wandTemplates.clear();
		
		Set<String> wandKeys = properties.getKeys(false);
		for (String key : wandKeys)
		{
			ConfigurationSection wandNode = properties.getConfigurationSection(key);
			wandNode.set("key", key);
			ConfigurationSection existing = wandTemplates.get(key);
			if (existing != null) {
				Set<String> overrideKeys = existing.getKeys(false);
				for (String propertyKey : overrideKeys) {
					existing.set(propertyKey, existing.get(key));
				}
			} else {
				wandTemplates.put(key,  wandNode);
			}
			if (!wandNode.getBoolean("enabled", true)) {
				wandTemplates.remove(key);
			}
		}
	}
	
	public static Collection<String> getWandKeys() {
		return wandTemplates.keySet();
	}
	
	public static Collection<ConfigurationSection> getWandTemplates() {
		return wandTemplates.values();
	}

    public static ConfigurationSection getWandTemplate(String key) {
        return wandTemplates.get(key);
    }

    public static WandMode parseWandMode(String modeString, WandMode defaultValue) {
		for (WandMode testMode : WandMode.values()) {
			if (testMode.name().equalsIgnoreCase(modeString)) {
				return testMode;
			}
		}
		
		return defaultValue;
	}
	
	private void updateActiveMaterial() {
		if (mage == null) return;
		
		if (activeMaterial == null) {
			mage.clearBuildingMaterial();
		} else {
			com.elmakers.mine.bukkit.api.block.MaterialBrush brush = mage.getBrush();
			brush.update(activeMaterial);
		}
	}
	
	public void toggleInventory() {
		if (!hasInventory) {
			if (activeSpell == null || activeSpell.length() == 0) {
				Set<String> spells = getSpells();
				// Sanity check, so it'll switch to inventory next time
				if (spells.size() > 1) hasInventory = true;
				if (spells.size() > 0) {
					activeSpell = spells.iterator().next();
				}
			}
			updateName();
			return;
		}
		if (!isInventoryOpen()) {
			openInventory();
		} else {
			closeInventory();
		}
	}
	
	@SuppressWarnings("deprecation")
	public void cycleInventory(int direction) {
		if (!hasInventory) {
			return;
		}
		if (isInventoryOpen()) {
			saveInventory();
			int inventoryCount = inventories.size();
			openInventoryPage = inventoryCount == 0 ? 0 : (openInventoryPage + inventoryCount + direction) % inventoryCount;
			updateInventory();
			if (mage != null && inventories.size() > 1) {
                if (inventoryCycleSound != null) {
                    mage.playSound(inventoryCycleSound.getSound(), inventoryCycleSound.getVolume(), inventoryCycleSound.getPitch());
                }
				mage.getPlayer().updateInventory();
			}
		}
	}

	public void cycleHotbar(int direction) {
		if (!hasInventory || getMode() != WandMode.INVENTORY) {
			return;
		}
		if (isInventoryOpen() && mage != null && hotbars.size() > 1) {
			saveInventory();
			int hotbarCount = hotbars.size();
			currentHotbar = hotbarCount == 0 ? 0 : (currentHotbar + hotbarCount + direction) % hotbarCount;
			updateHotbar();
			if (inventoryCycleSound != null) {
				mage.playSound(inventoryCycleSound.getSound(), inventoryCycleSound.getVolume(), inventoryCycleSound.getPitch());
			}
            updateHotbarStatus();
			mage.getPlayer().updateInventory();
		}
	}

    public void cycleInventory() {
        cycleInventory(1);
    }
	
	@SuppressWarnings("deprecation")
	private void openInventory() {
		if (mage == null) return;
		
		WandMode wandMode = getMode();
		if (wandMode == WandMode.CHEST) {
			inventoryIsOpen = true;
            if (inventoryOpenSound != null) {
                mage.playSound(inventoryOpenSound.getSound(), inventoryOpenSound.getVolume(), inventoryOpenSound.getPitch());
            }
            updateInventory();
			mage.getPlayer().openInventory(getDisplayInventory());
		} else if (wandMode == WandMode.INVENTORY) {
			if (hasStoredInventory()) return;
            if (storeInventory()) {
				inventoryIsOpen = true;
                if (inventoryOpenSound != null) {
                    mage.playSound(inventoryOpenSound.getSound(), inventoryOpenSound.getVolume(), inventoryOpenSound.getPitch());
                }
				updateInventory();
                updateHotbarStatus();
				mage.getPlayer().updateInventory();
			}
		}
	}
	
	public void closeInventory() {
		if (!isInventoryOpen()) return;
        controller.disableItemSpawn();
        WandMode mode = getMode();
        try {
            saveInventory();
            inventoryIsOpen = false;
            if (mage != null) {
                if (inventoryCloseSound != null) {
                    mage.playSound(inventoryCloseSound.getSound(), inventoryCloseSound.getVolume(), inventoryCloseSound.getPitch());
                }
                if (mode == WandMode.INVENTORY) {
                    restoreInventory();
                } else {
                    mage.getPlayer().closeInventory();
                }

                // Check for items the player might've glitched onto their body...
                PlayerInventory inventory = mage.getPlayer().getInventory();
                ItemStack testItem = inventory.getHelmet();
                if (isSpell(testItem) || isBrush(testItem)) {
                    inventory.setHelmet(new ItemStack(Material.AIR));
                    mage.getPlayer().updateInventory();
                }
                testItem = inventory.getBoots();
                if (isSpell(testItem) || isBrush(testItem)) {
                    inventory.setBoots(new ItemStack(Material.AIR));
                    mage.getPlayer().updateInventory();
                }
                testItem = inventory.getLeggings();
                if (isSpell(testItem) || isBrush(testItem)) {
                    inventory.setLeggings(new ItemStack(Material.AIR));
                    mage.getPlayer().updateInventory();
                }
                testItem = inventory.getChestplate();
                if (isSpell(testItem) || isBrush(testItem)) {
                    inventory.setChestplate(new ItemStack(Material.AIR));
                    mage.getPlayer().updateInventory();
                }
            }
        } catch (Throwable ex) {
            restoreInventory();
        }

        if (mode == WandMode.INVENTORY && mage != null) {
            try {
                mage.getPlayer().closeInventory();
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }
        controller.enableItemSpawn();
	}

    @Override
    public boolean fill(Player player) {
        return fill(player, 0);
    }

    @Override
	public boolean fill(Player player, int maxLevel) {
        Collection<String> currentSpells = new ArrayList<String>(getSpells());
        for (String spellKey : currentSpells) {
            SpellTemplate spell = controller.getSpellTemplate(spellKey);
            if (!spell.hasCastPermission(player))
            {
                removeSpell(spellKey);
            }
        }

		Collection<SpellTemplate> allSpells = controller.getPlugin().getSpellTemplates();
		for (SpellTemplate spell : allSpells)
		{
            if (maxLevel > 0 && spell.getSpellKey().getLevel() > maxLevel)
            {
                continue;
            }
			if (spell.hasCastPermission(player) && spell.hasIcon() && !spell.isHidden())
			{
				addSpell(spell.getKey());
			}
		}
		
		autoFill = false;
		saveState();
		
		return true;
	}

    protected boolean checkWandItem() {
        if (playerInventorySlot != null && mage != null && mage.isPlayer()) {
            Player player = mage.getPlayer();
            ItemStack currentItem = player.getInventory().getItem(playerInventorySlot);
            if (isWand(currentItem) &&
                NMSUtils.getHandle(currentItem) != NMSUtils.getHandle(item)) {
                item = currentItem;
                return true;
            }
        }

        return false;
    }

	public void activate(Mage mage, ItemStack wandItem, int slot) {
		if (mage == null || wandItem == null) return;
        id = null;

        Player player = mage.getPlayer();
        if (!canUse(player)) {
            mage.sendMessage(controller.getMessages().get("wand.bound").replace("$name", getOwner()));
            mage.setActiveWand(null);
            return;
        }

        if (this.isUpgrade) {
            controller.getLogger().warning("Activated an upgrade item- this shouldn't happen");
            return;
        }
        playerInventorySlot = slot;
		
		// Update held item, it may have been copied since this wand was created.
        boolean needsSave = NMSUtils.getHandle(this.item) != NMSUtils.getHandle(wandItem);
		this.item = wandItem;
		this.mage = mage;
        boolean forceUpdate = false;
		
		// Check for an empty wand and auto-fill
		if (!isUpgrade && (controller.fillWands() || autoFill)) {
            fill(mage.getPlayer(), controller.getMaxWandFillLevel());
		}

        if (isHeroes && player != null) {
            HeroesManager heroes = controller.getHeroes();
            if (heroes != null) {
                Set<String> skills = heroes.getSkills(player);
                Collection<String> currentSpells = new ArrayList<String>(getSpells());
                for (String spellKey : currentSpells) {
                    if (spellKey.startsWith("heroes*") && !skills.contains(spellKey.substring(7)))
                    {
                        removeSpell(spellKey);
                    }
                }

                // Hack to prevent messaging
                this.mage = null;
                for (String skillKey : skills)
                {
                    String heroesKey = "heroes*" + skillKey;
                    if (!spells.containsKey(heroesKey))
                    {
                        addSpell(heroesKey);
                    }
                }
                this.mage = mage;
            }
        }
		
		// Check for auto-organize
		if (autoOrganize && !isUpgrade) {
			organizeInventory(mage);
		}

        // Check for auto-alphabetize
        if (autoAlphabetize && !isUpgrade) {
            alphabetizeInventory();
        }

        // Check for spell or other special icons in the player's inventory
        Inventory inventory = player.getInventory();
        ItemStack[] items = inventory.getContents();
        for (int i = 0; i < items.length; i++) {
            ItemStack item = items[i];
            if (addItem(item)) {
                inventory.setItem(i, null);
                forceUpdate = true;
            }
        }
		
		// Check for auto-bind
		if (bound)
        {
            String mageName = ChatColor.stripColor(mage.getPlayer().getDisplayName());
            String mageId = mage.getPlayer().getUniqueId().toString();
            boolean ownerRenamed = owner != null && ownerId != null && ownerId.equals(mageId) && !owner.equals(mageName);

            if (ownerId == null || ownerId.length() == 0 || owner == null || ownerRenamed)
            {
                takeOwnership(mage.getPlayer());
                needsSave = true;
            }
		}

        // Check for randomized wands
        if (randomize) {
            randomize();
            forceUpdate = true;
        }
		
		checkActiveMaterial();

		mage.setActiveWand(this);
		if (usesMana()) {
            if (displayManaAsXp()) {
                storedXpLevel = player.getLevel();
                storedXpProgress = player.getExp();
            }
            updateMaxMana();
		}

        // Tick might save state, but it returns true to let us know when it does.
        if (!tick() && needsSave) {
            saveState();
        }
		updateActiveMaterial();
		updateName();
		updateLore();

        lastSoundEffect = 0;
        lastParticleEffect = 0;
        lastSpellCast = 0;
        lastLocationTime = 0;
        lastLocation = null;
        if (forceUpdate) {
            player.updateInventory();
        }
	}

    protected void randomize() {
        boolean modified = randomize;
        randomize = false;
        if (description.contains("$")) {
            String newDescription = controller.getMessages().escape(description);
            if (!newDescription.equals(description)) {
                description = newDescription;
                modified = true;
                updateLore();
                updateName();
            }
        }

        if (template != null && template.length() > 0) {
            ConfigurationSection wandConfig = wandTemplates.get(template);
            if (wandConfig != null && wandConfig.contains("icon")) {
                String iconKey = wandConfig.getString("icon");
                if (iconKey.contains(",")) {
                    Random r = new Random();
                    String[] keys = StringUtils.split(iconKey, ',');
                    iconKey = keys[r.nextInt(keys.length)];
                }
                setIcon(ConfigurationUtils.toMaterialAndData(iconKey));
                modified = true;
            }
        }

        if (modified) {
            saveState();
        }
    }
	
	protected void checkActiveMaterial() {
		if (activeMaterial == null || activeMaterial.length() == 0) {
			Set<String> materials = getBrushes();
			if (materials.size() > 0) {
				activeMaterial = materials.iterator().next();
			}
		}
	}

    @Override
	public boolean addItem(ItemStack item) {
		if (isUpgrade) return false;

		if (isModifiable() && isSpell(item) && !isSkill(item)) {
			String spellKey = getSpell(item);
            SpellTemplate currentSpell = getBaseSpell(spellKey);
			Set<String> spells = getSpells();
			if (!spells.contains(spellKey) && addSpell(spellKey)) {
				SpellTemplate spell = controller.getSpellTemplate(spellKey);
				if (spell != null) {
                    if (mage != null) {
                        if (currentSpell != null) {
                            String levelDescription = spell.getLevelDescription();
                            if (levelDescription == null || levelDescription.isEmpty()) {
                                levelDescription = spell.getName();
                            }
                            mage.sendMessage(controller.getMessages().get("wand.spell_upgraded").replace("$wand", getName()).replace("$name", currentSpell.getName()).replace("$level", levelDescription));
                            mage.sendMessage(spell.getUpgradeDescription().replace("$name", currentSpell.getName()));
                        } else {
                            mage.sendMessage(controller.getMessages().get("wand.spell_added").replace("$wand", getName()).replace("$name", spell.getName()));
                            activeSpell = spell.getKey();
                        }
                    }
                    return true;
				}
			}
		} else if (isModifiable() && isBrush(item)) {
			String materialKey = getBrush(item);
			Set<String> materials = getBrushes();
			if (!materials.contains(materialKey) && addBrush(materialKey)) {
                if (mage != null) {
                    Messages messages = controller.getMessages();
                    mage.sendMessage(messages.get("wand.brush_added").replace("$wand", getName()).replace("$name", MaterialBrush.getMaterialName(messages, materialKey)));
                }
                return true;
			}
		} else if (isUpgrade(item)) {
			Wand wand = new Wand(controller, item);
			return this.add(wand);
		}
		
		return false;
	}

    protected void updateEffects() {
        updateEffects(mage);
    }
	
	public void updateEffects(Mage mage) {
		if (mage == null) return;
		Player player = mage.getPlayer();
		if (player == null) return;
		
		// Update Bubble effects effects
		if (effectBubbles && effectColor != null) {
			CompatibilityUtils.addPotionEffect(player, effectColor.getColor());
		}
		
		Location location = mage.getLocation();
		long now = System.currentTimeMillis();
        Vector mageLocation = location.toVector();
		if (effectParticle != null && location != null && effectParticleInterval > 0 && effectParticleCount > 0) {
            boolean velocityCheck = true;
            if (effectParticleMinVelocity > 0) {
                if (lastLocation != null && lastLocationTime != 0) {
                    double velocitySquared = effectParticleMinVelocity * effectParticleMinVelocity;
                    Vector velocity = lastLocation.subtract(mageLocation);
                    velocity.setY(0);
                    double speedSquared = velocity.lengthSquared() * 1000 / (now - lastLocationTime);
                    velocityCheck = (speedSquared > velocitySquared);
                } else {
                    velocityCheck = false;
                }
            }
			if (velocityCheck && (lastParticleEffect == 0 || now > lastParticleEffect + effectParticleInterval)) {
                lastParticleEffect = now;
                Location effectLocation = player.getLocation();
                Location eyeLocation = player.getEyeLocation();
                effectLocation.setY(eyeLocation.getY() + effectParticleOffset);
                if (effectPlayer == null) {
					effectPlayer = new EffectRing(controller.getPlugin());
					effectPlayer.setParticleCount(1);
					effectPlayer.setIterations(1);
                    effectPlayer.setParticleOffset(0, 0, 0);
				}
                effectPlayer.setMaterial(location.getBlock().getRelative(BlockFace.DOWN));
                if (effectParticleData == 0)
                {
                    effectPlayer.setColor(getEffectColor());
                }
                else
                {
                    effectPlayer.setColor(null);
                }
				effectPlayer.setParticleType(effectParticle);
				effectPlayer.setParticleData(effectParticleData);
				effectPlayer.setSize(effectParticleCount);
                effectPlayer.setRadius((float)effectParticleRadius);
				effectPlayer.start(effectLocation, null);
			}
		}

        if (castSpell != null && location != null && castInterval > 0) {
            boolean velocityCheck = true;
            if (castMinVelocity > 0) {
                if (lastLocation != null && lastLocationTime != 0) {
                    double velocitySquared = castMinVelocity * castMinVelocity;
                    Vector velocity = lastLocation.subtract(mageLocation).multiply(-1);
                    if (castVelocityDirection != null) {
                        velocity = velocity.multiply(castVelocityDirection);

                        // This is kind of a hack to make jump-detection work.
                        if (castVelocityDirection.getY() < 0) {
                            velocityCheck = velocity.getY() < 0;
                        } else {
                            velocityCheck = velocity.getY() > 0;
                        }
                    }
                    if (velocityCheck)
                    {
                        double speedSquared = velocity.lengthSquared() * 1000 / (now - lastLocationTime);
                        velocityCheck = (speedSquared > velocitySquared);
                    }
                } else {
                    velocityCheck = false;
                }
            }
            if (velocityCheck && (lastSpellCast == 0 || now > lastSpellCast + castInterval)) {
                lastSpellCast = now;
                Spell spell = mage.getSpell(castSpell);
                if (spell != null) {
                    mage.setTrackCasts(false);
                    mage.setCostReduction(100);
                    mage.setQuiet(true);
                    try {
                        spell.cast(castParameters);
                    } catch (Exception ex) {
                        controller.getLogger().log(Level.WARNING, "Error casting aura spell " + spell.getKey(), ex);
                    }
                    mage.setQuiet(false);
                    mage.setTrackCasts(true);
                    mage.setCostReduction(0);
                }
            }
        }
		
		if (effectSound != null && location != null && controller.soundsEnabled() && effectSoundInterval > 0) {
			if (lastSoundEffect == 0 || now > lastSoundEffect + effectSoundInterval) {
                lastSoundEffect = now;
				mage.getLocation().getWorld().playSound(location, effectSound.getSound(), effectSound.getVolume(), effectSound.getPitch());
			}
		}

        lastLocation = mageLocation;
        lastLocationTime = now;
	}

    protected void updateDurability() {
        int maxDurability = item.getType().getMaxDurability();
        if (maxDurability > 0 && effectiveXpMax > 0) {
            int durability = (short)(xp * maxDurability / effectiveXpMax);
            durability = maxDurability - durability;
            if (durability >= maxDurability) {
                durability = maxDurability - 1;
            } else if (durability < 0) {
                durability = 0;
            }
            item.setDurability((short)durability);
        }
    }

    public boolean displayManaAsXp()
    {
        return !displayManaAsGlow && !displayManaAsDurability;
    }

	protected void updateMana() {
		if (mage != null && effectiveXpMax > 0) {
			Player player = mage.getPlayer();
            if (displayManaAsGlow) {
                if (xp == effectiveXpMax) {
                    CompatibilityUtils.addGlow(item);
                } else {
                    CompatibilityUtils.removeGlow(item);
                }
            }
            if (displayManaAsDurability) {
                updateDurability();
            }
			else {
                if (displayManaAsBar) {
                    if (!retainLevelDisplay) {
                        player.setLevel(0);
                    }
                    player.setExp(xp / (float)effectiveXpMax);
                } else {
                    player.setLevel((int)xp);
                    player.setExp(0);
                }
            }
		}
	}
	
	public boolean isInventoryOpen() {
		return mage != null && inventoryIsOpen;
	}
	
	public void deactivate() {
		if (mage == null) return;

        Player player = mage.getPlayer();
		if (effectBubbles && player != null) {
			CompatibilityUtils.removePotionEffect(player);
		}
		
		// This is a tying wands together with other spells, potentially
		// But with the way the mana system works, this seems like the safest route.
		mage.deactivateAllSpells();
		
		if (isInventoryOpen()) {
			closeInventory();
		}
        playerInventorySlot = null;
        storedInventory = null;
		
		if (usesMana() && displayManaAsXp() && player != null) {
            player.setLevel(storedXpLevel);
            player.setExp(storedXpProgress);
			storedXpProgress = 0;
			storedXpLevel = 0;
		}
        saveState();
		mage.setActiveWand(null);
		mage = null;
	}
	
	public Spell getActiveSpell() {
		if (mage == null || activeSpell == null || activeSpell.length() == 0) return null;
		return mage.getSpell(activeSpell);
	}

    public SpellTemplate getBaseSpell(String spellName) {
        SpellKey key = new SpellKey(spellName);
        Integer spellLevel = spellLevels.get(key.getBaseKey());
        if (spellLevel == null) return null;

        String spellKey = key.getBaseKey();
        if (key.isVariant()) {
            spellKey += "|" + key.getLevel();
        }
        return controller.getSpellTemplate(spellKey);
    }

    public String getActiveSpellKey() {
        return activeSpell;
    }

    public String getActiveBrushKey() {
        return activeMaterial;
    }

    public boolean cast() {
        return cast(getActiveSpell());
    }

	public boolean cast(Spell spell) {
        if (hasUses && uses <= 0) {
            use();
            return false;
        }
		if (spell != null) {
            Collection<String> castParameters = null;
            if (castOverrides != null && castOverrides.size() > 0) {
                castParameters = new ArrayList<String>();
                for (Map.Entry<String, String> entry : castOverrides.entrySet()) {
                    String[] key = StringUtils.split(entry.getKey(), ".");
                    if (key.length == 0) continue;
                    if (key.length == 2 && !key[0].equals("default") && !key[0].equals(spell.getSpellKey().getBaseKey()) && !key[0].equals(spell.getSpellKey().getKey())) {
                        continue;
                    }
                    castParameters.add(key.length == 2 ? key[1] : key[0]);
                    castParameters.add(entry.getValue());
                }
            }
			if (spell.cast(castParameters == null ? null : castParameters.toArray(EMPTY_PARAMETERS))) {
				Color spellColor = spell.getColor();
                use();
				if (spellColor != null && this.effectColor != null) {
					this.effectColor = this.effectColor.mixColor(spellColor, effectColorSpellMixWeight);
					// Note that we don't save this change.
					// The hope is that the wand will get saved at some point later
					// And we don't want to trigger NBT writes every spell cast.
					// And the effect color morphing isn't all that important if a few
					// casts get lost.
				}
                if (!isLocked() && spell instanceof MageSpell)
                {
                    MageSpell mageSpell = (MageSpell)spell;
                    MageSpell upgrade = mageSpell.getUpgrade();
                    long requiredCasts = mageSpell.getRequiredUpgradeCasts();
                    if (upgrade != null && requiredCasts > 0 && mageSpell.getCastCount() >= requiredCasts)
                    {
                        String upgradePath = mageSpell.getRequiredUpgradePath();
                        WandUpgradePath currentPath = (WandUpgradePath)getPath();
                        if (upgradePath == null || upgradePath.isEmpty() || (currentPath != null && currentPath.hasPath(upgradePath)))
                        {
                            addSpell(upgrade.getKey());
                            Messages messages = controller.getMessages();
                            String levelDescription = upgrade.getLevelDescription();
                            if (levelDescription == null || levelDescription.isEmpty()) {
                                levelDescription = upgrade.getName();
                            }
                            upgrade.playEffects("upgrade");
                            mage.sendMessage(messages.get("wand.spell_upgraded").replace("$name", upgrade.getName()).replace("$wand", getName()).replace("$level", levelDescription));
                            mage.sendMessage(upgrade.getUpgradeDescription().replace("$name", upgrade.getName()));
                        }
                    }
                }

                updateHotbarStatus();
				return true;
			}
		}
		
		return false;
	}
	
	@SuppressWarnings("deprecation")
	protected void use() {
		if (mage == null) return;
		if (hasUses) {
            if (uses > 0)
            {
                uses--;
            }
			if (uses <= 0) {
                ItemStack item = getItem();
                if (item.getAmount() > 1)
                {
                    item.setAmount(item.getAmount() - 1);
                }
                else
                {
                    Player player = mage.getPlayer();

                    deactivate();

                    PlayerInventory playerInventory = player.getInventory();
                    playerInventory.setItemInHand(new ItemStack(Material.AIR, 1));
                    player.updateInventory();
                }
			} else {
                saveState();
				updateName();
				updateLore();
			}
		}
	}

	public void onPlayerExpChange(PlayerExpChangeEvent event) {
		if (mage == null) return;

		if (addExperience(event.getAmount())) {
			event.setAmount(0);
		}
	}

    // Taken from NMS HumanEntity
    public static int getExpToLevel(int expLevel) {
        return expLevel >= 30 ? 112 + (expLevel - 30) * 9 : (expLevel >= 15 ? 37 + (expLevel - 15) * 5 : 7 + expLevel * 2);
    }

    public boolean addExperience(int xp) {
        if (usesMana() && displayManaAsXp()) {
            this.storedXpProgress += (float)xp / (float)getExpToLevel(this.storedXpLevel);

            for (; this.storedXpProgress >= 1.0F; this.storedXpProgress /= (float)getExpToLevel(this.storedXpLevel)) {
                this.storedXpProgress = (this.storedXpProgress - 1.0F) * (float)getExpToLevel(this.storedXpLevel);
                this.storedXpLevel++;
            }

            Player player = mage == null ? null : mage.getPlayer();
            if (player != null) {
                if (retainLevelDisplay) {
                    player.setLevel(storedXpLevel);
                }
                player.setExp(storedXpProgress);
            }
            return true;
        }

        return false;
    }

    public void updateExperience() {
        if (mage != null && mage.isPlayer() && usesMana() && displayManaAsXp()) {
            Player player = mage.getPlayer();
            storedXpProgress = player.getExp();
            storedXpLevel = player.getLevel();
        }
    }

    protected void updateHotbarStatus() {
        Player player = mage == null ? null : mage.getPlayer();
        if (player != null && LiveHotbar && getMode() == WandMode.INVENTORY && isInventoryOpen()) {
            Location location = mage.getLocation();
            for (int i = 0; i < HOTBAR_SIZE; i++) {
                ItemStack spellItem = player.getInventory().getItem(i);
                String spellKey = getSpell(spellItem);
                if (spellKey != null) {
                    Spell spell = mage.getSpell(spellKey);
                    if (spell != null) {
                        if (spell.canCast(location) && spell.getRemainingCooldown() == 0 && spell.getRequiredCost() == null) {
                            if (spellItem.getAmount() != 1) {
                                InventoryUtils.setCount(spellItem, 1);
                            }
                        } else {
                            if (spellItem.getAmount() != 0) {
                                InventoryUtils.setCount(spellItem, 0);
                            }
                        }
                    }
                }
            }
        }
    }
	
	public boolean tick() {
		if (mage == null || item == null) return false;
		
		Player player = mage.getPlayer();
		if (player == null) return false;

        boolean modified = checkWandItem();
        int maxDurability = item.getType().getMaxDurability();

        // Auto-repair wands
        if (!displayManaAsDurability && maxDurability > 0 && indestructible) {
            item.setDurability((short)0);
        }

		if (usesMana()) {
            long now = System.currentTimeMillis();
            if (isHeroes)
            {
                HeroesManager heroes = controller.getHeroes();
                if (heroes != null)
                {
                    effectiveXpMax = heroes.getMaxMana(player);
                    effectiveXpRegeneration = heroes.getManaRegen(player);
                    xpMax = effectiveXpMax;
                    xpRegeneration = effectiveXpRegeneration;
                    xp = heroes.getMana(player);
                    updateMana();
                }
            }
            else if (lastXpRegeneration > 0)
            {
                long delta = now - lastXpRegeneration;
                if (effectiveXpMax == 0 && xpMax > 0) {
                    effectiveXpMax = xpMax;
                }
                xp = Math.min(effectiveXpMax, xp + (float)xpRegeneration * (float)delta / 1000);
                updateMana();
            }
            lastXpRegeneration = now;
		}

        // Update hotbar glow
        updateHotbarStatus();

        if (!passive)
        {
            if (damageReductionFire > 0 && player.getFireTicks() > 0) {
                player.setFireTicks(0);
            }

            updateEffects();
        }

        if (modified) {
            saveState();
        }

        return modified;
	}

    public void armorUpdated() {
        int currentMana = effectiveXpMax;
        int currentManaRegen = effectiveXpRegeneration;
        updateMaxMana();
        if (currentMana != effectiveXpMax || effectiveXpRegeneration != currentManaRegen) {
            updateLore();
        }
    }

    protected void updateMaxMana() {
        if (isHeroes) return;

        float effectiveBoost = xpMaxBoost;
        float effectiveRegenBoost = xpRegenerationBoost;
        if (mage != null)
        {
            Collection<Wand> activeArmor = mage.getActiveArmor();
            for (Wand armorWand : activeArmor) {
                effectiveBoost += armorWand.getXpMaxBoost();
                effectiveRegenBoost += armorWand.getXpRegenerationBoost();
            }
        }
        effectiveXpMax = xpMax;
        if (effectiveBoost != 0) {
            effectiveXpMax = (int)Math.ceil(effectiveXpMax + effectiveBoost * effectiveXpMax);
        }
        effectiveXpRegeneration = xpRegeneration;
        if (effectiveRegenBoost != 0) {
            effectiveXpRegeneration = (int)Math.ceil(effectiveXpRegeneration + effectiveRegenBoost * effectiveXpRegeneration);
        }
    }

    public static Float getWandFloat(ItemStack item, String key) {
        try {
            Object wandNode = InventoryUtils.getNode(item, WAND_KEY);
            if (wandNode != null) {
                String value = InventoryUtils.getMeta(wandNode, key);
                if (value != null && !value.isEmpty()) {
                    return Float.parseFloat(value);
                }
            }
        } catch (Exception ex) {

        }
        return null;
    }

    public static String getWandString(ItemStack item, String key) {
        try {
            Object wandNode = InventoryUtils.getNode(item, WAND_KEY);
            if (wandNode != null) {
                return InventoryUtils.getMeta(wandNode, key);
            }
        } catch (Exception ex) {

        }
        return null;
    }
	
	public MagicController getMaster() {
		return controller;
	}
	
	public void cycleSpells() {
		Set<String> spellsSet = getSpells();
		ArrayList<String> spells = new ArrayList<String>(spellsSet);
		if (spells.size() == 0) return;
		if (activeSpell == null) {
			activeSpell = spells.get(0).split("@")[0];
			return;
		}
		
		int spellIndex = 0;
		for (int i = 0; i < spells.size(); i++) {
			if (spells.get(i).split("@")[0].equals(activeSpell)) {
				spellIndex = i;
				break;
			}
		}
		
		spellIndex = (spellIndex + 1) % spells.size();
		setActiveSpell(spells.get(spellIndex).split("@")[0]);
	}
	
	public void cycleMaterials() {
		Set<String> materialsSet = getBrushes();
		ArrayList<String> materials = new ArrayList<String>(materialsSet);
		if (materials.size() == 0) return;
		if (activeMaterial == null) {
			activeMaterial = materials.get(0).split("@")[0];
			return;
		}
		
		int materialIndex = 0;
		for (int i = 0; i < materials.size(); i++) {
			if (materials.get(i).split("@")[0].equals(activeMaterial)) {
				materialIndex = i;
				break;
			}
		}
		
		materialIndex = (materialIndex + 1) % materials.size();
		setActiveBrush(materials.get(materialIndex).split("@")[0]);
	}

	public Mage getActivePlayer() {
		return mage;
	}
	
	public Color getEffectColor() {
		return effectColor == null ? null : effectColor.getColor();
	}

    public ParticleEffect getEffectParticle() {
        return effectParticle;
    }

	public Inventory getHotbar() {
        WandMode mode = getMode();
        if (this.hotbars.size() == 0 || mode != WandMode.INVENTORY) return null;

		if (currentHotbar < 0 || currentHotbar >= this.hotbars.size())
		{
			currentHotbar = 0;
		}
		return this.hotbars.get(currentHotbar);
	}

    public int getHotbarCount() {
        if (getMode() != WandMode.INVENTORY) return 0;
        return hotbars.size();
    }

	public List<Inventory> getHotbars() {
		return hotbars;
	}
	
	public WandMode getMode() {
		return mode != null ? mode : controller.getDefaultWandMode();
	}

    public WandMode getBrushMode() {
        return brushMode != null ? brushMode : controller.getDefaultBrushMode();
    }
	
	public void setMode(WandMode mode) {
		this.mode = mode;
	}

    public void setBrushMode(WandMode mode) {
        this.brushMode = mode;
    }
	
	public boolean showCastMessages() {
		return quietLevel == 0;
	}
	
	public boolean showMessages() {
		return quietLevel < 2;
	}

    public boolean isStealth() {
        return quietLevel > 2;
    }

    @Override
    public void setPath(String path) {
        this.path = path;
    }

	/*
	 * Public API Implementation
	 */

    @Override
	public boolean isLost()
    {
		return this.id != null;
	}

    @Override
    public boolean isLost(com.elmakers.mine.bukkit.api.wand.LostWand lostWand) {
        return this.id != null && this.id.equals(lostWand.getId());
    }

    @Override
    public LostWand makeLost(Location location)
    {
        if (id == null || id.length() == 0) {
            id = UUID.randomUUID().toString();
            saveState();
        }
        return new LostWand(this, location);
    }

	@Override
	public void activate(com.elmakers.mine.bukkit.api.magic.Mage mage) {
		Player player = mage.getPlayer();
		if (!Wand.hasActiveWand(player)) {
			controller.getLogger().warning("Wand activated without holding a wand!");
			return;
		}
		
		if (mage instanceof Mage) {
			activate((Mage)mage, player.getItemInHand(), player.getInventory().getHeldItemSlot());
		}
	}

	@Override
	public void organizeInventory(com.elmakers.mine.bukkit.api.magic.Mage mage) {
        WandOrganizer organizer = new WandOrganizer(this, mage);
        organizer.organize();
        openInventoryPage = 0;
		currentHotbar = 0;
        autoOrganize = false;
        autoAlphabetize = false;

		// Force inventory re-load
		saveState();
		loadState();
		updateInventory();
    }

    @Override
    public void alphabetizeInventory() {
        WandOrganizer organizer = new WandOrganizer(this);
        organizer.alphabetize();
        openInventoryPage = 0;
		currentHotbar = 0;
        autoOrganize = false;
        autoAlphabetize = false;

		// Force inventory re-load
		saveState();
		loadState();
		updateInventory();
    }

	@Override
	public com.elmakers.mine.bukkit.api.wand.Wand duplicate() {
		ItemStack newItem = InventoryUtils.getCopy(item);
		Wand newWand = new Wand(controller, newItem);
		newWand.saveState();
		return newWand;
	}

	@Override
	public boolean configure(Map<String, Object> properties) {
		Map<Object, Object> convertedProperties = new HashMap<Object, Object>(properties);
		loadProperties(ConfigurationUtils.toNodeList(convertedProperties), false);
        saveState();
        updateName();
        updateLore();
		return true;
	}

	@Override
	public boolean upgrade(Map<String, Object> properties) {
		Map<Object, Object> convertedProperties = new HashMap<Object, Object>(properties);
		loadProperties(ConfigurationUtils.toNodeList(convertedProperties), true);
        saveState();
        updateName();
        updateLore();
		return true;
	}

	@Override
	public boolean isLocked() {
		return this.locked;
	}

    @Override
    public void unlock() {
        locked = false;
    }

    public boolean isPassive() {
        return passive;
    }

	@Override
	public boolean canUse(Player player) {
		if (!bound || ownerId == null || ownerId.length() == 0) return true;
		if (controller.hasPermission(player, "Magic.wand.override_bind", false)) return true;

		return ownerId.equalsIgnoreCase(player.getUniqueId().toString());
	}
	
	public boolean addSpell(String spellName) {
		if (!isModifiable()) return false;

        SpellKey spellKey = new SpellKey(spellName);
        if (hasSpell(spellKey)) {
            return false;
        }

        if (isInventoryOpen()) {
			saveInventory();
		}
        SpellTemplate template = controller.getSpellTemplate(spellName);
        if (template == null) {
            controller.getLogger().warning("Tried to add unknown spell to wand: " + spellName);
            return false;
        }

        // This handles adding via an alias
        if (hasSpell(template.getKey())) return false;

		ItemStack spellItem = createSpellIcon(template);
		if (spellItem == null) {
			return false;
		}
        spellKey = template.getSpellKey();
        int level = spellKey.getLevel();
        int inventoryCount = inventories.size();
        int spellCount = spells.size();

        // Special handling for spell upgrades
        Integer inventorySlot = null;
        Integer currentLevel = spellLevels.get(spellKey.getBaseKey());
        if (currentLevel != null) {
            if (activeSpell != null && !activeSpell.isEmpty()) {
                SpellKey currentKey = new SpellKey(activeSpell);
                if (currentKey.getBaseKey().equals(spellKey.getBaseKey())) {
                    activeSpell = spellKey.getKey();
                }
            }
            List<Inventory> allInventories = getAllInventories();
            int currentSlot = 0;
            for (Inventory inventory : allInventories) {
                ItemStack[] items = inventory.getContents();
                for (int index = 0; index < items.length; index++) {
                    ItemStack itemStack = items[index];
                    if (isSpell(itemStack)) {
                        SpellKey checkKey = new SpellKey(getSpell(itemStack));
                        if (checkKey.getBaseKey().equals(spellKey.getBaseKey())) {
                            inventorySlot = currentSlot;
                            inventory.setItem(index, null);
                            spells.remove(checkKey.getKey());
                        }
                    }
                    currentSlot++;
                }
            }
        }
        if (activeSpell == null || activeSpell.isEmpty()) {
            activeSpell = spellKey.getKey();
        }

        spellLevels.put(spellKey.getBaseKey(), level);
        spells.put(template.getKey(), inventorySlot);
		addToInventory(spellItem, inventorySlot);
		updateInventory();
		hasInventory = getSpells().size() + getBrushes().size() > 1;
        saveState();
		updateLore();

        if (mage != null && spells.size() != spellCount)
        {
            if (spellCount == 0)
            {
                String message = controller.getMessages().get("wand.spell_instructions", "").replace("$wand", getName());
                mage.sendMessage(message.replace("$spell", template.getName()));
            }
            else
            if (spellCount == 1)
            {
                mage.sendMessage(controller.getMessages().get("wand.inventory_instructions", "").replace("$wand", getName()));
            }
            if (inventoryCount == 1 && inventories.size() > 1)
            {
                mage.sendMessage(controller.getMessages().get("wand.page_instructions", "").replace("$wand", getName()));
            }
        }
		
		return true;
	}

	@Override
	public boolean add(com.elmakers.mine.bukkit.api.wand.Wand other) {
		if (other instanceof Wand) {
			return add((Wand)other);
		}
		
		return false;
	}

    @Override
	public boolean hasBrush(String materialKey) {
		return getBrushes().contains(materialKey);
	}
	
	@Override
	public boolean hasSpell(String spellName) {
		return hasSpell(new SpellKey(spellName));
	}

    public boolean hasSpell(SpellKey spellKey) {
        Integer level = spellLevels.get(spellKey.getBaseKey());
        return (level != null && level >= spellKey.getLevel());
    }
	
	@Override
	public boolean addBrush(String materialKey) {
		if (!isModifiable()) return false;
		if (hasBrush(materialKey)) return false;

        if (isInventoryOpen()) {
            saveInventory();
        }
		
		ItemStack itemStack = createBrushIcon(materialKey);
		if (itemStack == null) return false;

        int inventoryCount = inventories.size();
        int brushCount = brushes.size();

        brushes.put(materialKey, null);
		addToInventory(itemStack);
		if (activeMaterial == null || activeMaterial.length() == 0) {
			activateBrush(materialKey);
		} else {
			updateInventory();
		}
        hasInventory = getSpells().size() + getBrushes().size() > 1;
        saveState();
		updateLore();

        if (mage != null)
        {
            if (brushCount == 0)
            {
                mage.sendMessage(controller.getMessages().get("wand.brush_instructions", "").replace("$wand", getName()));
            }
            if (inventoryCount == 1 && inventories.size() > 1)
            {
                mage.sendMessage(controller.getMessages().get("wand.page_instructions", "").replace("$wand", getName()));
            }
        }

        return true;
	}

    @Override
    public void setActiveBrush(String materialKey) {
        activateBrush(materialKey);
        if (materialKey != null && mage != null) {
            com.elmakers.mine.bukkit.api.block.MaterialBrush brush = mage.getBrush();
            if (brush != null)
            {
                boolean eraseWasActive = brush.isEraseModifierActive();
                brush.activate(mage.getLocation(), materialKey);

                if (mage != null) {
                    BrushMode mode = brush.getMode();
                    if (mode == BrushMode.CLONE) {
                        mage.sendMessage(controller.getMessages().get("wand.clone_material_activated"));
                    } else if (mode == BrushMode.REPLICATE) {
                        mage.sendMessage(controller.getMessages().get("wand.replicate_material_activated"));
                    }
                    if (!eraseWasActive && brush.isEraseModifierActive()) {
                        mage.sendMessage(controller.getMessages().get("wand.erase_modifier_activated"));
                    }
                }
            }
        }
    }

    public void setActiveBrush(ItemStack itemStack) {
        if (!isBrush(itemStack)) return;
        setActiveBrush(getBrush(itemStack));
    }

	public void activateBrush(String materialKey) {
		this.activeMaterial = materialKey;
        saveState();
		updateName();
		updateActiveMaterial();
        updateHotbar();
	}

	@Override
	public void setActiveSpell(String activeSpell) {
        SpellKey spellKey = new SpellKey(activeSpell);
        activeSpell = spellKey.getBaseKey();
        if (!spellLevels.containsKey(activeSpell))
        {
            return;
        }
        spellKey = new SpellKey(spellKey.getBaseKey(), spellLevels.get(activeSpell));
		this.activeSpell = spellKey.getKey();
        saveState();
		updateName();
	}

	@Override
	public boolean removeBrush(String materialKey) {
		if (!isModifiable() || materialKey == null) return false;
		
		if (isInventoryOpen()) {
			saveInventory();
		}
		if (materialKey.equals(activeMaterial)) {
			activeMaterial = null;
		}
        brushes.remove(materialKey);
		List<Inventory> allInventories = getAllInventories();
		boolean found = false;
		for (Inventory inventory : allInventories) {
			ItemStack[] items = inventory.getContents();
			for (int index = 0; index < items.length; index++) {
				ItemStack itemStack = items[index];
				if (itemStack != null && isBrush(itemStack)) {
					String itemKey = getBrush(itemStack);
					if (itemKey.equals(materialKey)) {
						found = true;
						inventory.setItem(index, null);
					} else if (activeMaterial == null) {
						activeMaterial = materialKey;
					}
					if (found && activeMaterial != null) {
						break;
					}
				}
			}
		}
		updateActiveMaterial();
		updateInventory();
        saveState();
		updateName();
		updateLore();
		return found;
	}
	
	@Override
	public boolean removeSpell(String spellName) {
		if (!isModifiable()) return false;
		
		if (isInventoryOpen()) {
			saveInventory();
		}
		if (spellName.equals(activeSpell)) {
			activeSpell = null;
		}
        spells.remove(spellName);
        SpellKey spellKey = new SpellKey(spellName);
        spellLevels.remove(spellKey.getBaseKey());
		
		List<Inventory> allInventories = getAllInventories();
		boolean found = false;
		for (Inventory inventory : allInventories) {
			ItemStack[] items = inventory.getContents();
			for (int index = 0; index < items.length; index++) {
				ItemStack itemStack = items[index];
				if (itemStack != null && itemStack.getType() != Material.AIR && isSpell(itemStack)) {
					if (getSpell(itemStack).equals(spellName)) {
						found = true;
						inventory.setItem(index, null);
					} else if (activeSpell == null) {
						activeSpell = getSpell(itemStack);
					}
					if (found && activeSpell != null) {
						break;
					}
				}
			}
		}
        updateInventory();
        saveState();
        updateName();
        updateLore();

		return found;
	}

    @Override
    public Map<String, String> getOverrides()
    {
        return castOverrides == null ? new HashMap<String, String>() : new HashMap<String, String>(castOverrides);
    }

    @Override
    public void setOverrides(Map<String, String> overrides)
    {
        if (overrides == null) {
            this.castOverrides = null;
        } else {
            this.castOverrides = new HashMap<String, String>(overrides);
        }
    }

    @Override
    public void removeOverride(String key)
    {
        if (castOverrides != null) {
            castOverrides.remove(key);
        }
    }

    @Override
    public void setOverride(String key, String value)
    {
        if (castOverrides == null) {
            castOverrides = new HashMap<String, String>();
        }
        if (value == null || value.length() == 0) {
            castOverrides.remove(key);
        } else {
            castOverrides.put(key, value);
        }
    }

    public void setStoredXpLevel(int level) {
        this.storedXpLevel = level;
    }

    public int getStoredXpLevel() {
        return storedXpLevel;
    }

    public float getStoredXpProgress() {
        return storedXpProgress;
    }

    public boolean hasStoredInventory() {
        return storedInventory != null;
    }

    public Inventory getStoredInventory() {
        return storedInventory;
    }

    public boolean addToStoredInventory(ItemStack item) {
        if (storedInventory == null) {
            return false;
        }

        HashMap<Integer, ItemStack> remainder = storedInventory.addItem(item);
        return remainder.size() == 0;
    }

    public boolean storeInventory() {
        if (storedInventory != null) {
            if (mage != null) {
                mage.sendMessage("Your wand contains a previously stored inventory and will not activate, let go of it to clear.");
            }
            controller.getLogger().warning("Tried to store an inventory with one already present: " + (mage == null ? "?" : mage.getName()));
            return false;
        }

        Player player = mage.getPlayer();
        if (player == null) {
            return false;
        }
        PlayerInventory inventory = player.getInventory();
        storedInventory = CompatibilityUtils.createInventory(null, inventory.getSize(), "Stored Inventory");

        // Make sure we don't store any spells or magical materials, just in case
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length; i++) {
            if (Wand.isSpell(contents[i]) && !Wand.isSkill(contents[i])) {
                contents[i] = null;
            }
        }
        storedInventory.setContents(contents);
        inventory.clear();
        if (controller.isInventoryBackupEnabled()) {
            saveState();
        }

        return true;
    }

    @SuppressWarnings("deprecation")
    public boolean restoreInventory() {
        if (storedInventory == null) {
            return false;
        }
        Player player = mage.getPlayer();
        if (player == null) {
            return false;
        }
        PlayerInventory inventory = player.getInventory();
        inventory.setContents(storedInventory.getContents());
        storedInventory = null;
        saveState();

        player.updateInventory();

        return true;
    }

    public boolean isBound() {
        return bound;
    }

    public int getSpellLevel(String spellKey) {
        SpellKey key = new SpellKey(spellKey);
        Integer level = spellLevels.get(key.getBaseKey());
        return level == null ? 0 : level;
    }

    public Integer getPlayerInventorySlot()
    {
        return playerInventorySlot;
    }

    @Override
    public MageController getController() {
        return controller;
    }

    protected Map<String, Integer> getSpellInventory() {
        return new HashMap<String, Integer>(spells);
    }

    protected Map<String, Integer> getBrushInventory() {
        return new HashMap<String, Integer>(brushes);
    }

    protected void updateSpellInventory(Map<String, Integer> updateSpells) {
        for (Map.Entry<String, Integer> spellEntry : spells.entrySet()) {
            String spellKey = spellEntry.getKey();
            Integer slot = updateSpells.get(spellKey);
            if (slot != null) {
                spellEntry.setValue(slot);
            }
        }
    }

    protected void updateBrushInventory(Map<String, Integer> updateBrushes) {
        for (Map.Entry<String, Integer> brushEntry : brushes.entrySet()) {
            String brushKey = brushEntry.getKey();
            Integer slot = updateBrushes.get(brushKey);
            if (slot != null) {
                brushEntry.setValue(slot);
            }
        }
    }

    public Map<PotionEffectType, Integer> getPotionEffects() {
        return potionEffects;
    }

    @Override
    public float getHealthRegeneration() {
        Integer level = potionEffects.get(PotionEffectType.REGENERATION);
        return level != null && level > 0 ? (float)level : 0;
    }

    @Override
    public float getHungerRegeneration()  {
        Integer level = potionEffects.get(PotionEffectType.SATURATION);
        return level != null && level > 0 ? (float)level : 0;
    }
}
