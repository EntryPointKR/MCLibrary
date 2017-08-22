package kr.rvs.mclibrary.util;

import kr.rvs.mclibrary.mock.MockItemFactory;
import kr.rvs.mclibrary.mock.MockItemMeta;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.plugin.SimplePluginManager;
import org.mockito.Mockito;

import java.util.logging.Logger;

/**
 * Created by Junhyeong Lim on 2017-07-26.
 */
public class MockFactory extends Mockito {
    public static Server createMockServer() {
        Server server = mock(Server.class);
        when(server.getLogger()).thenReturn(Logger.getGlobal());
        when(server.getPluginManager()).thenReturn(
                new SimplePluginManager(server, new SimpleCommandMap(server)));
        when(server.getItemFactory()).thenReturn(new MockItemFactory());

        return server;
    }

    public static CommandSender createCommandSender() { // TODO
        CommandSender sender = mock(
                CommandSender.class,
                withSettings().extraInterfaces(Player.class)
        );
        doAnswer(invocation -> {
            String fixed = StringUtils.join((String[]) invocation.getArguments()[0], '\n');
            System.out.println(ChatColor.stripColor(fixed));
            return null;
        }).when(sender).sendMessage((String[]) any());
        return sender;
    }

    public static ItemFactory createItemFactory() {
        ItemFactory factory = mock(ItemFactory.class);
        when(factory.getItemMeta(any())).thenReturn(new MockItemMeta());

        return factory;
    }
}
