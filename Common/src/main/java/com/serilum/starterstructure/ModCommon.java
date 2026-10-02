package com.serilum.starterstructure;

import com.serilum.starterstructure.config.ConfigHandler;
import com.serilum.starterstructure.util.Util;

public class ModCommon {

	public static void init() {
		ConfigHandler.initConfig();
		load();
	}

	private static void load() {
		Util.initDirs();
	}
}