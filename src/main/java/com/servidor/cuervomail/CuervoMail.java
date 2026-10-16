package com.servidor.cuervomail;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class CuervoMail extends JavaPlugin implements CommandExecutor {

    @Override
    public void onEnable() {
        if (getCommand("cuervo") != null) {
            getCommand("cuervo").setExecutor(this);
        }
        getLogger().info("CuervoMail activado correctamente en Archlight 1.20.1!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando solo lo pueden usar jugadores.");
            return true;
        }

        Player emisor = (Player) sender;

        if (args.length < 3 || !args[0].equalsIgnoreCase("enviar")) {
            emisor.sendMessage(ChatColor.RED + "Uso correcto: /cuervo enviar <jugador> <mensaje>");
            return true;
        }

        Player objetivo = Bukkit.getPlayer(args[1]);
        if (objetivo == null || !objetivo.isOnline()) {
            emisor.sendMessage(ChatColor.RED + "El jugador destino no está conectado.");
            return true;
        }

        StringBuilder mensajeBuilder = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            mensajeBuilder.append(args[i]).append(" ");
        }
        String mensaje = mensajeBuilder.toString().trim();

        double distancia = 0;
        if (emisor.getWorld().equals(objetivo.getWorld())) {
            distancia = emisor.getLocation().distance(objetivo.getLocation());
        } else {
            distancia = 1000;
        }

        long delayTicks = 100L + (long)(distancia / 100.0) * 20L;
        long segundosAproximados = delayTicks / 20L;

        emisor.sendMessage(ChatColor.DARK_GRAY + "[Cuervo] " + ChatColor.GRAY + 
            "Has liberado un cuervo con tu carta. Tardará aprox. " + segundosAproximados + "s en llegar.");
        emisor.playSound(emisor.getLocation(), Sound.ENTITY_PARROT_FLY, 1.0f, 0.5f);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!objetivo.isOnline()) {
                    emisor.sendMessage(ChatColor.RED + "El cuervo no encontró a " + objetivo.getName() + " porque se desconectó.");
                    return;
                }

                Parrot cuervo = (Parrot) objetivo.getWorld().spawnEntity(
                    objetivo.getLocation().add(0, 2, 0), EntityType.PARROT);
                
                cuervo.setVariant(Parrot.Variant.GRAY);
                cuervo.setCustomName(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Cuervo Mensajero");
                cuervo.setCustomNameVisible(true);

                objetivo.playSound(objetivo.getLocation(), Sound.ENTITY_PARROT_AMBIENT, 1.0f, 0.6f);
                objetivo.spawnParticle(Particle.SQUID_INK, objetivo.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.05);

                objetivo.sendMessage("");
                objetivo.sendMessage(ChatColor.DARK_GRAY + "--------------------------------------");
                objetivo.sendMessage(ChatColor.DARK_GRAY + "  Un cuervo ha aterrizado con un mensaje:");
                objetivo.sendMessage(ChatColor.GOLD + "  De: " + ChatColor.WHITE + emisor.getName());
                objetivo.sendMessage(ChatColor.YELLOW + "  \"" + mensaje + "\"");
                objetivo.sendMessage(ChatColor.DARK_GRAY + "--------------------------------------");
                objetivo.sendMessage("");

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (cuervo.isValid()) {
                            cuervo.getWorld().spawnParticle(Particle.SQUID_INK, cuervo.getLocation(), 10, 0.2, 0.2, 0.2, 0.02);
                            cuervo.remove();
                        }
                    }
                }.runTaskLater(CuervoMail.getPlugin(CuervoMail.class), 80L);
            }
        }.runTaskLater(this, delayTicks);

        return true;
    }
}
