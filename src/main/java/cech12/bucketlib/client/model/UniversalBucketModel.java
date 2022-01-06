package cech12.bucketlib.client.model;

import cech12.bucketlib.api.BucketLibApi;
import cech12.bucketlib.item.UniversalBucketItem;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Transformation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.CompositeModelState;
import net.minecraftforge.client.model.ForgeModelBakery;
import net.minecraftforge.client.model.IModelConfiguration;
import net.minecraftforge.client.model.IModelLoader;
import net.minecraftforge.client.model.ItemLayerModel;
import net.minecraftforge.client.model.ItemMultiLayerBakedModel;
import net.minecraftforge.client.model.ItemTextureQuadConverter;
import net.minecraftforge.client.model.PerspectiveMapWrapper;
import net.minecraftforge.client.model.geometry.IModelGeometry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * This implementation is based on net.minecraftforge.client.model.DynamicBucketModel.
 * Multiple changes were done to simplify the class
 */
public class UniversalBucketModel implements IModelGeometry<UniversalBucketModel> {

    // minimal Z offset to prevent depth-fighting
    private static final float NORTH_Z_FLUID = 7.498f / 16f;
    private static final float SOUTH_Z_FLUID = 8.502f / 16f;

    @Nonnull
    private final Fluid fluid;
    @Nullable
    private final ResourceLocation otherContent;

    private final boolean isCracked;

    public UniversalBucketModel(@Nonnull Fluid fluid, @Nullable ResourceLocation otherContent, boolean isCracked) {
        this.fluid = fluid;
        this.otherContent = otherContent;
        this.isCracked = isCracked;
    }

    /**
     * Returns a new UniversalBucketModel with the given fluid.
     */
    public UniversalBucketModel withFluid(Fluid newFluid, boolean isCracked) {
        return new UniversalBucketModel(newFluid, null, isCracked);
    }

    /**
     * Returns a new UniversalBucketModel with the given other content.
     */
    public UniversalBucketModel withOtherContent(ResourceLocation otherContent, boolean isCracked) {
        return new UniversalBucketModel(Fluids.EMPTY, otherContent, isCracked);
    }

    @Override
    public BakedModel bake(IModelConfiguration owner, ModelBakery bakery, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelTransform, ItemOverrides overrides, ResourceLocation modelLocation) {
        Material particleLocation = owner.isTexturePresent("particle") ? owner.resolveTexture("particle") : null;

        Material baseLocation = null;
        if (this.otherContent != null) {
            if (this.isCracked && owner.isTexturePresent("crackedLowerBase")) {
                baseLocation = owner.resolveTexture("crackedLowerBase");
            }
            if (baseLocation == null && owner.isTexturePresent("lowerBase")) {
                baseLocation = owner.resolveTexture("lowerBase");
            }
        } else {
            if (this.isCracked && owner.isTexturePresent("crackedBase")) {
                baseLocation = owner.resolveTexture("crackedBase");
            }
            if (baseLocation == null && owner.isTexturePresent("base")) {
                baseLocation = owner.resolveTexture("base");
            }
        }

        Material fluidMaskLocation = null;
        if (this.fluid != Fluids.EMPTY) {
            if (this.isCracked && owner.isTexturePresent("crackedFluidMask")) {
                fluidMaskLocation = owner.resolveTexture("crackedFluidMask");
            }
            if (fluidMaskLocation == null && owner.isTexturePresent("fluidMask")) {
                fluidMaskLocation = owner.resolveTexture("fluidMask");
            }
        }

        //TODO otherContent

        ModelState transformsFromModel = owner.getCombinedTransform();

        TextureAtlasSprite fluidSprite = fluid != Fluids.EMPTY ? spriteGetter.apply(ForgeHooksClient.getBlockMaterial(fluid.getAttributes().getStillTexture())) : null;

        ImmutableMap<ItemTransforms.TransformType, Transformation> transformMap =
                PerspectiveMapWrapper.getTransforms(new CompositeModelState(transformsFromModel, modelTransform));

        TextureAtlasSprite particleSprite = particleLocation != null ? spriteGetter.apply(particleLocation) : null;

        if (particleSprite == null) particleSprite = fluidSprite;

        // if the fluid is lighter than air, will manipulate the initial state to be rotated 180deg to turn it upside down
        //TODO
        /*
        if (fluid != Fluids.EMPTY && fluid.getAttributes().isLighterThanAir())
        {
            modelTransform = new SimpleModelTransform(
                    modelTransform.getRotation().blockCornerToCenter().compose(
                            new Transformation(null, new Quaternion(0, 0, 1, 0), null, null)).blockCenterToCorner());
        }
         */

        Transformation transform = modelTransform.getRotation();

        ItemMultiLayerBakedModel.Builder builder = ItemMultiLayerBakedModel.builder(owner, particleSprite, new ContainedFluidOverrideHandler(overrides, bakery, owner, this), transformMap);

        if (baseLocation != null) {
            // build base (insidest)
            builder.addQuads(ItemLayerModel.getLayerRenderType(false), ItemLayerModel.getQuadsForSprites(ImmutableList.of(baseLocation), transform, spriteGetter));
        }

        if (fluidMaskLocation != null && fluidSprite != null) {
            TextureAtlasSprite templateSprite = spriteGetter.apply(fluidMaskLocation);
            if (templateSprite != null) {
                // build liquid layer (inside)
                int luminosity = this.fluid.getAttributes().getLuminosity();
                int color = this.fluid.getAttributes().getColor();
                builder.addQuads(ItemLayerModel.getLayerRenderType(true), ItemTextureQuadConverter.convertTexture(transform, templateSprite, fluidSprite, NORTH_Z_FLUID, Direction.NORTH, color, 1, luminosity));
                builder.addQuads(ItemLayerModel.getLayerRenderType(true), ItemTextureQuadConverter.convertTexture(transform, templateSprite, fluidSprite, SOUTH_Z_FLUID, Direction.SOUTH, color, 1, luminosity));
            }
        }

        builder.setParticle(particleSprite);

        return builder.build();
    }

