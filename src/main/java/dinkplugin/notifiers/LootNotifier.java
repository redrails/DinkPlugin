package dinkplugin.notifiers;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import dinkplugin.message.Embed;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.message.templating.impl.JoiningReplacement;
import dinkplugin.notifiers.data.LootNotificationData;
import dinkplugin.notifiers.data.SerializedItemStack;
import dinkplugin.util.ConfigUtil;
import dinkplugin.util.ItemUtils;
import dinkplugin.util.SerializedLoot;
import dinkplugin.util.Utils;
import dinkplugin.util.WorldUtils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.plugins.loottracker.LootTrackerConfig;
import net.runelite.client.plugins.loottracker.LootTrackerPlugin;
import net.runelite.client.util.QuantityFormatter;
import net.runelite.http.api.loottracker.LootRecordType;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class LootNotifier extends BaseNotifier {

    private static final String RL_LOOT_PLUGIN_NAME = LootTrackerPlugin.class.getSimpleName().toLowerCase();

    @Inject
    private Gson gson;

    @Inject
    private ItemManager itemManager;

    @Inject
    private ConfigManager configManager;

    @Inject
    private ScheduledExecutorService executor;

    private final Collection<Pattern> itemNameAllowlist = new CopyOnWriteArrayList<>();
    private final Collection<Pattern> itemNameDenylist = new CopyOnWriteArrayList<>();

    private final Cache<String, Integer> killCounts = CacheBuilder.newBuilder()
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .build();

    @Override
    public boolean isEnabled() {
        return config.notifyLoot() && super.isEnabled();
    }

    @Override
    protected String getWebhookUrl() {
        return config.lootWebhook();
    }

    public void reset() {
        killCounts.invalidateAll();
    }

    public void init() {
        itemNameAllowlist.clear();
        itemNameAllowlist.addAll(
            ConfigUtil.readDelimited(config.lootItemAllowlist())
                .map(Utils::regexify)
                .collect(Collectors.toList())
        );

        itemNameDenylist.clear();
        itemNameDenylist.addAll(
            ConfigUtil.readDelimited(config.lootItemDenylist())
                .map(Utils::regexify)
                .collect(Collectors.toList())
        );
    }

    public void onConfigChanged(String key, String value) {
        Collection<Pattern> itemNames;
        if ("lootItemAllowlist".equals(key)) {
            itemNames = itemNameAllowlist;
        } else if ("lootItemDenylist".equals(key)) {
            itemNames = itemNameDenylist;
        } else {
            return;
        }

        itemNames.clear();
        itemNames.addAll(
            ConfigUtil.readDelimited(value)
                .map(Utils::regexify)
                .collect(Collectors.toList())
        );
    }

    public void onGameMessage(String message) {
        // update cached KC via boss chat message with robustness for chat event coming before OR after the loot event
        KillCountNotifier.parseBoss(message).ifPresent(pair -> {
            String boss = pair.getKey();
            Integer kc = pair.getValue();

            // Update cache if no mapping exists (i.e., this is the first boss kill & chat event occurred first)
            // We store kc - 1 since incrementAndGetKillCount will increment; kc - 1 + 1 == kc
            Integer existingMapping = killCounts.asMap().putIfAbsent(boss, kc - 1);
            if (existingMapping != null) {
                // KC already cached, but we don't know if this boss message appeared before/after the loot event.
                // If after, we should store kc. If before, we should store kc - 1.
                // Given this uncertainty, we wait so that the loot event has passed, and then we can store latest kc.
                executor.schedule(() -> {
                    killCounts.asMap().merge(boss, kc, Math::max);
                }, 15, TimeUnit.SECONDS);
            }
        });
    }

    public void onNpcLootReceived(NpcLootReceived event) {
        if (!isEnabled()) {
            // increment kill count
            killCounts.asMap().computeIfPresent(event.getNpc().getName(), (k, v) -> v + 1);
            return;
        }

        NPC npc = event.getNpc();
        int id = npc.getId();
        if (id == NpcID.THE_WHISPERER || id == NpcID.THE_WHISPERER_12205 || id == NpcID.THE_WHISPERER_12206 || id == NpcID.THE_WHISPERER_12207) {
            // Upstream does not fire NpcLootReceived for the whisperer, since they do not hold a reference to the NPC.
            // So, we use LootReceived instead (and return here just in case they change their implementation).
            return;
        }

        this.handleNotify(event.getItems(), npc.getName(), LootRecordType.NPC);
    }

    public void onPlayerLootReceived(PlayerLootReceived event) {
        if (WorldUtils.isSafeArea(client))
            return;

        if (config.includePlayerLoot() && isEnabled())
            this.handleNotify(event.getItems(), event.getPlayer().getName(), LootRecordType.PLAYER);
    }

    public void onLootReceived(LootReceived lootReceived) {
        if (!isEnabled()) return;

        // only consider non-NPC and non-PK loot
        if (lootReceived.getType() == LootRecordType.EVENT || lootReceived.getType() == LootRecordType.PICKPOCKET) {
            if (!config.lootIncludeClueScrolls() && StringUtils.startsWithIgnoreCase(lootReceived.getName(), "Clue Scroll")) {
                // skip clue scroll loot, depending on config
                return;
            }

            this.handleNotify(lootReceived.getItems(), lootReceived.getName(), lootReceived.getType());
        } else if (lootReceived.getType() == LootRecordType.NPC && "The Whisperer".equalsIgnoreCase(lootReceived.getName())) {
            // Special case: upstream fires LootReceived for the whisperer, but not NpcLootReceived
            this.handleNotify(lootReceived.getItems(), lootReceived.getName(), lootReceived.getType());
        }
    }

    private void handleNotify(Collection<ItemStack> items, String dropper, LootRecordType type) {
        final Integer kc = type == LootRecordType.NPC ? incrementAndGetKillCount(dropper) : null;
        final int minValue = config.minLootValue();
        final boolean icons = config.lootIcons();

        Collection<ItemStack> reduced = ItemUtils.reduceItemStack(items);
        List<SerializedItemStack> serializedItems = new ArrayList<>(reduced.size());
        List<Embed> embeds = new ArrayList<>(icons ? reduced.size() : 0);

        JoiningReplacement.JoiningReplacementBuilder lootMessage = JoiningReplacement.builder().delimiter("\n");
        long totalStackValue = 0;
        boolean sendMessage = false;
        SerializedItemStack max = null;

        for (ItemStack item : reduced) {
            SerializedItemStack stack = ItemUtils.stackFromItem(itemManager, item.getId(), item.getQuantity());
            long totalPrice = stack.getTotalPrice();
            boolean worthy = totalPrice >= minValue || matches(itemNameAllowlist, stack.getName());
            if (worthy && !matches(itemNameDenylist, stack.getName())) {
                sendMessage = true;
                lootMessage.component(ItemUtils.templateStack(stack, true));
                if (icons) embeds.add(Embed.ofImage(ItemUtils.getItemImageUrl(item.getId())));
                if (max == null || totalPrice > max.getTotalPrice()) {
                    max = stack;
                }
            }
            serializedItems.add(stack);
            totalStackValue += totalPrice;
        }

        if (sendMessage) {
            boolean screenshot = config.lootSendImage() && totalStackValue >= config.lootImageMinValue();
            Template notifyMessage = Template.builder()
                .template(config.lootNotifyMessage())
                .replacementBoundary("%")
                .replacement("%USERNAME%", Replacements.ofText(Utils.getPlayerName(client)))
                .replacement("%LOOT%", lootMessage.build())
                .replacement("%TOTAL_VALUE%", Replacements.ofText(QuantityFormatter.quantityToStackSize(totalStackValue)))
                .replacement("%SOURCE%", Replacements.ofText(dropper))
                .build();
            createMessage(screenshot,
                NotificationBody.builder()
                    .text(notifyMessage)
                    .embeds(embeds)
                    .extra(new LootNotificationData(serializedItems, dropper, type, kc))
                    .type(NotificationType.LOOT)
                    .thumbnailUrl(ItemUtils.getItemImageUrl(max.getId()))
                    .build()
            );
        }
    }

    private Integer incrementAndGetKillCount(String npcName) {
        Integer stored = getStoredKillCount(npcName);
        if (stored == null) {
            killCounts.asMap().computeIfPresent(npcName, (k, v) -> v + 1);
            return null;
        }
        return killCounts.asMap().compute(npcName, (npc, cached) -> {
            if (cached == null || cached < stored) {
                return stored + 1;
            }
            return cached + 1;
        });
    }

    /**
     * @param npcName {@link NPC#getName()}
     * @return the kill count stored by the base runelite loot tracker plugin
     */
    private Integer getStoredKillCount(String npcName) {
        if ("false".equals(configManager.getConfiguration(RuneLiteConfig.GROUP_NAME, RL_LOOT_PLUGIN_NAME))) {
            // assume stored kc is useless if loot tracker plugin is disabled
            return null;
        }
        String json = configManager.getConfiguration(LootTrackerConfig.GROUP,
            configManager.getRSProfileKey(),
            "drops_NPC_" + npcName
        );
        if (json == null) {
            // no kc stored implies first kill
            return 0;
        }
        try {
            return gson.fromJson(json, SerializedLoot.class).getKills();
        } catch (JsonSyntaxException e) {
            // should not occur unless loot tracker changes stored loot pojo structure
            log.warn("Failed to read kills from loot tracker config", e);
            return null;
        }
    }

    private static boolean matches(Collection<Pattern> regexps, String input) {
        for (Pattern regex : regexps) {
            if (regex.matcher(input).find())
                return true;
        }
        return false;
    }

}
