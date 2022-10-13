package me.quared.region.util;

import me.quared.region.RegionPlugin;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringUtil {

	private static final Pattern HEX_PATTERN = Pattern.compile("&(#\\w{6})");

	private StringUtil() {
		throw new UnsupportedOperationException("This class is not meant to be instantiated.");
	}

	public static List<String> colourize(String... strings) {
		return colourize(Arrays.stream(strings).toList());
	}

	public static List<String> colourize(List<String> strings) {
		List<String> finalList = new ArrayList<>();
		for (String s : strings) finalList.add(colourize(s));
		return finalList;
	}

	public static String colourize(String message) {
		Matcher matcher = HEX_PATTERN.matcher(net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', message));
		StringBuilder buffer = new StringBuilder();

		while (matcher.find()) {
			matcher.appendReplacement(buffer, net.md_5.bungee.api.ChatColor.of(matcher.group(1)).toString());
		}

		return matcher.appendTail(buffer).toString();
	}

	public static String stripColour(String message) {
		return ChatColor.stripColor(colourize(message));
	}

	public static String getHumanReadableNumber(long number) {
		if (number >= 1000000000) {
			return String.format("%.2fB", number / 1000000000.0);
		} else if (number >= 1000000) {
			return String.format("%.2fM", number / 1000000.0);
		} else if (number >= 100000) {
			return String.format("%.2fL", number / 100000.0);
		} else if (number >= 1000) {
			return String.format("%.2fK", number / 1000.0);
		}

		return String.valueOf(number);
	}

	public static List<String> formatStrings(List<String> list, Object... params) {
		return colourize(Arrays.asList(String.format(String.join("\n", list), params).split("\n")));
	}

	public static String formatString(String string, Object... params) {
		return colourize(String.format(string, params));
	}


	public static String getMessage(String path) {
		FileConfiguration lang = RegionPlugin.getPlugin(RegionPlugin.class).getLang();
		return StringUtil.colourize(lang.getString("messages.global-prefix") + RegionPlugin.getPlugin(RegionPlugin.class).getLang().getString("messages." + path));
	}

	public static String getMessage(String path, String... format) {
		FileConfiguration lang = RegionPlugin.getPlugin(RegionPlugin.class).getLang();
		return String.format(StringUtil.colourize(lang.getString("messages.global-prefix") + lang.getString("messages." + path)), format);
	}

}
