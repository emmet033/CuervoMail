package com.servidor.cuervomail;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CuervoMail extends JavaPlugin implements CommandExecutor {

    private int tiempoEntrega;
    private int cooldownSegundos;
    private Sound sonidoEntrega;
    private float sonidoVolumen;
    private float sonidoTono;
    private Material itemRequerido;
    private int cantidadRequerida;

    // Guardar los tiempos del cooldown por jugador
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        cargarConfiguracion();

        if (getCommand("cuervo") != null) {
            getCommand("cuervo").setExecutor(this);
        }

        getLogger().info("CuervoMail activado correctamente.");
    }

    public void cargarConfiguracion() {
        FileConfiguration config = getConfig();

        this.tiempoEntrega = config.getInt("tiempo-entrega-segundos", 10);
        this.cooldownSegundos = config.getInt("cooldown-segundos", 30);

        String sonidoNombre = config.getString("sonido-entrega", "ENTITY_PARROT_AMBIENT");
        try {
            this.sonidoEntrega = Sound.valueOf(sonidoNombre.toUpperCase());
        } catch (Exception e) {
            getLogger().warning("El sonido '" + sonidoNombre + "' no es válido. Usando ENTITY_PARROT_AMBIENT por defecto.");
            this.sonidoEntrega = Sound.ENTITY_PARROT_AMBIENT;
        }

        this.sonidoVolumen = (float) config.getDouble("sonido-volumen", 1.0);
        this.sonidoTono = (float) config.getDouble("sonido-tono", 1.0);

        String materialNombre = config.getString("item-requerido.material", "PAPER");
        this.itemRequerido = Material.matchMaterial(materialNombre.toUpperCase());
        if (this.itemRequerido == null) {
            this.itemRequerido = Material.PAPER;
        }

        this.cantidadRequerida = config.getInt("item-requerido.cantidad", 1);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Solo los jugadores pueden ejecutar este comando.");
            return true;
        }

        Player jugador = (Player) sender;

        if (args.length >= 3 && args[0].equalsIgnoreCase("enviar")) {

            // Verificación del Cooldown
            UUID uuid = jugador.getUniqueId();
            long tiempoActual = System.currentTimeMillis();
            if (cooldowns.containsKey(uuid)) {
                long tiempoPasado = (tiempoActual - cooldowns.get(uuid)) / 1000;
                if (tiempoPasado < this.cooldownSegundos) {
                    long restante = this.cooldownSegundos - tiempoPasado;
                    jugador.sendMessage(ChatColor.RED + "Debes esperar " + restante + " segundos antes de enviar otra carta.");
                    return true;
                }
            }

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

            ItemStack consumo = new ItemStack(this.itemRequerido, this.cantidadRequerida);
            if (!jugador.getInventory().containsAtLeast(consumo, this.cantidadRequerida)) {
                jugador.sendMessage(ChatColor.RED + "Necesitas " + this.cantidadRequerida + " de " 
                        + this.itemRequerido.name() + " para enviar una carta.");
                return true;
            }

            // Consumir el ítem y registrar tiempo de cooldown
            jugador.getInventory().removeItem(consumo);
            cooldowns.put(uuid, tiempoActual);

            jugador.sendMessage(ChatColor.GREEN + "¡Carta enviada! El mensajero llegará en " + this.tiempoEntrega + " segundos.");

            enviarCarta(destinatario, mensaje);
            return true;
        }

        return false;
    }

    public void enviarCarta(Player destinatario, String mensaje) {
        long delayTicks = (long) this.tiempoEntrega * 20L;

        Bukkit.getScheduler().runTaskLater(this, new Runnable() {
            @Override
            public void run() {
                if (destinatario.isOnline()) {
                    // Reproducir el sonido configurado directamente al jugador destinatario
                    destinatario.playSound(destinatario.getLocation(), sonidoEntrega, sonidoVolumen, sonidoTono);

                    // Entregar el mensaje en el chat
                    destinatario.sendMessage("§a[CuervoMail] §fHas recibido una carta: §e" + mensaje);
                }
            }
        }, delayTicks);
    }
}
