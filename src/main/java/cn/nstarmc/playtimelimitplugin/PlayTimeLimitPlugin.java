package cn.nstarmc.playtimelimitplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

public class PlayTimeLimitPlugin extends JavaPlugin {
    private FileConfiguration config;
    private File configFile;
    private File dataFile;
    private FileConfiguration data;

    @Override
    public void onEnable() {
        // 生成默认配置文件
        saveDefaultConfig();
        config = getConfig();
        configFile = new File(getDataFolder(), "config.yml");

        // 生成data.yml文件
        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);

// 注册命令
        getCommand("playtime").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                UUID uuid = player.getUniqueId();
                int playTime = data.getInt(uuid.toString());
                int maxPlayTime = config.getInt("max-play-time");
                int remainingTime = maxPlayTime - playTime;
                int hours = remainingTime / 60;
                int minutes = remainingTime % 60;
                player.sendMessage(ChatColor.GREEN + "你今天还能玩" + ChatColor.YELLOW + hours + ChatColor.GREEN + "小时" + ChatColor.YELLOW + minutes + ChatColor.GREEN + "分钟。");
            }
            return true;
        });


// 注册事件监听器
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPlayerJoin(PlayerJoinEvent event) {
                Player player = event.getPlayer();
                UUID uuid = player.getUniqueId();
                int playTime = data.getInt(uuid.toString());
                int maxPlayTime = config.getInt("max-play-time");
                // 从配置文件中读取重置小时和分钟
                int resetHour = config.getInt("resetHour");
                int resetMinute = config.getInt("resetMinute");
                if (playTime >= maxPlayTime) {
                    player.kickPlayer(ChatColor.RED + "你今天的游戏时间已用完。每日游戏时间限制为" + ChatColor.YELLOW + maxPlayTime + ChatColor.RED + "分钟。\n" + ChatColor.GREEN + "时间重置会在" + resetHour + ":" + String.format("%02d", resetMinute) + "(UTC+8)。");
                } else {
                    int remainingTime = maxPlayTime - playTime;
                    int hours = remainingTime / 60;
                    int minutes = remainingTime % 60;
                    player.sendMessage(ChatColor.GREEN + "欢迎回来！你今天还能玩" + ChatColor.YELLOW + hours + ChatColor.GREEN + "小时" + ChatColor.YELLOW + minutes + ChatColor.GREEN + "分钟。");
                }
            }
        }, this);




// 每分钟检查一次玩家游戏时间
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                int playTime = data.getInt(uuid.toString());
                int maxPlayTime = config.getInt("max-play-time");
                // 从配置文件中读取重置小时和分钟
                int resetHour = config.getInt("resetHour");
                int resetMinute = config.getInt("resetMinute");
                if (playTime >= maxPlayTime) {
                    player.kickPlayer(ChatColor.RED + "你今天的游戏时间已用完。每日游戏时间限制为" + ChatColor.YELLOW + maxPlayTime + ChatColor.RED + "分钟。\n" + ChatColor.GREEN + "时间重置会在" + resetHour + ":" + String.format("%02d", resetMinute) + "(UTC+8)。");
                } else if (maxPlayTime - playTime <= 5) {
                    player.sendMessage(ChatColor.RED + "注意：你今天只剩下" + ChatColor.YELLOW + (maxPlayTime - playTime) + ChatColor.RED + "分钟的游戏时间了。");
                    data.set(uuid.toString(), playTime + 1);
                    saveData();
                } else {
                    data.set(uuid.toString(), playTime + 1);
                    saveData();
                }
            }
        }, 0L, 1200L);

        // 从配置文件中读取重置小时和分钟
        int resetHour = config.getInt("resetHour");
        int resetMinute = config.getInt("resetMinute");

        // 计划每分钟运行一次任务
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC+8"));
            if (now.getHour() == resetHour && now.getMinute() == resetMinute) {
                for (String key : data.getKeys(false)) {
                    data.set(key, 0);
                }
                saveData();
            }
        }, 0L, 1200L);

        // 插件加载完成后，在控制台输出绿色文本：“防沉迷插件成功加载！-by luluxiaoyu”
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "防沉迷插件成功加载！-by luluxiaoyu");
    }


    @Override
    public void onDisable() {
        saveData();
    }

    private void saveData() {
        try {
            data.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
