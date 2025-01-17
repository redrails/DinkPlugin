package dinkplugin.notifiers;

import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.notifiers.data.CollectionNotificationData;
import dinkplugin.util.ItemSearcher;
import net.runelite.api.GameState;
import net.runelite.api.ItemID;
import net.runelite.api.ScriptID;
import net.runelite.api.VarClientStr;
import net.runelite.api.Varbits;
import net.runelite.api.events.VarbitChanged;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.function.BiFunction;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CollectionNotifierTest extends MockedNotifierTest {

    private static final int TOTAL_ENTRIES = 1443;

    @Bind
    @InjectMocks
    CollectionNotifier notifier;

    @Bind
    @Mock
    ItemSearcher itemSearcher;

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();

        // init client mocks
        when(client.getVarbitValue(Varbits.COLLECTION_LOG_NOTIFICATION)).thenReturn(1);
        when(client.getVarpValue(CollectionNotifier.COMPLETED_VARP)).thenReturn(0);
        when(client.getVarpValue(CollectionNotifier.TOTAL_VARP)).thenReturn(TOTAL_ENTRIES);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        notifier.onTick();

        VarbitChanged initCompleted = new VarbitChanged();
        initCompleted.setVarpId(CollectionNotifier.COMPLETED_VARP);
        initCompleted.setValue(0);
        notifier.onVarPlayer(initCompleted);

        // init config mocks
        when(config.notifyCollectionLog()).thenReturn(true);
        when(config.collectionSendImage()).thenReturn(false);
        when(config.collectionNotifyMessage()).thenReturn("%USERNAME% has added %ITEM% to their collection");
    }

    @Test
    void testNotify() {
        String item = "Seercull";
        int price = 23_000;
        when(itemSearcher.findItemId(item)).thenReturn(ItemID.SEERCULL);
        when(itemManager.getItemPrice(ItemID.SEERCULL)).thenReturn(price);

        // send fake message
        notifier.onChatMessage("New item added to your collection log: " + item);

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(String.format("%s has added {{item}} to their collection", PLAYER_NAME))
                        .replacement("{{item}}", Replacements.ofWiki(item))
                        .build()
                )
                .extra(new CollectionNotificationData(item, ItemID.SEERCULL, (long) price, 1, TOTAL_ENTRIES))
                .type(NotificationType.COLLECTION)
                .build()
        );
    }

    @Test
    void testNotifyPopup() {
        // prepare item
        String item = "Seercull";
        int price = 23_000;
        when(itemSearcher.findItemId(item)).thenReturn(ItemID.SEERCULL);
        when(itemManager.getItemPrice(ItemID.SEERCULL)).thenReturn(price);

        // update mocks
        when(client.getVarbitValue(Varbits.COLLECTION_LOG_NOTIFICATION)).thenReturn(3);
        when(client.getVarcStrValue(VarClientStr.NOTIFICATION_TOP_TEXT)).thenReturn("Collection log");
        when(client.getVarcStrValue(VarClientStr.NOTIFICATION_BOTTOM_TEXT)).thenReturn("New item:<br>" + item);

        // send chat event (to be ignored)
        notifier.onChatMessage("New item added to your collection log: " + item);

        // ensure no notification yet
        Mockito.verifyNoInteractions(messageHandler);

        // send script events
        notifier.onScript(ScriptID.NOTIFICATION_START);
        notifier.onScript(ScriptID.NOTIFICATION_DELAY);

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(String.format("%s has added {{item}} to their collection", PLAYER_NAME))
                        .replacement("{{item}}", Replacements.ofWiki(item))
                        .build()
                )
                .extra(new CollectionNotificationData(item, ItemID.SEERCULL, (long) price, 1, TOTAL_ENTRIES))
                .type(NotificationType.COLLECTION)
                .build()
        );
    }

    @Test
    void testNotifyFresh() {
        notifier.reset();

        /*
         * first notification: no varbit data
         */
        when(client.getVarpValue(CollectionNotifier.COMPLETED_VARP)).thenReturn(0);
        when(client.getVarpValue(CollectionNotifier.TOTAL_VARP)).thenReturn(0);

        String item = "Seercull";
        int price = 23_000;
        when(itemSearcher.findItemId(item)).thenReturn(ItemID.SEERCULL);
        when(itemManager.getItemPrice(ItemID.SEERCULL)).thenReturn(price);

        // send fake message
        notifier.onChatMessage("New item added to your collection log: " + item);

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(String.format("%s has added {{item}} to their collection", PLAYER_NAME))
                        .replacement("{{item}}", Replacements.ofWiki(item))
                        .build()
                )
                .extra(new CollectionNotificationData(item, ItemID.SEERCULL, (long) price, null, null))
                .type(NotificationType.COLLECTION)
                .build()
        );

        /*
         * jagex sends varbit data shortly after the notification
         */
        BiFunction<Integer, Integer, VarbitChanged> varpEvent = (id, value) -> {
            VarbitChanged e = new VarbitChanged();
            e.setVarpId(id);
            e.setValue(value);
            return e;
        };

        when(client.getVarpValue(CollectionNotifier.COMPLETED_VARP)).thenReturn(1);
        notifier.onVarPlayer(varpEvent.apply(CollectionNotifier.COMPLETED_VARP, 1));

        when(client.getVarpValue(CollectionNotifier.TOTAL_VARP)).thenReturn(TOTAL_ENTRIES);
        notifier.onVarPlayer(varpEvent.apply(CollectionNotifier.TOTAL_VARP, TOTAL_ENTRIES));

        when(client.getVarpValue(CollectionNotifier.COMPLETED_VARP)).thenReturn(100);
        notifier.onVarPlayer(varpEvent.apply(CollectionNotifier.COMPLETED_VARP, 100));

        notifier.onTick();

        /*
         * a later notification occurs
         */
        String item2 = "Seers ring";
        int price2 = 420_000;
        when(itemSearcher.findItemId(item2)).thenReturn(ItemID.SEERS_RING);
        when(itemManager.getItemPrice(ItemID.SEERS_RING)).thenReturn(price2);

        // send fake message
        notifier.onChatMessage("New item added to your collection log: " + item2);

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(String.format("%s has added {{item}} to their collection", PLAYER_NAME))
                        .replacement("{{item}}", Replacements.ofWiki(item2))
                        .build()
                )
                .extra(new CollectionNotificationData(item2, ItemID.SEERS_RING, (long) price2, 101, TOTAL_ENTRIES))
                .type(NotificationType.COLLECTION)
                .build()
        );
    }

    @Test
    void testIgnore() {
        // send unrelated message
        notifier.onChatMessage("New item added to your backpack: weed");

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testDisabled() {
        // disable notifier
        when(config.notifyCollectionLog()).thenReturn(false);

        // send fake message
        String item = "Seercull";
        notifier.onChatMessage("New item added to your collection log: " + item);

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

}
