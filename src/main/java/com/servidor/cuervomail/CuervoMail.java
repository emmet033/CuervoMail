package com.servidor.cuervomail;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class CuervoMail extends JavaPlugin {

    private int tiempoEntrega;
    private EntityType tipoEntidad;
    private Material itemRequerido;
    private int cantidadRequerida;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        cargarConfiguracion();

        if (getCommand("cuervo") != null) {
            getCommand("cuervo").setExecutor(new CuervoComando(this));
        }

        getLogger().info("CuervoMail activado correctamente.");
    }

    public void cargarConfiguracion() {
        FileConfiguration config = getConfig();

        this.tiempoEntrega = config.getInt("tiempo-entrega-segundos", 10);

        String entidadNombre = config.getString("entidad-mensajera", "PARROT");
        try {
            this.tipoEntidad = EntityType.valueOf(entidadNombre.toUpperCase());
        } catch (IllegalArgumentException e) {
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

    public void enviarCarta(Player destinatario, String mensaje) {
        Location locDestino = destinatario.getLocation();
        long delayTicks = this.tiempoEntrega * 20L;

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (destinatario.isOnline()) {
                Entity mensajero = locDestino.getWorld().spawnEntity(locDestino.add(0, 1, 0), this.tipoEntidad);
                mensajero.setCustomName("§6Mensajero Real");
                mensajero.setCustomNameVisible(true);

                destinatario.sendMessage("§a[CuervoMail] §fHas recibido una carta: §e" + mensaje);

                Bukkit.getScheduler().runTaskLater(this, () -> {
                    if (mensajero.isValid()) {
                        mensajero.remove();
                    }
                }, 60L);
            }
        }, delayTicks);
    }

    public int getTiempoEntrega() { return tiempoEntrega; }
    public EntityType getTipoEntidad() { return tipoEntidad; }
    public Material getItemRequerido() { return itemRequerido; }
    public int getCantidadRequerida() { return cantidadRequerida; }
}
