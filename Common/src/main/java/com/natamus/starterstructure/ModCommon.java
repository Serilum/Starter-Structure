package com.natamus.starterstructure;

import com.natamus.starterstructure.config.ConfigHandler;
import com.natamus.starterstructure.util.Util;

public class ModCommon {

	public static void init() {
		ConfigHandler.initConfig();
		load();
	}

	private static void load() {
		Util.initDirs();
	}
}