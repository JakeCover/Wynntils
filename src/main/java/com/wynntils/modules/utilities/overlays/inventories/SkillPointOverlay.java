/*
 *  * Copyright © Wynntils - 2018 - 2020.
 */

package com.wynntils.modules.utilities.overlays.inventories;

import com.wynntils.Reference;
import com.wynntils.core.events.custom.GuiOverlapEvent;
import com.wynntils.core.framework.enums.SkillPoint;
import com.wynntils.core.framework.enums.SpellType;
import com.wynntils.core.framework.instances.PlayerInfo;
import com.wynntils.core.framework.interfaces.Listener;
import com.wynntils.core.framework.rendering.ScreenRenderer;
import com.wynntils.core.framework.rendering.SmartFontRenderer;
import com.wynntils.core.framework.rendering.colors.CommonColors;
import com.wynntils.core.utils.ItemUtils;
import com.wynntils.core.utils.Utils;
import com.wynntils.modules.core.overlays.inventories.ChestReplacer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SkillPointOverlay implements Listener {

    @SubscribeEvent
    public void onDrawScreen(GuiOverlapEvent.ChestOverlap.DrawScreen e) {
        if (!Reference.onWorld) return;
        if (!Utils.isCharacterInfoPage(e.getGui())) return;

        addManaTables(e.getGui());
    }

    @SubscribeEvent
    public void onChestInventory(GuiOverlapEvent.ChestOverlap.DrawScreen.Pre e) {
        Matcher m = Utils.CHAR_INFO_PAGE_TITLE.matcher(e.getGui().getLowerInv().getName());
        if (!m.find()) return;

        // FIXME: Not really used -- we should keep track of this
        int skillPointsRemaining = Integer.parseInt(m.group(1));

        for (int i = 0; i < e.getGui().getLowerInv().getSizeInventory(); i++) {
            ItemStack stack = e.getGui().getLowerInv().getStackInSlot(i);
            if (stack.isEmpty() || !stack.hasDisplayName()) continue; // display name also checks for tag compound

            String lore = TextFormatting.getTextWithoutFormattingCodes(ItemUtils.getStringLore(stack));
            String name = TextFormatting.getTextWithoutFormattingCodes(stack.getDisplayName());
            int value;

            if (name.contains("Upgrade")) {// Skill Points
                int start = lore.indexOf(" points ") - 3;

                String number = lore.substring(start, start + 3).trim();

                value = Integer.parseInt(number);
            } else if (name.contains("Profession")) { // Profession Icons
                int start = lore.indexOf("Level: ") + 7;
                int end = lore.indexOf("XP: ");

                value = Integer.parseInt(lore.substring(start, end));
            } else if (name.contains("'s Info")) { // Combat level on Info
                int start = lore.indexOf("Combat Lv: ") + 11;
                int end = lore.indexOf("Class: ");

                value = Integer.parseInt(lore.substring(start, end));
            } else if (name.contains("Damage Info")) { //Average Damage
                Pattern pattern = Pattern.compile("Total Damage \\(\\+Bonus\\): ([0-9]+)-([0-9]+)");
                Matcher m2  = pattern.matcher(lore);
                if (!m2.find()) continue;

                int min = Integer.parseInt(m2.group(1));
                int max = Integer.parseInt(m2.group(2));

                value = Math.round((max + min) / 2.0f);
            } else if (name.contains("Daily Rewards")) { //Daily Reward Multiplier
                int start = lore.indexOf("Streak Multiplier: ") + 19;
                int end = lore.indexOf("Log in everyday to");

                value = Integer.parseInt(lore.substring(start, end));
            } else continue;

            stack.setCount(value <= 0 ? 1 : value);
        }
    }

    private String remainingLevelsDescription(int remainingLevels) {
        return "" + TextFormatting.GOLD + remainingLevels + TextFormatting.GRAY + " point" + (remainingLevels == 1 ? "" : "s");
    }

    private int getIntelligencePoints(ItemStack stack) {
        String lore = TextFormatting.getTextWithoutFormattingCodes(ItemUtils.getStringLore(stack));
        int start = lore.indexOf(" points ") - 3;

        return Integer.parseInt(lore.substring(start, start + 3).trim());
    }

    public void addManaTables(ChestReplacer gui) {
        ItemStack stack = gui.getLowerInv().getStackInSlot(11);
        if (stack.isEmpty() || !stack.hasDisplayName()) return; // display name also checks for tag compound

        int intelligencePoints = getIntelligencePoints(stack);
        if (stack.getTagCompound().hasKey("wynntilsAnalyzed")) return;

        int closestUpgradeLevel = Integer.MAX_VALUE;
        int level = PlayerInfo.getPlayerInfo().getLevel();

        List<String> newLore = new LinkedList<>();

        for (int j = 0; j < 4; j++) {
            SpellType spell = SpellType.forClass(PlayerInfo.getPlayerInfo().getCurrentClass(), j + 1);

            if (spell.getUnlockLevel(1) <= level) {
                int nextUpgrade = spell.getNextManaReduction(level, intelligencePoints);
                if (nextUpgrade < closestUpgradeLevel) {
                    closestUpgradeLevel = nextUpgrade;
                }
                int manaCost = spell.getManaCost(level, intelligencePoints);
                String spellName = PlayerInfo.getPlayerInfo().isCurrentClassReskinned() ? spell.getReskinned() : spell.getName();
                String spellInfo = TextFormatting.LIGHT_PURPLE + spellName + " Spell: " + TextFormatting.AQUA
                        + "-" + manaCost + " ✺";
                if (nextUpgrade < Integer.MAX_VALUE) {
                    spellInfo += TextFormatting.GRAY + " (-" + (manaCost - 1) + " ✺ in "
                            + remainingLevelsDescription(nextUpgrade - intelligencePoints) + ")";
                }
                newLore.add(spellInfo);
            }
        }

        List<String> loreTag = new LinkedList<>(ItemUtils.getLore(stack));
        if (closestUpgradeLevel < Integer.MAX_VALUE) {
            loreTag.add("");
            loreTag.add(TextFormatting.GRAY + "Next upgrade: At " + TextFormatting.WHITE + closestUpgradeLevel
                    + TextFormatting.GRAY + " points (in " + remainingLevelsDescription(closestUpgradeLevel - intelligencePoints) + ")");
        }

        loreTag.add("");
        loreTag.addAll(newLore);

        ItemUtils.replaceLore(stack, loreTag);
        stack.getTagCompound().setBoolean("wynntilsAnalyzed", true);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onChestGui(GuiOverlapEvent.ChestOverlap.HoveredToolTip.Pre e) {
        if (!Reference.onWorld) return;
        if (!Utils.isCharacterInfoPage(e.getGui())) return;

        for (Slot s : e.getGui().inventorySlots.inventorySlots) {
            String name = TextFormatting.getTextWithoutFormattingCodes(s.getStack().getDisplayName());
            SkillPoint skillPoint = SkillPoint.findSkillPoint(name);
            if (skillPoint != null) {
                ScreenRenderer.beginGL(e.getGui().getGuiLeft() , e.getGui().getGuiTop());
                GlStateManager.translate(0, 0, 251);
                ScreenRenderer r = new ScreenRenderer();
                RenderHelper.disableStandardItemLighting();
                r.drawString(skillPoint.getColoredSymbol(), s.xPos + 2, s.yPos, CommonColors.WHITE, SmartFontRenderer.TextAlignment.LEFT_RIGHT, SmartFontRenderer.TextShadow.NONE);
                ScreenRenderer.endGL();
            }
        }
    }

}
