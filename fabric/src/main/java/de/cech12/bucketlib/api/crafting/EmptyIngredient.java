package de.cech12.bucketlib.api.crafting;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.cech12.bucketlib.BucketLibMod;
import de.cech12.bucketlib.api.BucketLib;
import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class EmptyIngredient implements CustomIngredient {

    protected Item item;
    protected TagKey<Item> tag;
    private List<ItemStack> matchingStacks;

    public EmptyIngredient(Item item, TagKey<Item> tag) {
        this.item = item;
        this.tag = tag;
    }

    public EmptyIngredient(Item item) {
        this(item, null);
    }

    public EmptyIngredient(TagKey<Item> tag) {
        this(null, tag);
    }

    public EmptyIngredient() {
        this((Item) null, null);
    }

    public EmptyIngredient(Optional<ResourceLocation> itemOptional, Optional<TagKey<Item>> tagOptional) {
        this(itemOptional.map(BuiltInRegistries.ITEM::get).orElse(null), tagOptional.orElse(null));
    }

    @Override
    public boolean test(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }
        if (this.item == null && this.tag == null && itemStack.getItem() == Items.BUCKET) {
            return true;
        }
        if (this.item != null && itemStack.getItem() == this.item
                || this.tag != null && itemStack.is(this.tag)
                || this.item == null && this.tag == null && itemStack.getItem() instanceof UniversalBucketItem) {
            return BucketLibUtil.isEmpty(itemStack);
        }
        return false;
    }

    @Override
    public List<ItemStack> getMatchingStacks() {
        if (this.matchingStacks == null) {
            this.matchingStacks = new ArrayList<>();
            if (this.item == null && this.tag == null) {
                this.matchingStacks.add(new ItemStack(Items.BUCKET));
            }
            BucketLibMod.getRegisteredBuckets().forEach(universalBucketItem -> {
                ItemStack universalBucketItemStack = new ItemStack(universalBucketItem);
                if (this.item != null && universalBucketItem == this.item
                        || this.tag != null && universalBucketItemStack.is(this.tag)
                        || this.item == null && this.tag == null) {
                    this.matchingStacks.add(universalBucketItemStack);
                }
            });
        }
        return this.matchingStacks;
    }

    @Override
    public boolean requiresTesting() {
        return true;
    }

    @Override
    public CustomIngredientSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    public static final Codec<EmptyIngredient> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    ResourceLocation.CODEC.optionalFieldOf("item").forGetter(i -> Optional.of(BuiltInRegistries.ITEM.getKey(i.item))),
                    TagKey.codec(BuiltInRegistries.ITEM.key()).optionalFieldOf("tag").forGetter(i -> Optional.ofNullable(i.tag))
            ).apply(builder, EmptyIngredient::new)
    );

    public static final class Serializer implements CustomIngredientSerializer<EmptyIngredient> {
        public static final Serializer INSTANCE = new Serializer();
        public static final ResourceLocation NAME = new ResourceLocation(BucketLib.MOD_ID, "empty");

        private Serializer() {}

        @Override
        public ResourceLocation getIdentifier() {
            return NAME;
        }

        @Override
        public EmptyIngredient read(JsonObject json) {
            String item = json.get("item").getAsString();
            String tagId = json.get("tagId").getAsString();
            if (!tagId.isEmpty()) {
                TagKey<Item> tag = TagKey.create(Registries.ITEM, new ResourceLocation(tagId));
                return new EmptyIngredient(tag);
            }
            if (item.isEmpty()) {
                throw new IllegalArgumentException("Cannot create a item ingredient with no item or tag.");
            }

            return new EmptyIngredient(BuiltInRegistries.ITEM.get(new ResourceLocation(item)));
        }

        @Override
        public void write(JsonObject json, EmptyIngredient ingredient) {
            json.addProperty("item", ingredient.item != null ? Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(ingredient.item)).toString() : "");
            json.addProperty("tagId", ingredient.tag != null ? ingredient.tag.location().toString() : "");
        }

        @Nonnull
        @Override
        public EmptyIngredient read(FriendlyByteBuf buffer) {
            String item = buffer.readUtf();
            String tagId = buffer.readUtf();
            if (!item.isEmpty()) {
                return new EmptyIngredient(BuiltInRegistries.ITEM.get(new ResourceLocation(item)));
            }
            if (!tagId.isEmpty()) {
                TagKey<Item> tag = TagKey.create(Registries.ITEM, new ResourceLocation(tagId));
                return new EmptyIngredient(tag);
            }
            return new EmptyIngredient();
        }

        @Override
        public void write(@Nonnull FriendlyByteBuf buffer, @Nonnull EmptyIngredient ingredient) {
            buffer.writeUtf(ingredient.item != null ? Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(ingredient.item)).toString() : "");
            buffer.writeUtf(ingredient.tag != null ? ingredient.tag.location().toString() : "");
        }

        public Codec<EmptyIngredient> getCodec(boolean allowEmpty) {
            return CODEC;
        }
    }

}
