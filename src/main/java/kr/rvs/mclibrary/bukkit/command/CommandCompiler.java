package kr.rvs.mclibrary.bukkit.command;

import kr.rvs.mclibrary.bukkit.command.internal.CommandArguments;
import kr.rvs.mclibrary.bukkit.command.internal.CompositeCommand;
import kr.rvs.mclibrary.bukkit.command.internal.ExecutableCommand;
import kr.rvs.mclibrary.bukkit.command.internal.ICommand;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Created by Junhyeong Lim on 2017-09-19.
 */
public class CommandCompiler {
    private static final Pattern ARGS_PATTERN = Pattern.compile(" ", Pattern.LITERAL);
    private CompositeCommand command = new CompositeCommand();

    public void addCommand(SubCommand subCommand) {
        CommandArguments args = new CommandArguments(Arrays.asList(ARGS_PATTERN.split(subCommand.args())));
        int lastIndex = args.size() - 1;
        String lastArg = lastIndex >= 0 && args.get(0) != null ? args.remove(lastIndex--) : "";
        CompositeCommand ctx = this.command;

        for (int i = 0; i <= lastIndex; i++) {
            String arg = args.get(i);
            ctx = ctx.computeIfAbsent(arg, k -> new CompositeCommand());
        }

        ctx.put(lastArg, new ExecutableCommand(subCommand));
    }

    public ICommand getCommand() {
        return command;
    }
}
