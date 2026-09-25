package com.matejpcs.netflared;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.Path;

@Mod("netflared")
public final class NetflaredNeoForge {
    private NetflaredCore core;
    private MinecraftServer server;

    public NetflaredNeoForge() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void commands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
            Commands.literal("netflared")
                .requires(source -> source.hasPermission(4))
                .executes(context -> {
                    run(context.getSource(), new String[0]);
                    return 1;
                })
                .then(Commands.literal("status").executes(context -> {
                    run(context.getSource(), new String[]{"status"});
                    return 1;
                }))
                .then(Commands.literal("info").executes(context -> {
                    run(context.getSource(), new String[]{"info"});
                    return 1;
                }))
                .then(Commands.literal("connect").executes(context -> {
                    run(context.getSource(), new String[]{"connect"});
                    return 1;
                }))
                .then(Commands.literal("disconnect").executes(context -> {
                    run(context.getSource(), new String[]{"disconnect"});
                    return 1;
                }))
                .then(Commands.literal("reset").executes(context -> {
                    run(context.getSource(), new String[]{"reset"});
                    return 1;
                }))
                .then(Commands.literal("setup")
                    .then(Commands.argument("args", StringArgumentType.greedyString())
                        .executes(context -> {
                            run(context.getSource(), new String[]{
                                "setup",
                                StringArgumentType.getString(context, "args")
                            });
                            return 1;
                        })))
        );
    }

    @SubscribeEvent
    public void started(ServerStartedEvent event) {
        server = event.getServer();
        try {
            core = new NetflaredCore(
                Path.of("config", "netflared"),
                () -> new NetflaredCore.ServerInfo(server.getPort(), "neoforge"),
                server::execute
            );
            core.autoConnect();
        } catch (Exception exception) {
            server.sendSystemMessage(Component.literal(
                "Netflared failed to initialize: " + exception.getMessage()
            ));
        }
    }

    @SubscribeEvent
    public void stopping(ServerStoppingEvent event) {
        if (core != null) {
            core.shutdown();
        }
    }

    private void run(CommandSourceStack source, String[] args) {
        if (core == null) {
            source.sendFailure(Component.literal("Netflared is not initialized."));
            return;
        }

        core.handle(new NetflaredCore.Sender() {
            @Override
            public boolean admin() {
                return source.hasPermission(4);
            }

            @Override
            public void send(String message) {
                source.sendSystemMessage(Component.literal(message.replaceAll("§.", "")));
            }
        }, args);
    }
}
