package dinkplugin.util;

import com.google.common.collect.ImmutableSet;
import dinkplugin.domain.AccountType;
import lombok.experimental.UtilityClass;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.WorldType;
import net.runelite.api.annotations.Varbit;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

@UtilityClass
public class WorldUtils {

    private final Set<WorldType> IGNORED_WORLDS = EnumSet.of(WorldType.PVP_ARENA, WorldType.QUEST_SPEEDRUNNING, WorldType.BETA_WORLD, WorldType.NOSAVE_MODE, WorldType.TOURNAMENT_WORLD);

    private final Set<Integer> BA_REGIONS = ImmutableSet.of(7508, 7509, 10322);
    private final Set<Integer> CASTLE_WARS_REGIONS = ImmutableSet.of(9520, 9620);
    private final Set<Integer> CLAN_WARS_REGIONS = ImmutableSet.of(12621, 12622, 12623, 13130, 13131, 13133, 13134, 13135, 13386, 13387, 13390, 13641, 13642, 13643, 13644, 13645, 13646, 13647, 13899, 13900, 14155, 14156);
    private final Set<Integer> COX_REGIONS = ImmutableSet.of(12889, 13136, 13137, 13138, 13139, 13140, 13141, 13145, 13393, 13394, 13395, 13396, 13397, 13401);
    private final Set<Integer> GALVEK_REGIONS = ImmutableSet.of(6486, 6487, 6488, 6489, 6742, 6743, 6744, 6745);
    private final Set<Integer> GAUNTLET_REGIONS = ImmutableSet.of(7512, 7768, 12127); // includes CG
    private final Set<Integer> LMS_REGIONS = ImmutableSet.of(13658, 13659, 13660, 13914, 13915, 13916, 13918, 13919, 13920, 14174, 14175, 14176, 14430, 14431, 14432);
    private final Set<Integer> POH_REGIONS = ImmutableSet.of(7257, 7513, 7514, 7769, 7770, 8025, 8026);
    private final Set<Integer> SOUL_REGIONS = ImmutableSet.of(8493, 8748, 8749, 9005);
    private final Set<Integer> TOA_REGIONS = ImmutableSet.of(14160, 14162, 14164, 14674, 14676, 15184, 15186, 15188, 15696, 15698, 15700);
    private final int BURTHORPE_REGION = 8781;
    private final int INFERNO_REGION = 9043;
    private final int NMZ_REGION = 9033;
    private final int TZHAAR_CAVE = 9551;
    public final @VisibleForTesting int TZHAAR_PIT = 9552;

    /**
     * @see <a href="https://oldschool.runescape.wiki/w/RuneScape:Varbit/6104">Wiki</a>
     */
    private final @Varbit int DRAGON_SLAYER_II_PROGRESS = 6104;

    /**
     * @see <a href="https://chisel.weirdgloop.org/varbs/display?varbit=6104#ChangeFrequencyTitle">Chisel</a>
     */
    private final int DRAGON_SLAYER_II_COMPLETED = 215;

    public static WorldPoint getLocation(Client client) {
        return getLocation(client, client.getLocalPlayer());
    }

    public static WorldPoint getLocation(Client client, Actor actor) {
        if (client.isInInstancedRegion())
            return WorldPoint.fromLocalInstance(client, actor.getLocalLocation());

        return actor.getWorldLocation();
    }

    public boolean isIgnoredWorld(Set<WorldType> worldType) {
        return !Collections.disjoint(IGNORED_WORLDS, worldType);
    }

    public boolean isPvpWorld(Set<WorldType> worldType) {
        return worldType.contains(WorldType.PVP) || worldType.contains(WorldType.DEADMAN);
    }

    public boolean isPvpSafeZone(Client client) {
        Widget widget = client.getWidget(ComponentID.PVP_SAFE_ZONE);
        return widget != null && !widget.isHidden();
    }

    public boolean isBarbarianAssault(int regionId) {
        return BA_REGIONS.contains(regionId);
    }

    public boolean isBurthorpeGameRoom(int regionId) {
        return regionId == BURTHORPE_REGION;
    }

    public boolean isCastleWars(int regionId) {
        return CASTLE_WARS_REGIONS.contains(regionId);
    }

    public boolean isChambersOfXeric(int regionId) {
        return COX_REGIONS.contains(regionId);
    }

    public boolean isClanWars(int regionId) {
        return CLAN_WARS_REGIONS.contains(regionId);
    }

    public boolean isGalvekRematch(Client client, int regionId) {
        return GALVEK_REGIONS.contains(regionId) && client.getVarbitValue(DRAGON_SLAYER_II_PROGRESS) >= DRAGON_SLAYER_II_COMPLETED;
    }

    public boolean isGauntlet(int regionId) {
        return GAUNTLET_REGIONS.contains(regionId);
    }

    public boolean isInferno(int regionId) {
        return regionId == INFERNO_REGION;
    }

    public boolean isLastManStanding(Client client) {
        if (LMS_REGIONS.contains(getLocation(client).getRegionID()))
            return true;

        Widget widget = client.getWidget(ComponentID.LMS_INGAME_INFO);
        return widget != null && !widget.isHidden();
    }

    public boolean isNightmareZone(int regionId) {
        return regionId == NMZ_REGION;
    }

    public boolean isPestControl(Client client) {
        Widget widget = client.getWidget(ComponentID.PEST_CONTROL_BLUE_SHIELD);
        return widget != null && !widget.isHidden();
    }

    public boolean isPlayerOwnedHouse(int regionId) {
        return POH_REGIONS.contains(regionId);
    }

    public boolean isSafeArea(Client client) {
        int regionId = getLocation(client).getRegionID();

        if (isAmascutTombs(regionId)) {
            // ToA is technically a dangerous activity, but multiple attempts can be permitted
            // the real TOA death is detected via game message in death notifier
            // However: any TOA death is still dangerous for hardcore (group) ironmen
            return !Utils.getAccountType(client).isHardcore();
        }

        if (isGauntlet(regionId)) {
            // Players can't take items in or out of (Corrupted) Gauntlet, so these deaths are effectively safe
            // However: any Gauntlet death is still dangerous for hardcore (group) ironmen
            return !Utils.getAccountType(client).isHardcore();
        }

        if (isBarbarianAssault(regionId) || isChambersOfXeric(regionId) || isInferno(regionId) ||
            isNightmareZone(regionId) || isTzHaarFightCave(regionId) || isPestControl(client) ||
            isGalvekRematch(client, regionId)) {
            // All PvM activities are dangerous for Hardcore group iron players
            return Utils.getAccountType(client) != AccountType.HARDCORE_GROUP_IRONMAN;
        }

        return isCastleWars(regionId) || isClanWars(regionId) || isSoulWars(regionId) ||
            isPlayerOwnedHouse(regionId) || isLastManStanding(client) || isTzHaarFightPit(regionId);
    }

    public boolean isSoulWars(int regionId) {
        return SOUL_REGIONS.contains(regionId);
    }

    public boolean isAmascutTombs(int regionId) {
        return TOA_REGIONS.contains(regionId);
    }

    public boolean isTzHaarFightCave(int regionId) {
        return regionId == TZHAAR_CAVE;
    }

    public boolean isTzHaarFightPit(int regionId) {
        return regionId == TZHAAR_PIT;
    }

}
