package mod.azure.aftershock.common.blocks;

import mod.azure.aftershock.common.AftershockMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TickingLightEntity extends BlockEntity {
    private int lifespan = 0;

    public TickingLightEntity(BlockPos blockPos, BlockState blockState) {
        super(AftershockMod.ModMobs.TICKING_LIGHT_ENTITY, blockPos, blockState);
    }

    public void refresh(int lifeExtension) {
        lifespan = -lifeExtension;
    }

    private void tick() {
        if (lifespan++ >= 5) {
            if (level.getBlockState(getBlockPos()).getBlock() instanceof TickingLightBlock)
                level.setBlockAndUpdate(getBlockPos(), Blocks.AIR.defaultBlockState());
            else
                setRemoved();
        }
    }

    public static void tick(Level world, BlockPos blockPos, BlockState blockState,
                            TickingLightEntity blockEntity) {
        blockEntity.tick();
    }
}