    @Override
    public Collection<Material> getTextures(IModelConfiguration owner, Function<ResourceLocation, UnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors)
    {
        Set<Material> textures = Sets.newHashSet();

        if (owner.isTexturePresent("particle")) textures.add(owner.resolveTexture("particle"));
        if (owner.isTexturePresent("base")) textures.add(owner.resolveTexture("base"));
        if (owner.isTexturePresent("lowerBase")) textures.add(owner.resolveTexture("lowerBase"));
        if (owner.isTexturePresent("fluidMask")) textures.add(owner.resolveTexture("fluidMask"));
        if (owner.isTexturePresent("crackedBase")) textures.add(owner.resolveTexture("crackedBase"));
        if (owner.isTexturePresent("crackedLowerBase")) textures.add(owner.resolveTexture("crackedLowerBase"));
        if (owner.isTexturePresent("crackedFluidMask")) textures.add(owner.resolveTexture("crackedFluidMask"));

        return textures;
    }

    public enum Loader implements IModelLoader<UniversalBucketModel>
    {
        INSTANCE;

        @Override
        public void onResourceManagerReload(@Nonnull ResourceManager resourceManager)
        {
            // no need to clear cache since we create a new model instance
        }

        @Override
        @Nonnull
        public UniversalBucketModel read(@Nonnull JsonDeserializationContext deserializationContext, @Nonnull JsonObject modelContents)
        {
            // create new model
            return new UniversalBucketModel(Fluids.EMPTY, null, false);
        }
    }

    private static final class ContainedFluidOverrideHandler extends ItemOverrides
    {
        private static final ResourceLocation REBAKE_LOCATION = new ResourceLocation(BucketLibApi.MOD_ID, "bucket_override");

        private final Map<String, BakedModel> cache = Maps.newHashMap(); // contains all the baked models since they'll never change
        private final ItemOverrides nested;
        private final ModelBakery bakery;
        private final IModelConfiguration owner;
        private final UniversalBucketModel parent;

        private boolean isCracked;

        private ContainedFluidOverrideHandler(ItemOverrides nested, ModelBakery bakery, IModelConfiguration owner, UniversalBucketModel parent)
        {
            this.nested = nested;
            this.bakery = bakery;
            this.owner = owner;
            this.parent = parent;
        }

        @Nullable
        @Override
        public BakedModel resolve(@Nonnull BakedModel originalModel, @Nonnull ItemStack stack, @Nullable ClientLevel world, @Nullable LivingEntity entity, int number)
        {
            BakedModel overriden = nested.resolve(originalModel, stack, world, entity, number);
            if (overriden != originalModel) return overriden;
            if (stack.getItem() instanceof UniversalBucketItem) {
                UniversalBucketItem bucket = (UniversalBucketItem) stack.getItem();
                Fluid fluid = FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY).getFluid();
                String name = fluid.getRegistryName().toString();
                //reset cache if temperature config changed
                boolean isCracked = bucket.isCracked(stack);
                if (this.isCracked != isCracked) {
                    this.isCracked = isCracked;
                    cache.clear();
                }
                if (!cache.containsKey(name)) {
                    UniversalBucketModel unbaked = this.parent.withFluid(fluid, isCracked);
                    BakedModel bakedModel = unbaked.bake(owner, bakery, ForgeModelBakery.defaultTextureGetter(), BlockModelRotation.X0_Y0, this, REBAKE_LOCATION);
                    cache.put(name, bakedModel);
                    return bakedModel;
                }
                return cache.get(name);
            }
            return originalModel;
        }
    }

}