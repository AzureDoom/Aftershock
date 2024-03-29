package mod.azure.aftershock.client.render.american;

import com.mojang.blaze3d.vertex.PoseStack;

import mod.azure.aftershock.client.model.american.AmericanGraboidEntityModel;
import mod.azure.aftershock.common.entities.american.AmericanGraboidEntity;
import mod.azure.azurelib.renderer.GeoEntityRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

@Environment(EnvType.CLIENT)
public class AmericanGraboidEntityRenderer extends GeoEntityRenderer<AmericanGraboidEntity> {

	public AmericanGraboidEntityRenderer(EntityRendererProvider.Context context) {
		super(context, new AmericanGraboidEntityModel());
		this.shadowRadius = 0.5f;
	}

	@Override
	protected float getDeathMaxRotation(AmericanGraboidEntity entityLivingBaseIn) {
		return 0;
	}

	@Override
	public void render(AmericanGraboidEntity entity, float entityYaw, float partialTicks, PoseStack stack, MultiBufferSource bufferIn, int packedLightIn) {
		var scaleFactor = 0.3f + ((entity.getGrowth() / 1200) / 2.0f);
		stack.scale(entity.getGrowth() > 1200 ? 1.1F : scaleFactor, entity.getGrowth() > 1200 ? 1.1F : scaleFactor, entity.getGrowth() > 1200 ? 1.1F : scaleFactor);
		if (entity.isInSand() && !entity.isSearching() && entity.getAttckingState() <= 2)
			stack.translate(0, -2.9, 0);
		if (entity.isInSand() && entity.isSearching()) 
			stack.translate(0, -0.1, 0);
		if (entity.isInSand() && entity.getAttckingState() == 5)
			stack.translate(0, -0.5, 0);
		super.render(entity, entityYaw, partialTicks, stack, bufferIn, packedLightIn);
	}
}
