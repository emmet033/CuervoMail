package com.servidor.cuervomail;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class CuervoMail extends JavaPlugin implements CommandExecutor {

    private int tiempoEntrega;
    private EntityType tipoEntidad;
    private Material itemRequerido;
    private int cantidadRequerida;

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

        String entidadNombre = config.getString("entidad-mensajera", "PARROT");
        try {
            this.tipoEntidad = EntityType.valueOf(entidadNombre.toUpperCase());
        } catch (Exception e) {
            getLogger().warning("La entidad '" + entidadNombre + "' no es válida. Usando PARROT por defecto.");
            this.tipoEntidad = EntityType.PARROT;
        }

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

            jugador.getInventory().removeItem(consumo);
            jugador.sendMessage(ChatColor.GREEN + "¡Carta enviada! El mensajero llegará en " + this.tiempoEntrega + " segundos.");

            enviarCarta(destinatario, mensaje);
            return true;
        }

        return false;
    }

    public void enviarCarta(Player destinatario, String mensaje) {
        Location locDestino = destinatario.getLocation().clone();
        locDestino.add(0, 1, 0);
        
        long delayTicks = (long) this.tiempoEntrega * 20L;

        Bukkit.getScheduler().runTaskLater(this, new Runnable() {
            @Override
            public void run() {
                if (destinatario.isOnline()) {
                    Entity mensajero = locDestino.getWorld().spawnEntity(locDestino, tipoEntidad);
                    mensajero.setCustomName("§6Mensajero Real");
                    mensajero.setCustomNameVisible(true);

                    // Si es una entidad viva, quitamos la persistencia para que se pueda borrar sin problemas
                    if (mensajero instanceof LivingEntity) {
                        ((LivingEntity) mensajero).setRemoveWhenFarAway(true);
                    }

                    destinatario.sendMessage("§a[CuervoMail] §fHas recibido una carta: §e" + mensaje);

                    // Espera 3 segundos (60 ticks) y elimina la entidad
                    Bukkit.getScheduler().runTaskLater(CuervoMail.this, new Runnable() {
                        @Override
                        public void run() {
                            if (mensajero != null) {
                                mensajero.remove(); // Elimina la entidad del mundo
                            }
                        }
                    }, 60L);
                }
            }
        }, delayTicks);
    }

    public Material getItemRequerido() { return itemRequerido; }
    public int getCantidadRequerida() { return cantidadRequerida; }
}
