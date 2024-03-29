package mod.azure.aftershock.client;

import mod.azure.aftershock.client.render.SeismographBlockRenderer;
import mod.azure.aftershock.client.render.ShellRender;
import mod.azure.aftershock.client.render.american.AmericanBlasterEntityRenderer;
import mod.azure.aftershock.client.render.american.AmericanDirtDragonEntityRenderer;
import mod.azure.aftershock.client.render.american.AmericanGraboidEntityRenderer;
import mod.azure.aftershock.client.render.american.AmericanShreikerEntityRenderer;
import mod.azure.aftershock.common.AftershockMod.ModMobs;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public class AftershockClientInit implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModMobs.AMERICAN_BLASTER, AmericanBlasterEntityRenderer::new);
		EntityRendererRegistry.register(ModMobs.AMERICAN_SHREIKER, AmericanShreikerEntityRenderer::new);
		EntityRendererRegistry.register(ModMobs.AMERICAN_GRABOID, AmericanGraboidEntityRenderer::new);
		EntityRendererRegistry.register(ModMobs.AMERICAN_DIRT_DRAGON, AmericanDirtDragonEntityRenderer::new);
//		EntityRendererRegistry.register(ModMobs.TROPICAL_BLASTER, TropicalBlasterEntityRenderer::new);
//		EntityRendererRegistry.register(ModMobs.TROPICAL_SHREIKER, TropicalShreikerEntityRenderer::new);
//		EntityRendererRegistry.register(ModMobs.TROPICAL_GRABOID, TropicalGraboidEntityRenderer::new);
//		EntityRendererRegistry.register(ModMobs.TROPICAL_DIRT_DRAGON, TropicalDirtDragonEntityRenderer::new);
		BlockEntityRenderers.register(ModMobs.SEIMOGRAPH, (Context ctx) -> new SeismographBlockRenderer());
		EntityRendererRegistry.register(ModMobs.SHELL, (ctx) -> new ShellRender(ctx));
	}
}