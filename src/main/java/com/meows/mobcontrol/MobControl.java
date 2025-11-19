package com.meows.mobcontrol;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Random;

public class MobControl extends JavaPlugin implements Listener {

    private File configFile;
    private FileConfiguration config;
    private final Random random = new Random();

    @Override
    public void onEnable() {
        // Сообщение о успешной загрузке плагина (зеленый цвет)
        getLogger().info("\u001B[32mПлагин успешно загружен!\u001B[0m");

        // Сохранение и загрузка конфигурации
        saveDefaultConfig();
        configFile = new File(getDataFolder(), "config.yml");
        config = YamlConfiguration.loadConfiguration(configFile);

        // Регистрация событий и команды
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("mobcontrol").setExecutor((sender, command, label, args) -> {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                reloadConfigFile();
                sender.sendMessage("§aConfig reloaded!");
            } else {
                sender.sendMessage("§cUsage: /mobcontrol reload");
            }
            return true;
        });
    }

    @Override
    public void onDisable() {
        // Сообщение о выключении плагина (красный цвет)
        getLogger().info("\u001B[31mПлагин успешно выключен!\u001B[0m");
    }

    private void reloadConfigFile() {
        reloadConfig();
        config = YamlConfiguration.loadConfiguration(configFile);
        getLogger().info("\u001B[33mКонфигурация перезагружена.\u001B[0m");
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        EntityType type = event.getEntityType();

        switch (type) {
            // Зомби
            case ZOMBIE -> applyZombieAttributes(event, "zombie.normal");
            case DROWNED -> applyZombieAttributes(event, "zombie.drowned");
            case ZOMBIE_VILLAGER -> applyZombieAttributes(event, "zombie.villager");
            case HUSK -> applyZombieAttributes(event, "zombie.husk");
            case ZOMBIFIED_PIGLIN -> applyZombieAttributes(event, "zombie.zombie_pigman");

            // Скелеты
            case SKELETON -> applyAttributes(event, "skeleton.normal");
            case STRAY -> applyAttributes(event, "skeleton.stray");
            case WITHER_SKELETON -> applyAttributes(event, "skeleton.wither");
            case BOGGED -> applyBoggedAttributes(event);

            // Пауки
            case SPIDER -> applyAttributes(event, "spider");
            case CAVE_SPIDER -> applyAttributes(event, "spider.cave");

            // Крипер
            case CREEPER -> handleCreeperSpawn(event);

            // Новые мобы 1.21
            case BREEZE -> applyBreezeAttributes(event);
            case ARMADILLO -> applyHealthOnly(event, "armadillo");

            // Новые мобы 1.19
            case WARDEN -> applyWardenAttributes(event);
            case ALLAY -> applyHealthOnly(event, "allay");
            case FROG -> applyHealthOnly(event, "frog");
            case TADPOLE -> applyHealthOnly(event, "tadpole");

            // Новые мобы 1.20
            case CAMEL -> applyHealthOnly(event, "camel");
            case SNIFFER -> applyHealthOnly(event, "sniffer");

            // Другие популярные мобы
            case PHANTOM -> applyAttributes(event, "phantom");
            case PILLAGER -> applyPillagerAttributes(event);
            case VINDICATOR -> applyAttributes(event, "vindicator");
            case EVOKER -> applyAttributes(event, "evoker");
            case VEX -> applyAttributes(event, "vex");
            case RAVAGER -> applyAttributes(event, "ravager");
            case PIGLIN -> applyPiglinAttributes(event);
            case PIGLIN_BRUTE -> applyAttributes(event, "piglin_brute");
            case HOGLIN -> applyAttributes(event, "hoglin");
            case ZOGLIN -> applyAttributes(event, "zoglin");
            case ENDERMAN -> applyAttributes(event, "enderman");
            case ENDERMITE -> applyAttributes(event, "endermite");
            case SILVERFISH -> applyAttributes(event, "silverfish");
            case WITCH -> applyHealthOnly(event, "witch");
            case GUARDIAN -> applyAttributes(event, "guardian");
            case ELDER_GUARDIAN -> applyAttributes(event, "elder_guardian");
            case SHULKER -> applyAttributes(event, "shulker");
            case GHAST -> applyGhastAttributes(event);
            case BLAZE -> applyBlazeAttributes(event);
            case SLIME -> applySlimeAttributes(event);
            case MAGMA_CUBE -> applyMagmaCubeAttributes(event);
            case STRIDER -> applyHealthOnly(event, "strider");

            // Боссы
            case ENDER_DRAGON -> applyEnderDragonAttributes(event);
            case WITHER -> applyWitherAttributes(event);

            default -> {
                // Новый моб из 1.21.4 - проверка по имени типа (для совместимости с разными
                // версиями API)
                if (type.name().equals("SKREEPER")) {
                    applyAttributes(event, "skreeper");
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Обработка стрел от различных мобов
        if (event.getDamager() instanceof Arrow arrow && arrow.getShooter() instanceof Entity) {
            Entity shooter = (Entity) arrow.getShooter();

            // Проверяем Bogged отдельно, так как он не наследуется от Skeleton
            if (shooter instanceof org.bukkit.entity.Bogged) {
                double arrowDamage = config.getDouble("skeleton.bogged.arrow_damage", 2.0);
                event.setDamage(arrowDamage);
                return;
            }

            // Проверяем различные типы скелетов
            if (shooter instanceof Skeleton skeleton) {
                if (skeleton instanceof org.bukkit.entity.WitherSkeleton) {
                    // Wither skeleton (обычно не стреляет, но если стреляет)
                    double arrowDamage = config.getDouble("skeleton.wither.arrow_damage", 8.0);
                    event.setDamage(arrowDamage);
                } else if (skeleton instanceof org.bukkit.entity.Stray) {
                    // Stray стреляет стрелами с эффектом замедления
                    double arrowDamage = config.getDouble("skeleton.stray.arrow_damage", 4.0);
                    event.setDamage(arrowDamage);
                } else {
                    // Обычный скелет
                    double arrowDamage = config.getDouble("skeleton.normal.arrow_damage", 5.0);
                    event.setDamage(arrowDamage);
                }
                return;
            }

            // Обработка арбалетов (Pillager, Piglin)
            if (shooter instanceof org.bukkit.entity.Pillager) {
                double crossbowDamage = config.getDouble("pillager.crossbow_damage", 5.0);
                event.setDamage(crossbowDamage);
                return;
            }

            if (shooter instanceof org.bukkit.entity.Piglin) {
                double crossbowDamage = config.getDouble("piglin.crossbow_damage", 5.0);
                event.setDamage(crossbowDamage);
                return;
            }
        }

        // Настройка урона BreezeWindCharge
        if (event.getDamager() instanceof BreezeWindCharge breezeWindCharge) {
            double breezeWindChargeDamage = config.getDouble("breeze.wind_charge_damage", 6.0);
            event.setDamage(breezeWindChargeDamage);
        }

        // Настройка урона от файрболов (Ghast, Blaze)
        if (event.getDamager() instanceof org.bukkit.entity.Fireball fireball) {
            org.bukkit.projectiles.ProjectileSource shooter = fireball.getShooter();
            if (shooter instanceof org.bukkit.entity.Ghast) {
                double fireballDamage = config.getDouble("ghast.fireball_damage", 6.0);
                event.setDamage(fireballDamage);
            } else if (shooter instanceof org.bukkit.entity.Blaze) {
                double fireballDamage = config.getDouble("blaze.fireball_damage", 6.0);
                event.setDamage(fireballDamage);
            }
        }

        // Настройка урона от черепов Иссушителя
        if (event.getDamager() instanceof org.bukkit.entity.WitherSkull) {
            double skullDamage = config.getDouble("wither.skull_damage", 8.0);
            event.setDamage(skullDamage);
        }
    }

    private void handleCreeperSpawn(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof Creeper creeper) {
            // Задаем шанс спавна заряженного крипера
            double chargedChance = config.getDouble("creeper.charged_chance", 0.1); // По умолчанию 10%

            if (random.nextDouble() < chargedChance) {
                creeper.setPowered(true); // Установить крипера как заряженного
                // Устанавливаем радиус взрыва для заряженного крипера
                int chargedRadius = (int) config.getDouble("creeper.charged_explosion_radius", 6.0);
                creeper.setExplosionRadius(chargedRadius);
            } else {
                // Устанавливаем радиус взрыва для обычного крипера
                int normalRadius = (int) config.getDouble("creeper.explosion_radius", 3.0);
                creeper.setExplosionRadius(normalRadius);
            }
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        // Обработка взрыва крипера - радиус уже установлен при спавне
        // Это событие можно использовать для дополнительной логики в будущем
        // Например, логирование или изменение списка блоков для разрушения
        if (event.getEntity() instanceof Creeper) {
            // Радиус взрыва уже был установлен при спавне крипера
        }
    }

    private void applyZombieAttributes(CreatureSpawnEvent event, String path) {
        var entity = event.getEntity();
        double health = config.getDouble(path + ".health", 20.0);
        entity.setMaxHealth(health);
        entity.setHealth(health); // Чтобы сразу отображалось новое здоровье

        if (entity instanceof LivingEntity livingEntity) {
            double damage = config.getDouble(path + ".damage", 2.0);
            livingEntity.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE)
                    .setBaseValue(damage);
        }

        // Устанавливаем способность зомби ломать двери
        if (entity instanceof org.bukkit.entity.Zombie zombie) {
            boolean canBreakDoors = config.getBoolean("zombie.can_break_doors", true);
            zombie.setCanBreakDoors(canBreakDoors);
        }
    }

    private void applyAttributes(CreatureSpawnEvent event, String path) {
        var entity = event.getEntity();
        double health = config.getDouble(path + ".health", 20.0);
        entity.setMaxHealth(health);
        entity.setHealth(health); // Чтобы сразу отображалось новое здоровье

        if (entity instanceof LivingEntity livingEntity) {
            double damage = config.getDouble(path + ".damage", 2.0);
            livingEntity.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE)
                    .setBaseValue(damage);
        }
    }

    private void applyBreezeAttributes(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof Breeze breeze) {
            double breezeHealth = config.getDouble("breeze.health", 30.0);
            double breezeDamage = config.getDouble("breeze.damage", 6.0);

            breeze.setMaxHealth(breezeHealth);
            breeze.setHealth(breezeHealth);

            if (breeze.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE) != null) {
                breeze.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(breezeDamage);
            }
        }
    }

    private void applyBoggedAttributes(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Bogged bogged) {
            double health = config.getDouble("skeleton.bogged.health", 16.0);
            double damage = config.getDouble("skeleton.bogged.damage", 2.0);

            bogged.setMaxHealth(health);
            bogged.setHealth(health);

            if (bogged.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE) != null) {
                bogged.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(damage);
            }
        }
    }

    private void applyWardenAttributes(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Warden warden) {
            double health = config.getDouble("warden.health", 500.0);
            double damage = config.getDouble("warden.damage", 30.0);

            warden.setMaxHealth(health);
            warden.setHealth(health);

            if (warden.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE) != null) {
                warden.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(damage);
            }
        }
    }

    private void applyPillagerAttributes(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Pillager pillager) {
            double health = config.getDouble("pillager.health", 24.0);
            double damage = config.getDouble("pillager.damage", 5.0);

            pillager.setMaxHealth(health);
            pillager.setHealth(health);

            if (pillager.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE) != null) {
                pillager.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(damage);
            }
        }
    }

    private void applyPiglinAttributes(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Piglin piglin) {
            double health = config.getDouble("piglin.health", 16.0);
            double damage = config.getDouble("piglin.damage", 5.0);

            piglin.setMaxHealth(health);
            piglin.setHealth(health);

            if (piglin.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE) != null) {
                piglin.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(damage);
            }
        }
    }

    private void applyGhastAttributes(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Ghast ghast) {
            double health = config.getDouble("ghast.health", 10.0);
            double damage = config.getDouble("ghast.damage", 6.0);

            ghast.setMaxHealth(health);
            ghast.setHealth(health);

            if (ghast.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE) != null) {
                ghast.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(damage);
            }
        }
    }

    private void applyBlazeAttributes(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Blaze blaze) {
            double health = config.getDouble("blaze.health", 20.0);
            double damage = config.getDouble("blaze.damage", 6.0);

            blaze.setMaxHealth(health);
            blaze.setHealth(health);

            if (blaze.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE) != null) {
                blaze.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(damage);
            }
        }
    }

    private void applySlimeAttributes(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Slime slime) {
            int size = slime.getSize();
            String sizeKey;
            if (size == 1) {
                sizeKey = "small";
            } else if (size == 2) {
                sizeKey = "medium";
            } else {
                sizeKey = "large";
            }

            double health = config.getDouble("slime." + sizeKey + ".health",
                    size == 1 ? 1.0 : size == 2 ? 4.0 : 16.0);
            double damage = config.getDouble("slime." + sizeKey + ".damage",
                    size == 1 ? 0.0 : size == 2 ? 2.0 : 4.0);

            slime.setMaxHealth(health);
            slime.setHealth(health);

            if (slime.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE) != null) {
                slime.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(damage);
            }
        }
    }

    private void applyMagmaCubeAttributes(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.MagmaCube magmaCube) {
            int size = magmaCube.getSize();
            String sizeKey;
            if (size == 1) {
                sizeKey = "small";
            } else if (size == 2) {
                sizeKey = "medium";
            } else {
                sizeKey = "large";
            }

            double health = config.getDouble("magma_cube." + sizeKey + ".health",
                    size == 1 ? 1.0 : size == 2 ? 4.0 : 16.0);
            double damage = config.getDouble("magma_cube." + sizeKey + ".damage",
                    size == 1 ? 0.0 : size == 2 ? 2.0 : 4.0);

            magmaCube.setMaxHealth(health);
            magmaCube.setHealth(health);

            if (magmaCube.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE) != null) {
                magmaCube.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(damage);
            }
        }
    }

    private void applyEnderDragonAttributes(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.EnderDragon dragon) {
            double health = config.getDouble("ender_dragon.health", 200.0);
            double damage = config.getDouble("ender_dragon.damage", 10.0);

            dragon.setMaxHealth(health);
            dragon.setHealth(health);

            if (dragon.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE) != null) {
                dragon.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(damage);
            }
        }
    }

    private void applyWitherAttributes(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Wither wither) {
            double health = config.getDouble("wither.health", 300.0);
            double damage = config.getDouble("wither.damage", 12.0);

            wither.setMaxHealth(health);
            wither.setHealth(health);

            if (wither.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE) != null) {
                wither.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(damage);
            }
        }
    }

    private void applyHealthOnly(CreatureSpawnEvent event, String path) {
        var entity = event.getEntity();
        double health = config.getDouble(path + ".health", 20.0);
        entity.setMaxHealth(health);
        entity.setHealth(health);
    }
}
