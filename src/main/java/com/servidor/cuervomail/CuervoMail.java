package com.servidor.cuervomail;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
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

    private int cooldownSegundos;
    private int cooldownMasivoSegundos;
    private double bloquesPorSegundo;
    private int tiempoEntreMundos;

    private Sound sonidoEntrega;
    private float sonidoVolumen;
    private float sonidoTono;

    private Material itemIndividual;
    private int cantidadIndividual;

    private Material itemMasivo;

    // Cooldowns por jugador
    private final Map<UUID, Long> cooldownsIndividuales = new HashMap<>();
    private final Map<UUID, Long> cooldownsMasivos = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        cargarConfiguracion();

        // Registrar los comandos
        if (getCommand("cuervo") != null) getCommand("cuervo").setExecutor(this);
        if (getCommand("cuervostaff") != null) getCommand("cuervostaff").setExecutor(this);
        if (getCommand("cuervomasivo") != null) getCommand("cuervomasivo").setExecutor(this);
        if (getCommand("cuervomasivostaff") != null) getCommand("cuervomasivostaff").setExecutor(this);

        getLogger().info("CuervoMail v1.0 activado correctamente.");
    }

    public void cargarConfiguracion() {
        FileConfiguration config = getConfig();

        this.cooldownSegundos = config.getInt("cooldown-segundos", 30);
        this.bloquesPorSegundo = config.getDouble("bloques-por-segundo", 5.0);
        if (this.bloquesPorSegundo <= 0) this.bloquesPorSegundo = 5.0;

        this.tiempoEntreMundos = config.getInt("tiempo-entre-mundos-segundos", 20);

        String sonidoNombre = config.getString("sonido-entrega", "ENTITY_PARROT_AMBIENT");
        try {
            this.sonidoEntrega = Sound.valueOf(sonidoNombre.toUpperCase());
        } catch (Exception e) {
            this.sonidoEntrega = Sound.ENTITY_PARROT_AMBIENT;
        }

        this.sonidoVolumen = (float) config.getDouble("sonido-volumen", 1.0);
        this.sonidoTono = (float) config.getDouble("sonido-tono", 1.0);

        // Individual
        String matInd = config.getString("envio-individual.item-requerido", "PAPER");
        this.itemIndividual = Material.matchMaterial(matInd.toUpperCase());
        if (this.itemIndividual == null) this.itemIndividual = Material.PAPER;
        this.cantidadIndividual = config.getInt("envio-individual.cantidad", 1);

        // Masivo
        this.cooldownMasivoSegundos = config.getInt("envio-masivo.cooldown-segundos", 120);
        String matMas = config.getString("envio-masivo.item-requerido", "PAPER");
        this.itemMasivo = Material.matchMaterial(matMas.toUpperCase());
        if (this.itemMasivo == null) this.itemMasivo = Material.PAPER;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase();

        if (cmd.equals("cuervo")) {
            return procesarCuervoNormal(sender, args);
        } else if (cmd.equals("cuervostaff")) {
            return procesarCuervoStaff(sender, args);
        } else if (cmd.equals("cuervomasivo")) {
            return procesarCuervoMasivo(sender, args);
        } else if (cmd.equals("cuervomasivostaff")) {
            return procesarCuervoMasivoStaff(sender, args);
        }

        return false;
    }

    // 1. /cuervo enviar <jugador> <mensaje>
    private boolean procesarCuervoNormal(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Solo los jugadores pueden ejecutar este comando.");
            return true;
        }

        Player jugador = (Player) sender;

        if (args.length >= 3 && args[0].equalsIgnoreCase("enviar")) {
            UUID uuid = jugador.getUniqueId();
            long tiempoActual = System.currentTimeMillis();

            if (cooldownsIndividuales.containsKey(uuid)) {
                long pasado = (tiempoActual - cooldownsIndividuales.get(uuid)) / 1000;
                if (pasado < this.cooldownSegundos) {
                    jugador.sendMessage(ChatColor.RED + "Debes esperar " + (this.cooldownSegundos - pasado) + " segundos.");
                    return true;
                }
            }

            Player destinatario = Bukkit.getPlayer(args[1]);
            if (destinatario == null || !destinatario.isOnline()) {
                jugador.sendMessage(ChatColor.RED + "El jugador " + args[1] + " no está en línea.");
                return true;
            }

            String mensaje = unirArgumentos(args, 2);

            ItemStack consumo = new ItemStack(this.itemIndividual, this.cantidadIndividual);
            if (!jugador.getInventory().containsAtLeast(consumo, this.cantidadIndividual)) {
                jugador.sendMessage(ChatColor.RED + "Necesitas " + this.cantidadIndividual + " de " + this.itemIndividual.name() + ".");
                return true;
            }

            jugador.getInventory().removeItem(consumo);
            cooldownsIndividuales.put(uuid, tiempoActual);

            int tiempoEspera = calcularTiempoEntrega(jugador.getLocation(), destinatario.getLocation());
            jugador.sendMessage(ChatColor.GREEN + "¡Carta enviada! Llegará en " + tiempoEspera + " segundos.");

            programarEntrega(destinatario, mensaje, tiempoEspera);
            return true;
        }
        return false;
    }

    // 2. /cuervostaff <jugador> <mensaje> (Sin cooldown, sin costo, inmediato)
    private boolean procesarCuervoStaff(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /cuervostaff <jugador> <mensaje>");
            return true;
        }

        Player destinatario = Bukkit.getPlayer(args[0]);
        if (destinatario == null || !destinatario.isOnline()) {
            sender.sendMessage(ChatColor.RED + "El jugador " + args[0] + " no está en línea.");
            return true;
        }

        String mensaje = unirArgumentos(args, 1);
        entregarCartaInmediata(destinatario, "§c[CuervoStaff] §f" + mensaje);
        sender.sendMessage(ChatColor.GREEN + "¡Carta de staff entregada inmediatamente a " + destinatario.getName() + "!");
        return true;
    }

    // 3. /cuervomasivo <mensaje> (Con costo escalonado por cantidad de jugadores y cooldown)
    private boolean procesarCuervoMasivo(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Solo los jugadores pueden usar este comando.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Uso: /cuervomasivo <mensaje>");
            return true;
        }

        Player jugador = (Player) sender;
        UUID uuid = jugador.getUniqueId();
        long tiempoActual = System.currentTimeMillis();

        if (cooldownsMasivos.containsKey(uuid)) {
            long pasado = (tiempoActual - cooldownsMasivos.get(uuid)) / 1000;
            if (pasado < this.cooldownMasivoSegundos) {
                jugador.sendMessage(ChatColor.RED + "Debes esperar " + (this.cooldownMasivoSegundos - pasado) + " segundos para usar el correo masivo.");
                return true;
            }
        }

        int conectados = Bukkit.getOnlinePlayers().size();
        int papelesRequeridos = obtenerPapelesRequeridosMasivo(conectados);

        ItemStack consumo = new ItemStack(this.itemMasivo, papelesRequeridos);
        if (!jugador.getInventory().containsAtLeast(consumo, papelesRequeridos)) {
            jugador.sendMessage(ChatColor.RED + "Con " + conectados + " jugadores conectados necesitas " + papelesRequeridos + " de " + this.itemMasivo.name() + ".");
            return true;
        }

        jugador.getInventory().removeItem(consumo);
        cooldownsMasivos.put(uuid, tiempoActual);

        String mensaje = unirArgumentos(args, 0);
        jugador.sendMessage(ChatColor.GREEN + "¡Envío masivo iniciado! Enviando carta a los " + conectados + " jugadores...");

        for (Player destinatario : Bukkit.getOnlinePlayers()) {
            int tiempoEspera = calcularTiempoEntrega(jugador.getLocation(), destinatario.getLocation());
            programarEntrega(destinatario, "§e[CuervoMasivo] §f" + mensaje, tiempoEspera);
        }

        return true;
    }

    // 4. /cuervomasivostaff <mensaje> (Sin costo, sin cooldown, inmediato a todos)
    private boolean procesarCuervoMasivoStaff(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Uso: /cuervomasivostaff <mensaje>");
            return true;
        }

        String mensaje = unirArgumentos(args, 0);
        for (Player destinatario : Bukkit.getOnlinePlayers()) {
            entregarCartaInmediata(destinatario, "§c[CuervoMasivoStaff] §f" + mensaje);
        }

        sender.sendMessage(ChatColor.GREEN + "¡Mensaje masivo de staff entregado inmediatamente a todos los jugadores!");
        return true;
    }

    // Métodos auxiliares
    private int calcularTiempoEntrega(Location origen, Location destino) {
        if (origen.getWorld() == null || destino.getWorld() == null || !origen.getWorld().equals(destino.getWorld())) {
            return this.tiempoEntreMundos;
        }

        double distancia = origen.distance(destino);
        int tiempo = (int) Math.round(distancia / this.bloquesPorSegundo);
        return Math.max(1, tiempo); // Como mínimo 1 segundo de espera
    }

    private int obtenerPapelesRequeridosMasivo(int conectados) {
        FileConfiguration config = getConfig();
        if (config.contains("envio-masivo.escalas")) {
            for (String clave : config.getConfigurationSection("envio-masivo.escalas").getKeys(false)) {
                int min = config.getInt("envio-masivo.escalas." + clave + ".min-jugadores", 1);
                int max = config.getInt("envio-masivo.escalas." + clave + ".max-jugadores", 999);
                if (conectados >= min && conectados <= max) {
                    return config.getInt("envio-masivo.escalas." + clave + ".cantidad-requerida", 3);
                }
            }
        }
        return 3;
    }

    private void programarEntrega(Player destinatario, String mensaje, int segundosEspera) {
        long delayTicks = segundosEspera * 20L;
        Bukkit.getScheduler().runTaskLater(this, () -> entregarCartaInmediata(destinatario, mensaje), delayTicks);
    }

    private void entregarCartaInmediata(Player destinatario, String mensaje) {
        if (destinatario.isOnline()) {
            destinatario.playSound(destinatario.getLocation(), sonidoEntrega, sonidoVolumen, sonidoTono);
            destinatario.sendMessage("§a[CuervoMail] §fHas recibido una carta: " + mensaje);
        }
    }

    private String unirArgumentos(String[] args, int inicio) {
        StringBuilder builder = new StringBuilder();
        for (int i = inicio; i < args.length; i++) {
            builder.append(args[i]).append(" ");
        }
        return builder.toString().trim();
    }
}
