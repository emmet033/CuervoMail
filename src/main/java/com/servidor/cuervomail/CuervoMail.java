package com.servidor.cuervomail;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CuervoComando implements CommandExecutor {

    private final CuervoMail plugin;

    public CuervoComando(CuervoMail plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Solo los jugadores pueden ejecutar este comando.");
            return true;
        }

        Player jugador = (Player) sender;

        if (args.length >= 3 && args[0].equalsIgnoreCase("enviar")) {
            String nombreDestinatario = args[1];
            Player destinatario = Bukkit.getPlayer(nombreDestinatario);

            if (destinatario == null || !destinatario.isOnline()) {
                jugador.sendMessage(ChatColor.RED + "El jugador " + nombreDestinatario + " no está en línea.");
                return true;
            }

            StringBuilder mensajeBuilder = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                mensajeBuilder.append(args[i]).append(" ");
            }
            String mensaje = mensajeBuilder.toString().trim();

            ItemStack itemRequerido = new ItemStack(plugin.getItemRequerido(), plugin.getCantidadRequerida());
            if (!jugador.getInventory().containsAtLeast(itemRequerido, plugin.getCantidadRequerida())) {
                jugador.sendMessage(ChatColor.RED + "Necesitas " + plugin.getCantidadRequerida() + " de " 
                        + plugin.getItemRequerido().name() + " para enviar una carta.");
                return true;
            }

            jugador.getInventory().removeItem(itemRequerido);
            jugador.sendMessage(ChatColor.GREEN + "¡Carta enviada! El mensajero llegará en " + plugin.getTiempoEntrega() + " segundos.");

            plugin.enviarCarta(destinatario, mensaje);
            return true;
        }

        return false;
    }
}
