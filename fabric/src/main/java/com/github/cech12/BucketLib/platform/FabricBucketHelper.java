package com.github.cech12.BucketLib.platform;

import com.github.cech12.BucketLib.mixin.MobBucketItemAccessor;
import com.github.cech12.BucketLib.platform.services.IBucketHelper;
import net.fabricmc.fabric.mixin.transfer.BucketItemAccessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.level.material.Fluid;

/**
 * The bucket helper implementation for Fabric.
 */
public class FabricBucketHelper implements IBucketHelper {

    @Override
    public Fluid getFluidOfBucketItem(BucketItem bucket) {
        return ((BucketItemAccessor) bucket).fabric_getFluid();
    }

    @Override
    public EntityType<?> getEntityTypeOfMobBucketItem(MobBucketItem mobBucketItem) {
        return ((MobBucketItemAccessor) mobBucketItem).bucketlib_getEntityType();
    }
}
