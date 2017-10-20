package kr.rvs.mclibrary.bukkit.command;

import kr.rvs.mclibrary.MCLibrary;
import kr.rvs.mclibrary.Static;
import kr.rvs.mclibrary.bukkit.command.annotation.Command;
import kr.rvs.mclibrary.bukkit.command.annotation.SubCommand;
import kr.rvs.mclibrary.bukkit.command.annotation.TabCompleter;
import kr.rvs.mclibrary.bukkit.command.completor.ReflectiveCompleter;
import kr.rvs.mclibrary.bukkit.command.duplex.ComplexCommand;
import kr.rvs.mclibrary.bukkit.command.duplex.CompositionCommand;
import kr.rvs.mclibrary.bukkit.command.executor.AnnotationProxyExecutor;
import kr.rvs.mclibrary.bukkit.command.executor.ReflectiveExecutor;
import kr.rvs.mclibrary.bukkit.plugin.PluginUtils;
import kr.rvs.mclibrary.reflection.ClassProbe;
import kr.rvs.mclibrary.reflection.ConstructorEx;
import kr.rvs.mclibrary.reflection.FieldEx;
import kr.rvs.mclibrary.reflection.Reflections;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Created by Junhyeong Lim on 2017-09-29.
 */
public class CommandManager { // TODO: Cleaning
    public static final CommandFactory DEF_FACTORY = (commandClass, adaptor) -> {
        ConstructorEx constructorEx = Reflections.getConstructorEx(commandClass, CommandAdaptor.class);
        return constructorEx.newInstance(adaptor).orElseGet(() -> {
            Constructor constructor = commandClass.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            try {
                return constructor.newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException(e);
            }
        });
    };
    private final CommandMap commandMap;

    public CommandManager(CommandMap commandMap) {
        this.commandMap = commandMap;
    }

