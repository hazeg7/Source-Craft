package com.hazeg7.sourcecraft.client;

import com.hazeg7.sourcecraft.client.gui.SourceStudioScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class SourceModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return SourceStudioScreen::new;
	}
}
