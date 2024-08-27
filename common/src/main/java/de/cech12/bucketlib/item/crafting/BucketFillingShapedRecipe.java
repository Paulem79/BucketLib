package de.cech12.bucketlib.item.crafting;

import com.google.gson.JsonObject;
import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.platform.Services;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.*;

import static de.cech12.bucketlib.item.InventoryUtils.getItemsInInventory;

public class BucketFillingShapedRecipe extends ShapedRecipe {

    private final CraftingBookCategory category;
    private final NonNullList<Ingredient> ingredients;
    private final BucketFillingType fillingType;
    private final Fluid fluid;
    private final Block block;
    private final EntityType<?> entityType;

    public BucketFillingShapedRecipe(ResourceLocation location, String group, CraftingBookCategory category, NonNullList<Ingredient> ingredients, BucketFillingType fillingType, Optional<Fluid> fluid, Optional<Block> block, Optional<EntityType<?>> entityType) {
        super(location, group, category, 3, 3, ingredients,
                getAssembledBucket(fillingType, fluid.orElse(null), block.orElse(null), entityType.orElse(null),
                        ingredients
                                .stream()
                                .map(ingredient -> Arrays.stream(ingredient.getItems())
                                        .toList())
                                .flatMap(List::stream)
                                .toList()
                ));
        this.category = category;
        this.ingredients = ingredients;
        this.fillingType = fillingType;
        this.fluid = fluid.orElse(null);
        this.block = block.orElse(null);
        this.entityType = entityType.orElse(null);
    }

    private static ItemStack getAffectedBucket(List<ItemStack> itemStacks) {
        for (ItemStack stack : itemStacks) {
            if (stack.getItem() instanceof UniversalBucketItem && BucketLibUtil.isEmpty(stack)) {
                ItemStack bucket = stack.copy();
                bucket.setCount(1);
                return bucket;
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack getAssembledBucket(BucketFillingType fillingType, Fluid fluid, Block block, EntityType<?> entityType, List<ItemStack> itemStacks) {
        ItemStack bucket = getAffectedBucket(itemStacks);
        if (bucket.getItem() instanceof UniversalBucketItem universalBucketItem) {
            if (fillingType == BucketFillingType.BLOCK && universalBucketItem.canHoldBlock(block)) {
                return BucketLibUtil.addBlock(bucket, block);
            } else if (fillingType == BucketFillingType.ENTITY && universalBucketItem.canHoldEntity(entityType) && (fluid == null || universalBucketItem.canHoldFluid(fluid))) {
                if (fluid != null) {
                    bucket = BucketLibUtil.addFluid(bucket, fluid);
                }
                return BucketLibUtil.addEntityType(bucket, entityType);
            } else if (fillingType == BucketFillingType.FLUID && universalBucketItem.canHoldFluid(fluid)) {
                return BucketLibUtil.addFluid(bucket, fluid);
            } else if (fillingType == BucketFillingType.MILK && universalBucketItem.canMilkEntities()) {
                return BucketLibUtil.addMilk(bucket);
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    @Override
    public boolean matches(@Nonnull CraftingContainer inv, @Nonnull Level level) {
        ItemStack bucket = getAffectedBucket(getItemsInInventory(inv));
        if (bucket == ItemStack.EMPTY) {
            return false;
        }
        UniversalBucketItem universalBucketItem = ((UniversalBucketItem)bucket.getItem());
        return super.matches(inv, level)
                && (this.fillingType != BucketFillingType.BLOCK || universalBucketItem.canHoldBlock(this.block))
                && (this.fillingType != BucketFillingType.ENTITY || (universalBucketItem.canHoldEntity(this.entityType) && (this.fluid == null || universalBucketItem.canHoldFluid(this.fluid))))
                && (this.fillingType != BucketFillingType.FLUID || universalBucketItem.canHoldFluid(this.fluid))
                && (this.fillingType != BucketFillingType.MILK || universalBucketItem.canMilkEntities());
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    @Override
    @Nonnull
    public ItemStack assemble(@Nonnull CraftingContainer inv, @Nonnull RegistryAccess registryAccess) {
        return getAssembledBucket(this.fillingType, this.fluid, this.block, this.entityType, getItemsInInventory(inv));
    }


    @Override
    @Nonnull
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    public static class Serializer implements RecipeSerializer<BucketFillingShapedRecipe> {

        public static final Serializer INSTANCE = new Serializer();

        public Serializer() {
        }

        @Override
        public BucketFillingShapedRecipe fromJson(ResourceLocation resourceLocation, JsonObject jsonObject) {
            return null;
        }

        @Override
        @Nonnull
        public BucketFillingShapedRecipe fromNetwork(@NotNull ResourceLocation resourceLocation, FriendlyByteBuf buf) {
            int $$2 = buf.readVarInt();
            int $$3 = buf.readVarInt();
            BucketFillingType fillingType = BucketFillingType.valueOf(buf.readUtf());
            String $$4 = buf.readUtf();
            CraftingBookCategory $$5 = buf.readEnum(CraftingBookCategory.class);
            NonNullList<Ingredient> $$6 = NonNullList.withSize($$2 * $$3, Ingredient.EMPTY);
            Optional<Fluid> fluid = Optional.empty();
            Optional<Block> block = Optional.empty();
            Optional<EntityType<?>> entityType = Optional.empty();
            if (fillingType == BucketFillingType.BLOCK) {
                block = Optional.of(Services.REGISTRY.getBlock(buf.readResourceLocation()));
            } else if (fillingType == BucketFillingType.ENTITY) {
                entityType = Optional.of(Services.REGISTRY.getEntityType(buf.readResourceLocation()));
                fluid = Optional.of(Services.REGISTRY.getFluid(buf.readResourceLocation()));
                if (fluid.get() == Fluids.EMPTY) {
                    fluid = Optional.empty();
                }
            } else if (fillingType == BucketFillingType.FLUID) {
                fluid = Optional.of(Services.REGISTRY.getFluid(buf.readResourceLocation()));
            }

            $$6.replaceAll(ignored -> Ingredient.fromNetwork(buf));

            return new BucketFillingShapedRecipe(resourceLocation, $$4, $$5, $$6, fillingType, fluid, block, entityType);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, BucketFillingShapedRecipe recipe) {
            buf.writeUtf(recipe.getGroup());
            buf.writeEnum(recipe.category);
            for (Ingredient $$2 : recipe.ingredients) {
                $$2.toNetwork(buf);
            }
            buf.writeEnum(recipe.fillingType);
            if (recipe.fillingType == BucketFillingType.BLOCK) {
                buf.writeResourceLocation(Objects.requireNonNull(Services.REGISTRY.getBlockLocation(recipe.block)));
            } else if (recipe.fillingType == BucketFillingType.ENTITY) {
                buf.writeResourceLocation(Objects.requireNonNull(Services.REGISTRY.getEntityTypeLocation(recipe.entityType)));
                buf.writeResourceLocation(Objects.requireNonNull(Services.REGISTRY.getFluidLocation((recipe.fluid != null ? recipe.fluid : Fluids.EMPTY))));
            } else if (recipe.fillingType == BucketFillingType.FLUID) {
                buf.writeResourceLocation(Services.REGISTRY.getFluidLocation(recipe.fluid));
            }
        }
    }

}
