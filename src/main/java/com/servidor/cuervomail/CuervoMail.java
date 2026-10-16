package com.servidor.cuervomail;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class CuervoMail extends JavaPlugin {

    private int tiempoEntrega;
    private Material itemRequerido;
    private int cantidadRequerida;

    @Override
    public onEnable() {
        // Guarda el config.yml predeterminado si no existe la carpeta/archivo
        saveDefaultConfig();
        
        // Carga los valores desde la configuración
        cargarConfiguracion();

        getLogger().info("CuervoMail ha sido activado correctamente.");
    }

    public void cargarConfiguracion() {
        FileConfiguration config = getConfig();

        // Obtener el tiempo de entrega en segundos (predeterminado 10)
        this.tiempoEntrega = config.getInt("tiempo-entrega-segundos", 10);

        // Obtener el item requerido (predeterminado PAPER)
        String materialNombre = config.getString("item-requerido.material", "PAPER");
        this.itemRequerido = Material.matchMaterial(materialNombre.toUpperCase());
        if (this.itemRequerido == null) {
            this.itemRequerido = Material.PAPER; // Respaldo por si escriben mal el nombre del item
        }

        // Obtener la cantidad (predeterminado 1)
        this.cantidadRequerida = config.getInt("item-requerido.cantidad", 1);
    }

    // Métodos Getter para consultar los valores desde tus comandos/eventos
    public int getTiempoEntrega() {
        return tiempoEntrega;
    }

    public Material getItemRequerido() {
        return itemRequerido;
    }

    public int getCantidadRequerida() {
        return cantidadRequerida;
    }
}
