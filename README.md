# Dink

Dink sends webhook messages upon noteworthy in-game events.
While Dink supports the Discord webhook format (with rich embeds and optional screenshots), it also includes additional metadata that allows custom webhook servers to analyze messages or even generate their own messages.
This project was forked from UniversalDiscordNotifier, but has more features, reliability, configurability, testing, and maintainer activity.

Have a suggestion (e.g., new notifier, additional data), bug report (as rare as it may be), or question? Let us know on our [issue tracker](https://github.com/pajlads/DinkPlugin/issues)!

## Basic Setup

To use this plugin, a webhook URL is required; you can obtain one from Discord with the following steps:  
<sub>If you already have a link, skip to step 4.</sub>

1. Click the server name (at the top-left of your screen) and select `Server Settings`.
2. Select the `Integrations` tab on the left side and click `Create Webhook` (if other webhooks already exist, click `View Webhooks` and `New Webhook`).
3. Click the newly created webhook, select the target Discord channel, and click `Copy Webhook URL`.
4. Paste the copied link into the `Primary Webhook URLs` box in the Dink plugin settings.
5. (Optional): If you would like different webhook URLs to receive different sets of notifications, you can instead paste the link into each relevant box in the `Webhook Overrides` section. Note: when a notifier has an override URL, the notifier ignores the primary URL box.

## Notifiers

- [Death](#death): Send a webhook message upon dying (with special configuration for PK deaths)
- [Collection](#collection): Send a webhook message upon adding an item to your collection log
- [Level](#level): Send a webhook message upon leveling up a skill (with support for virtual levels)
- [Loot](#loot): Send a webhook message upon receiving valuable loot
- [Slayer](#slayer): Send a webhook message upon completing a slayer task (with a customizable point threshold)
- [Quests](#quests): Send a webhook message upon completing a quest
- [Clue Scrolls](#clue-scrolls): Send a webhook message upon solving a clue scroll (with customizable tier/value thresholds)
- [Kill Count](#kill-count): Send a webhook message upon defeating a boss (with special configuration for personal best times)
- [Combat Achievements](#combat-achievements): Send a webhook message upon completing a combat task (with customizable tier threshold)
- [Achievement Diaries](#achievement-diary): Send a webhook message upon completing an achievement diary (with customizable difficulty threshold)
- [Pet](#pet): Send a webhook message upon receiving a pet
- [Speedrunning](#speedrunning): Send a webhook message upon completing a quest speedrun (with special configuration for personal best times)
- [BA Gambles](#ba-gambles): Sends a webhook message upon receiving high level gambles from Barbarian Assault
- [Player Kills](#player-kills): Sends a webhook message upon killing another player (while hitsplats are still visible)
- [Group Storage](#group-storage): Sends a webhook message upon Group Ironman Shared Bank transactions (i.e., depositing or withdrawing items)
- [Grand Exchange](#grand-exchange): Sends a webhook message upon buying or selling items on the GE (with customizable value threshold)
- [Leagues](#leagues): Sends a webhook message upon completing a Leagues IV task or unlocking a region/relic

## Other Setup

Some notifiers require in-game settings to be configured to send chat messages upon certain events (so these events can serve as triggers for webhook notifications).

- Collection notifier requires `Settings > All Settings > Chat > Collection log - New addition notification` (or `New addition popup`) to be enabled
- Pet notifier recommends `Settings > All Settings > Chat > Untradeable loot notifications` to be enabled (which requires `Settings > All Settings > Chat > Loot drop notifications`) in order to determine the name of the pet
- For Kill Count notifier, ensure you do _not_ enable `Settings > All Settings > Chat > Filter out boss kill-count with spam-filter` (note: this setting is already disabled by default by Jagex)

### Example

![img.png](img.png)

## Advanced Features

- Multiple webhook urls are supported; simply place each on a separate line
- Each notifier can send webhook messages to separate "override" urls
- Screenshots can be individually configured for each notifier
- Screenshots are compressed if needed to comply with Discord limits
- The chat box (and private messages above chat) can be hidden from screenshots
- The plugin can skip notifications for player names that do not comply with the user-configured RSN filter list
- Users can choose whether their webhook messages are sent in Discord's rich embed format or a traditional format
- The player name in Discord rich embeds can be linked to various tracking services (from HiScores to Wise Old Man)
- Discord rich embed footers can be customized with user-specified text and image url
- When network issues occur, Dink can make repeated attempts to send the webhook (with exponential backoff)
- Notifications can be sent to [Discord Forum Channels](https://support.discord.com/hc/en-us/articles/6208479917079-Forum-Channels-FAQ); append `?forum` to the end of the webhook url to create a new thread per message or use `?thread_id=123456` to post to an existing forum thread (be sure to change `123456` with the actual thread ID)
- Character [metadata](#metadata) can be sent to custom webhook handlers on login for tracking relevant statistics.

## Chat Commands

### Export Current Configuration via `::dinkexport`

Dink allows you to export your current plugin configuration to the clipboard via the `::dinkexport` chat command.

You can share this produced JSON to friends who want to send similarly configured messages.

This export includes settings across all of the notifiers, but omits webhook URLs. If you also want to include webhook URLs in the export, you can use the `all` parameter to the command: `::dinkexport all`.

If you _only_ want to export the webhook URLs, run the `::dinkexport webhooks` chat command.

You can export just the settings for select notifiers.  
Simply run: `::dinkexport <notifier section header name without spaces>`.  
For example: `::dinkexport pet` or `::dinkexport collectionlog`.

#### Examples

- Export notifier settings, primary webhook URLs & webhook override URLs  
  `::dinkexport all`
- Export Slayer & BA Gambles Notifier settings  
  `::dinkexport slayer bagambles`
- Export webhook overrides only  
  `::dinkexport webhookoverrides`
- Export all webhooks & the Levels notifier settings:  
  `::dinkexport webhooks levels`

### Import Configuration via `::dinkimport`

With the output of the above command (`::dinkexport`) copied to your clipboard, you can merge these settings with your own via the `::dinkimport` chat command.

This import can replace all of your notifier settings.
However, webhook URL lists, filtered RSNs, and filtered item names for the loot notifier would be combined, rather than outright replaced.
If you would like all settings overwritten rather than merged during import, simply press the `Reset` button at the bottom of the plugin settings panel to clear out all settings (including URLs) before running `::dinkimport`.

After an import, if the dink plugin settings panel was open, simply close and open it for the updated configuration to be reflected in the user interface.

Note: There is no undo button for this command, so consider making a backup of your current Dink configuration by using the `::dinkexport all` command explained above and saving that to a file on your computer.

Warning: If you import override URLs for a notifier (that previously did not have any overrides), this will result in the plugin no longer sending messages from that notifier to your old primary URLs.
As such, you can manually add your primary URLs to the newly populated override URL boxes so that notifications are still sent to the old primary URLs.

---

## Notifier Configuration

Most of the config options are self-explanatory. But the notification messages for each notification type also
contain some words that will be replaced with in-game values.

### All messages:

`%USERNAME%` will be replaced with the username of the player.

<details>
  <summary>JSON Example:</summary>

```json5
{
  "content": "Text message as set by the user",
  "extra": {},
  "type": "NOTIFICATION_TYPE",
  "playerName": "your rsn",
  "embeds": []
}
```

</details>

The examples below omit `embeds` and `playerName` keys because they are always the same.

### Death:

`%VALUELOST%` will be replaced with the price of the items you lost. If you died in PvP, `%PKER%` will be replaced with the name of your killer.

By default, to avoid spam, Dink will ignore deaths from the following [safe](https://oldschool.runescape.wiki/w/Minigames#Safe) activities/areas: Barbarian Assault, Castle Wars, Chambers of Xeric (CoX), Clan Wars, Last Man Standing (LMS), Nightmare Zone (NMZ), Pest Control, player-owned houses (POH), Soul Wars, TzHaar Fight Pit.
However, PvM deaths as a hardcore group ironman are _not_ considered to be safe (and _will_ trigger a notification in these areas).
Lastly, Dink makes exceptions for Inferno and TzHaar Fight Cave; deaths in these areas _do_ trigger notifications (despite technically being safe).

**Note**: If _Distinguish PvP deaths_ is disabled, the message content will be the non-PvP version.

<details>
  <summary>JSON for non-combat death:</summary>

```json5
{
  "content": "%USERNAME% has died...",
  "extra": {
    "valueLost": 300,
    "isPvp": false,
    "keptItems": [],
    "lostItems": [
      {
        "id": 314,
        "quantity": 100,
        "priceEach": 3,
        "name": "Feather"
      }
    ]
  },
  "type": "DEATH"
}
```

</details>

<details>
  <summary>JSON for PvP scenarios:</summary>

```json5
{
  "content": "%USERNAME% has just been PKed by %PKER% for %VALUELOST% gp...",
  "extra": {
    "valueLost": 300,
    "isPvp": true,
    "killerName": "%PKER%",
    "keptItems": [],
    "lostItems": [
      {
        "id": 314,
        "quantity": 100,
        "priceEach": 3,
        "name": "Feather"
      }
    ]
  },
  "type": "DEATH"
}
```

</details>

<details>
  <summary>JSON for NPC scenarios:</summary>

```json5
{
  "content": "%USERNAME% has died...",
  "extra": {
    "valueLost": 300,
    "isPvp": false,
    "killerName": "%NPC%",
    "killerNpcId": 69,
    "keptItems": [],
    "lostItems": [
      {
        "id": 314,
        "quantity": 100,
        "priceEach": 3,
        "name": "Feather"
      }
    ]
  },
  "type": "DEATH"
}
```

</details>

### Collection:

`%ITEM%` will be replaced with the item that was dropped for the collection log.

<details>
  <summary>JSON for Collection Notifications:</summary>

```json5
{
  "content": "%USERNAME% has added %ITEM% to their collection",
  "extra": {
    "itemName": "Zamorak chaps",
    "itemId": 10372,
    "price": 500812,
    "completedEntries": 420,
    "totalEntries": 1443
  },
  "type": "COLLECTION"
}
```

</details>

### Level:

`%SKILL%` will be replaced with the skill name and level that was achieved

`%TOTAL_LEVEL%` will be replaced with the updated total level across all skills.

<details>
  <summary>JSON for Levelups:</summary>

```json5
{
  "content": "%USERNAME% has levelled %SKILL%",
  "extra": {
    "levelledSkills": {
      // These are the skills that dinked
      "Skill name": 30
    },
    "allSkills": {
      // These are all the skills
      "Skill name": 30,
      "Other skill": 1
    },
    "combatLevel": {
      "value": 50,
      "increased": false
    }
  },
  "type": "LEVEL"
}
```

Note: Level 127 in JSON corresponds to attaining max experience in a skill (200M).

</details>

### Loot:

`%LOOT%` will be replaced with a list of the loot and value of said loot

`%TOTAL_VALUE%` will be replaced with the total value of the looted items

`%SOURCE%` will be replace with the source that dropped or gave the loot

<details>
  <summary>JSON for Loot Notifications:</summary>

```json5
{
  "content": "%USERNAME% has looted: \n\n%LOOT%\nFrom: %SOURCE%",
  "extra": {
    "items": [
      {
        // type of this object is SerializedItemStack

        "id": 1234,
        "quantity": 1,
        "priceEach": 42069,
        // priceEach is the GE price of the item
        "name": "Some item"
      }
    ],
    "source": "Giant rat",
    "category": "NPC",
    "killCount": 60
  },
  "type": "LOOT"
}
```

Note: `killCount` is only specified for NPC loot with the base RuneLite Loot Tracker plugin enabled.

</details>

### Slayer:

`%TASK%` will be replaced with the task that you have completed. E.g. `50 monkeys`

`%TASKCOUNT%` will be replaced with the number of tasks that you have completed.

`%POINTS%` will be replaced with the number of points you obtained from the task

<details>
  <summary>JSON for Slayer Notifications:</summary>

```json5
{
  "content": "%USERNAME% has completed a slayer task: %TASK%, getting %POINTS% points and making that %TASKCOUNT% tasks completed",
  "extra": {
    "slayerTask": "Slayer task name",
    "slayerCompleted": "30",
    "slayerPoints": "15",
    "killCount": 135,
    "monster": "Kalphite"
  },
  "type": "SLAYER"
}
```

</details>

### Quests:

`%QUEST%` will be replaced with the name of the quest completed

<details>
  <summary>JSON for Quest Notifications:</summary>

```json5
{
  "content": "%USERNAME% has completed a quest: %QUEST%",
  "extra": {
    "questName": "Dragon Slayer I",
    "completedQuests": 22,
    "totalQuests": 156,
    "questPoints": 44,
    "totalQuestPoints": 293
  },
  "type": "QUEST"
}
```

</details>

### Clue Scrolls:

`%CLUE%` will be replaced with the type of clue (beginner, easy, etc...)

`%LOOT%` will be replaced with the loot that was obtained from the casket

`%TOTAL_VALUE%` will be replaced with the total value of the items from the reward casket

`%COUNT%` will be replaced by the number of times that you have completed that tier of clue scrolls

<details>
  <summary>JSON for Clue Notifications:</summary>

```json5
{
  "content": "%USERNAME% has completed a %CLUE% clue, they have completed %COUNT%.\nThey obtained:\n\n%LOOT%",
  "extra": {
    "clueType": "Beginner",
    "numberCompleted": 123,
    "items": [
      {
        // the type of this object SerializedItemStack

        "id": 1234,
        "quantity": 1,
        "priceEach": 42069,
        // priceEach is the GE price of the item
        "name": "Some item"
      }
    ]
  },
  "type": "CLUE"
}
```

</details>

### Kill Count:

`%BOSS%` will be replaced with the boss name (be it the NPC, raid, etc.)

`%COUNT%` will be replaced with the kill count (or, generically: completion count)

<details>
  <summary>JSON for Kill Count Notifications:</summary>

```json5
{
  "content": "%USERNAME% has defeated %BOSS% with a completion count of %COUNT%",
  "extra": {
    "boss": "King Black Dragon",
    "count": 69,
    "gameMessage": "Your King Black Dragon kill count is: 69."
  },
  "type": "KILL_COUNT"
}
```

Note: when `boss` is `Penance Queen`, `count` refers to the high level gamble count, rather than kill count.

</details>

### Combat Achievements:

`%TIER%` will be replaced with the combat achievement tier (e.g., Easy, Hard, Grandmaster)

`%TASK%` will be replaced with the name of the combat task (e.g., Peach Conjurer)

`%POINTS%` will be replaced with the number of points you earned from the combat achievement.

`%TOTAL_POINTS%` will be replaced with the total points that have been earned across tasks.

If the task completion unlocked rewards for a tier, `%COMPLETED%` will be replaced with the tier that was completed.

<details>
  <summary>JSON for Combat Achievement Notifications:</summary>

```json5
{
  "content": "%USERNAME% has completed %TIER% combat task: %TASK%",
  "extra": {
    "tier": "GRANDMASTER",
    "task": "Peach Conjurer",
    "taskPoints": 6,
    "totalPoints": 1337,
    "tierProgress": 517,
    "tierTotalPoints": 645
  },
  "type": "COMBAT_ACHIEVEMENT"
}
```

</details>

<details>
  <summary>JSON for Combat Achievement Tier Completion Notifications:</summary>

```json5
{
  "content": "%USERNAME% has unlocked the rewards for the %COMPLETED% tier, by completing the combat task: %TASK%",
  "extra": {
    "tier": "GRANDMASTER",
    "task": "Peach Conjurer",
    "taskPoints": 6,
    "totalPoints": 1465,
    "tierProgress": 0,
    "tierTotalPoints": 540,
    "justCompletedTier": "MASTER"
  },
  "type": "COMBAT_ACHIEVEMENT"
}
```

</details>

### Achievement Diary:

`%AREA%` will be replaced with the geographic area of the achievement diary tasks (e.g., Varrock)

`%DIFFICULTY%` will be replaced with the level of the achievement diary (e.g., Hard)

`%TOTAL%` will be replaced with the total number of achievement diaries completed across all locations and difficulties

`%TASKS_COMPLETE%` will be replaced with the number of tasks completed across all locations and difficulties

`%TASKS_TOTAL%` will be replaced with the total number of tasks possible across all locations and difficulties

`%AREA_TASKS_COMPLETE%` will be replaced with the number of tasks completed within the area

`%AREA_TASKS_TOTAL%` will be replaced with the total number of tasks possible within the area

<details>
  <summary>JSON for Achievement Diary Notifications:</summary>

```json5
{
  "content": "%USERNAME% has completed the %DIFFICULTY% %AREA% Achievement Diary, for a total of %TOTAL% diaries completed",
  "extra": {
    "area": "Varrock",
    "difficulty": "HARD",
    "total": 15,
    "tasksCompleted": 152,
    "tasksTotal": 492,
    "areaTasksCompleted": 37,
    "areaTasksTotal": 42
  },
  "type": "ACHIEVEMENT_DIARY"
}
```

</details>

### Pet:

<details>
  <summary>JSON for Pet Notifications:</summary>

```json5
{
  "content": "%USERNAME% has a funny feeling they are being followed",
  "extra": {
    "petName": "Ikkle hydra",
    "milestone": "5,000 killcount",
    "duplicate": false
  },
  "type": "PET"
}
```

Note: `petName` is only included if the game sent it to your chat via untradeable drop or collection log or clan notifications. `milestone` is only included if a clan notification was triggered.

</details>

### Speedrunning:

`%QUEST%` will be replaced with the name of the quest (e.g., Cook's Assistant)

`%TIME%` will be replaced with the time for the latest run

`%BEST%` will be replaced with the personal best time for this quest (note: only if the run was not a PB)

<details>
  <summary>JSON for Personal Best Speedrun Notifications:</summary>

```json5
{
  "content": "%USERNAME% has just beat their personal best in a speedrun of %QUEST% with a time of %TIME%",
  "extra": {
    "questName": "Cook's Assistant",
    "personalBest": "1:13.20",
    "currentTime": "1:13.20",
    "isPersonalBest": true
  },
  "type": "SPEEDRUN"
}
```

</details>

<details>
  <summary>JSON for Normal Speedrun Notifications:</summary>

```json5
{
  "content": "%USERNAME% has just finished a speedrun of %QUEST% with a time of %TIME% (their PB is %BEST%)",
  "extra": {
    "questName": "Cook's Assistant",
    "personalBest": "1:13.20",
    "currentTime": "1:22.20"
  },
  "type": "SPEEDRUN"
}
```

</details>

### BA Gambles:

`%COUNT%` will be replaced with the high level gamble count

`%LOOT%` will be replaced with the loot received from the gamble
(by default, this is included only in rare loot notifications)

<details>
  <summary>JSON for BA Gambles Notifications:</summary>

```json5
{
  "content": "%USERNAME% has reached %COUNT% high gambles",
  "extra": {
    "gambleCount": 500,
    "items": [
      {
        "id": 3122,
        "quantity": 1,
        "priceEach": 35500,
        "name": "Granite shield"
      }
    ]
  },
  "type": "BARBARIAN_ASSAULT_GAMBLE"
}
```

</details>

### Player Kills:

`%TARGET%` will be replaced with the victim's user name

Note: `world` and `location` are _not_ sent if the user has disabled the "Include Location" notifier setting.

<details>
  <summary>JSON for PK Notifications:</summary>

```json5
{
  "content": "%USERNAME% has PK'd %TARGET%",
  "type": "PLAYER_KILL",
  "playerName": "%USERNAME%",
  "accountType": "NORMAL",
  "extra": {
    "victimName": "%TARGET%",
    "victimCombatLevel": 69,
    "victimEquipment": {
      "AMULET": {
        "id": 1731,
        "priceEach": 1987,
        "name": "Amulet of power"
      },
      "WEAPON": {
        "id": 1333,
        "priceEach": 14971,
        "name": "Rune scimitar"
      },
      "TORSO": {
        "id": 1135,
        "priceEach": 4343,
        "name": "Green d'hide body"
      },
      "LEGS": {
        "id": 1099,
        "priceEach": 2077,
        "name": "Green d'hide chaps"
      },
      "HANDS": {
        "id": 1065,
        "priceEach": 1392,
        "name": "Green d'hide vambraces"
      }
    },
    "world": 394,
    "location": {
      "x": 3334,
      "y": 4761,
      "plane": 0
    },
    "myHitpoints": 20,
    "myLastDamage": 12
  }
}
```

</details>

### Group Storage:

`%DEPOSITED%` will be replaced with the list of deposited items

`%WITHDRAWN%` will be replaced with the list of withdrawn items

<details>
  <summary>JSON for GIM Bank Notifications:</summary>

```json5
{
  "content": "%USERNAME% has deposited: %DEPOSITED% | %USERNAME% has withdrawn: %WITHDRAWN%",
  "type": "GROUP_STORAGE",
  "playerName": "%USERNAME%",
  "accountType": "HARDCORE_GROUP_IRONMAN",
  "extra": {
    "groupName": "group name",
    "deposits": [
      {
        "id": 315,
        "name": "Shrimps",
        "quantity": 2,
        "priceEach": 56
      },
      {
        "id": 1205,
        "name": "Bronze dagger",
        "quantity": 1,
        "priceEach": 53
      }
    ],
    "withdrawals": [
      {
        "id": 1265,
        "name": "Bronze pickaxe",
        "quantity": 1,
        "priceEach": 22
      }
    ],
    "netValue": 143
  }
}
```

</details>

### Grand Exchange:

`%TYPE%` will be replaced with the transaction type (i.e., bought or sold)

`%ITEM%` will be replaced with the transacted item

`%STATUS%` will be replaced with the offer status (i.e., Completed, In Progress, or Cancelled)

<details>
  <summary>JSON for GE Notifications:</summary>

```json5
{
  "content": "%USERNAME% %TYPE% %ITEM% on the GE",
  "type": "GRAND_EXCHANGE",
  "playerName": "%USERNAME%",
  "accountType": "NORMAL",
  "extra": {
    "slot": 1,
    "status": "SOLD",
    "item": {
      "id": 314,
      "quantity": 2,
      "priceEach": 3,
      "name": "Feather"
    },
    "marketPrice": 2,
    "targetPrice": 3,
    "targetQuantity": 2,
    "sellerTax": 0
  }
}
```

Unlike `GrandExchangeOfferChanged#getSlot`, `extra.slot` is one-indexed;
values can range from 1 to 8 (inclusive) for members, and 1 to 3 (inclusive) for F2P.

See [javadocs](https://static.runelite.net/api/runelite-api/net/runelite/api/GrandExchangeOfferState.html) for the possible values of `extra.status`.

</details>

### Leagues:

Leagues notifications include: region unlocked, relic unlocked, and task completed (with customizable difficulty threshold).

Each of these events can be independently enabled or disabled in the notifier settings.

<details>
  <summary>JSON for Area Unlock Notifications:</summary>

```json5
{
  "type": "LEAGUES_AREA",
  "content": "%USERNAME% selected their second region: Kandarin.",
  "playerName": "%USERNAME%",
  "accountType": "IRONMAN",
  "seasonalWorld": true,
  "extra": {
    "area": "Kandarin",
    "index": 2,
    "tasksCompleted": 200,
    "tasksUntilNextArea": 200
  }
}
```

Note: `index` refers to the order of region unlocks.
Here, Kandarin was the second region selected.
For all players, Karamja is the _zeroth_ region selected (and there is no notification for Misthalin).

</details>

<details>
  <summary>JSON for Relic Chosen Notifications:</summary>

```json5
{
  "type": "LEAGUES_RELIC",
  "content": "%USERNAME% unlocked a Tier 1 Relic: Production Prodigy.",
  "playerName": "%USERNAME%",
  "accountType": "IRONMAN",
  "seasonalWorld": true,
  "extra": {
    "relic": "Production Prodigy",
    "tier": 1,
    "requiredPoints": 0,
    "totalPoints": 20,
    "pointsUntilNextTier": 480
  }
}
```

</details>

<details>
  <summary>JSON for Task Completed Notifications:</summary>

```json5
{
  "type": "LEAGUES_TASK",
  "content": "%USERNAME% completed a Easy task: Pickpocket a Citizen.",
  "playerName": "%USERNAME%",
  "accountType": "IRONMAN",
  "seasonalWorld": true,
  "extra": {
    "taskName": "Pickpocket a Citizen",
    "difficulty": "EASY",
    "taskPoints": 10,
    "totalPoints": 30,
    "tasksCompleted": 3,
    "pointsUntilNextRelic": 470,
    "pointsUntilNextTrophy": 2470
  }
}
```

</details>

<details>
  <summary>JSON for Task Notifications that unlocked a Trophy:</summary>

```json5
{
  "type": "LEAGUES_TASK",
  "content": "%USERNAME% completed a Hard task, The Frozen Door, unlocking the Bronze trophy!",
  "playerName": "%USERNAME%",
  "accountType": "IRONMAN",
  "seasonalWorld": true,
  "extra": {
    "taskName": "The Frozen Door",
    "difficulty": "HARD",
    "taskPoints": 80,
    "totalPoints": 2520,
    "tasksCompleted": 119,
    "tasksUntilNextArea": 81,
    "pointsUntilNextRelic": 1480,
    "pointsUntilNextTrophy": 2480,
    "earnedTrophy": "Bronze"
  }
}
```

</details>

Note: Fields like `tasksUntilNextArea`, `pointsUntilNextRelic`, and `pointsUntilNextTrophy` can be omitted
if there is no next level of progression (i.e., all three regions selected, all relic tiers unlocked, all trophies acquired).

### Metadata:

On login, Dink can submit a character summary containing data that spans multiple notifiers to a custom webhook handler (configurable in the `Advanced` section). This login notification is delayed by at least 5 seconds in order to gather all of the relevant data. However, `collectionLog` data can be missing if the user does not have the Character Summary tab selected (since the client otherwise is not sent that data).

<details>
  <summary>JSON for Login Notifications:</summary>

```json5
{
  "content": "%USERNAME% logged into World %WORLD%",
  "type": "LOGIN",
  "playerName": "%USERNAME%",
  "accountType": "NORMAL",
  "clanName": "Dink QA",
  "extra": {
    "world": 338,
    "collectionLog": {
      "completed": 651,
      "total": 1477
    },
    "combatAchievementPoints": {
      "completed": 503,
      "total": 2005
    },
    "achievementDiary": {
      "completed": 42,
      "total": 48
    },
    "achievementDiaryTasks": {
      "completed": 477,
      "total": 492
    },
    "barbarianAssault": {
      "highGambleCount": 0
    },
    "skills": {
      "totalExperience": 346380298,
      "totalLevel": 2164,
      "levels": {
        "Hunter": 90,
        "Thieving": 86,
        "Runecraft": 86,
        "Construction": 86,
        "Cooking": 103,
        "Magic": 106,
        "Fletching": 99,
        "Herblore": 91,
        "Firemaking": 100,
        "Attack": 107,
        "Fishing": 92,
        "Crafting": 96,
        "Hitpoints": 111,
        "Ranged": 110,
        "Mining": 88,
        "Smithing": 91,
        "Agility": 82,
        "Woodcutting": 96,
        "Slayer": 104,
        "Defence": 103,
        "Strength": 104,
        "Prayer": 91,
        "Farming": 100
      },
      "experience": {
        "Hunter": 5420696,
        "Thieving": 3696420,
        "Runecraft": 3969420,
        "Construction": 3680085,
        "Cooking": 19696420,
        "Magic": 28008135,
        "Fletching": 13696420,
        "Herblore": 5969420,
        "Firemaking": 14420666,
        "Attack": 30696420,
        "Fishing": 6632248,
        "Crafting": 9696420,
        "Hitpoints": 46969666,
        "Ranged": 42069420,
        "Mining": 4696420,
        "Smithing": 6428696,
        "Agility": 2666420,
        "Woodcutting": 9696666,
        "Slayer": 21420696,
        "Defence": 21212121,
        "Strength": 23601337,
        "Prayer": 6369666,
        "Farming": 15666420
      }
    },
    "questCount": {
      "completed": 156,
      "total": 158
    },
    "questPoints": {
      "completed": 296,
      "total": 300
    },
    "slayer": {
      "points": 2204,
      "streak": 1074
    },
    "pets": [
      {
        "itemId": 11995,
        "name": "Pet chaos elemental"
      },
      {
        "itemId": 13071,
        "name": "Chompy chick"
      }
    ]
  }
}
```

Note: `clanName` requires `Advanced > Send Clan Name` to be enabled (default: on). The `groupIronClanName` and `discordUser` fields also have similar toggles in the Advanced config section.

Note: `extra.pets` requires the base Chat Commands plugin to be enabled.

</details>

## Credits

This plugin uses code from [Universal Discord Notifier](https://github.com/MidgetJake/UniversalDiscordNotifier).
