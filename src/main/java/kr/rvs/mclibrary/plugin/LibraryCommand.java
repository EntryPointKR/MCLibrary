package kr.rvs.mclibrary.plugin;

import kr.rvs.mclibrary.MCLibrary;
import kr.rvs.mclibrary.bukkit.MCUtils;
import kr.rvs.mclibrary.bukkit.collection.PlayerHashSet;
import kr.rvs.mclibrary.bukkit.command.CommandArguments;
import kr.rvs.mclibrary.bukkit.command.CommandType;
import kr.rvs.mclibrary.bukkit.command.annotation.Command;
import kr.rvs.mclibrary.bukkit.inventory.newgui.ListData;
import kr.rvs.mclibrary.bukkit.item.ItemBuilder;
import kr.rvs.mclibrary.bukkit.player.CommandSenderWrapper;
import kr.rvs.mclibrary.bukkit.player.Players;
import kr.rvs.mclibrary.gson.GsonUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.io.File;

/**
 * Created by Junhyeong Lim on 2017-09-20.
 */
@Command(args = "mclibrary")
public class LibraryCommand {
    private static final PlayerHashSet<Player> INFO_LISTENERS = new PlayerHashSet<>();
    private final MCLibrary instance = (MCLibrary) MCLibrary.getPlugin();

    public static void init(MCLibrary plugin) {
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            @SuppressWarnings("deprecation")
            public void onInteract(PlayerInteractEvent event) {
                if (MCUtils.isOffHandSupport() && event.getHand() != EquipmentSlot.HAND)
                    return;

                Player player = event.getPlayer();
                Block block = event.getClickedBlock();
                if (block == null || !INFO_LISTENERS.contains(player))
                    return;

                Location loc = block.getLocation();
                CommandSenderWrapper wrapper = new CommandSenderWrapper(player);
                wrapper.sendMessage("---------------------------------------------");
                wrapper.sendMessage("&eworld: &f" + loc.getWorld().getName());
                wrapper.sendMessage(String.format("&ex: &f%s (%s)", loc.getBlockX(), loc.getX()));
                wrapper.sendMessage(String.format("&ey: &f%s (%s)", loc.getBlockY(), loc.getY()));
                wrapper.sendMessage(String.format("&ez: &f%s (%s)", loc.getBlockZ(), loc.getZ()));
                wrapper.sendMessage(String.format("&eblock: &f%s:%s (%s)", block.getTypeId(), block.getData(), block.getType().name()));
            }
        }, plugin);
    }

    @Command(
            args = "reload",
            perm = "mclibrary.reload",
            desc = "설정파일을 리로드합니다."
    )
    public void reloadCommand(CommandSenderWrapper wrapper, CommandArguments args) {
        FileConfiguration config = instance.getConfig();
        instance.reloadConfig();
        instance.configInit();
        for (String key : config.getKeys(true)) {
            wrapper.sendMessage(key + ": " + config.get(key));
        }
    }

    @Command(
            args = "killall",
            perm = "mclibrary.killall",
            usage = "[월드]",
            desc = "모든 몬스터를 제거합니다."
    )
    public void killallCommand(CommandSenderWrapper wrapper, CommandArguments args) {
        World world = args.isEmpty() ? wrapper.getWorldOrThrow() : args.getWorldOrThrow(0);
        world.getEntities().stream()
                .filter(entity -> entity instanceof Creature)
                .forEach(Entity::remove);
    }

    @Command(
            type = CommandType.PLAYER,
            args = "gui",
            perm = "mclibrary.gui",
            desc = "테스트 gui 를 엽니다."
    )
    public void guiCommand(CommandSenderWrapper wrapper, CommandArguments args) {
//        new SimpleGUI(new GUIData(InventoryType.CHEST)
//                .title("MCLibrary GUI")
//                .item(13, new ItemStack(Material.CHEST)))
//                .handler(handlers -> handlers.add(
//                        CancelHandler.TOP,
//                        new ClickHandler().handler(13, event ->
//                                GUI.sendMessage(event, "&aHello, MCLibrary"))
//                ))
//                .open(wrapper.getPlayerOrThrow());

        ListData data = ListData.ofRow(5);
        for (int i = 0; i < 45; i++) {
            data.addItem(new ItemBuilder(Material.DIRT).display(String.valueOf(i)).build());
        }
        new kr.rvs.mclibrary.bukkit.inventory.newgui.PageGUI(data)
                .handler(handlers -> handlers.add(event -> System.out.println("Invoked")))
                .open(wrapper.getPlayerOrThrow());
    }

    @Command(
            type = CommandType.PLAYER,
            args = "receive",
            perm = "mclibrary.receive",
            desc = "클릭한 위치의 블럭 정보를 출력합니다."
    )
    public void blockInfo(CommandSenderWrapper wrapper, CommandArguments args) {
        Player player = wrapper.getPlayerOrThrow();
        String message;
        if (!INFO_LISTENERS.contains(player)) {
            INFO_LISTENERS.add(player);
            message = "&aOn";
        } else {
            INFO_LISTENERS.remove(player);
            message = "&cOff";
        }
        wrapper.sendMessage(message);
    }

    @Command(
            args = "heal",
            perm = "mclibrary.heal",
            usage = "[플레이어]",
            desc = "체력과 허기를 회복합니다."
    )
    @SuppressWarnings("deprecation")
    public void heal(CommandSenderWrapper wrapper, CommandArguments args) {
        Player player = args.isEmpty() ? wrapper.getPlayerOrThrow() : args.getPlayerOrThrow(0);
        Players.setHealth(player, Players.getMaxHealth(player));
        player.setFoodLevel(30);
        wrapper.sendMessage("완료.");
    }

    @Command(
            type = CommandType.PLAYER,
            args = "serialize",
            usage = "[파일이름]",
            desc = "손에 든 아이템을 plugins/MCLibrary/items 폴더에 파일 형태로 직렬화합니다.",
            perm = "mclibrary.serialize"
    )
    @SuppressWarnings("deprecation")
    public void serialize(CommandSenderWrapper sender, CommandArguments args) {
        ItemStack item = sender.getItemInHandOrThrow("손에 아이템을 들어주세요.");
        String fileName = "items/" + args.get(0, "serialize") + ".json";
        File file = new File(MCLibrary.getPlugin().getDataFolder(), fileName);
        GsonUtils.write(file, item, ex -> sender.sendMessage("에러가 발생했습니다." + ex.toString()));
        sender.sendMessage("다음 파일에 저장됩니다. " + file.getPath());
    }
}
