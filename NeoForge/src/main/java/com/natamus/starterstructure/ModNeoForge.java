package com.natamus.starterstructure;

import com.natamus.collective.check.RegisterMod;
import com.natamus.starterstructure.neoforge.config.IntegrateNeoForgeConfig;
import com.natamus.starterstructure.neoforge.events.NeoForgeStructureCreationEvents;
import com.natamus.starterstructure.neoforge.events.NeoForgeStructureProtectionEvents;
import com.natamus.starterstructure.neoforge.events.NeoForgeStructureSpawnPointEvents;
import com.natamus.starterstructure.util.Reference;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Reference.MOD_ID)
public class ModNeoForge {
	
	public ModNeoForge() {
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		modEventBus.addListener(this::loadComplete);

		setGlobalConstants();
		ModCommon.init();

		IntegrateNeoForgeConfig.registerScreen(ModLoadingContext.get());

		RegisterMod.register(Reference.NAME, Reference.MOD_ID, Reference.VERSION, Reference.ACCEPTED_VERSIONS);
	}

	private void loadComplete(final FMLLoadCompleteEvent event) {
		NeoForge.EVENT_BUS.register(NeoForgeStructureProtectionEvents.class);
		NeoForge.EVENT_BUS.register(NeoForgeStructureCreationEvents.class);
		NeoForge.EVENT_BUS.register(NeoForgeStructureSpawnPointEvents.class);
	}

	private static void setGlobalConstants() {

	}
}