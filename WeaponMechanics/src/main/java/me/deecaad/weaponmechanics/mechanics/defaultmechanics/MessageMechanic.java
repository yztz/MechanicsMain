package me.deecaad.weaponmechanics.mechanics.defaultmechanics;

import me.deecaad.core.compatibility.CompatibilityAPI;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerEnumException;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.file.SerializerVersionException;
import me.deecaad.core.placeholder.PlaceholderAPI;
import me.deecaad.core.utils.EnumUtil;
import me.deecaad.core.utils.StringUtil;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.mechanics.CastData;
import me.deecaad.weaponmechanics.mechanics.IMechanic;
import me.deecaad.weaponmechanics.mechanics.Mechanics;
import me.deecaad.weaponmechanics.wrappers.MessageHelper;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Content;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class MessageMechanic implements IMechanic<MessageMechanic> {

    private boolean sendGlobally;
    private boolean sendGloballyForWorld;
    private ChatData chatData;
    private String actionBar;
    private int actionBarTime;
    private TitleData titleData;
    private BossBarData bossBarData;

    /**
     * Empty constructor to be used as serializer
     */
    public MessageMechanic() {
        if (Mechanics.hasMechanic(getKeyword())) return;
        Mechanics.registerMechanic(WeaponMechanics.getPlugin(), this);
    }

    public MessageMechanic(boolean sendGlobally, boolean sendGloballyForWorld, ChatData chatData, String actionBar, int actionBarTime, TitleData titleData, BossBarData bossBarData) {
        this.sendGlobally = sendGlobally;
        this.sendGloballyForWorld = sendGloballyForWorld;
        this.chatData = chatData;
        this.actionBar = actionBar;
        this.actionBarTime = actionBarTime;
        this.titleData = titleData;
        this.bossBarData = bossBarData;
    }

    @Override
    public void use(CastData castData) {
        if (sendGlobally) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                send(castData, player);
            }
            return;
        } else if (sendGloballyForWorld) {
            for (Player player : castData.getCastLocation().getWorld().getPlayers()) {
                send(castData, player);
            }
            return;
        }
        send(castData, (Player) castData.getCaster());
    }

    private void send(CastData castData, Player player) {
        MessageHelper messageHelper = WeaponMechanics.getPlayerWrapper(player).getMessageHelper();

        // If this would be "boolean" it would throw null pointer
        Boolean fetchWeaponInfoValue = castData.getData(CommonDataTags.WEAPON_INFO.name(), Boolean.class);
        boolean isWeaponInfoCast = fetchWeaponInfoValue != null && fetchWeaponInfoValue;

        Map<String, String> tempPlaceholders = null;
        String shooterName = castData.getData(CommonDataTags.SHOOTER_NAME.name(), String.class);
        String victimName = castData.getData(CommonDataTags.VICTIM_NAME.name(), String.class);
        if (shooterName != null || victimName != null) {
            tempPlaceholders = new HashMap<>();
            tempPlaceholders.put("%shooter%", shooterName != null ? shooterName : player.getName());
            tempPlaceholders.put("%victim%", victimName);
        }

        if (chatData != null) {
            String chatMessage = PlaceholderAPI.applyPlaceholders(chatData.message, player, castData.getWeaponStack(), castData.getWeaponTitle(), tempPlaceholders);
            TextComponent chatMessageComponent = new TextComponent(chatMessage);
            if (chatData.clickEventAction != null) {
                chatMessageComponent.setClickEvent(new ClickEvent(chatData.clickEventAction,
                        PlaceholderAPI.applyPlaceholders(chatData.clickEventValue, player, castData.getWeaponStack(), castData.getWeaponTitle(), tempPlaceholders)));
            }
            if (chatData.hoverEventValue != null) {
                Content content = new Text(TextComponent.fromLegacyText(
                        PlaceholderAPI.applyPlaceholders(chatData.hoverEventValue, player, castData.getWeaponStack(), castData.getWeaponTitle(), tempPlaceholders)));
                chatMessageComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, content));
            }
            player.spigot().sendMessage(ChatMessageType.CHAT, chatMessageComponent);
        }

        if (actionBar != null && (!isWeaponInfoCast || !messageHelper.denyInfoActionBar())) {

            String actionBarMessage = PlaceholderAPI.applyPlaceholders(actionBar, player, castData.getWeaponStack(), castData.getWeaponTitle(), tempPlaceholders);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(actionBarMessage));

            if (!isWeaponInfoCast) {
                if (actionBarTime > 40) {

                    messageHelper.updateActionBarTime(actionBarTime);

                    Map<String, String> finalTempPlaceholders = tempPlaceholders;
                    new BukkitRunnable() {
                        int ticker = 0;

                        @Override
                        public void run() {

                            String actionBarMessage = PlaceholderAPI.applyPlaceholders(actionBar, player, castData.getWeaponStack(), castData.getWeaponTitle(), finalTempPlaceholders);
                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(actionBarMessage));

                            ticker += 40;
                            if (ticker >= actionBarTime) {
                                cancel();
                            }
                        }
                    }.runTaskTimerAsynchronously(WeaponMechanics.getPlugin(), 40, 40);
                } else {
                    messageHelper.updateActionBarTime(40);
                }
            }
        }

        if (titleData != null && (!isWeaponInfoCast || !messageHelper.denyInfoTitle())) {
            String titleMessage = PlaceholderAPI.applyPlaceholders(titleData.title, player, castData.getWeaponStack(), castData.getWeaponTitle(), tempPlaceholders);
            String subtitleMessage = PlaceholderAPI.applyPlaceholders(titleData.subtitle, player, castData.getWeaponStack(), castData.getWeaponTitle(), tempPlaceholders);
            if (CompatibilityAPI.getVersion() < 1.11) {

                // By default it fade in, stay and fade out takes 60 ticks
                if (!isWeaponInfoCast) messageHelper.updateTitleTime(60);

                player.sendTitle(titleMessage, subtitleMessage);
            } else {

                if (!isWeaponInfoCast) messageHelper.updateTitleTime(titleData.fadeIn + titleData.stay + titleData.fadeOut);

                player.sendTitle(titleMessage, subtitleMessage, titleData.fadeIn, titleData.stay, titleData.fadeOut);
            }
        }

        if (bossBarData != null) {
            String bossBarMessage = PlaceholderAPI.applyPlaceholders(bossBarData.title, player, castData.getWeaponStack(), castData.getWeaponTitle(), tempPlaceholders);

            BarColor color = (BarColor) bossBarData.barColor;
            BarStyle style = (BarStyle) bossBarData.barStyle;

            if (isWeaponInfoCast) {

                // This is here to only use ONE boss bar for weapon info displaying
                BossBar bossBar = messageHelper.getCurrentInfoBossBar();
                if (bossBar == null) {
                    // Not found, create new one
                    bossBar = Bukkit.createBossBar(bossBarMessage, color, style);
                    bossBar.addPlayer(player);
                    messageHelper.setCurrentInfoBossBar(bossBar);
                } else {
                    // Found, cancel last one's cancel task and set new title and other things
                    Bukkit.getScheduler().cancelTask(messageHelper.getCurrentInfoBossBarTask());
                    bossBar.setTitle(bossBarMessage);
                    bossBar.setColor(color);
                    bossBar.setStyle(style);
                }
                // Update the cancel task id
                messageHelper.setCurrentInfoBossBarTask(new BukkitRunnable() {
                    @Override
                    public void run() {
                        // Remove all players, remove boss bar from MessageHandler and remove task id
                        // If this code is reached, new boss bar will be made next time
                        messageHelper.getCurrentInfoBossBar().removeAll();
                        messageHelper.setCurrentInfoBossBar(null);
                        messageHelper.setCurrentInfoBossBarTask(0);
                    }
                }.runTaskLaterAsynchronously(WeaponMechanics.getPlugin(), bossBarData.time).getTaskId());
            } else {
                BossBar bossBar = Bukkit.createBossBar(bossBarMessage, color, style);

                bossBar.addPlayer(player);
                new BukkitRunnable() {
                    public void run() {
                        bossBar.removeAll();
                    }
                }.runTaskLaterAsynchronously(WeaponMechanics.getPlugin(), bossBarData.time);
            }
        }
    }

    public boolean hasBossBar() {
        return bossBarData != null;
    }

    @Override
    public boolean requirePlayer() {
        // Make it require player IF global send isn't enabled
        // Since then this message is always directly for player
        return !sendGlobally && !sendGloballyForWorld;
    }

    @Override
    public String getKeyword() {
        return "Message";
    }

    @Override
    @Nonnull
    public MessageMechanic serialize(SerializeData data) throws SerializerException {

        // CHAT
        ChatData chatData = null;
        String chatMessage = data.of("Chat.Message").assertType(String.class).get(null);
        if (chatMessage != null) {

            ClickEvent.Action clickEventAction = null;
            String clickEventValue = null;
            String hoverEventValue = data.of("Chat.Hover_Text").assertType(String.class).get(null);
            if (hoverEventValue != null) hoverEventValue = StringUtil.color(hoverEventValue);

            String clickEventString = data.of("Chat.Click").assertType(String.class).get(null);
            if (clickEventString != null) {
                String[] split = clickEventString.split("-", 2);
                if (split.length != 2) {
                    throw data.exception(null, "Chat Click Action should be formatted like: <ClickEvent.Action>-<value>",
                            SerializerVersionException.forValue(clickEventString));
                }

                clickEventAction = EnumUtil.getIfPresent(ClickEvent.Action.class, split[0])
                        .orElseThrow(() -> new SerializerEnumException(this, ClickEvent.Action.class, split[0], false, data.of("Chat.Click").getLocation()));
                clickEventValue = StringUtil.color(split[1]);
            }
            chatData = new ChatData(StringUtil.color(chatMessage), clickEventAction, clickEventValue, hoverEventValue);
        }

        // ACTION BAR
        String actionBarMessage = data.of("Action_Bar.Message").assertType(String.class).get(null);
        if (actionBarMessage != null) actionBarMessage = StringUtil.color(actionBarMessage);
        int actionBarTime = data.of("Action_Bar.Time").assertRange(40, Integer.MAX_VALUE).getInt(0);

        // TITLE
        TitleData titleData = null;
        String titleMessage = data.of("Title.Title").assertType(String.class).get(null);
        String subtitleMessage = data.of("Title.Subtitle").assertType(String.class).get(null);
        if (titleMessage != null || subtitleMessage != null) {

            int fadeIn = data.of("Title.Fade_In").assertPositive().getInt(0);
            int stay = data.of("Title.Stay").assertPositive().getInt(40);
            int fadeOut = data.of("Title.Fade_Out").assertPositive().getInt(0);

            titleData = new TitleData(StringUtil.color(titleMessage), StringUtil.color(subtitleMessage), fadeIn, stay, fadeOut);
        }

        // BOSS BAR
        BossBarData bossBarData = null;
        String bossBarMessage = data.of("Boss_Bar.Title").assertType(String.class).get(null);
        if (bossBarMessage != null) {
            BarColor barColor = data.of("Boss_Bar.Bar_Color").getEnum(BarColor.class, BarColor.RED);
            BarStyle barStyle = data.of("Boss_Bar.Bar_Style").getEnum(BarStyle.class, BarStyle.SOLID);
            int time = data.of("Boss_Bar.Time").assertExists().assertPositive().getInt();
            bossBarData = new BossBarData(StringUtil.color(bossBarMessage), barColor, barStyle, time);
        }

        if (chatData == null && actionBarMessage == null && titleData == null && bossBarData == null) {
            throw data.exception(null, "Tried to use a Message Mechanic without any messages added!",
                    "If you do not want to send any messages, please remove the key from your config.");
        }

        boolean sendGlobally = data.of("Send_Globally").getBool(false);
        boolean sendGloballyForWorld = data.of("Send_Globally_For_World").getBool(false);

        return new MessageMechanic(sendGlobally, sendGloballyForWorld, chatData, actionBarMessage, actionBarTime, titleData, bossBarData);
    }

    private static class ChatData {

        public String message;
        public ClickEvent.Action clickEventAction;
        public String clickEventValue;
        // No need for HoverEvent.Action since only SHOW_TEXT is usable
        public String hoverEventValue;

        public ChatData(String message, ClickEvent.Action clickEventAction, String clickEventValue, String hoverEventValue) {
            this.message = message;
            this.clickEventAction = clickEventAction;
            this.clickEventValue = clickEventValue;
            this.hoverEventValue = hoverEventValue;
        }
    }

    private static class TitleData {

        public String title;
        public String subtitle;
        public int fadeIn;
        public int stay;
        public int fadeOut;

        public TitleData(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
            this.title = title;
            this.subtitle = subtitle;
            if (this.subtitle != null && this.title == null) {
                this.title = "";
            }
            this.fadeIn = fadeIn == 0 ? 10 : fadeIn;
            this.stay = stay == 0 ? 20 : stay;
            this.fadeOut = fadeOut == 0 ? 10 : fadeOut;
        }
    }

    private static class BossBarData {

        public String title;
        public Object barColor;
        public Object barStyle;
        public int time;

        public BossBarData(String title, Object barColor, Object barStyle, int time) {
            this.title = title;
            this.barColor = barColor == null ? BarColor.WHITE : barColor;
            this.barStyle = barStyle == null ? BarStyle.SOLID : barStyle;
            this.time = time == 0 ? 20 : time;
        }
    }
}