    public CommandManager() {
        PluginManager manager = Bukkit.getPluginManager();
        try {
            Field mapField = SimplePluginManager.class.getDeclaredField("commandMap");
            mapField.setAccessible(true);
            commandMap = (CommandMap) mapField.get(manager);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    public void registerAll(MCLibrary superPlugin) {
        for (Plugin plugin : PluginUtils.getDependPlugins(superPlugin)) {
            FieldEx fileField = Reflections.getFieldEx(JavaPlugin.class, "file");
            fileField.<File>get(plugin).ifPresent(file -> {
                ClassProbe probe = new ClassProbe(file);
                Set<Class<?>> commandClasses = probe.getTypesAnnotatedWith(Command.class);
                Set<Class<?>> subCommandClasses = probe.getTypesAnnotatedWith(SubCommand.class);

                for (Class<?> subCommandClass : subCommandClasses) {
                    SubCommand subCommand = subCommandClass.getAnnotation(SubCommand.class);
                    commandClasses.removeAll(Arrays.asList(subCommand.value()));
                }

                for (Class<?> annotatedClass : commandClasses) {
                    if (annotatedClass.isMemberClass())
                        continue;
                    registerCommand(annotatedClass, plugin);
                }
            });
        }
    }

    private Optional<Command> getCommandAnnotation(Class<?> commandClass) {
        Optional<Command> optional = Reflections.getAnnotation(commandClass, Command.class);
        if (optional.isPresent()) {
            return optional;
        } else {
            Static.log(commandClass.getSimpleName() + " is not annotation presented");
            return Optional.empty();
        }
    }

    public void registerCommand(Class<?> commandClass, CommandFactory factory, Plugin plugin) {
        getCommandAnnotation(commandClass).ifPresent(commandAnnot -> {
            ComplexCommand complexCommand = new ComplexCommand();
            String[] args = commandAnnot.args().split(" ");
            String firstArg = args[0];
            CommandAdaptor adaptor = new CommandAdaptor(
                    firstArg,
                    commandAnnot.desc(),
                    commandAnnot.usage(),
                    Arrays.asList(args),
                    complexCommand,
                    plugin
            );
            commandMap.register(firstArg, plugin.getName(), adaptor);

            Static.log("&eCommand \"" + firstArg + "\" register from " + plugin.getName());
            ComplexCommand newComplexCommand = complexCommand.setupComposite(args, 1, args.length);
            Object instance = factory.create(commandClass, adaptor);
            registerCommandFromMethod(commandClass, instance, newComplexCommand);
            registerCommandFromSubClass(commandClass, adaptor, factory, complexCommand);
        });
    }

    public void registerCommand(Class<?> commandClass, Plugin plugin) {
        registerCommand(commandClass, DEF_FACTORY, plugin);
    }

    public void registerCommandFromMethod(Class<?> commandClass, Object instance,
                                          ComplexCommand complexCommand) {
        for (Method method : commandClass.getDeclaredMethods()) {
            Command commandAnnot = method.getAnnotation(Command.class);
            TabCompleter completerAnnot = method.getAnnotation(TabCompleter.class);
            if (commandAnnot == null && completerAnnot == null)
                continue;

            String args = commandAnnot != null ?
                    commandAnnot.args() :
                    completerAnnot.args();
            String[] splited = args.split(" ");
            String lastArg = splited[splited.length - 1];
            ComplexCommand newComplexCommand = complexCommand.setupComposite(splited, 0, splited.length - 1);
            ICommand command = newComplexCommand.computeIfAbsent(lastArg, k -> new CompositionCommand());

            if (command instanceof ComplexCommand) {
                ICommand absoluteCommand = ((ComplexCommand) command).getAbsoluteCommand();
                command = absoluteCommand != null ? absoluteCommand : command;
            }
            if (!(command instanceof CompositionCommand)) {
                Static.log("CompositionCommand expected, but " + command.getClass().getSimpleName());
                newComplexCommand.put(lastArg, command = new CompositionCommand());
            }

            CompositionCommand compositionCommand = (CompositionCommand) command;

            if (commandAnnot != null) {
                ReflectiveExecutor reflectiveExecutor = new ReflectiveExecutor(instance, method);
                AnnotationProxyExecutor proxyExecutor = new AnnotationProxyExecutor(commandAnnot, reflectiveExecutor);
                compositionCommand.setExecutable(proxyExecutor);
                compositionCommand.setCommandInfo(proxyExecutor);
            }
            if (completerAnnot != null) {
                compositionCommand.setCompletable(new ReflectiveCompleter(instance, method));
            }
            Static.log("... " + Arrays.toString(splited));
        }
    }

    public void registerCommandFromSubClass(Class<?> commandClass, CommandAdaptor adaptor, CommandFactory factory, ComplexCommand complexCommand) {
        Set<Class<?>> subClasses = new HashSet<>(Arrays.asList(commandClass.getDeclaredClasses()));
        Reflections.getAnnotation(commandClass, SubCommand.class).ifPresent(subCommandAnnot ->
                subClasses.addAll(Arrays.asList(subCommandAnnot.value())));
        for (Class<?> subClass : subClasses) {
            registerCommandFromClass(subClass, adaptor, factory, complexCommand);
        }
    }

    public void registerCommandFromClass(Class<?> commandClass, CommandAdaptor adaptor, CommandFactory factory, ComplexCommand complexCommand) {
        getCommandAnnotation(commandClass).ifPresent(commandAnnot -> {
            Object instance = factory.create(commandClass, adaptor);
            String[] args = commandAnnot.args().split(" ");
            ComplexCommand newComplexCommand = complexCommand.setupComposite(args, 0, args.length);
            registerCommandFromMethod(commandClass, instance, newComplexCommand);
            registerCommandFromSubClass(commandClass, adaptor, factory, complexCommand);
        });
    }

    interface CommandFactory {
        Object create(Class<?> commandClass, CommandAdaptor adaptor);
    }
}
